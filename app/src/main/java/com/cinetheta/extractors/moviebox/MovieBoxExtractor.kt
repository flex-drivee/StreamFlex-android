package com.cinetheta.extractors.moviebox

import android.net.Uri
import com.google.gson.JsonElement
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.JsonParser
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.providers.moviebox.MovieBoxCrypto

class MovieBoxExtractor : BaseExtractor() {

    override val hostType = HostType.MOVIEBOX

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // url passed from MovieBoxDetails is already fully qualified:
        // "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$id&episode=$ep"
        val playUrl = source.url
        val injectedLang = source.metadata["language"] ?: ""
        val streams = mutableListOf<StreamLink>()
        val nextSources = mutableListOf<ProviderSource>()
        
        // Derive base origin from the actual play URL host (not hardcoded) to match sign-cookie domain
        val parsedUri = try { android.net.Uri.parse(playUrl) } catch (_: Exception) { null }
        val baseOrigin = if (parsedUri?.scheme != null && parsedUri.host != null)
            "${parsedUri.scheme}://${parsedUri.host}"
        else
            "https://api6.aoneroom.com"
        
        // 1. Fetch play-info
        val playHeaders = MovieBoxCrypto.getHeaders(
            method = "GET",
            url = playUrl,
            body = null
        )

        val playRequest = RequestBuilder()
            .url(playUrl)
            .get()
            .headers(playHeaders)
            .build()

