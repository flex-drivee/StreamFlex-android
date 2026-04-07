package com.streamflex.app.data.extractors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import android.util.Base64
import org.json.JSONObject

class Hdhub4uExtractor {

    suspend fun extract(pageUrl: String): List<String> = withContext(Dispatchers.IO) {
        val streamLinks = mutableListOf<String>()

        try {
            // 1. Fetch the actual Movie details page
            val document = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cookie", "xla=s4t")
                .get()

            // 2. Find the download/stream links using the exact Regex from Phisher
            // They look for specific resolutions in h3 and h4 tags
            val rawLinks = document.select("h3 a:matches(480|720|1080|2160|4K), h4 a:matches(480|720|1080|2160|4K)")
                .map { it.attr("href") }

            // 3. Process the links to bypass the redirector
            for (rawLink in rawLinks) {
                val realLink = if (rawLink.contains("?id=")) {
                    getRedirectLinks(rawLink)
                } else {
                    rawLink
                }

                if (realLink.isNotEmpty()) {
                    streamLinks.add(realLink)
                }
            }

            // Note: Currently we are returning the final Hosting site URLs (like HubCloud or HubDrive)
            // In the next step, we will need to extract the actual .m3u8/.mp4 from those specific hosts.

        } catch (e: Exception) {
            android.util.Log.e("HDHub4u_DEBUG", "Extraction Failed: ${e.message}")
        }

        // Fallback for testing
        if (streamLinks.isEmpty()) {
            streamLinks.add("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        }

        return@withContext streamLinks
    }

    // --- Ported directly from Phisher's Utils.kt to bypass the Anti-Bot ---

    private fun getRedirectLinks(url: String): String {
        return try {
            val docHtml = Jsoup.connect(url).get().html()
            val regex = "s\\('o','([A-Za-z0-9+/=]+)'|ck\\('_wp_http_\\d+','([^']+)'".toRegex()

            val combinedString = buildString {
                regex.findAll(docHtml).forEach { matchResult ->
                    val extractedValue = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
                    if (!extractedValue.isNullOrEmpty()) append(extractedValue)
                }
            }

            val decodedString = String(Base64.decode(pen(String(Base64.decode(String(Base64.decode(combinedString, Base64.DEFAULT)), Base64.DEFAULT))), Base64.DEFAULT))
            val jsonObject = JSONObject(decodedString)

            val encodedurl = String(Base64.decode(jsonObject.optString("o", ""), Base64.DEFAULT)).trim()
            val data = String(Base64.decode(jsonObject.optString("data", ""), Base64.DEFAULT)).trim()
            val wphttp1 = jsonObject.optString("blog_url", "").trim()

            val directlink = runCatching {
                Jsoup.connect("$wphttp1?re=$data").get().select("body").text().trim()
            }.getOrDefault("").trim()

            encodedurl.ifEmpty { directlink }
        } catch (e: Exception) {
            android.util.Log.e("HDHub4u_DEBUG", "Error resolving links: $e")
            ""
        }
    }

    // Custom ROT13 decoder ported from Utils.kt
    private fun pen(value: String): String {
        return value.map {
            when (it) {
                in 'A'..'Z' -> ((it - 'A' + 13) % 26 + 'A'.code).toChar()
                in 'a'..'z' -> ((it - 'a' + 13) % 26 + 'a'.code).toChar()
                else -> it
            }
        }.joinToString("")
    }
}