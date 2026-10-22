package com.cinetheta.providers.piratexplay

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI

class PirateXplayExtractor : BaseExtractor() {

    override val hostType = HostType.PIRATEXPLAY

    companion object {
        private const val TAG = "PirateXplayExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = coroutineScope {
        val pageUrl = source.url
        val baseUrl = source.metadata["baseUrl"] ?: PirateXplayConfig.DEFAULT_DOMAIN

        // 1. Fetch the episode/movie page
        val req = RequestBuilder()
            .url(pageUrl)
            .header("Referer", baseUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@coroutineScope emptyResult()

        val doc = HtmlParser.parse(html)
        val extractedSources = mutableListOf<ProviderSource>()
        val directStreams = mutableListOf<StreamLink>()

        // 2. Collect ALL iframe embed sources from the page (src and data-src)
        val allIframeUrls = doc.select("iframe").mapNotNull {
            val src = it.attr("src").takeIf { s -> s.isNotBlank() }
                ?: it.attr("data-src").takeIf { s -> s.isNotBlank() }
            if (src.isNullOrBlank()) null else src.trim()
        }.distinct()

        val internalPlayerUrls = mutableListOf<String>()

        for (rawUrl in allIframeUrls) {
            val fullUrl = if (rawUrl.startsWith("http")) rawUrl else try {
                URI(baseUrl).resolve(rawUrl).toString()
            } catch (_: Exception) {
                if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else "$baseUrl/$rawUrl"
            }

            if (fullUrl.contains("public/player")) {
                internalPlayerUrls.add(fullUrl)
            } else {
                // If it's a proxy link e.g. /proxy/play.php?url=...
                val unwrappedUrl = if (fullUrl.contains("play.php?url=")) {
                    fullUrl.substringAfter("play.php?url=").substringBefore("&")
                } else {
                    fullUrl
                }

                val detectedType = HostDetector.detect(unwrappedUrl)

                if (HostDetector.isDirect(detectedType)) {
                    directStreams.add(
                        StreamLink(
                            name = "${PirateXplayConfig.PROVIDER_NAME} • Direct",
                            url = unwrappedUrl,
                            quality = source.quality,
                            host = detectedType,
                            referer = baseUrl
                        )
                    )
                } else if (detectedType != HostType.UNKNOWN) {
                    extractedSources.add(
                        ProviderSource(
                            provider = PirateXplayConfig.PROVIDER_NAME,
                            host = "${PirateXplayConfig.PROVIDER_NAME} • ${detectedType.name}",
                            hostType = detectedType,
                            url = unwrappedUrl,
                            referer = baseUrl
                        )
                    )
                }
            }
        }

        // 3. Process any internal player iframes (public/player)
        if (internalPlayerUrls.isNotEmpty()) {
            val internalJobs = internalPlayerUrls.map { playerUrl ->
                async(Dispatchers.IO) {
                    fetchServerOptions(playerUrl, baseUrl)
                }
            }
            val internalSources = internalJobs.awaitAll().flatten()
            extractedSources.addAll(internalSources)
        }

        Logger.i("[$TAG] Discovered ${directStreams.size} direct stream(s) and ${extractedSources.size} embed source(s) for $pageUrl")
        return@coroutineScope result(streams = directStreams, sources = extractedSources)
    }

    private suspend fun fetchServerOptions(iframeUrl: String, baseUrl: String): List<ProviderSource> {
        val absoluteUrl = if (iframeUrl.startsWith("http")) iframeUrl else try {
            URI(baseUrl).resolve(iframeUrl).toString()
        } catch (_: Exception) {
            if (iframeUrl.startsWith("/")) "$baseUrl$iframeUrl" else "$baseUrl/$iframeUrl"
        }

        val req = RequestBuilder()
            .url(absoluteUrl)
            .header("Referer", baseUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return emptyList()

        val doc = HtmlParser.parse(html)
        val serverOptions = doc.select(".server-option[data-link]")
        
        return serverOptions.mapNotNull { option ->
            val dataLink = option.attr("data-link")
            val language = option.attr("data-language").takeIf { it.isNotBlank() } ?: "Multi"
            
            if (dataLink.isNotBlank()) {
                val resolvedHostType = HostDetector.detect(dataLink)
                if (resolvedHostType != HostType.UNKNOWN) {
                    ProviderSource(
                        provider = PirateXplayConfig.PROVIDER_NAME,
                        host = "${PirateXplayConfig.PROVIDER_NAME} • ${resolvedHostType.name} ($language)",
                        hostType = resolvedHostType,
                        url = dataLink,
                        referer = baseUrl,
                        metadata = mapOf("language" to language)
                    )
                } else null
            } else {
                null
            }
        }
    }
}
