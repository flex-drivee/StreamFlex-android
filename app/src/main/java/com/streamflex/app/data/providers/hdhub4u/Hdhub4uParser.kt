package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.domain.models.ContentType
import com.streamflex.app.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Hdhub4uParser {
    // The working domain from the plugin
    private val mainUrl = "https://hdhub4u.rehab"

    // The required headers to bypass basic bot protection
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0",
        "Cookie" to "xla=s4t",
        "Referer" to mainUrl
    )

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        try {
            // Using the exact Pingora API from the Phisher plugin
            val searchUrl = "https://search.pingora.fyi/collections/post/documents/search" +
                    "?q=${query.replace(" ", "+")}" +
                    "&query_by=post_title,category" +
                    "&query_by_weights=4,2" +
                    "&sort_by=sort_by_date:desc" +
                    "&limit=15"

            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            if (connection.responseCode == 200) {
                val jsonResponse = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(jsonResponse)
                val hits = jsonObject.optJSONArray("hits") ?: return@withContext emptyList()

                for (i in 0 until hits.length()) {
                    val document = hits.getJSONObject(i).getJSONObject("document")

                    val title = document.optString("post_title")
                    val permalink = document.optString("permalink") // This is the movie URL
                    val thumbnail = document.optString("post_thumbnail")

                    results.add(
                        SearchResult(
                            id = permalink, // Send the URL to the Extractor
                            title = title,
                            poster = thumbnail,
                            type = ContentType.MOVIE,
                            year = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HDHub4u_DEBUG", "API Search Failed: ${e.message}")
        }

        return@withContext results
    }
}