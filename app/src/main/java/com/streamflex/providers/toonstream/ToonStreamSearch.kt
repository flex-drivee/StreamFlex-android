package com.streamflex.providers.toonstream

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.NetworkUtils
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToonStreamSearch {

    /**
     * Search for anime/content on ToonStream.
     * Endpoint: https://toon-stream.site/?s={query}
     */
    suspend fun search(
        query   : String,
        baseUrl : String = ToonStreamConfig.DEFAULT_DOMAIN
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val request = RequestBuilder()
            .url("$baseUrl/s?q=${NetworkUtils.encode(query)}")
            .header("Referer", baseUrl)
            .build()

        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val html = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyList()
                parse(html, baseUrl)
            }
            else -> emptyList()
        }
    }

    private fun parse(html: String, baseUrl: String): List<SearchResult> {
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
