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
                
                val list = JsonParser.array(data, "streams") // "streams" in actual API
                for (item in list) {
                    val path = JsonParser.string(item, "url") ?: continue // "url" in actual API
                    val qualityStr = JsonParser.string(item, "resolutions") ?: ""
                    
                    val quality = when {
                        qualityStr.contains("1080") -> Quality.P1080
                        qualityStr.contains("720") -> Quality.P720
                        qualityStr.contains("480") -> Quality.P480
                        qualityStr.contains("360") -> Quality.P360
                        else -> Quality.UNKNOWN
                    }

                    val signCookie = JsonParser.string(item, "signCookie")
                    val streamHeaders = if (!signCookie.isNullOrBlank()) {
                        mapOf("Cookie" to signCookie)
                    } else {
                        emptyMap()
                    }
                    
                    streams.add(
                        StreamLink(
                            name = "MovieBox $qualityStr",
                            url = path,
                            quality = quality,
                            host = HostType.MOVIEBOX,
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
