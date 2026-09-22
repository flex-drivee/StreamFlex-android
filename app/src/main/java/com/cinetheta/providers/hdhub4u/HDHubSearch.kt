package com.cinetheta.providers.hdhub4u

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.NetworkUtils
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.core.parser.JsonParser
import com.cinetheta.core.parser.SearchResultParser
import com.cinetheta.core.parser.TransportResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult

/**
 * HDHub4U Search implementation.
 *
 * Primary: Native WordPress HTML search on the provider's active domain.
 * Secondary fallback: Typesense search API.
 */
class HDHubSearch : SearchResultParser {

    companion object {
        private const val TAG = "HDHubSearch"
        private const val SEARCH_API =
            "https://search.pingora.fyi/collections/post/documents/search"
        private const val PROVIDER = "HDHub4u"
    }

    suspend fun search(
        query: String,
        baseUrl: String = HDHubConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // 1. Primary: Direct WordPress HTML Search on the active baseUrl
        val htmlResults = searchHtml(cleanQuery, baseUrl)
        if (htmlResults.isNotEmpty()) {
            Logger.d("Found ${htmlResults.size} HTML search results for '$query' on $baseUrl", TAG)
            return@withContext htmlResults
        }

        // 2. Secondary: Fallback to Typesense API if HTML search yielded nothing
        val apiResults = searchApi(cleanQuery, baseUrl)
        if (apiResults.isNotEmpty()) {
            Logger.d("Found ${apiResults.size} Typesense search results for '$query'", TAG)
            return@withContext apiResults
        }

        emptyList()
    }

    private suspend fun searchHtml(query: String, baseUrl: String): List<SearchResult> {
        return try {
            val searchUrl = "${baseUrl.trimEnd('/')}/?s=${NetworkUtils.encode(query)}"
            val request = RequestBuilder()
                .url(searchUrl)
                .header("Referer", baseUrl)
                .header("Cookie", HDHubConfig.COOKIE)
                .build()

            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val html = response.data.bodyAsString()
                    if (html.isBlank()) return emptyList()
                    parseHtml(html, baseUrl)
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Logger.w("HTML search failed for '$query': ${e.message}", TAG)
            emptyList()
        }
    }

    private fun parseHtml(html: String, baseUrl: String): List<SearchResult> {
        val doc = HtmlParser.parse(html, baseUrl)
        val results = mutableListOf<SearchResult>()

        val items = doc.select("li.thumb, div.recent-movies, article.post, li.post, .post-item, div.archive-post")
        val candidateItems = if (items.isNotEmpty()) items else doc.select("figure a[href], .entry-title a[href]")

        for (item in candidateItems) {
            val anchor = item.selectFirst("figure a[href], a[href]") ?: continue
            val rawHref = anchor.attr("href").trim()
            if (rawHref.isBlank() || rawHref == "#" || rawHref.startsWith("javascript:")) continue
            if (rawHref.contains("/category/") || rawHref.contains("/tag/") ||
                rawHref.contains("/disclaimer") || rawHref.contains("/how-to-download") ||
                rawHref.contains("/join-our-group") || rawHref.contains("/request-a-movie")) continue

            val detailUrl = if (rawHref.startsWith("http")) {
                rawHref
            } else {
                "${baseUrl.trimEnd('/')}/${rawHref.trimStart('/')}"
            }

            val title = (item.selectFirst("p.entry-title, .entry-title, h2, h3, .title")?.text()
                ?: anchor.attr("title"))
                .trim()
            if (title.isBlank()) continue

            val img = item.selectFirst("img")
            val poster = img?.attr("src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("data-src")?.takeIf { it.isNotBlank() }

            val isSeries = title.contains("Season", ignoreCase = true) ||
                    title.contains("Series", ignoreCase = true) ||
                    detailUrl.contains("season", ignoreCase = true)

            val mediaType = if (isSeries) MediaType.TV else MediaType.MOVIE

            if (results.none { it.url == detailUrl }) {
                results += HDHubMapper.toSearchResult(
                    title = title,
                    detailUrl = detailUrl,
                    poster = poster,
                    year = null,
                    mediaType = mediaType
                )
            }
        }

        return results
    }

    private suspend fun searchApi(query: String, baseUrl: String): List<SearchResult> {
        return try {
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

            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val jsonString = response.data.body?.toString(Charsets.UTF_8) ?: return emptyList()
                    val rawTransport = TransportResult.TextResponse(
                        text = jsonString,
                        url = SEARCH_API
                    )
                    parse(rawTransport, baseUrl)
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Logger.w("Typesense API search failed: ${e.message}", TAG)
            emptyList()
        }
    }

    override fun parse(raw: TransportResult): List<SearchResult> {
        return parse(raw, HDHubConfig.DEFAULT_DOMAIN)
    }

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