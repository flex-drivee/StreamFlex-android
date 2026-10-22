package com.cinetheta.extractors.animedekho

import android.util.Base64
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.providers.animedekho.AnimeDekhoConfig
import com.cinetheta.providers.animedekho.AnimeDekhoMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

class AnimeDekhoExtractor : BaseExtractor() {

    override val hostType = HostType.ANIMEDEKHO

    companion object {
        private const val TAG = "AnimeDekhoExtractor"

        @Volatile
        private var cachedDgtCookie: String? = null
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = coroutineScope {
        var baseUrl = (source.metadata["baseUrl"] ?: AnimeDekhoConfig.DEFAULT_DOMAIN).trimEnd('/')
        if (baseUrl.contains("hindisubanime", ignoreCase = true) || baseUrl.contains("animedekho.app", ignoreCase = true)) {
            baseUrl = AnimeDekhoConfig.DEFAULT_DOMAIN
        }
        val safeSourceUrl = if (source.url.contains("hindisubanime", ignoreCase = true) || source.url.contains("animedekho.app", ignoreCase = true)) {
            source.url.replace("https://hindisubanime.co", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("http://hindisubanime.co", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("https://animedekho.app", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("http://animedekho.app", AnimeDekhoConfig.DEFAULT_DOMAIN)
        } else {
            source.url
        }
        var postId = source.metadata["postId"] ?: ""
        var episodeId = source.metadata["episodeId"]?.toIntOrNull() ?: 0

        val extractedSources = mutableListOf<ProviderSource>()
        val extractedStreams = mutableListOf<StreamLink>()

        // 1. If postId is missing or episodeId is 0, resolve from page HTML
        if (postId.isBlank() || episodeId == 0) {
            val req = RequestBuilder()
                .url(safeSourceUrl)
                .header("Referer", "$baseUrl/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(8_000L)
                .build()
            val html = when (val res = HttpClient.execute(req)) {
                is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
                else -> null
            }
            if (html != null) {
                if (postId.isBlank()) {
                    postId = extractPostIdFromHtml(html)
                }
                if (episodeId == 0) {
                    episodeId = extractEpisodeIdFromHtml(html)
                }
            }
        }

        // 2. Try the animedekho.tv watch REST API
        if (postId.isNotBlank()) {
            val watchResult = extractFromWatchApi(baseUrl, postId, episodeId)
            extractedSources.addAll(watchResult.first)
            extractedStreams.addAll(watchResult.second)

            if (extractedSources.isNotEmpty() || extractedStreams.isNotEmpty()) {
                Logger.d("[$TAG] Watch API resolved ${extractedSources.size} sources and ${extractedStreams.size} streams for postId=$postId ep=$episodeId")
                return@coroutineScope result(streams = extractedStreams, sources = extractedSources)
            }
        }

        // 3. Fallback to legacy ?trdekho= server probing
        val isMovie = source.metadata["isMovie"]?.toBoolean() ?: false
        val trtype = if (isMovie) 1 else 2
        val termId = postId.ifBlank { "0" }

        if (termId != "0") {
            val jobs = (1..AnimeDekhoConfig.MAX_SERVERS).map { serverIdx ->
                async(Dispatchers.IO) {
                    fetchIframeSource(baseUrl, termId, serverIdx, trtype)
                }
            }

            val probedSources = jobs.awaitAll()
                .filterNotNull()
                .map { (iframeSrc, serverIdx) ->
                    val ht = resolveHostType(iframeSrc)
                    Logger.d("[$TAG] Server $serverIdx → $ht : $iframeSrc")
                    AnimeDekhoMapper.toProviderSource(
                        iframeUrl = iframeSrc,
                        hostType  = ht,
                        referer   = baseUrl,
                        metadata  = mapOf("server" to serverIdx.toString())
                    )
                }
            extractedSources.addAll(probedSources)
        }

        return@coroutineScope result(streams = extractedStreams, sources = extractedSources)
    }

    private suspend fun extractFromWatchApi(
        baseUrl: String,
        postId: String,
        episodeId: Int,
        extraCookie: String = ""
    ): Pair<List<ProviderSource>, List<StreamLink>> = coroutineScope {
        val watchUrl = "$baseUrl/wp-json/dgt/v1/watch"
        val bodyJson = "{\"post_id\":${postId.toIntOrNull() ?: 0},\"episode_id\":$episodeId}"

        val activeCookie = when {
            extraCookie.isNotBlank() -> extraCookie
            !cachedDgtCookie.isNullOrBlank() -> cachedDgtCookie.orEmpty()
            else -> ""
        }

        val reqBuilder = RequestBuilder()
            .url(watchUrl)
            .post(bodyJson.toByteArray(Charsets.UTF_8))
            .header("Content-Type", "application/json")
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(10_000L)

        if (activeCookie.isNotBlank()) {
            reqBuilder.header("Cookie", activeCookie)
        }

        val jsonStr = when (val res = HttpClient.execute(reqBuilder.build())) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@coroutineScope emptyList<ProviderSource>() to emptyList<StreamLink>()

        val sourcesList = mutableListOf<ProviderSource>()
        val streamsList = mutableListOf<StreamLink>()

        try {
            val root = JSONObject(jsonStr)

            // Handle AnimeDekho dgt_gate system — when gated=true, fetch the gate URL to obtain
            // the dgt_verified cookie, then retry the Watch API with the cookie.
            val isGated = root.optBoolean("gated", false)
            if (isGated) {
                cachedDgtCookie = null

                val gateUrl = root.optString("gate").takeIf { it.isNotBlank() }
                    ?: root.optString("gate_url").takeIf { it.isNotBlank() }
                Logger.d("[$TAG] Watch API returned gated=true. Gate URL: $gateUrl")

                if (!gateUrl.isNullOrBlank() && extraCookie.isBlank()) {
                    val gateCookie = fetchDgtGateCookie(gateUrl, baseUrl)
                    if (!gateCookie.isNullOrBlank()) {
                        Logger.d("[$TAG] Got dgt cookie: $gateCookie — retrying Watch API")
                        return@coroutineScope extractFromWatchApi(baseUrl, postId, episodeId, gateCookie)
                    } else {
                        Logger.w("[$TAG] Could not get dgt cookie from gate URL")
                    }
                }
                // Gate failed or retry already failed — fall through and return empty
                return@coroutineScope emptyList<ProviderSource>() to emptyList<StreamLink>()
            }

            val sourcesArr = root.optJSONArray("sources") ?: JSONArray()

            val jobs = (0 until sourcesArr.length()).map { idx ->
                val sObj = sourcesArr.optJSONObject(idx) ?: return@map null
                val serverName = sObj.optString("server")
                val qualityStr = sObj.optString("quality").ifBlank { "Multi-Audio" }
                val languageStr = sObj.optString("language").ifBlank { "1080p" }
                val embedUrl = sObj.optString("embed")
                val dlUrl = sObj.optString("download")

                async(Dispatchers.IO) {
                    val resolvedSources = mutableListOf<ProviderSource>()
                    val resolvedStreams = mutableListOf<StreamLink>()

                    // 1. Process Embed URL
                    if (embedUrl.isNotBlank()) {
                        when {
                            embedUrl.contains("player.php?url=", ignoreCase = true) ||
                            embedUrl.contains("/embed/", ignoreCase = true) -> {
                                val iframeSrc = fetchIframeFromPlayerPage(embedUrl, baseUrl)
                                val finalUrl = iframeSrc ?: embedUrl
                                val ht = resolveHostType(finalUrl)
                                resolvedSources.add(
                                    AnimeDekhoMapper.toProviderSource(
                                        iframeUrl = finalUrl,
                                        hostType  = ht,
                                        referer   = baseUrl,
                                        quality   = resolveQuality(languageStr),
                                        metadata  = mapOf(
                                            "server" to "$serverName ($qualityStr)",
                                            "quality" to qualityStr
                                        )
                                    )
                                )
                            }
                            embedUrl.contains("/aaa/myth/play2.php", ignoreCase = true) -> {
                                // Extract direct video streams from fetch.php if present
                                val directStreams = fetchNeoCdnDirectStreams(embedUrl, baseUrl, qualityStr)
                                resolvedStreams.addAll(directStreams)

                                // Also decode base64 id parameter
                                val queryId = embedUrl.substringAfter("id=", "").substringBefore("&")
                                if (queryId.isNotBlank()) {
                                    val decoded = decodeBase64Safe(queryId)
                                    if (decoded.startsWith("http")) {
                                        val ht = resolveHostType(decoded)
                                        resolvedSources.add(
                                            AnimeDekhoMapper.toProviderSource(
                                                iframeUrl = decoded,
                                                hostType  = ht,
                                                referer   = baseUrl,
                                                quality   = resolveQuality(languageStr),
                                                metadata  = mapOf(
                                                    "server" to "$serverName ($qualityStr)",
                                                    "quality" to qualityStr
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                            else -> {
                                val ht = resolveHostType(embedUrl)
                                resolvedSources.add(
                                    AnimeDekhoMapper.toProviderSource(
                                        iframeUrl = embedUrl,
                                        hostType  = ht,
                                        referer   = baseUrl,
                                        quality   = resolveQuality(languageStr),
                                        metadata  = mapOf(
                                            "server" to "$serverName ($qualityStr)",
                                            "quality" to qualityStr
                                        )
                                    )
                                )
                            }
                        }
                    }

                    // 2. Process Download URL (for Hubcloud, Pixel, etc.)
                    if (dlUrl.isNotBlank()) {
                        if (dlUrl.contains("download.php?url=", ignoreCase = true)) {
                            val dlHref = fetchHrefFromDownloadPage(dlUrl, baseUrl)
                            if (dlHref != null) {
                                val ht = resolveHostType(dlHref)
                                resolvedSources.add(
                                    AnimeDekhoMapper.toProviderSource(
                                        iframeUrl = dlHref,
                                        hostType  = ht,
                                        referer   = baseUrl,
                                        quality   = resolveQuality(languageStr),
                                        metadata  = mapOf(
                                            "server" to "$serverName - Fast DL",
                                            "quality" to qualityStr
                                        )
                                    )
                                )
                            }
                        } else if (dlUrl.contains("/aaa/myth/dl2.php", ignoreCase = true)) {
                            val queryId = dlUrl.substringAfter("id=", "").substringBefore("&")
                            if (queryId.isNotBlank()) {
                                val decoded = decodeBase64Safe(queryId)
                                if (decoded.startsWith("http")) {
                                    val ht = resolveHostType(decoded)
                                    resolvedSources.add(
                                        AnimeDekhoMapper.toProviderSource(
                                            iframeUrl = decoded,
                                            hostType  = ht,
                                            referer   = baseUrl,
                                            quality   = resolveQuality(languageStr),
                                            metadata  = mapOf(
                                                "server" to "$serverName - DL",
                                                "quality" to qualityStr
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    resolvedSources to resolvedStreams
                }
            }

            for (res in jobs.filterNotNull().awaitAll()) {
                sourcesList.addAll(res.first)
                streamsList.addAll(res.second)
            }
        } catch (e: Exception) {
            Logger.w("[$TAG] Failed to parse watch json: ${e.message}")
        }

        sourcesList to streamsList
    }

    /**
     * Fetches the AnimeDekho dgt_gate page headlessly to obtain the dgt_verified cookie.
     * The gate page renders an HTML form containing a hidden input `shortlink`
     * with the verification URL (?dgt_verify=1&e=...&i=...&t=...&next=...).
     * Requesting that verification shortlink directly returns HTTP 302 Found with
     * the `Set-Cookie: dgt_verified=<token>; Max-Age=86400; path=/` header.
     * Caches the cookie in memory and returns it.
     */
    private suspend fun fetchDgtGateCookie(gateUrl: String, baseUrl: String): String? {
        return try {
            Logger.d("[$TAG] Fetching dgt gate page: $gateUrl")
            // Step 1: Hit the gate URL to get HTML containing the shortlink
            val gateReq = RequestBuilder()
                .url(gateUrl)
                .header("Referer", "$baseUrl/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(12_000L)
                .build()

            val gateHtml = when (val res = HttpClient.execute(gateReq)) {
                is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
                else -> null
            } ?: return null

            // Step 2: Extract shortlink value from HTML
            val shortlinkMatch = Regex("""name=["']shortlink["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(gateHtml)
                ?: Regex("""value=["']([^"']+)["'][^>]*name=["']shortlink["']""", RegexOption.IGNORE_CASE).find(gateHtml)

            var shortlink = shortlinkMatch?.groupValues?.get(1)
            if (shortlink.isNullOrBlank()) {
                val doc = HtmlParser.parse(gateHtml)
                shortlink = doc.selectFirst("input[name=shortlink]")?.attr("value")
            }

            if (shortlink.isNullOrBlank()) {
                val verifyUrlMatch = Regex("""(https?://[^"'\s]+[?&]dgt_verify=1[^"'\s]*)""").find(gateHtml)
                shortlink = verifyUrlMatch?.groupValues?.get(1)
            }

            if (shortlink.isNullOrBlank()) {
                Logger.w("[$TAG] No shortlink found in gate HTML")
                return null
            }

            // Step 3: Unescape HTML entities
            val targetUrl = shortlink
                .replace("&#038;", "&")
                .replace("&amp;", "&")

            Logger.d("[$TAG] Requesting shortlink to obtain cookie: $targetUrl")

            // Step 4: GET targetUrl with followRedirects(false) so we capture 302 Set-Cookie
            val verifyReq = RequestBuilder()
                .url(targetUrl)
                .header("Referer", gateUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .followRedirects(false)
                .timeout(10_000L)
                .build()

            val cookieFromHeader = when (val res = HttpClient.execute(verifyReq)) {
                is NetworkResult.Success -> {
                    val setCookieHeaders = res.data.headers.entries
                        .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
                        .flatMap { it.value }

                    setCookieHeaders
                        .firstOrNull { it.contains("dgt_verified", ignoreCase = true) }
                        ?.substringBefore(";")
                        ?.trim()
                }
                else -> null
            }

            // Fallback: check OkHttp CookieJar
            val finalCookie = if (!cookieFromHeader.isNullOrBlank()) {
                cookieFromHeader
            } else {
                HttpClient.getCookies(baseUrl)
                    .firstOrNull { it.name.equals("dgt_verified", ignoreCase = true) }
                    ?.let { "${it.name}=${it.value}" }
            }

            if (!finalCookie.isNullOrBlank()) {
                Logger.d("[$TAG] Successfully obtained dgt cookie: $finalCookie")
                cachedDgtCookie = finalCookie
                finalCookie
            } else {
                Logger.w("[$TAG] Failed to extract dgt_verified cookie from shortlink response")
                null
            }
        } catch (e: Exception) {
            Logger.w("[$TAG] fetchDgtGateCookie failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchIframeFromPlayerPage(url: String, baseUrl: String): String? {
        val reqBuilder = RequestBuilder()
            .url(url)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(6_000L)

        cachedDgtCookie?.let {
            reqBuilder.header("Cookie", it)
        }

        val html = when (val res = HttpClient.execute(reqBuilder.build())) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return null

        val doc = HtmlParser.parse(html)
        return doc.selectFirst("iframe[src]")?.attr("src")?.takeIf { it.isNotBlank() }
    }

    private suspend fun fetchHrefFromDownloadPage(url: String, baseUrl: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(6_000L)
            .build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return null

        val doc = HtmlParser.parse(html)
        return doc.selectFirst("a[href*='hubcloud'], a[href*='drive'], a[href*='pixel']")?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("a.btn[href], a[href^='http']")?.attr("href")?.takeIf { it.isNotBlank() }
    }

    private suspend fun fetchNeoCdnDirectStreams(
        play2Url: String,
        baseUrl: String,
        qualityStr: String
    ): List<StreamLink> {
        val req = RequestBuilder()
            .url(play2Url)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(6_000L)
            .build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return emptyList()

        val fetchMatch = Regex("""fetch\(["'](/aaa/myth/fetch\.php\?id=[^"']+)["']\)""").find(html)
            ?: return emptyList()
        val fetchUrl = "$baseUrl${fetchMatch.groupValues[1]}"

        val fetchReq = RequestBuilder()
            .url(fetchUrl)
            .header("Referer", play2Url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(6_000L)
            .build()
        val fetchJson = when (val res = HttpClient.execute(fetchReq)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return emptyList()

        val streams = mutableListOf<StreamLink>()
        try {
            val root = JSONObject(fetchJson)
            val sources = root.optJSONArray("sources") ?: JSONArray()
            for (i in 0 until sources.length()) {
                val s = sources.optJSONObject(i) ?: continue
                val sUrl = s.optString("url")
                val sType = s.optString("type")
                if (sUrl.isNotBlank()) {
                    streams.add(
                        StreamLink(
                            name        = "NeoCDN - $sType ($qualityStr)",
                            url         = sUrl,
                            quality     = resolveQuality(sType),
                            host        = HostType.DIRECT,
                            headers     = mapOf("Referer" to "$baseUrl/"),
                            contentType = com.cinetheta.core.network.detector.ContentType.VIDEO
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return streams
    }

    private fun decodeBase64Safe(encoded: String): String {
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT or Base64.URL_SAFE)
            String(bytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractPostIdFromHtml(html: String): String {
        Regex("""data-series=["'](\d+)["']""").find(html)?.let { return it.groupValues[1] }
        Regex("""data-title=["'](\d+)["']""").find(html)?.let { return it.groupValues[1] }
        Regex("""data-snapshot=["'][^"']*&quot;id&quot;:(\d+)""").find(html)?.let { return it.groupValues[1] }
        Regex("""data-snapshot=["'][^"']*"id":(\d+)""").find(html)?.let { return it.groupValues[1] }
        val shortlinkMatch = Regex("""[?&]p=(\d+)""").find(html)
        if (shortlinkMatch != null) return shortlinkMatch.groupValues[1]

        val postidMatch = Regex("""postid-(\d+)""").find(html)
        if (postidMatch != null) return postidMatch.groupValues[1]

        val commentMatch = Regex("""comment_post_ID['"]\s+value=['"](\d+)['"]""").find(html)
        if (commentMatch != null) return commentMatch.groupValues[1]

        val termMatch = Regex("""term-(\d+)""").find(html)
        if (termMatch != null) return termMatch.groupValues[1]

        return ""
    }

    private fun extractEpisodeIdFromHtml(html: String): Int {
        Regex("""data-term=["'](\d+)["']""").find(html)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        Regex("""data-episode=["'][^"']*&quot;id&quot;:(\d+)""").find(html)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        Regex("""data-episode=["'][^"']*"id":(\d+)""").find(html)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        Regex("""term-(\d+)""").find(html)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        return 0
    }

    private suspend fun fetchIframeSource(
        baseUrl   : String,
        termId    : String,
        serverIdx : Int,
        trtype    : Int
    ): Pair<String, Int>? {
        val url = "$baseUrl/?trdekho=$serverIdx&trid=$termId&trtype=$trtype"
        val req = RequestBuilder()
            .url(url)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val html = res.data.body?.toString(Charsets.UTF_8) ?: return null
                val doc  = HtmlParser.parse(html)
                val src  = doc.selectFirst("iframe[src]")?.attr("src")
                    ?.takeIf { it.isNotBlank() } ?: return null
                src to serverIdx
            }
            else -> null
        }
    }

    private fun resolveHostType(url: String): HostType {
        val lower = url.lowercase()
        return when {
            "abyssplayer.com" in lower || "playhydrax.com" in lower || "short.icu" in lower -> HostType.ABYSS
            "vidmoly" in lower                                       -> HostType.VIDMOLY
            "rubystm.com" in lower || "streamruby" in lower         -> HostType.STREAMRUBY
            "gdmirrorbot" in lower                                   -> HostType.GDMIRRORBOT
            "cloudy.upns" in lower                                   -> HostType.CLOUDY
            "turbovidhls" in lower || "emturbovid" in lower         -> HostType.TURBOVID
            "strmup.to" in lower                                     -> HostType.STREAMUP
            "xerver.xyz" in lower                                    -> HostType.XERVER
            "goblin" in lower                                        -> HostType.REDIRECT
            "pixel" in lower                                         -> HostType.REDIRECT
            "vidcloud.upns" in lower                                 -> HostType.CLOUDY
            "ravok.buzz" in lower || "hubcloud" in lower             -> HostType.HUBCLOUD
            lower.endsWith(".mp4") || lower.endsWith(".mkv")        -> HostType.DIRECT
            ".m3u8" in lower                                         -> HostType.M3U8
            else                                                     -> HostType.UNKNOWN
        }
    }

    private fun resolveQuality(quality: String): Quality {
        val q = quality.lowercase()
        return when {
            "4k" in q || "2160" in q -> Quality.P2160
            "1080" in q              -> Quality.P1080
            "720" in q               -> Quality.P720
            "480" in q               -> Quality.P480
            "360" in q               -> Quality.P360
            else                     -> Quality.UNKNOWN
        }
    }
}
