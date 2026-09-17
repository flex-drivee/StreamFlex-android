package com.cinetheta.providers.moviebox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.JsonParser
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import org.json.JSONObject

class MovieBoxSearch {

    suspend fun search(query: String, baseUrl: String, page: Int = 1): List<SearchResult> {
        val searchUrl = "$baseUrl/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = "{\"keyword\":\"$query\",\"page\":1,\"perPage\":20}"

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
                        val blockType = JsonParser.string(resultBlock, "type") ?: ""
                        val blockSubjectType = JsonParser.int(resultBlock, "subjectType") ?: 0
                        val blockTitle = JsonParser.string(resultBlock, "title")?.lowercase() ?: ""

                        val subjects = JsonParser.array(resultBlock, "subjects")
                        for (item in subjects) {
                            val id = JsonParser.string(item, "subjectId") ?: continue
                            val title = JsonParser.string(item, "title") ?: continue
                            
                            val coverObj = JsonParser.objectOf(item, "cover")
                            val poster = coverObj?.let { JsonParser.string(it, "url") }
                            
                            val itemType = JsonParser.string(item, "type") ?: ""
                            val itemSubjectType = JsonParser.int(item, "subjectType") ?: 0
                            val titleHasTvMarkers = Regex("(?i)\\b(?:season|series|s\\d+|ep\\.?\\s*\\d+|complete)\\b").containsMatchIn(title)

                            val isTv = itemType.equals("tv", ignoreCase = true) ||
                                       itemType.equals("series", ignoreCase = true) ||
                                       itemSubjectType == 2 ||
                                       blockType.equals("tv", ignoreCase = true) ||
                                       blockType.equals("series", ignoreCase = true) ||
                                       blockSubjectType == 2 ||
                                       blockTitle.contains("tv") ||
                                       blockTitle.contains("series") ||
                                       titleHasTvMarkers

                            val mediaType = if (isTv) MediaType.TV else MediaType.MOVIE

                            // Check for adult content
                            val titleLower = title.lowercase()
                            val genresArray = JsonParser.array(item, "genres")
                            val genreNames = genresArray.mapNotNull { JsonParser.string(it, "name")?.lowercase() }.joinToString(" ")
                            val combinedText = "$titleLower $genreNames"
                            val adultRegex = Regex("\\b(porn|adult|erotic|18\\+|xxx)\\b", RegexOption.IGNORE_CASE)
                            val isAdult = adultRegex.containsMatchIn(combinedText)
                            
                            if (isAdult) {
                                continue
                            }

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

                    val cleanQ = query.trim()
                    val sequelRegex = Regex("""\b([2-9]|II|III|IV|V)\b""", RegexOption.IGNORE_CASE)
                    val queryHasSequel = sequelRegex.containsMatchIn(cleanQ)

                    results.sortedByDescending { res ->
                        var score = 0
                        val t = res.title.trim()
                        if (t.equals(cleanQ, ignoreCase = true)) score += 100
                        else if (t.startsWith(cleanQ, ignoreCase = true)) score += 50
                        else if (t.contains(cleanQ, ignoreCase = true)) score += 25
                        
                        if (!queryHasSequel && sequelRegex.containsMatchIn(t)) {
                            score -= 40
                        }
                        score
                    }
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
            url = url
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
