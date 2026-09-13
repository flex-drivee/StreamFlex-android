package com.cinetheta.app.data.extractors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import android.util.Base64
import org.json.JSONObject

class Hdhub4uExtractor {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    private val cookie = "xla=s4t"

    // 1. Centralized helper to make EVERY request look like a real browser
    private fun fetchDocument(url: String): Document {
        return Jsoup.connect(url)
            .userAgent(userAgent)
            .header("Cookie", cookie)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Upgrade-Insecure-Requests", "1")
            .ignoreHttpErrors(true)
            .ignoreContentType(true) // This prevents the "zero bytes" crash!
            .timeout(15000)
            .get()
    }

    suspend fun extract(pageUrl: String): List<String> = withContext(Dispatchers.IO) {
        val streamLinks = mutableListOf<String>()

        try {
            val activeDomain = getActiveDomain()
            val fixedUrl = pageUrl.replace(Regex("https?://[^/]+"), activeDomain)


            // Use the new fetchDocument helper
            val document = fetchDocument(fixedUrl)

            val targetElements = document.select("h3 a:matches(480|720|1080|2160|4K), h4 a:matches(480|720|1080|2160|4K), .page-body > div a")
            val potentialLinks = targetElements.map { it.attr("abs:href") }.filter { it.isNotBlank() }.distinct()


            val hostLinks = mutableListOf<String>()


            val skipPatterns = listOf(
                "how-to-download",
                "discord",
                "imdb",
                "facebook",
                "twitter",
                "telegram",
                "/category/",
                "/tag/"
            )

            for (rawLink in potentialLinks) {
                val linkLower = rawLink.lowercase()

                // 🚫 Skip garbage EARLY
                if (skipPatterns.any { linkLower.contains(it) }) {
                    continue
                }

                val realLink = if (linkLower.contains("?id=")) getRedirectLinks(rawLink) else rawLink

                // ✅ Only allow known host domains
                if (
                    realLink.contains("hubcloud", true) ||
                    realLink.contains("hubdrive", true) ||
                    realLink.contains("hubcdn", true) ||
                    realLink.contains("hblinks", true) ||
                    realLink.contains("hubstream", true)
                ) {
                    if (!hostLinks.contains(realLink)) {
                        hostLinks.add(realLink)
                    }
                }
            }


            val sortedLinks = hostLinks.sortedBy {
                when {
                    it.contains("hubcdn", true) -> 1
                    it.contains("hubcloud", true) -> 2
                    it.contains("hubdrive", true) -> 3
                    it.contains("hblinks", true) -> 4
                    it.contains("hubstream", true) -> 5
                    else -> 6
                }
            }


            for (hostLink in sortedLinks) {
                val finalVideoUrl = resolveHostToVideo(hostLink)

                if (finalVideoUrl.isNotEmpty()) {
                    streamLinks.add(finalVideoUrl)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (streamLinks.isEmpty()) {
            streamLinks.add("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        }

        return@withContext streamLinks.distinct()
    }

    private fun getActiveDomain(): String {
        return try {
            val json = Jsoup.connect("https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/domains.json")
                .ignoreContentType(true)
                .execute()
                .body()
            JSONObject(json).optString("HDHUB4u", "https://new5.hdhub4u.fo")
        } catch (e: Exception) {
            "https://new5.hdhub4u.fo"
        }
    }

    private fun resolveHostToVideo(url: String): String {
        try {
            var currentUrl = url

            if (currentUrl.contains("hblinks", ignoreCase = true)) {
                val hbDoc = fetchDocument(currentUrl)
                val hbLink = hbDoc.select("h3 a, h5 a, div.entry-content p a").map { it.attr("abs:href") }
                    .firstOrNull { it.contains("hubcloud", true) || it.contains("hubdrive", true) || it.contains("hubcdn", true) }
                if (hbLink != null) {
                    currentUrl = hbLink
                }
            }

            if (currentUrl.contains("hubdrive", ignoreCase = true)) {
                val driveDoc = fetchDocument(currentUrl)
                val hubCloudLink = driveDoc.select("a.btn, a[class*='btn']").attr("abs:href")
                if (hubCloudLink.isNotEmpty()) {
                    currentUrl = hubCloudLink
                }
            }

            if (currentUrl.contains("hubcdn", ignoreCase = true)) {
                val cdnDoc = fetchDocument(currentUrl)
                val scriptText = cdnDoc.selectFirst("script:containsData(var reurl)")?.data()

                val encodedUrl = Regex("""reurl\s*=\s*"([^"]+)"""")
                    .find(scriptText ?: "")
                    ?.groupValues?.get(1)
                    ?.substringAfter("?r=")

                if (encodedUrl != null) {
                    val decodedUrl = String(Base64.decode(encodedUrl, Base64.DEFAULT)).substringAfterLast("link=")
                    if (decodedUrl.isNotEmpty()) return decodedUrl
                }
            }

            if (currentUrl.contains("hubcloud", ignoreCase = true)) {
                val cloudDoc = fetchDocument(currentUrl)

                // STRATEGY A: Prioritize exact, direct video links (Google Servers, MKV, MP4)
                val directFile = cloudDoc.select("a[href]").map { it.attr("abs:href") }
                    .firstOrNull {
                        it.contains("googleusercontent.com") ||
                                it.endsWith(".mkv") ||
                                it.endsWith(".mp4") ||
                                it.contains(".m3u8")
                    }
                if (directFile != null) return directFile

                // STRATEGY B: Dig through buttons, but handle gamerxyt proxies
                val buttons = cloudDoc.select("a.btn, a[class*='btn']")
                for (btn in buttons) {
                    val link = btn.attr("abs:href")
                    val label = btn.text().lowercase()

                    if (label.contains("download") || label.contains("server") || label.contains("fsl") || label.contains("direct") || label.contains("play")) {

                        // If Hubcloud is hiding the video behind gamerxyt.com, we must visit the proxy page
                        if (link.contains("gamerxyt.com") || link.contains(".php")) {
                            try {
                                val proxyDoc = fetchDocument(link)

                                // Look for the true Google video link inside the proxy page
                                val realLink =
                                    proxyDoc.select("a[href*='googleusercontent']").map { it.attr("abs:href") }.firstOrNull()
                                        ?: proxyDoc.select("source[src]").map { it.attr("abs:src") }.firstOrNull()
                                        ?: Regex("""https://[^"]+googleusercontent[^"]+""")
                                            .find(proxyDoc.html())
                                            ?.value
                                if (realLink != null) return realLink

                            } catch(e: Exception) {
                            }
                            continue // Skip to the next button if proxy failed
                        }

                        // If it's a normal link (not a php proxy), return it
                        if (
                            link.contains("googleusercontent.com") ||
                            link.endsWith(".mp4") ||
                            link.endsWith(".mkv") ||
                            link.contains(".m3u8")
                        ) {
                            return link
                        }
                    }
                }

                val source = cloudDoc.selectFirst("source[src]")?.attr("src")
                if (source != null && source.startsWith("http")) return source
            }

        } catch (e: Exception) {
        }
        return ""
    }

    private fun getRedirectLinks(url: String): String {
        return try {
            val docHtml = fetchDocument(url).html() // Use helper
            val regex = "s\\('o','([A-Za-z0-9+/=]+)'|ck\\('_wp_http_\\d+','([^']+)'".toRegex()

            val combinedString = buildString {
                regex.findAll(docHtml).forEach { matchResult ->
                    val extractedValue = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
                    if (!extractedValue.isNullOrEmpty()) append(extractedValue)
                }
            }
            if (combinedString.isEmpty()) return ""

            val step1 = String(Base64.decode(combinedString, Base64.DEFAULT))
            val step2 = String(Base64.decode(step1, Base64.DEFAULT))
            val step3 = pen(step2)
            val decodedString = String(Base64.decode(step3, Base64.DEFAULT))

            val jsonObject = JSONObject(decodedString)
            val encodedurl = String(Base64.decode(jsonObject.optString("o", ""), Base64.DEFAULT)).trim()
            val data = String(Base64.decode(jsonObject.optString("data", ""), Base64.DEFAULT)).trim()
            val wphttp1 = jsonObject.optString("blog_url", "").trim()

            val directlink = runCatching {
                if (wphttp1.isNotEmpty() && data.isNotEmpty()) {
                    fetchDocument("$wphttp1?re=$data")
                        .select("body")
                        .text()
                        .trim()
                } else ""
            }.getOrDefault("").trim()

            if (encodedurl.isNotEmpty()) encodedurl else directlink
        } catch (e: Exception) {
            ""
        }
    }

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