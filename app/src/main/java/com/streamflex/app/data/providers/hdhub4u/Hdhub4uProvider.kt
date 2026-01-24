package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.data.network.HttpClient
import org.jsoup.Jsoup

class Hdhub4uProvider {

    private val baseUrl = "https://new3.hdhub4u.fo"

    fun search(title: String, year: Int? = null): List<Hdhub4uSearchResult> {
        val url = "$baseUrl/?s=${title.replace(" ", "+")}"
        val response = HttpClient.get(url)
        val body = response.body?.string() ?: return emptyList()

        val doc = Jsoup.parse(body)
        val results = mutableListOf<Hdhub4uSearchResult>()

        doc.select("article").forEach { article ->
            val link = article.selectFirst("a")?.attr("href") ?: return@forEach
            val name = article.selectFirst("h2")?.text()?.trim() ?: return@forEach

            // Try extracting year from title if present
            val extractedYear = Regex(".*\\((\\d{4})\\).*").find(name)?.groupValues?.get(1)?.toIntOrNull()

            // Year filtering (optional)
            if (year != null && extractedYear != null && extractedYear != year) {
                return@forEach
            }

            results.add(
                Hdhub4uSearchResult(
                    title = name,
                    year = extractedYear,
                    detailUrl = link
                )
            )
        }

        return results
    }
}
