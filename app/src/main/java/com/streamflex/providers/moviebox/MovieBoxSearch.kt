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

        if (MovieBoxCrypto.xUserToken == null) {
            fetchXUserToken(baseUrl)
        }

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
                    // check if the response returns a new X-User token and update it
                    response.data.header("x-user")?.let {
                        MovieBoxCrypto.xUserToken = parseToken(it)
                    }
                    val json = response.data.bodyAsString()
                    val root = JsonParser.parse(json) ?: return@withContext emptyList()
                    val data = JsonParser.objectOf(root, "data") ?: return@withContext emptyList()
                    val resultsArray = JsonParser.array(data, "results")
                    
                    val results = mutableListOf<SearchResult>()
                    for (resultBlock in resultsArray) {
                        val subjects = JsonParser.array(resultBlock, "subjects")
                        for (item in subjects) {
                            val id = JsonParser.string(item, "subjectId") ?: continue
                            val title = JsonParser.string(item, "title") ?: continue
                            
                            val coverObj = JsonParser.objectOf(item, "cover")
                            val poster = coverObj?.let { JsonParser.string(it, "url") }
                            
                            val typeStr = JsonParser.string(item, "type") ?: "movie"
                            val isTv = typeStr.equals("tv", ignoreCase = true) || JsonParser.int(item, "subjectType") == 2
                            val mediaType = if (isTv) MediaType.TV else MediaType.MOVIE

                            // Year is not directly present, we can parse it from releaseDate (e.g. "2002-05-03")
                            val releaseDate = JsonParser.string(item, "releaseDate") ?: ""
                            val year = releaseDate.substringBefore("-").toIntOrNull() ?: 0

                            results.add(MovieBoxMapper.toSearchResult(
                                id = id,
                                title = title,
                                detailUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=$id", // Update detail URL
                                poster = poster,
                                year = year,
                                mediaType = mediaType
                            ))
                        }
                    }
                    results
                }
                else -> emptyList()
            }
        }
    }

    private fun parseToken(xUserHeader: String): String? {
        if (xUserHeader.isBlank()) return null
        return try {
            val root = JsonParser.parse(xUserHeader)
            JsonParser.string(root, "token")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchXUserToken(baseUrl: String) {
        val url = "$baseUrl/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"
        val headers = MovieBoxCrypto.getHeaders(
            method = "GET",
            url = url,
            body = null,
            contentType = null
        )

        val request = RequestBuilder()
            .url(url)
            .get()
            .headers(headers)
            .build()

        withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    response.data.header("x-user")?.let {
                        MovieBoxCrypto.xUserToken = parseToken(it)
                    }
                }
                else -> {}
            }
        }
    }
}
