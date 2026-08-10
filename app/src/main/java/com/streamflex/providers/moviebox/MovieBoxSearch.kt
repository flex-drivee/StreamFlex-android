package com.streamflex.providers.moviebox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult
import org.json.JSONObject

class MovieBoxSearch {

    suspend fun search(query: String, baseUrl: String): List<SearchResult> {
        val searchUrl = "$baseUrl/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = JSONObject().apply {
            put("keyword", query)
            put("page", 1)
            put("perPage", 20)
        }.toString()

        val headers = MovieBoxCrypto.getHeaders(
            method = "POST",
            url = searchUrl,
            body = jsonBody
        )

        val request = RequestBuilder()
            .url(searchUrl)
            .post(jsonBody.toByteArray(Charsets.UTF_8))
            .headers(headers)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val json = response.data.bodyAsString()
                    val root = JsonParser.parse(json) ?: return@withContext emptyList()
                    val list = JsonParser.array(root, "data")
                    
                    val results = mutableListOf<SearchResult>()
                    for (item in list) {
                        val id = JsonParser.string(item, "id") ?: continue
                        val title = JsonParser.string(item, "title") ?: continue
                        val poster = JsonParser.string(item, "coverUrl") // Note: coverUrl in actual API
                        val typeStr = JsonParser.string(item, "type") ?: "movie"
                        val isTv = typeStr.equals("tv", ignoreCase = true) || JsonParser.int(item, "box_type") == 2
                        val mediaType = if (isTv) MediaType.TV else MediaType.MOVIE

                        results.add(MovieBoxMapper.toSearchResult(
                            id = id,
                            title = title,
                            detailUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=$id", // Update detail URL
                            poster = poster,
                            year = JsonParser.int(item, "year"),
                            mediaType = mediaType
                        ))
                    }
                    results
                }
                else -> emptyList()
            }
        }
    }
}
