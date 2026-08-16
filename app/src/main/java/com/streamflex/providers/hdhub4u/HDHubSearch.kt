package com.streamflex.providers.hdhub4u

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.parser.SearchResultParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.network.NetworkUtils
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult

/**
 * HDHub4U Search implementation.
 *
 * Implements [SearchResultParser] (Phase 1.3 & Phase 1.7) to decouple
 * Typesense CDN search JSON parsing from network transport.
 * Uses Gson via [JsonParser] for cross-platform (Android & JVM Unit Test) compatibility.
 */
class HDHubSearch : SearchResultParser {

    companion object {
        private const val SEARCH_API =
            "https://search.pingora.fyi/collections/post/documents/search"

        private const val PROVIDER = "HDHub4u"
    }

    /**
     * Executes the Typesense search API call and parses the result.
     *
     * @param query The search query string.
     * @param baseUrl The currently resolved primary domain from [DomainResolver].
     */
    suspend fun search(
        query: String,
        baseUrl: String = HDHubConfig.DEFAULT_DOMAIN
    ): List<SearchResult> {

        val request = RequestBuilder()
            .url(
                "$SEARCH_API" +
                        "?q=${NetworkUtils.encode(query)}" +
                        "&query_by=post_title,category" +
                        "&query_by_weights=4,2" +
                        "&sort_by=sort_by_date:desc" +
                        "&limit=15"
            )
            .header("Referer", baseUrl)
            .build()

        return withContext(Dispatchers.IO) {

            when (val response = HttpClient.execute(request)) {

                is NetworkResult.Success -> {
                    val jsonString = response.data.body
                        ?.toString(Charsets.UTF_8)
                        ?: return@withContext emptyList()

                    val rawTransport = TransportResult.TextResponse(
                        text = jsonString,
                        url = SEARCH_API
                    )

                    parse(rawTransport, baseUrl)
                }

                else -> emptyList()
            }
        }
    }

    override fun parse(raw: TransportResult): List<SearchResult> {
        return parse(raw, HDHubConfig.DEFAULT_DOMAIN)
    }

    /**
     * Parse a TransportResult with an explicit base URL for resolving relative links.
     */
    fun parse(raw: TransportResult, baseUrl: String): List<SearchResult> {
        val root = JsonParser.parse(raw.asString()) ?: return emptyList()

        val results = mutableListOf<SearchResult>()

        try {
            val hits = JsonParser.array(root, "hits")
            for (hit in hits) {
                val document = JsonParser.objectOf(hit, "document") ?: continue

                val title = JsonParser.string(document, "post_title") ?: ""
                val permalink = JsonParser.string(document, "permalink") ?: ""

                val poster = JsonParser.string(document, "post_thumbnail")
                    ?.takeIf { it.isNotBlank() }

                val path = if (permalink.startsWith("http")) {
                    permalink.substringAfter("://").substringAfter('/')
                } else {
                    permalink
                }

                val detailUrl = baseUrl.trimEnd('/') + "/" + path.trimStart('/')

                val category = (JsonParser.string(document, "category") ?: "").lowercase()
                val mediaType = if (
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
            return emptyList()
        }

        return results
    }
}