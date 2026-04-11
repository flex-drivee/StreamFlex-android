package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.data.extractors.Hdhub4uExtractor
import com.streamflex.app.domain.models.ContentType
import com.streamflex.app.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Hdhub4uParser {

    private val mainUrl = "https://new5.hdhub4u.fo"

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0...",
        "Cookie" to "xla=s4t",
        "Referer" to mainUrl
    )

    // -----------------------------
    // EXISTING FUNCTION (NO CHANGE)
    // -----------------------------
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        try {
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
                    val rawPermalink = document.optString("permalink")
                    val thumbnail = document.optString("post_thumbnail")

                    val absoluteUrl = if (rawPermalink.startsWith("http")) {
                        rawPermalink
                    } else {
                        "$mainUrl$rawPermalink"
                    }

                    results.add(
                        SearchResult(
                            id = absoluteUrl,
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

    // -----------------------------------------
    // ✅ ADD THIS FUNCTION RIGHT HERE 👇
    // -----------------------------------------
    suspend fun getMovieLinks(query: String): List<String> = withContext(Dispatchers.IO) {
        val links = mutableListOf<String>()

        try {
            // 1. Search
            val results = search(query)

            if (results.isEmpty()) {
                android.util.Log.e("HDHub4u_DEBUG", "No results found for: $query")
                return@withContext emptyList()
            }

            val firstResult = results.first()
            val pageUrl = firstResult.id

            android.util.Log.d("HDHub4u_DEBUG", "Opening page: $pageUrl")

            // 2. Extract links
            val extractedLinks = Hdhub4uExtractor().extract(pageUrl)

            links.addAll(extractedLinks)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext links
    }
}