        when (val response = HttpClient.execute(playRequest)) {
            is NetworkResult.Success -> {
                val json = response.data.bodyAsString()
                val root = JsonParser.parse(json)
                if (root != null) {
                    val data = JsonParser.objectOf(root, "data")
                    if (data != null) {
                        val globalSignCookie = JsonParser.string(data, "signCookie") ?: JsonParser.string(data, "signCookieRaw")
                        
                        // Parse subtitles/captions
                        val subtitlesList = mutableListOf<com.cinetheta.domain.models.Subtitle>()
                        val captions = JsonParser.array(data, "captions")
                        if (captions != null) {
                            for (cap in captions) {
                                val subUrl = JsonParser.string(cap, "url") ?: continue
                                val lang = JsonParser.string(cap, "lan") ?: JsonParser.string(cap, "language") ?: "en"
                                val label = JsonParser.string(cap, "lanName") ?: JsonParser.string(cap, "name") ?: lang.uppercase()
                                subtitlesList.add(com.cinetheta.domain.models.Subtitle(language = lang, label = label, url = subUrl))
                            }
                        }
                        // Parse streams and detectors
                        val list = JsonParser.array(data, "streams")
                        val detectors = JsonParser.array(data, "detectors")
                        
                        // NEW: MovieBox API changed where subtitles are. Fetch via separate APIs if needed.
                        if (subtitlesList.isEmpty()) {
                            val uri = android.net.Uri.parse(playUrl)
                            val subjectId = uri.getQueryParameter("subjectId")
                            val se = uri.getQueryParameter("se")
                            val ep = uri.getQueryParameter("ep")
                            
                            var capUrl: String? = null
                            if (subjectId != null) {
                                if (list != null && list.size > 0) {
                                    val streamId = JsonParser.string(list.get(0), "id")
                                    if (streamId != null) {
                                        capUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get-stream-captions?subjectId=$subjectId&streamId=$streamId" +
                                            (if (se != null) "&se=$se" else "") +
                                            (if (ep != null) "&ep=$ep" else "")
                                    }
                                } else if (detectors != null && detectors.size > 0) {
                                    val resourceId = JsonParser.string(detectors.get(0), "resourceId")
                                    if (resourceId != null) {
                                        val epSafe = ep ?: "0"
                                        capUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get-ext-captions?subjectId=$subjectId&resourceId=$resourceId&episode=$epSafe"
                                    }
                                }
                                
                                if (capUrl != null) {
                                    val capHeaders = MovieBoxCrypto.getHeaders(method = "GET", url = capUrl)
                                    val capRequest = RequestBuilder().url(capUrl).get().headers(capHeaders).build()
                                    val capResp = HttpClient.execute(capRequest)
                                    if (capResp is NetworkResult.Success) {
                                        val capJson = capResp.data.bodyAsString()
                                        val capRoot = JsonParser.parse(capJson)
                                        val capData = capRoot?.let { JsonParser.objectOf(it, "data") }
                                        if (capData != null) {
                                            for (key in listOf("captions", "subCaptions", "extCaptions")) {
                                                val caps = JsonParser.array(capData, key)
                                                if (caps != null) {
                                                    for (cap in caps) {
                                                        val subUrl = JsonParser.string(cap, "url") ?: continue
                                                        val lang = JsonParser.string(cap, "lan") ?: JsonParser.string(cap, "language") ?: "en"
                                                        val label = JsonParser.string(cap, "lanName") ?: JsonParser.string(cap, "name") ?: lang.uppercase()
                                                        subtitlesList.add(com.cinetheta.domain.models.Subtitle(language = lang, label = label, url = subUrl))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        parseStreamList(list, globalSignCookie, streams, nextSources, baseOrigin, injectedLang, subtitlesList)
                        parseStreamList(detectors, globalSignCookie, streams, nextSources, baseOrigin, injectedLang, subtitlesList)
                    }
                }
            }
            else -> {}
        }
        
        // 2. Fetch fallback get endpoint for resourceDetectors ONLY if we found no streams
        if (streams.isEmpty() && nextSources.isEmpty()) {
            try {
                val uri = parsedUri ?: Uri.parse(playUrl)
                val subjectId = uri.getQueryParameter("subjectId")
                if (!subjectId.isNullOrBlank()) {
                    // Determine base URL from playUrl (reuse parsedUri if available)
                    val baseUrl = baseOrigin
                    val getUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=$subjectId"
                    
                    val getHeaders = MovieBoxCrypto.getHeaders(
                        method = "GET",
                        url = getUrl,
                        body = null
                    )

                    val getRequest = RequestBuilder()
                        .url(getUrl)
                        .get()
                        .headers(getHeaders)
                        .build()
                        
                    when (val getResponse = HttpClient.execute(getRequest)) {
                        is NetworkResult.Success -> {
                            val getJson = getResponse.data.bodyAsString()
                            val getRoot = JsonParser.parse(getJson)
                            if (getRoot != null) {
                                val getData = JsonParser.objectOf(getRoot, "data")
                                if (getData != null) {
                                    val globalSignCookie = JsonParser.string(getData, "signCookie") ?: JsonParser.string(getData, "signCookieRaw")
                                    
                                    val resourceDetectors = JsonParser.array(getData, "resourceDetectors")
                                    parseStreamList(resourceDetectors, globalSignCookie, streams, nextSources, baseOrigin, injectedLang)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ExtractionResult(
            streams = streams.distinctBy { it.url },
            sources = nextSources.distinctBy { it.url }
        )
    }
    
    private fun parseStreamList(list: List<JsonElement>?, globalSignCookie: String?, streams: MutableList<StreamLink>, nextSources: MutableList<ProviderSource>, baseOrigin: String, injectedLang: String, subtitles: List<com.cinetheta.domain.models.Subtitle> = emptyList()) {
        if (list == null) return
        for (item in list) {
            // Per-stream signCookie takes precedence over global one
            val signCookie = JsonParser.string(item, "signCookie")
                ?: JsonParser.string(item, "signCookieRaw")
                ?: globalSignCookie

            // *** KEY FIX ***
            // The "url" field from the API is a PROMO/GATE video (0.9MB, 21 seconds).
            // The REAL stream URL is encoded in the signCookie:
            //   Edge-Cache-Cookie=urlprefix=<base64_encoded_cdn_url>:sign=...:t=...
            // We MUST decode the urlprefix to get the actual DASH/HLS CDN URL.
            val realUrl = extractRealUrlFromSignCookie(signCookie)
                ?: JsonParser.string(item, "url")
                ?: JsonParser.string(item, "resourceLink")
                ?: continue

            val qualityStr = JsonParser.string(item, "resolutions") ?: ""
            val rawLang = JsonParser.string(item, "language") ?: ""
            val language = if (injectedLang.isNotBlank()) injectedLang else rawLang
            val name = JsonParser.string(item, "name") ?: ""
            val codecName = JsonParser.string(item, "codecName") ?: ""

            val pathLower = realUrl.lowercase()
            
            // IGNORE 21-second promo videos from macdn (usually a fallback when signCookie extraction fails or isn't provided)
            if (pathLower.contains("macdn.aoneroom.com") || pathLower.contains("b164fbfb4347792950bdfbfb563d39d9.mp4")) {
                continue
            }

            // IGNORE dead HLS streams (MovieBox returns 404 for these CDN paths)
            if (pathLower.contains("/hls/")) {
                continue
            }

            val isDirectVideo = pathLower.contains(".m3u8")
                || pathLower.contains(".mpd")
                || pathLower.contains(".mp4")
                || pathLower.contains(".mkv")
                || pathLower.contains("sacdn.hakunaymatata.com")
                || pathLower.contains("sbcdn")
                || pathLower.contains("hakunaymatata.com")

            if (!isDirectVideo) {
                val extHost = when {
                    pathLower.contains("vidmoly")    -> HostType.VIDMOLY
                    pathLower.contains("turbovid")   -> HostType.TURBOVID
                    pathLower.contains("streamable") -> HostType.UNKNOWN
                    pathLower.contains("xerver")     -> HostType.XERVER
                    pathLower.contains("streamruby") -> HostType.STREAMRUBY
                    pathLower.contains("dood")       -> HostType.DOOD
                    pathLower.contains("mixdrop")    -> HostType.MIXDROP
                    pathLower.contains("streamtape") -> HostType.STREAMTAPE
                    else                             -> HostType.UNKNOWN
                }
                nextSources.add(
                    ProviderSource(
                        url = realUrl,
                        provider = "MovieBox",
                        host = extHost.name,
                        hostType = extHost
                    )
                )
                continue
            }

            val streamHeaders = mutableMapOf<String, String>()
            streamHeaders["Referer"] = "$baseOrigin/"
            streamHeaders["Origin"] = baseOrigin
            streamHeaders["User-Agent"] = com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT

            val cookiesMap = mutableMapOf<String, String>()
            if (!signCookie.isNullOrBlank()) {
                streamHeaders["Cookie"] = signCookie
                signCookie.split(";").forEach { cookiePart ->
                    val parts = cookiePart.trim().split("=", limit = 2)
                    if (parts.size == 2) {
                        cookiesMap[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            // MovieBox returns resolutions like "1080,720,480". We split them so the UI shows each link individually
            val resolutions = if (qualityStr.isNotBlank()) qualityStr.split(",") else listOf("")
            
            for (res in resolutions) {
                val streamName = buildString {
                    if (name.isNotBlank()) {
                        append(name)
                        if (language.isNotBlank()) append(" [$language]")
                    } else {
                        append("MovieBox")
                        if (res.isNotBlank()) append(" ${res.trim()}p")
                        if (codecName.isNotBlank()) append(" ($codecName)")
                        if (language.isNotBlank()) append(" [$language]")
                    }
                }

                val quality = when {
                    res.contains("1080") -> Quality.P1080
                    res.contains("720")  -> Quality.P720
                    res.contains("480")  -> Quality.P480
                    res.contains("360")  -> Quality.P360
                    name.contains("1080") -> Quality.P1080
                    name.contains("720")  -> Quality.P720
                    else                  -> Quality.UNKNOWN
                }

                streams.add(
                    StreamLink(
                        name = streamName,
                        url = realUrl,
                        quality = quality,
                        host = HostType.MOVIEBOX,
                        referer = "$baseOrigin/",
                        cookies = cookiesMap,
                        headers = streamHeaders,
                        contentType = when {
                            pathLower.contains(".m3u8") -> com.cinetheta.core.network.detector.ContentType.M3U8
                            pathLower.contains(".mpd")  -> com.cinetheta.core.network.detector.ContentType.DASH
                            pathLower.contains("/dash/")-> com.cinetheta.core.network.detector.ContentType.DASH
                            else                        -> com.cinetheta.core.network.detector.ContentType.VIDEO
                        },
                        adaptive = pathLower.contains(".m3u8")
                            || pathLower.contains(".mpd")
                            || pathLower.contains("/dash/"),
                        subtitles = subtitles
                    )
                )
            }
        }
    }

    /**
     * Extracts the real CDN stream URL from the signCookie string.
     *
     * The API returns signCookie in the form:
     *   Edge-Cache-Cookie=urlprefix=<base64url>:sign=<md5>:t=<epoch>
     * where <base64url> decodes to the base CDN path (e.g.
     *   https://sbcdn5.hakunaymatata.com/dash/4191963760367656968_0_0_720_h265/)
     * The final DASH manifest is that path + "index.mpd".
     *
     * This is the same logic as phisher's `extractPolicyResource()` in MovieBoxProvider.kt.
     */
    private fun extractRealUrlFromSignCookie(signCookie: String?): String? {
        if (signCookie.isNullOrBlank()) return null
        return try {
            // Match Edge-Cache-Cookie urlprefix
            val edgeCacheRegex = Regex("Edge-Cache-Cookie=urlprefix=([^:;\\s]+)")
            val match = edgeCacheRegex.find(signCookie)
            if (match != null) {
                val urlPrefixB64 = match.groupValues[1]
                // Convert URL-safe base64 to standard base64
                val std = urlPrefixB64.replace('_', '/').replace('-', '+')
                val pad = (4 - std.length % 4) % 4
                val padded = std + "=".repeat(pad)
                val decoded = android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
                    .toString(Charsets.UTF_8)
                    .trimEnd('/')
                
                return when {
                    decoded.endsWith(".mpd") || decoded.endsWith(".m3u8") -> decoded
                    decoded.contains("/hls/") -> "$decoded/index.m3u8"
                    else -> "$decoded/index.mpd"
                }
            }

            // Fallback: CloudFront-Policy cookie (for older streams)
            val cfRegex = Regex("CloudFront-Policy=([^;]+)")
            val cfMatch = cfRegex.find(signCookie)
            if (cfMatch != null) {
                val policyRaw = cfMatch.groupValues[1]
                val cfB64 = policyRaw.replace('-', '+').replace('~', '/').replace('_', '=')
                val cfRem = cfB64.length % 4
                val paddedCf = if (cfRem > 0) cfB64 + "=".repeat(4 - cfRem) else cfB64
                val decodedJson = android.util.Base64.decode(paddedCf, android.util.Base64.DEFAULT)
                    .toString(Charsets.UTF_8)
                val root = com.cinetheta.core.parser.JsonParser.parse(decodedJson)
                val stmtArray = com.cinetheta.core.parser.JsonParser.array(root, "Statement")
                val firstStmt = stmtArray.firstOrNull()
                val resource = try {
                    firstStmt?.asJsonObject?.get("Resource")?.asString
                } catch (e: Exception) {
                    null
                }
                if (resource != null && resource.isNotBlank()) {
                    var trimmed = resource!!
                    while (trimmed.endsWith("*") || trimmed.endsWith("/")) {
                        trimmed = trimmed.substring(0, trimmed.length - 1)
                    }
                    return when {
                        trimmed.endsWith(".mpd") || trimmed.endsWith(".m3u8") -> trimmed
                        trimmed.contains("/hls/") -> "$trimmed/index.m3u8"
                        else -> "$trimmed/index.mpd"
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
