package com.cinetheta.extractors.moviebox

import android.net.Uri
import com.google.gson.JsonElement
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.network.detector.ContentType
import com.cinetheta.core.parser.JsonParser
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.domain.models.Subtitle
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.providers.moviebox.MovieBoxConfig
import com.cinetheta.providers.moviebox.MovieBoxCrypto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Collections

/**
 * MovieBox extractor — updated for H5 Web Play API and direct CDN DASH/MP4 streams.
 *
 * Key changes (Oct 2026):
 *  1. Primary extraction uses the Web H5 API (`/wefeed-h5api-bff/subject/play`) across verified web domains
 *     (movie-box.co, movieboxhd.net, etc.) to completely bypass mobile `play-info/v2` notice video clips.
 *  2. Directly extracts official DASH `.mpd` manifests (`sbcdnw.hakunaymatata.com/dash/.../index_web.mpd`)
 *     with `Edge-Cache-Cookie` and `X-MB-Token`.
 *  3. Directly extracts high-speed signed CDN MP4 streams (`bcdnxw.hakunaymatata.com/tran-audio/...mp4`).
 *  4. Strict blacklist filtering: excludes deprecation notice video clips (80da5272..., b164fbfb..., /other/).
 *  5. Stream requests use `Referer: https://movie-box.co` (or the originating web domain) to avoid CDN 429 errors.
 *  6. Secondary fallback to `/resource` endpoint and `resourceDetectors` if Web H5 returns no streams.
 *
 * Reference: mesamirh/MovieBox-Tui (Rust, 2.5k★) and phisher98's MovieBoxProvider.
 */
class MovieBoxExtractor : BaseExtractor() {

    override val hostType = HostType.MOVIEBOX

    private data class DubItem(
        val subjectId: String,
        val lanName: String,
        val lanCode: String
    )

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        val playUrl = source.url
        val injectedLang = source.metadata["language"] ?: ""
        val streams = Collections.synchronizedList(mutableListOf<StreamLink>())
        val nextSources = Collections.synchronizedList(mutableListOf<ProviderSource>())
        val subtitlesList = Collections.synchronizedList(mutableListOf<Subtitle>())

        val parsedUri = try { Uri.parse(playUrl) } catch (_: Exception) { null }
        val baseOrigin = if (parsedUri?.scheme != null && parsedUri.host != null)
            "${parsedUri.scheme}://${parsedUri.host}"
        else
            MovieBoxConfig.DEFAULT_DOMAIN

        val subjectId = parsedUri?.getQueryParameter("subjectId")
            ?: parsedUri?.getQueryParameter("id")
            ?: source.metadata["subjectId"]
            ?: parsedUri?.lastPathSegment?.takeIf { it.all { c -> c.isDigit() } }

        val se = parsedUri?.getQueryParameter("se")?.toIntOrNull()
            ?: parsedUri?.getQueryParameter("season")?.toIntOrNull()
            ?: source.metadata["se"]?.toIntOrNull() ?: 0
        val ep = parsedUri?.getQueryParameter("ep")?.toIntOrNull()
            ?: parsedUri?.getQueryParameter("episode")?.toIntOrNull()
            ?: source.metadata["ep"]?.toIntOrNull() ?: 0

        if (subjectId.isNullOrBlank()) {
            return ExtractionResult(emptyList(), emptyList())
        }

        // Ensure anonymous token
        MovieBoxCrypto.ensureToken(baseOrigin)

        // ── 1. Fetch Subject Details to get detailUrl, detailPath, subtitles & dubs ──
        var detailPath = ""
        var detailUrl = ""
        var resourceDetectors: List<JsonElement>? = null
        var dubsArray: List<JsonElement>? = null

        try {
            val getUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get?subjectId=$subjectId"
            val getHeaders = MovieBoxCrypto.getHeaders(method = "GET", url = getUrl)
            val getRequest = RequestBuilder().url(getUrl).get().headers(getHeaders).build()
            val getResp = HttpClient.execute(getRequest)
            if (getResp is NetworkResult.Success) {
                val getJson = getResp.data.bodyAsString()
                val getRoot = JsonParser.parse(getJson)
                val getData = getRoot?.let { JsonParser.objectOf(it, "data") }
                if (getData != null) {
                    detailUrl = JsonParser.string(getData, "detailUrl") ?: ""
                    detailPath = JsonParser.string(getData, "detailPath") ?: ""
                    resourceDetectors = JsonParser.array(getData, "resourceDetectors")
                    dubsArray = JsonParser.array(getData, "dubs")
                }
            }
        } catch (e: Exception) {
            com.cinetheta.core.logger.Logger.e("MovieBoxGetDetails", e)
        }

