package com.cinetheta.extractors.toonstream

import com.cinetheta.core.constants.Constants
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.network.detector.HostDetector
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.providers.toonstream.ToonStreamConfig
import com.cinetheta.providers.toonstream.ToonStreamMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URI

class ToonStreamExtractor : BaseExtractor() {

    override val hostType = HostType.TOONSTREAM

    companion object {
        private const val TAG = "ToonStreamExtractor"
        private val DIRECT_STREAM_REGEX = Regex("""["'](https?://[^"'\s<>]+\.(?:m3u8|mp4)(?:[^"'\s<>]*)?)["']""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val pageUrl = source.url
        val baseUrl = try {
            val uri = URI(pageUrl)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            source.metadata["baseUrl"] ?: ToonStreamConfig.DEFAULT_DOMAIN
        }

        Logger.d("[ToonStreamExtractor] Extracting from: $pageUrl", TAG)

        val html = fetchHtml(pageUrl, baseUrl) ?: return@withContext emptyResult()
        val doc = HtmlParser.parse(html, baseUrl)

        // 1. Gather all potential iframe URLs from DooPlay player options & containers
        val rawIframes = mutableListOf<String>()

        // Primary DooPlay selectors: #aa-options > div > iframe, .playex iframe, etc.
        val optionIframes = doc.select("#aa-options > div > iframe, #aa-options iframe, .playex iframe, div.player-container iframe, div.play-box-iframe iframe, div[id^=source-player] iframe")
        for (el in optionIframes) {
            val src = el.attr("data-src").ifBlank { el.attr("src") }.trim()
            if (src.isNotBlank() && src != "#" && !src.startsWith("javascript:")) {
                rawIframes.add(fixUrl(src, pageUrl))
            }
        }

        // DooPlay AJAX options fallback: #playeroptionsul li or li.dooplay_player_option
        val ajaxPlayerOptions = doc.select("#playeroptionsul li, li.dooplay_player_option, .options li")
        for (li in ajaxPlayerOptions) {
            val postId = li.attr("data-post").trim()
            val nume = li.attr("data-nume").trim()
            val type = li.attr("data-type").trim().ifBlank { "tv" }
            if (postId.isNotBlank() && nume.isNotBlank()) {
                val ajaxReq = RequestBuilder()
                    .url("$baseUrl/wp-admin/admin-ajax.php")
                    .post("action=doo_player_ajax&post=$postId&nume=$nume&type=$type".toByteArray(Charsets.UTF_8))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", pageUrl)
                    .build()
                when (val res = HttpClient.execute(ajaxReq)) {
                    is NetworkResult.Success -> {
                        val body = res.data.bodyAsString()
                        val iframeMatch = Regex("""<iframe[^>]+src=["']([^"']+)["']""").find(body)?.groupValues?.get(1)
                            ?: Regex("""["']embed_url["']\s*:\s*["']([^"']+)["']""").find(body)?.groupValues?.get(1)
                        if (!iframeMatch.isNullOrBlank()) {
                            rawIframes.add(fixUrl(iframeMatch.replace("\\/", "/"), pageUrl))
                        }
                    }
                    else -> {}
                }
            }
        }

        // Generic fallback: all iframes on page
        if (rawIframes.isEmpty()) {
            val allIframes = doc.select("iframe[src], iframe[data-src]")
            for (el in allIframes) {
                val src = el.attr("data-src").ifBlank { el.attr("src") }.trim()
                if (src.isNotBlank() && src != "#" && !src.startsWith("javascript:") && !src.contains("recaptcha") && !src.contains("facebook")) {
                    rawIframes.add(fixUrl(src, pageUrl))
                }
            }
        }

        val embedUrls = rawIframes.distinct()
        Logger.d("[ToonStreamExtractor] Found ${embedUrls.size} candidate embed URLs", TAG)

        val extractedStreams = mutableListOf<StreamLink>()
        val extractedSources = mutableListOf<ProviderSource>()

