package com.streamflex.app.data.extractors

import com.streamflex.app.data.network.HttpClient
import com.streamflex.app.domain.models.VideoStream
import org.jsoup.Jsoup

object Hdhub4uExtractor {

    fun extract(detailUrl: String): List<VideoStream> {
        val response = HttpClient.get(detailUrl)
        val html = response.body?.string() ?: return emptyList()
        val doc = Jsoup.parse(html)

        val streams = mutableListOf<VideoStream>()

        // --- WATCH ONLINE buttons ---
        doc.select("a").forEach { link ->
            val href = link.attr("href")
            val text = link.text().lowercase()

            when {
                "hubstream" in href -> {
                    streams += VideoStream(
                        url = href,
                        quality = "Auto",
                        provider = "HubStream"
                    )
                }

                "hdstream4u" in href || "hubcdn" in href -> {
                    streams += VideoStream(
                        url = href,
                        quality = "Auto",
                        provider = "HDStream"
                    )
                }
            }
        }

        return streams.distinctBy { it.url }
    }
}