        // Derive detailPath & resolvedDomain from detailUrl if empty
        var resolvedDomain: String? = null
        if (detailUrl.isNotBlank()) {
            val parsedDetailUri = try { Uri.parse(detailUrl) } catch (_: Exception) { null }
            if (parsedDetailUri?.scheme != null && parsedDetailUri.host != null) {
                resolvedDomain = "${parsedDetailUri.scheme}://${parsedDetailUri.host}"
            }
            if (detailPath.isBlank() && parsedDetailUri != null) {
                val pathSegment = parsedDetailUri.lastPathSegment
                if (!pathSegment.isNullOrBlank()) {
                    detailPath = pathSegment
                }
            }
        }

        // ── 2. Primary: Extract official DASH manifests & direct CDN MP4 streams via Web H5 API ──
        fetchWebH5Streams(
            subjectId = subjectId,
            se = se,
            ep = ep,
            detailPath = detailPath,
            resolvedDomain = resolvedDomain,
            injectedLang = injectedLang,
            streams = streams,
            subtitles = subtitlesList
        )

        // ── 3. Fallback: If Web H5 returned no streams, query /resource & resourceDetectors ──
        if (streams.isEmpty()) {
            // Fetch /resource endpoint
            fetchResourceEndpoint(
                subjectId = subjectId,
                se = se,
                ep = ep,
                baseOrigin = baseOrigin,
                streams = streams,
                nextSources = nextSources,
                injectedLang = injectedLang,
                subtitles = subtitlesList
            )

            // Parse legacy resourceDetectors if still empty
            if (streams.isEmpty() && resourceDetectors != null && resourceDetectors.isNotEmpty()) {
                val mainLang = injectedLang.ifBlank { "Original Audio" }
                parseStreamList(
                    list = resourceDetectors,
                    globalSignCookie = null,
                    streams = streams,
                    nextSources = nextSources,
                    baseOrigin = baseOrigin,
                    injectedLang = mainLang,
                    subtitles = subtitlesList,
                    isNativeApi = false,
                    targetSe = se,
                    targetEp = ep
                )
            }
        }

        // ── 4. Captions: Fetch external captions if inline ones were missing ──
        if (subtitlesList.isEmpty()) {
            fetchExternalCaptions(
                subjectId = subjectId,
                streamList = null,
                detectors = resourceDetectors,
                se = se,
                ep = ep,
                baseOrigin = baseOrigin,
                out = subtitlesList
            )
        }

        // Update streams with captured subtitles if they weren't attached initially
        val finalStreams = streams.distinctBy { "${it.url}_${it.name}" }.map { stream ->
            if (stream.subtitles.isEmpty() && subtitlesList.isNotEmpty()) {
                stream.copy(subtitles = subtitlesList.toList())
            } else {
                stream
            }
        }

        // ── 5. Dubs: Fetch multi-audio streams if present ──
        if (injectedLang.isBlank() && dubsArray != null && dubsArray.isNotEmpty()) {
            val dubList = mutableListOf<DubItem>()
            for (dubElem in dubsArray) {
                val dubId = JsonParser.string(dubElem, "subjectId") ?: continue
                val lanName = JsonParser.string(dubElem, "lanName") ?: ""
                val lanCode = JsonParser.string(dubElem, "lanCode") ?: ""
                if (dubId != subjectId) {
                    dubList.add(DubItem(dubId, lanName, lanCode))
                }
            }

            if (dubList.isNotEmpty()) {
                try {
                    coroutineScope {
                        dubList.map { dub ->
                            async {
                                fetchDubStreams(
                                    dub = dub,
                                    se = se,
                                    ep = ep,
                                    baseOrigin = baseOrigin,
                                    streams = streams,
                                    nextSources = nextSources,
                                    subtitles = subtitlesList
                                )
                            }
                        }.awaitAll()
                    }
                } catch (e: Exception) {
                    com.cinetheta.core.logger.Logger.e("MovieBoxDubExtraction", e)
                }
            }
        }

