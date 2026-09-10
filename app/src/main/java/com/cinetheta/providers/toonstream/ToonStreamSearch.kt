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

    /**
     * Search for anime/content on ToonStream.
     * Endpoint: https://toon-stream.site/?s={query}  (standard WordPress search)
     *
     * Note: toon-stream.site uses Cloudflare Bot Management. The CloudflareKiller
     * interceptor handles this automatically via WebView on first request.
     */
    suspend fun search(
        query   : String,
        baseUrl : String = ToonStreamConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val request = RequestBuilder()
            .url("$baseUrl/?s=${NetworkUtils.encode(query)}")
            .header("Referer", baseUrl)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .build()

        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val html = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyList()
                parse(html, baseUrl)
            }
            else -> emptyList()
        }
    }

    internal fun parse(html: String, baseUrl: String): List<SearchResult> {
        val document = HtmlParser.parse(html)
        val results  = mutableListOf<SearchResult>()

        for (article in document.select("article")) {
            val anchor = article.selectFirst("a") ?: continue
            val href   = anchor.attr("abs:href").ifBlank {
                val raw = anchor.attr("href")
                if (raw.startsWith("http")) raw else "$baseUrl/${raw.trimStart('/')}"
            }

            val titleElement = article.selectFirst("header h2") ?: article.selectFirst("h2, .title")
            val title  = titleElement?.text()?.replace("Watch Online", "")?.trim()
                ?: anchor.attr("title").trim()

            if (title.isBlank()) continue

            val img    = article.selectFirst("figure img") ?: article.selectFirst("img")
            val posterUrlRaw = img?.attr("src")?.takeIf { it.isNotBlank() }
            val poster = if (posterUrlRaw?.startsWith("http") == false) "https:$posterUrlRaw" else posterUrlRaw

            val mediaType = when {
                href.contains("series", ignoreCase = true) ||
                href.contains("tv",  ignoreCase = true) -> MediaType.TV
                else -> MediaType.MOVIE
            }

            results += ToonStreamMapper.toSearchResult(
                title     = title,
                detailUrl = href,
                poster    = poster,
                mediaType = mediaType
            )
        }

        return results
    }
}
