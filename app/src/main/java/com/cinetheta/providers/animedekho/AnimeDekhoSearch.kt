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
     * Endpoints:
     * - https://animedekho.tv/browse/?q={query}
     * - https://animedekho.tv/?s={query}
     */
    suspend fun search(
        query   : String,
        baseUrl : String = AnimeDekhoConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val encodedQ = NetworkUtils.encode(query)
        val searchUrls = listOf(
            "$baseUrl/browse/?q=$encodedQ",
            "$baseUrl/?s=$encodedQ"
        ).distinct()

        for (url in searchUrls) {
            val request = RequestBuilder()
                .url(url)
                .header("Referer", "$baseUrl/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .timeout(10_000L)
                .build()

            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val html = response.data.body?.toString(Charsets.UTF_8)
                    if (!html.isNullOrBlank()) {
                        val results = parse(html, baseUrl)
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

        // 1. Try animedekho.tv theme: <a class="dgt-card" ...>
        val cards = document.select("a.dgt-card")
        if (cards.isNotEmpty()) {
            for (card in cards) {
                val href = card.attr("abs:href").ifBlank {
                    val raw = card.attr("href")
                    if (raw.startsWith("http")) raw else "$baseUrl/${raw.trimStart('/')}"
                }
                val postId = card.attr("data-id").trim()
                val title = card.selectFirst(".dgt-card__title")?.text()?.trim()
                    ?: card.attr("title").trim()
                if (title.isBlank()) continue

                val img = card.selectFirst("img")
                val poster = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }

                val meta = card.selectFirst(".dgt-card__meta")?.text() ?: ""
                val badgeEp = card.selectFirst(".dgt-badge--ep")?.text()
                val isSeries = meta.contains("Series", ignoreCase = true) ||
                    href.contains("serie", ignoreCase = true) ||
                    href.contains("season", ignoreCase = true) ||
                    badgeEp != null

                val mediaType = if (isSeries) MediaType.TV else MediaType.MOVIE

                // Extract year from meta e.g. "2007 · Series"
                val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(meta)
                val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()

                results += SearchResult(
                    id           = if (postId.isNotBlank()) postId else href.hashCode().toString(),
                    url          = href,
                    providerId   = AnimeDekhoConfig.PROVIDER_ID,
                    providerName = AnimeDekhoConfig.PROVIDER_NAME,
                    title        = title,
                    poster       = poster,
                    year         = year,
                    mediaType    = mediaType
                )
            }
            if (results.isNotEmpty()) return results
        }

        // 2. Fallback to legacy <article> structure
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

            val isSeries = href.contains("serie", ignoreCase = true) ||
                href.contains("season", ignoreCase = true) ||
                href.contains("/epi/", ignoreCase = true) ||
                href.contains("/tv/", ignoreCase = true) ||
                href.contains("/show/", ignoreCase = true) ||
                article.selectFirst(".season-episode, .watch-btn:contains(Series)") != null

            val mediaType = if (isSeries) MediaType.TV else MediaType.MOVIE

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
