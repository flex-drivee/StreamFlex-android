package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.data.network.HttpClient
import okhttp3.Response
import org.jsoup.Jsoup

data class SearchCandidate(
    val title: String,
    val year: Int?,
    val url: String,
)

class Hdhub4uProvider {

    private val domains = listOf(
        "https://new3.hdhub4u.fo",
        "https://hdhub4u.ch",
        "https://hdhub4u.wiki"
    )

    private fun resolveUrl(path: String): String {
        return domains.first() + path
    }

    /**
     * Search movies by title, returns unordered candidates list
     */
    fun searchMovies(title: String): List<SearchCandidate> {
        val query = title.replace(" ", "+")
        val url = "${domains.first()}/?s=$query"

        val response: Response = HttpClient.get(url)
        val html = response.body?.string() ?: return emptyList()

        val doc = Jsoup.parse(html)

        // HDHub4u search results are usually in article blocks
        val articles = doc.select("article")

        val results = mutableListOf<SearchCandidate>()

        for (article in articles) {
            val link = article.selectFirst("a") ?: continue
            val rawTitle = link.attr("title").ifBlank { link.text() }.trim()
            val href = link.attr("href")

            if (rawTitle.isBlank() || href.isBlank()) continue

            results.add(
                SearchCandidate(
                    title = rawTitle,
                    year = extractYear(rawTitle),
                    url = href
                )
            )
        }

        return results
    }

    /**
     * Extract year from title like: Title (2023)
     */
    private fun extractYear(str: String): Int? {
        val regex = Regex("(19|20)\\d{2}")
        val match = regex.find(str)
        return match?.value?.toIntOrNull()
    }
}