        return ExtractionResult(
            streams = finalStreams,
            sources = nextSources.distinctBy { it.url }
        )
    }

    // ── H5 Web Stream Extraction ─────────────────────────────────────────────────────

    /**
     * Primary Extraction via Web H5 BFF Play API.
     * Bypasses the mobile play-info/v2 notice video clips ("update the app") by requesting
     * the web player API across verified web domains (movie-box.co, movieboxhd.net, etc.).
     *
     * Returns:
     *  - Official adaptive DASH .mpd manifests with Edge-Cache-Cookie and X-MB-Token.
     *  - Direct CDN MP4 streams with fresh signed URLs.
     */
    private suspend fun fetchWebH5Streams(
        subjectId: String,
        se: Int,
        ep: Int,
        detailPath: String,
        resolvedDomain: String?,
        injectedLang: String,
        streams: MutableList<StreamLink>,
        subtitles: MutableList<Subtitle>
    ) {
        val candidateDomains = buildList {
            if (!resolvedDomain.isNullOrBlank()) add(resolvedDomain)
            addAll(MovieBoxConfig.WEB_DOMAINS)
        }.distinct()

        for (domain in candidateDomains) {
            try {
                val h5PlayUrl = buildString {
                    append("$domain/wefeed-h5api-bff/subject/play?subjectId=$subjectId")
                    if (se > 0) append("&se=$se")
                    if (ep > 0) append("&ep=$ep")
                    if (detailPath.isNotBlank()) append("&detailPath=${Uri.encode(detailPath)}")
                    append("&streamSignType=1&supportCodecs%5Bhevc%5D=1&supportCodecs%5Bh264%5D=1")
                }

                val refererUrl = "$domain/movies/$detailPath?id=$subjectId&type=/movie/detail&detailSe=&detailEp=&lang=en"

                val h5Headers = mutableMapOf(
                    "User-Agent" to MovieBoxConfig.WEB_USER_AGENT,
                    "Referer" to refererUrl,
                    "Accept" to "application/json",
                    "x-client-info" to "{\"timezone\":\"Asia/Calcutta\"}",
                    "x-request-lang" to "en",
                    "x-vip-restrict" to "0",
                    "x-no-high-risk-restrict" to "0",
                    "x-source" to ""
                )
                MovieBoxCrypto.xUserToken?.let { h5Headers["Authorization"] = "Bearer $it" }

                val request = RequestBuilder()
                    .url(h5PlayUrl)
                    .get()
                    .headers(h5Headers)
                    .build()

                val resp = HttpClient.execute(request)
                if (resp !is NetworkResult.Success) continue

                val json = resp.data.bodyAsString()
                val root = JsonParser.parse(json) ?: continue
                val code = JsonParser.int(root, "code")
                if (code != null && code != 0) continue

                val data = JsonParser.objectOf(root, "data") ?: continue

                // 1. Parse Captions from H5 play response
                parseCaptions(data, subtitles)

                var foundStreams = false

                // 2. DASH Manifests
                val dashList = JsonParser.array(data, "dash")
                if (dashList != null) {
                    for (item in dashList) {
                        val rawUrl = JsonParser.string(item, "url") ?: continue
                        if (isBlacklisted(rawUrl)) continue

                        val signCookie = JsonParser.string(item, "signCookie")
                            ?: JsonParser.string(item, "signCookieRaw") ?: ""
                        val resolutions = JsonParser.string(item, "resolutions") ?: "1080,720,480"
                        val codecName = JsonParser.string(item, "codecName") ?: "hevc"
                        val quality = parseQuality(resolutions, rawUrl)

                        val streamHeaders = mutableMapOf(
                            "User-Agent" to MovieBoxConfig.WEB_USER_AGENT,
                            "Referer" to domain,
                            "Accept" to "*/*"
                        )
                        val cookiesMap = mutableMapOf<String, String>()
                        if (signCookie.isNotBlank()) {
                            streamHeaders["Cookie"] = signCookie
                            streamHeaders["X-MB-Token"] = signCookie
                            signCookie.split(";").forEach { cookiePart ->
                                val parts = cookiePart.trim().split("=", limit = 2)
                                if (parts.size == 2) cookiesMap[parts[0].trim()] = parts[1].trim()
                            }
                        }

                        val cleanLang = injectedLang
                            .replace(" dub", "", ignoreCase = true)
                            .replace(" Audio", "", ignoreCase = true)
                            .trim()

                        val streamName = buildString {
                            append("MovieBox • DASH")
                            if (cleanLang.isNotBlank()) append(" • $cleanLang Audio")
                            if (quality != Quality.UNKNOWN) append(" • ${quality.label}")
                            if (codecName.isNotBlank()) append(" ($codecName)")
                        }

                        streams.add(
                            StreamLink(
                                name = streamName,
                                url = rawUrl,
                                quality = quality,
                                host = HostType.MOVIEBOX,
                                referer = domain,
                                cookies = cookiesMap,
                                headers = streamHeaders,
                                contentType = ContentType.DASH,
                                adaptive = true,
                                subtitles = subtitles
                            )
                        )
                        foundStreams = true
                    }
                }

                // 3. Direct CDN MP4 Streams
                val streamList = JsonParser.array(data, "streams")
                if (streamList != null) {
                    for (item in streamList) {
                        val rawUrl = JsonParser.string(item, "url") ?: continue
                        if (isBlacklisted(rawUrl)) continue

                        val signCookie = JsonParser.string(item, "signCookie")
                            ?: JsonParser.string(item, "signCookieRaw") ?: ""
                        val resolutions = JsonParser.string(item, "resolutions")
                            ?: JsonParser.string(item, "resolution") ?: ""
                        val codecName = JsonParser.string(item, "codecName") ?: "h264"
                        val quality = parseQuality(resolutions, rawUrl)

                        val streamHeaders = mutableMapOf(
                            "User-Agent" to MovieBoxConfig.WEB_USER_AGENT,
                            "Referer" to domain,
                            "Accept" to "*/*"
                        )
                        val cookiesMap = mutableMapOf<String, String>()
                        if (signCookie.isNotBlank()) {
                            streamHeaders["Cookie"] = signCookie
                            streamHeaders["X-MB-Token"] = signCookie
                            signCookie.split(";").forEach { cookiePart ->
                                val parts = cookiePart.trim().split("=", limit = 2)
                                if (parts.size == 2) cookiesMap[parts[0].trim()] = parts[1].trim()
                            }
                        }

                        val cleanLang = injectedLang
                            .replace(" dub", "", ignoreCase = true)
                            .replace(" Audio", "", ignoreCase = true)
                            .trim()

                        val streamName = buildString {
                            append("MovieBox • DIRECT")
                            if (cleanLang.isNotBlank()) append(" • $cleanLang Audio")
                            if (quality != Quality.UNKNOWN) append(" • ${quality.label}")
                            if (codecName.isNotBlank()) append(" ($codecName)")
                        }

                        streams.add(
                            StreamLink(
                                name = streamName,
                                url = rawUrl,
                                quality = quality,
                                host = HostType.MOVIEBOX,
                                referer = domain,
                                cookies = cookiesMap,
                                headers = streamHeaders,
                                contentType = ContentType.VIDEO,
                                adaptive = false,
                                subtitles = subtitles
                            )
                        )
                        foundStreams = true
                    }
                }

                if (foundStreams) {
                    break // Stop iterating candidate domains once streams are discovered
                }
            } catch (e: Exception) {
                com.cinetheta.core.logger.Logger.e("MovieBoxH5Play", e)
            }
        }
    }

    private fun parseCaptions(data: JsonElement?, out: MutableList<Subtitle>) {
        if (data == null) return
        for (key in listOf("captions", "subCaptions", "extCaptions")) {
            val list = JsonParser.array(data, key) ?: continue
            for (cap in list) {
                val subUrl = JsonParser.string(cap, "url") ?: continue
                if (subUrl.isBlank() || isBlacklisted(subUrl) || subUrl.contains("aa348f2541d13ffe")) continue
                val lang = JsonParser.string(cap, "lan") ?: JsonParser.string(cap, "language") ?: "en"
                val label = JsonParser.string(cap, "lanName") ?: JsonParser.string(cap, "name") ?: lang.uppercase()
                if (out.none { it.url == subUrl }) {
                    out.add(Subtitle(language = lang, label = label, url = subUrl))
                }
            }
        }
    }

    private fun parseQuality(qualityStr: String, url: String = ""): Quality {
        val combined = "$qualityStr $url".lowercase()
        return when {
            combined.contains("2160") || combined.contains("4k") -> Quality.P2160
            combined.contains("1440") -> Quality.P1440
            combined.contains("1080") -> Quality.P1080
            combined.contains("720")  -> Quality.P720
            combined.contains("480")  -> Quality.P480
            combined.contains("360")  -> Quality.P360
            else                      -> Quality.UNKNOWN
        }
    }

    // ── Fallback Helpers ─────────────────────────────────────────────────────────────

    /** Build the play-info/v2 URL from the source URL, preserving se/ep if present */
    fun buildPlayInfoV2Url(
        baseOrigin: String,
        subjectId: String?,
        se: Int,
        ep: Int,
        originalUrl: String
    ): String {
        if (subjectId != null) {
            return buildString {
                append("$baseOrigin/wefeed-mobile-bff/subject-api/play-info/v2?subjectId=$subjectId")
                if (se > 0) append("&se=$se")
                if (ep > 0) append("&ep=$ep")
            }
        }
        return originalUrl
            .replace("/play-info?", "/play-info/v2?")
            .replace("/play-info/v2/v2?", "/play-info/v2?")
    }

    /**
     * Fetch the `/resource` CDN endpoint.
     * Returns `list[]` of resource items — each with `resourceLink` / `url` direct CDN links.
     * Reference: MovieBox-Tui `get_resources` and `moviebox_resource_json_to_releases`.
     */
    private suspend fun fetchResourceEndpoint(
        subjectId: String,
        se: Int,
        ep: Int,
        baseOrigin: String,
        streams: MutableList<StreamLink>,
        nextSources: MutableList<ProviderSource>,
        injectedLang: String,
        subtitles: List<Subtitle>
    ) {
        try {
            val resourceUrl = buildString {
                append("$baseOrigin/wefeed-mobile-bff/subject-api/resource?subjectId=$subjectId&page=1&perPage=20")
                if (se > 0 && ep > 0) append("&se=$se&ep=$ep")
            }

            val headers = MovieBoxCrypto.getHeaders(method = "GET", url = resourceUrl, body = null)
            val request = RequestBuilder().url(resourceUrl).get().headers(headers).build()

            when (val resp = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val json = resp.data.bodyAsString()
                    val root = JsonParser.parse(json) ?: return
                    val data = JsonParser.objectOf(root, "data") ?: root.let { JsonParser.parse(resp.data.bodyAsString()) } ?: return

                    val signCookie = JsonParser.string(data, "signCookie")
                        ?: JsonParser.string(data, "signCookieRaw")

                    val list = JsonParser.array(data, "list")
                    val lang = injectedLang.ifBlank { "Original Audio" }
                    parseStreamList(list, signCookie, streams, nextSources, baseOrigin, lang, subtitles, isNativeApi = false, targetSe = se, targetEp = ep)
                }
                else -> {}
            }
        } catch (e: Exception) {
            com.cinetheta.core.logger.Logger.e("MovieBoxResourceEndpoint", e)
        }
    }

    /**
     * Fetch dubs via subject-api/get and expand resourceDetectors for each dub subjectId.
     */
    private suspend fun fetchDubsViaGet(
        subjectId: String,
        se: Int,
        ep: Int,
        baseOrigin: String,
        injectedLang: String,
        streams: MutableList<StreamLink>,
        nextSources: MutableList<ProviderSource>,
        subtitles: List<Subtitle>
    ) {
        try {
            val getUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get?subjectId=$subjectId"
            val getHeaders = MovieBoxCrypto.getHeaders(method = "GET", url = getUrl, body = null)
            val getRequest = RequestBuilder().url(getUrl).get().headers(getHeaders).build()

            when (val getResponse = HttpClient.execute(getRequest)) {
                is NetworkResult.Success -> {
                    val getJson = getResponse.data.bodyAsString()
                    val getRoot = JsonParser.parse(getJson)
                    if (getRoot != null) {
                        val getData = JsonParser.objectOf(getRoot, "data")
                        if (getData != null) {
                            val globalSignCookie = JsonParser.string(getData, "signCookie")
                                ?: JsonParser.string(getData, "signCookieRaw")

                            val resourceDetectors = JsonParser.array(getData, "resourceDetectors")
                            val mainLang = injectedLang.ifBlank { "Original Audio" }
                            parseStreamList(resourceDetectors, globalSignCookie, streams, nextSources, baseOrigin, mainLang, subtitles, isNativeApi = false, targetSe = se, targetEp = ep)

                            if (injectedLang.isBlank()) {
                                val dubsArray = JsonParser.array(getData, "dubs")
                                val dubList = mutableListOf<DubItem>()
                                if (dubsArray != null) {
                                    for (dubElem in dubsArray) {
                                        val dubId = JsonParser.string(dubElem, "subjectId") ?: continue
                                        val lanName = JsonParser.string(dubElem, "lanName") ?: ""
                                        val lanCode = JsonParser.string(dubElem, "lanCode") ?: ""
                                        if (dubId != subjectId) {
                                            dubList.add(DubItem(dubId, lanName, lanCode))
                                        }
                                    }
                                }

                                if (dubList.isNotEmpty()) {
                                    coroutineScope {
                                        dubList.map { dub ->
                                            async {
                                                fetchDubStreams(
                                                    dub = dub,
                                                    se = se,
                                                    ep = ep,
                                                    baseOrigin = baseOrigin,
                                                    streams = streams,
                                                    nextSources = nextSources,
                                                    subtitles = subtitles
                                                )
                                            }
                                        }.awaitAll()
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            com.cinetheta.core.logger.Logger.e("MovieBoxDubExtraction", e)
        }
    }

    private suspend fun fetchDubStreams(
        dub: DubItem,
        se: Int,
        ep: Int,
        baseOrigin: String,
        streams: MutableList<StreamLink>,
        nextSources: MutableList<ProviderSource>,
        subtitles: List<Subtitle>
    ) {
        try {
            val dubUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get?subjectId=${dub.subjectId}"
            val dubHeaders = MovieBoxCrypto.getHeaders(method = "GET", url = dubUrl, body = null)
            val req = RequestBuilder().url(dubUrl).get().headers(dubHeaders).build()
            val resp = HttpClient.execute(req)

            if (resp is NetworkResult.Success) {
                val json = resp.data.bodyAsString()
                val root = JsonParser.parse(json) ?: return
                val data = JsonParser.objectOf(root, "data") ?: return

                val signCookie = JsonParser.string(data, "signCookie")
                    ?: JsonParser.string(data, "signCookieRaw")

                val detectors = JsonParser.array(data, "resourceDetectors")
                parseStreamList(
                    list = detectors,
                    globalSignCookie = signCookie,
                    streams = streams,
                    nextSources = nextSources,
                    baseOrigin = baseOrigin,
                    injectedLang = dub.lanName,
                    subtitles = subtitles,
                    isNativeApi = false,
                    targetSe = se,
                    targetEp = ep
                )
            }
        } catch (e: Exception) {
            com.cinetheta.core.logger.Logger.e("fetchDubStreams", e)
        }
    }

    private suspend fun fetchExternalCaptions(
        subjectId: String,
        streamList: List<JsonElement>?,
        detectors: List<JsonElement>?,
        se: Int,
        ep: Int,
        baseOrigin: String,
        out: MutableList<Subtitle>
    ) {
        var capUrl: String? = null
        if (streamList != null && streamList.isNotEmpty()) {
            val streamId = JsonParser.string(streamList[0], "id")
            if (streamId != null) {
                capUrl = "$baseOrigin/wefeed-mobile-bff/subject-api/get-stream-captions?subjectId=$subjectId&streamId=$streamId" +
                    (if (se > 0) "&se=$se" else "") +
                    (if (ep > 0) "&ep=$ep" else "")
            }
        } else if (detectors != null && detectors.isNotEmpty()) {
            val resourceId = JsonParser.string(detectors[0], "resourceId")
            if (resourceId != null) {
                val epSafe = if (ep > 0) ep.toString() else "0"
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
                                if (subUrl.isBlank() || isBlacklisted(subUrl) || subUrl.contains("aa348f2541d13ffe")) continue
                                val lang = JsonParser.string(cap, "lan") ?: JsonParser.string(cap, "language") ?: "en"
                                val label = JsonParser.string(cap, "lanName") ?: JsonParser.string(cap, "name") ?: lang.uppercase()
                                if (out.none { it.url == subUrl }) {
                                    out.add(Subtitle(language = lang, label = label, url = subUrl))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Stream Parser ─────────────────────────────────────────────────────────────

    private fun parseStreamList(
        list: List<JsonElement>?,
        globalSignCookie: String?,
        streams: MutableList<StreamLink>,
        nextSources: MutableList<ProviderSource>,
        baseOrigin: String,
        injectedLang: String,
        subtitles: List<Subtitle> = emptyList(),
        isNativeApi: Boolean = true,
        targetSe: Int = 0,
        targetEp: Int = 0
    ) {
        if (list == null) return
        for (item in list) {
            val resList = JsonParser.array(item, "resolutionList")
            if (resList != null && resList.isNotEmpty()) {
                parseStreamList(resList, globalSignCookie, streams, nextSources, baseOrigin, injectedLang, subtitles, isNativeApi, targetSe, targetEp)
                continue
            }

            if (targetSe > 0 || targetEp > 0) {
                val itemSe = JsonParser.int(item, "se") ?: 0
                val itemEp = JsonParser.int(item, "ep") ?: JsonParser.int(item, "episode") ?: 0
                if (itemSe > 0 || itemEp > 0) {
                    if (itemSe != targetSe || itemEp != targetEp) continue
                }
            }

            val signCookie = JsonParser.string(item, "signCookie")
                ?: JsonParser.string(item, "signCookieRaw")
                ?: globalSignCookie

            val format = JsonParser.string(item, "format") ?: ""
            val rawUrl = JsonParser.string(item, "url")
            val downloadUrl = JsonParser.string(item, "downloadUrl")
            val resourceLink = JsonParser.string(item, "resourceLink")

            val directCdnCandidate = when {
                downloadUrl != null && isCdnUrl(downloadUrl) -> downloadUrl
                resourceLink != null && isCdnUrl(resourceLink) -> resourceLink
                rawUrl != null && isCdnUrl(rawUrl) -> rawUrl
                else -> null
            }

            val realUrl = extractRealUrlFromSignCookie(signCookie, format)
                ?: directCdnCandidate
                ?: rawUrl
                ?: downloadUrl
                ?: resourceLink
                ?: continue

            val pathLower = realUrl.lowercase()

            if (isBlacklisted(pathLower)) continue

            val isMovieBoxCdn = isCdnUrl(pathLower) || !signCookie.isNullOrBlank()

            if (!isMovieBoxCdn) {
                val extHost = when {
                    pathLower.contains("vidmoly") -> HostType.VIDMOLY
                    pathLower.contains("turbovid") -> HostType.TURBOVID
                    pathLower.contains("xerver") -> HostType.XERVER
                    pathLower.contains("streamruby") -> HostType.STREAMRUBY
                    pathLower.contains("dood") -> HostType.DOOD
                    pathLower.contains("mixdrop") -> HostType.MIXDROP
                    pathLower.contains("streamtape") -> HostType.STREAMTAPE
                    else -> null
                }
                if (extHost != null) {
                    nextSources.add(
                        ProviderSource(
                            url = realUrl,
                            provider = "MovieBox",
                            host = extHost.name,
                            hostType = extHost
                        )
                    )
                }
                continue
            }

            val isWebCdn = pathLower.contains("bcdnxw.") ||
                           pathLower.contains("sbcdnw.") ||
                           pathLower.contains("movie-box.co") ||
                           pathLower.contains("movieboxhd.net")

            val streamHeaders = mutableMapOf<String, String>()
            streamHeaders["User-Agent"] = MovieBoxCrypto.userAgent
            streamHeaders["Accept"] = "*/*"
            if (isWebCdn) {
                streamHeaders["Referer"] = MovieBoxConfig.STREAM_REFERER
            }

            val cookiesMap = mutableMapOf<String, String>()
            if (!signCookie.isNullOrBlank()) {
                streamHeaders["Cookie"] = signCookie
                streamHeaders["X-MB-Token"] = signCookie
                signCookie.split(";").forEach { cookiePart ->
                    val parts = cookiePart.trim().split("=", limit = 2)
                    if (parts.size == 2) cookiesMap[parts[0].trim()] = parts[1].trim()
                }
            }

            val isDash = pathLower.contains(".mpd") || pathLower.contains("/dash/") || format.equals("DASH", ignoreCase = true)
            val isHls = pathLower.contains(".m3u8") || pathLower.contains("/hls/") || format.equals("HLS", ignoreCase = true)
            val formatLabel = if (isDash) "DASH" else if (isHls) "HLS" else "DIRECT"

            val qualityStr = JsonParser.string(item, "resolutions")
                ?: JsonParser.string(item, "resolution")
                ?: ""
            val rawLang = JsonParser.string(item, "language") ?: ""
            val language = if (injectedLang.isNotBlank()) injectedLang else rawLang
            val name = JsonParser.string(item, "name") ?: ""
            val codecName = JsonParser.string(item, "codecName") ?: ""

            val quality = parseQuality("$qualityStr $name", realUrl)

            val cleanLang = language
                .replace(" dub", "", ignoreCase = true)
                .replace(" Audio", "", ignoreCase = true)
                .trim()

            val streamName = buildString {
                append("MovieBox • $formatLabel")
                if (cleanLang.isNotBlank()) append(" • $cleanLang Audio")
                if (quality != Quality.UNKNOWN) append(" • ${quality.label}")
                if (codecName.isNotBlank()) append(" ($codecName)")
            }

            streams.add(
                StreamLink(
                    name = streamName,
                    url = realUrl,
                    quality = quality,
                    host = HostType.MOVIEBOX,
                    referer = if (isWebCdn) MovieBoxConfig.STREAM_REFERER else null,
                    cookies = cookiesMap,
                    headers = streamHeaders,
                    contentType = when {
                        isDash -> ContentType.DASH
                        isHls -> ContentType.M3U8
                        else -> ContentType.VIDEO
                    },
                    adaptive = isDash || isHls,
                    subtitles = subtitles
                )
            )
        }
    }

    // ── URL Utilities ─────────────────────────────────────────────────────────────

    /**
     * Known-bad / deprecation-notice URL patterns.
     * Rejects notice clips ("update the app"), promo campaigns, and bad hashes.
     */
    private fun isBlacklisted(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("macdn.aoneroom.com/other/") ||
               lower.contains("80da52722ee7de7a4303290af6783424") ||
               lower.contains("b164fbfb4347792950bdfbfb563d39d9") ||
               lower.contains("1c7de0bd3393702d9191801f15f88f8d") ||
               lower.contains("9a0461bc39da389663bf3dbb17091d3f") ||
               lower.contains("/notice.mp4") ||
               lower.contains("update_app") ||
               lower.contains("campaign")
    }

    private fun isCdnUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (isBlacklisted(lower)) return false
        return lower.contains("hakunaymatata.com") ||
               lower.contains("aoneroom.com") ||
               lower.contains("inmoviebox.com") ||
               lower.contains("movie-box.co") ||
               lower.contains("movieboxhd.net")
    }

    /**
     * Extracts the real CDN stream URL from the signCookie string.
     */
    private fun extractRealUrlFromSignCookie(signCookie: String?, format: String? = null): String? {
        if (signCookie.isNullOrBlank()) return null
        return try {
            val edgeCacheRegex = Regex("Edge-Cache-Cookie=urlprefix=([^:;\\s]+)")
            val match = edgeCacheRegex.find(signCookie)
            if (match != null) {
                val urlPrefixB64 = match.groupValues[1]
                val decoded = base64DecodeToString(urlPrefixB64)?.trimEnd('/')
                if (!decoded.isNullOrBlank()) {
                    val decodedLower = decoded.lowercase()
                    val isHlsDecoded = decodedLower.contains("/hls/")
                    val isDashDecoded = decodedLower.contains("/dash/")

                    if (isHlsDecoded && format.equals("MP4", ignoreCase = true)) return null

                    return when {
                        decoded.endsWith(".mpd") || decoded.endsWith(".m3u8") -> decoded
                        isHlsDecoded -> "$decoded/index.m3u8"
                        isDashDecoded -> "$decoded/index.mpd"
                        else -> null
                    }
                }
            }

            val cfRegex = Regex("CloudFront-Policy=([^;]+)")
            val cfMatch = cfRegex.find(signCookie)
            if (cfMatch != null) {
                val policyRaw = cfMatch.groupValues[1]
                val decodedJson = base64DecodeToString(policyRaw)
                if (!decodedJson.isNullOrBlank()) {
                    val root = JsonParser.parse(decodedJson)
                    val stmtArray = JsonParser.array(root, "Statement")
                    val firstStmt = stmtArray?.firstOrNull()
                    val resource = try {
                        firstStmt?.asJsonObject?.get("Resource")?.asString
                    } catch (_: Exception) { null }
                    if (!resource.isNullOrBlank()) {
                        val trimmed = resource.trimEnd('*', '/')
                        val isHls = format.equals("HLS", ignoreCase = true) || trimmed.contains("/hls/")
                        val isDash = trimmed.contains("/dash/")
                        return when {
                            trimmed.endsWith(".mpd") || trimmed.endsWith(".m3u8") -> trimmed
                            isHls -> "$trimmed/index.m3u8"
                            isDash -> "$trimmed/index.mpd"
                            else -> null
                        }
                    }
                }
            }
            null
        } catch (_: Exception) { null }
    }

    private fun base64DecodeToString(input: String): String? {
        val clean = input.trim()
        val std = clean.replace('_', '/').replace('-', '+').replace('~', '/')
        val pad = (4 - std.length % 4) % 4
        val padded = if (pad in 1..3) std + "=".repeat(pad) else std

        try {
            val bytes = java.util.Base64.getDecoder().decode(padded)
            return String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        try {
            val bytes = java.util.Base64.getUrlDecoder().decode(clean)
            return String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        try {
            val bytes = android.util.Base64.decode(padded, android.util.Base64.DEFAULT or android.util.Base64.URL_SAFE)
            return String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        return null
    }
}
