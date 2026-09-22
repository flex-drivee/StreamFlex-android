package com.cinetheta.providers.animedekho

import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.NetworkUtils
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.core.parser.TransportResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimeDekhoSearch {

    /**
     * Search for anime/content on AnimeDekho.
     * Endpoint: https://animedekho.app/?s={query}
     * HTML: <article> elements with <a href>, <h2> title, <img> poster.
     */
    suspend fun search(
        query   : String,
        baseUrl : String = AnimeDekhoConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val domainsToTry = listOf(baseUrl, AnimeDekhoConfig.DEFAULT_DOMAIN, AnimeDekhoConfig.MIRROR_DOMAIN).distinct()
        for (domain in domainsToTry) {
            val request = RequestBuilder()
                .url("$domain/?s=${NetworkUtils.encode(query)}")
                .header("Referer", domain)
                .timeout(8_000L)
                .build()

            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val html = response.data.body?.toString(Charsets.UTF_8)
                    if (!html.isNullOrBlank()) {
                        val results = parse(html, domain)
                        if (results.isNotEmpty()) {
                            return@withContext results
                        }
                    }
                }
                else -> continue
            }
        }
        emptyList()
    }

    internal fun parse(html: String, baseUrl: String): List<SearchResult> {
        val document = HtmlParser.parse(html)
        val results  = mutableListOf<SearchResult>()

        for (article in document.select("article")) {
            val anchor = article.selectFirst("a[href]") ?: continue
            val href   = anchor.attr("abs:href").ifBlank {
                val raw = anchor.attr("href")
                if (raw.startsWith("http")) raw else "$baseUrl/${raw.trimStart('/')}"
            }

            val title  = (article.selectFirst("h2, h3, .title, .entry-title")?.text()?.trim()
                ?: anchor.attr("title").trim()).takeIf { it.isNotBlank() } ?: continue

            val img    = article.selectFirst("img")
            val poster = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() }

            // URLs like /series-hindi/ → TV, /movie-hindi/ or /movies-hindi/ → MOVIE
            val mediaType = when {
                href.contains("series", ignoreCase = true) ||
                href.contains("/epi/",  ignoreCase = true) -> MediaType.TV
                else -> MediaType.MOVIE
            }

            results += AnimeDekhoMapper.toSearchResult(
                title     = title,
                detailUrl = href,
                poster    = poster,
                mediaType = mediaType
            )
        }

        return results
    }
}
