package com.streamflex.providers.hdhub4u

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.network.NetworkUtils
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult

class HDHubSearch {

    companion object {

        private const val BASE_URL = "https://new3.hdhub4u.cl"

        private const val SEARCH_API =
            "https://search.pingora.fyi/collections/post/documents/search"

        private const val PROVIDER = "HDHub4u"
    }


    suspend fun search(query: String): List<SearchResult> {

        val results = mutableListOf<SearchResult>()

        val request = RequestBuilder()
            .url(
                "$SEARCH_API" +
                        "?q=${NetworkUtils.encode(query)}" +
                        "&query_by=post_title,category" +
                        "&query_by_weights=4,2" +
                        "&sort_by=sort_by_date:desc" +
                        "&limit=15"
            )
            .header("Referer", BASE_URL)
            .build()

        return withContext(Dispatchers.IO) {

            when (val response = HttpClient.execute(request)) {

                is NetworkResult.Success -> {

                    try {

                        val jsonString =
                            response.data.body
                                ?.toString(Charsets.UTF_8)
                                ?: return@withContext emptyList()

                        val json =
                            JsonParser.parseObject(jsonString)
                                ?: return@withContext emptyList()

                        val hits =
                            json.optJSONArray("hits")
                                ?: return@withContext emptyList()

                        for (i in 0 until hits.length()) {

                            val document = hits
                                .getJSONObject(i)
                                .getJSONObject("document")

                            val title =
                                document.optString("post_title")

                            val permalink =
                                document.optString("permalink")

                            val poster =
                                document.optString("post_thumbnail")
                                    .takeIf { it.isNotBlank() }

                            val detailUrl =
                                if (permalink.startsWith("http"))
                                    permalink
                                else
                                    BASE_URL + permalink

                            val category =
                                document.optString("category")
                                    .lowercase()

                            val mediaType =
                                if (
                                    category.contains("series") ||
                                    category.contains("tv")
                                ) {
                                    MediaType.TV
                                } else {
                                    MediaType.MOVIE
                                }

                            results += HDHubMapper.toSearchResult(
                                title = title,
                                detailUrl = detailUrl,
                                poster = poster,
                                year = null,
                                mediaType = mediaType
                            )
                        }

                    } catch (_: Exception) {

                        return@withContext emptyList()

                    }

                    results
                }

                else -> emptyList()
            }
        }
    }
}