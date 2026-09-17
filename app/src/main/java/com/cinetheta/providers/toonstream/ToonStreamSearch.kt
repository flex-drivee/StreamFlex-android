package com.cinetheta.providers.toonstream

import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.NetworkUtils
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToonStreamSearch {

    companion object {
        private const val TAG = "ToonStreamSearch"
    }

    /**
     * Search for anime/content on ToonStream.
     * Tries toonstream.vip (no Cloudflare block) first, then falls back to toon-stream.site.
     */
    suspend fun search(
        query   : String,
        baseUrl : String = ToonStreamConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val domainsToTry = listOf(ToonStreamConfig.DEFAULT_DOMAIN, baseUrl, ToonStreamConfig.FALLBACK_DOMAIN).distinct()

        // Clean query: colons (':') and special chars break WordPress search queries
        val cleanQuery = query.replace(":", " ").replace("-", " ").replace(Regex("\\s+"), " ").trim()
        val queriesToTry = mutableListOf(cleanQuery)
        
        // Also add short query (first 2-3 words) if query has subtitle or many words
        val words = cleanQuery.split(" ")
        if (words.size > 2) {
            val shortQ = words.take(2).joinToString(" ")
            if (!queriesToTry.contains(shortQ)) {
                queriesToTry.add(shortQ)
            }
        }

        for (domain in domainsToTry) {
            for (q in queriesToTry) {
                val encodedQ = NetworkUtils.encode(q)
                // CloudStream reference uses /s?q=$query&type=all&page=1; fallback to /?s=$encodedQ
                val urlsToTry = listOf(
                    "$domain/s?q=$encodedQ&type=all&page=1",
                    "$domain/?s=$encodedQ"
                )

                for (searchUrl in urlsToTry) {
                    val request = RequestBuilder()
                        .url(searchUrl)
                        .header("Referer", domain)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Upgrade-Insecure-Requests", "1")
                        .build()

                    when (val response = HttpClient.execute(request)) {
                        is NetworkResult.Success -> {
                            val html = response.data.bodyAsString()
                            if (html.isBlank()) continue

                            val results = parse(html, domain)
                            if (results.isNotEmpty()) {
                                com.cinetheta.core.logger.Logger.d("Found ${results.size} results for '$q' on $domain ($searchUrl)", TAG)
                                return@withContext results
                            }
                        }
                        else -> continue
                    }
                }
            }
        }

        return@withContext emptyList()
    }

    internal fun parse(html: String, baseUrl: String): List<SearchResult> {
        val document = HtmlParser.parse(html, baseUrl)
        val results  = mutableListOf<SearchResult>()

        // CloudStream reference uses #movies-a > ul > li
        // Also include DooPlay and generic archive fallback selectors
        val items = document.select("#movies-a > ul > li, div.result-item, article.item-movies, article.item-tvshows, article.post, article")

        for (item in items) {
            val anchor = item.selectFirst("article > a[href], .title a, .details .title a, header h2 a, h3 a, h2 a, a[href]") ?: continue
            val rawHref = anchor.attr("href").trim()
            if (rawHref.isBlank() || rawHref == "#" || rawHref.startsWith("javascript:")) continue

            val href = if (rawHref.startsWith("//")) {
                "https:$rawHref"
            } else if (rawHref.startsWith("http")) {
                rawHref
            } else {
                "$baseUrl/${rawHref.trimStart('/')}"
            }

            // CloudStream reference: StringsKt.replace(StringsKt.trim(select("article > header > h2").text()), "Watch Online", "")
            val title = (item.selectFirst("article > header > h2, header.entry-header > h2, .title a, .details .title a, header h2 a, h3 a, h2 a, header h2, h3, h2, .title")?.text()
                ?: anchor.attr("title"))
                .replace("Watch Online", "", ignoreCase = true)
                .trim()

            if (title.isBlank()) continue

            // CloudStream reference: select("article figure img") or select("article > div.post-thumbnail > figure > img")
            val img = item.selectFirst("article > div.post-thumbnail > figure > img, article figure img, div.post-thumbnail figure img, .poster img, .thumbnail img, figure img, img")
            val posterUrlRaw = img?.attr("src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("abs:src")?.takeIf { it.isNotBlank() }

            val poster = when {
                posterUrlRaw == null -> null
                posterUrlRaw.startsWith("//") -> "https:$posterUrlRaw"
                posterUrlRaw.startsWith("http") -> posterUrlRaw
                else -> "$baseUrl/${posterUrlRaw.trimStart('/')}"
            }

            val mediaType = when {
                href.contains("/movies/", ignoreCase = true) -> MediaType.MOVIE
                else -> MediaType.TV
            }

            // Deduplicate by URL
            if (results.none { it.url == href }) {
                results += ToonStreamMapper.toSearchResult(
                    title     = title,
                    detailUrl = href,
                    poster    = poster,
                    mediaType = mediaType
                )
            }
        }

        return results
    }
}
