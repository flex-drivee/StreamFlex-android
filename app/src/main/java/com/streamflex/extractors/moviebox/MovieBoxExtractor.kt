package com.streamflex.extractors.moviebox

import android.net.Uri
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
import org.json.JSONObject

class MovieBoxExtractor : BaseExtractor() {

    override val hostType = HostType.MOVIEBOX

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // url passed from MovieBoxDetails is already fully qualified:
        // "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$id&episode=$ep"
        val playUrl = source.url
        
        val headers = MovieBoxCrypto.getHeaders(
            method = "GET",
            url = playUrl,
            body = null
        )

        val request = RequestBuilder()
            .url(playUrl)
            .get()
            .headers(headers)
            .build()

        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val json = response.data.bodyAsString()
                val root = JsonParser.parse(json) ?: return emptyResult()
                val data = JsonParser.objectOf(root, "data") ?: return emptyResult()
                
                val streams = mutableListOf<StreamLink>()
                val globalSignCookie = JsonParser.string(data, "signCookie") ?: JsonParser.string(data, "signCookieRaw")
                
                val list = JsonParser.array(data, "streams")
                for (item in list) {
                    val path = JsonParser.string(item, "url") ?: continue
                    val qualityStr = JsonParser.string(item, "resolutions") ?: ""
                    val language = JsonParser.string(item, "language") ?: ""
                    
                    val quality = when {
                        qualityStr.contains("1080") -> Quality.P1080
                        qualityStr.contains("720") -> Quality.P720
                        qualityStr.contains("480") -> Quality.P480
                        qualityStr.contains("360") -> Quality.P360
                        else -> Quality.UNKNOWN
                    }

                    val signCookie = JsonParser.string(item, "signCookie") 
                        ?: JsonParser.string(item, "signCookieRaw") 
                        ?: globalSignCookie

                    val baseOrigin = "https://api3.aoneroom.com"
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
                        append("MovieBox")
                        if (qualityStr.isNotBlank()) append(" $qualityStr")
                        if (language.isNotBlank()) append(" [$language]")
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
                
                return result(streams)
            }
            else -> return emptyResult()
        }
    }
}
