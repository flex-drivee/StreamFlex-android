package com.streamflex.extractors.moviebox

import android.net.Uri
import com.google.gson.JsonElement
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.providers.moviebox.MovieBoxCrypto

class MovieBoxExtractor : BaseExtractor() {

    override val hostType = HostType.MOVIEBOX

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // url passed from MovieBoxDetails is already fully qualified:
        // "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$id&episode=$ep"
        val playUrl = source.url
        val streams = mutableListOf<StreamLink>()
        
        val baseOrigin = "https://api6.aoneroom.com" // Changed from api3 to api6 for stability
        
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
                        
                        // Parse streams
                        val list = JsonParser.array(data, "streams")
                        parseStreamList(list, globalSignCookie, streams, baseOrigin)
                        
                        // Parse detectors
                        val detectors = JsonParser.array(data, "detectors")
                        parseStreamList(detectors, globalSignCookie, streams, baseOrigin)
                    }
                }
            }
            else -> {}
        }
        
        // 2. Fetch fallback get endpoint for resourceDetectors
        try {
            val uri = Uri.parse(playUrl)
            val subjectId = uri.getQueryParameter("subjectId")
            if (!subjectId.isNullOrBlank()) {
                // Determine base URL from playUrl
                val baseUrl = "${uri.scheme}://${uri.host}"
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
                                parseStreamList(resourceDetectors, globalSignCookie, streams, baseOrigin)
                            }
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (streams.isNotEmpty()) {
            result(streams.distinctBy { it.url }) // Remove duplicates
        } else {
            emptyResult()
        }
    }
    
    private fun parseStreamList(list: List<JsonElement>?, globalSignCookie: String?, streams: MutableList<StreamLink>, baseOrigin: String) {
        if (list == null) return
        for (item in list) {
            val path = JsonParser.string(item, "url") ?: JsonParser.string(item, "resourceLink") ?: continue
            val qualityStr = JsonParser.string(item, "resolutions") ?: ""
            val language = JsonParser.string(item, "language") ?: ""
            val name = JsonParser.string(item, "name") ?: ""
            
            val quality = when {
                qualityStr.contains("1080") -> Quality.P1080
                qualityStr.contains("720") -> Quality.P720
                qualityStr.contains("480") -> Quality.P480
                qualityStr.contains("360") -> Quality.P360
                name.contains("1080") -> Quality.P1080
                name.contains("720") -> Quality.P720
                else -> Quality.UNKNOWN
            }

            val signCookie = JsonParser.string(item, "signCookie") 
                ?: JsonParser.string(item, "signCookieRaw") 
                ?: globalSignCookie

            val streamHeaders = mutableMapOf<String, String>()
            streamHeaders["Referer"] = "$baseOrigin/"
            streamHeaders["Origin"] = baseOrigin
            streamHeaders["User-Agent"] = com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT
            
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
            
            val streamName = buildString {
                if (name.isNotBlank()) {
                    append(name)
                } else {
                    append("MovieBox")
                    if (qualityStr.isNotBlank()) append(" $qualityStr")
                    if (language.isNotBlank()) append(" [$language]")
                }
            }
            
            streams.add(
                StreamLink(
                    name = streamName,
                    url = path,
                    quality = quality,
                    host = HostType.MOVIEBOX,
                    referer = "$baseOrigin/",
                    cookies = cookiesMap,
                    headers = streamHeaders,
                    contentType = when {
                        path.contains(".m3u8") -> com.streamflex.core.network.detector.ContentType.M3U8
                        path.contains(".mpd") -> com.streamflex.core.network.detector.ContentType.DASH
                        else -> com.streamflex.core.network.detector.ContentType.VIDEO
                    },
                    adaptive = path.contains(".m3u8") || path.contains(".mpd")
                )
            )
        }
    }
}
