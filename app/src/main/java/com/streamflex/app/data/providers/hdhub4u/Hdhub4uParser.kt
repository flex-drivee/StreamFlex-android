package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.data.network.HttpClient
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.models.ContentType
import org.jsoup.Jsoup

object Hdhub4uParser {

    private const val BASE_URL = "https://new3.hdhub4u.fo"

    /**
     * Search movies / shows by query
     */
    fun search(query: String): List<SearchResult> {
        val searchUrl = "$BASE_URL/?s=${query.trim().replace(" ", "+")}"

        val response = HttpClient.get(searchUrl)
        val html = response.body?.string().orEmpty()

        val document = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        // Your confirmed selector
        val items = document.select("#results-grid li.movie-card")

        for (item in items) {
            val linkEl = item.selectFirst("a") ?: continue
            val titleEl = item.selectFirst("h3.movie-title")
            val imgEl = item.selectFirst("img")

            val detailUrl = linkEl.absUrl("href")
            val title = titleEl?.text()?.trim().orEmpty()
            val poster = imgEl?.absUrl("src")

            // Very basic type detection (we'll improve later)
            val type = if (title.contains("Season", ignoreCase = true) ||
                title.contains("Episodes", ignoreCase = true)
            ) {
                ContentType.SHOW
            } else {
                ContentType.MOVIE
            }

            results.add(
                SearchResult(
                    title = title,
                    id = detailUrl,
                    poster = poster,
                    type = type,
                    provider = "HDHub4u"
                )
            )
        }

        return results
    }

    /**
     * Load detail page (movie or show)
     * For now: returns download page URLs only
     */
    fun load(detailUrl: String): List<String> {
        val response = HttpClient.get(detailUrl)
        val html = response.body?.string().orEmpty()
        val document = Jsoup.parse(html)

        val downloadLinks = mutableListOf<String>()

        // HDHub4u usually puts download buttons in <a> tags
        document.select("a").forEach { a ->
            val href = a.absUrl("href")
            if (
                href.contains("hubdrive", true) ||
                href.contains("hubstream", true) ||
                href.contains("mixdrop", true) ||
                href.contains("streamsb", true)
            ) {
                downloadLinks.add(href)
            }
        }

        return downloadLinks.distinct()
    }
}