        for ((index, embedUrl) in embedUrls.withIndex()) {
            if (index > 0) delay(100)

            if (embedUrl.startsWith("https://www.youtube.com/", ignoreCase = true)) {
                continue
            }

            // Direct media check (.m3u8 or .mp4)
            if (embedUrl.contains(".m3u8") || embedUrl.contains(".mp4")) {
                extractedStreams.add(
                    StreamLink(
                        name = "ToonStream \u2022 Server ${index + 1}",
                        url = embedUrl,
                        quality = source.quality,
                        host = HostType.DIRECT,
                        referer = pageUrl
                    )
                )
                continue
            }

            // 1. Check if embedUrl is directly recognized by HostDetector
            val directType = HostDetector.detect(embedUrl)
            if (directType != HostType.UNKNOWN && directType != HostType.REDIRECT) {
                Logger.d("[ToonStreamExtractor] Server ${index + 1} directly handled: $embedUrl -> $directType", TAG)
                extractedSources.add(
                    ToonStreamMapper.toProviderSource(
                        iframeUrl = embedUrl,
                        hostType  = directType,
                        referer   = pageUrl,
                        metadata  = mapOf("server" to (index + 1).toString(), "baseUrl" to baseUrl)
                    )
                )
                continue
            }

            // 2. If not handled directly (e.g. internal wrapper /player/?id=... or proxy player),
            // fetch the page and extract the real inner iframe (CloudStream reference behavior)
            val resolvedUrl = resolveNestedIframe(embedUrl, pageUrl)
            Logger.d("[ToonStreamExtractor] Server ${index + 1}: $embedUrl -> resolved: $resolvedUrl", TAG)

            if (resolvedUrl.contains(".m3u8") || resolvedUrl.contains(".mp4")) {
                extractedStreams.add(
                    StreamLink(
                        name = "ToonStream \u2022 Server ${index + 1}",
                        url = resolvedUrl,
                        quality = source.quality,
                        host = HostType.DIRECT,
                        referer = embedUrl
                    )
                )
                continue
            }

            val resolvedType = HostDetector.detect(resolvedUrl)
            val finalType = if (resolvedType == HostType.UNKNOWN) {
                if (resolvedUrl.contains("/e/") || resolvedUrl.contains("/v/") || resolvedUrl.contains("/embed/")) {
                    HostType.HDSTREAM4U
                } else {
                    HostType.REDIRECT
                }
            } else {
                resolvedType
            }

            extractedSources.add(
                ToonStreamMapper.toProviderSource(
                    iframeUrl = resolvedUrl,
                    hostType  = finalType,
                    referer   = embedUrl,
                    metadata  = mapOf("server" to (index + 1).toString(), "baseUrl" to baseUrl)
                )
            )
        }

        Logger.i("[ToonStreamExtractor] Extracted ${extractedStreams.size} direct streams, ${extractedSources.size} next sources", TAG)
        return@withContext result(streams = extractedStreams, sources = extractedSources)
    }

    private suspend fun resolveNestedIframe(url: String, referer: String): String {
        return try {
            val html = fetchHtml(url, referer) ?: return url
            val doc = HtmlParser.parse(html, url)
            val innerIframe = doc.selectFirst("iframe[src], iframe[data-src]")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }?.trim()?.takeIf { it.isNotBlank() }

            if (innerIframe != null) {
                fixUrl(innerIframe, url)
            } else {
                // Check if the HTML directly contains an m3u8 or mp4 stream
                val directMatch = DIRECT_STREAM_REGEX.find(html)?.groupValues?.get(1)
                directMatch ?: url
            }
        } catch (_: Exception) {
            url
        }
    }

    private fun fixUrl(url: String, base: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> {
                try {
                    val uri = URI(base)
                    "${uri.scheme}://${uri.host}$url"
                } catch (_: Exception) {
                    url
                }
            }
            else -> {
                try {
                    val uri = URI(base)
                    "${uri.scheme}://${uri.host}/$url"
                } catch (_: Exception) {
                    url
                }
            }
        }
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.bodyAsString()
            else -> null
        }
    }
}
