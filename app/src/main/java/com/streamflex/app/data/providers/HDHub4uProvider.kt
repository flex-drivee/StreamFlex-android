package com.streamflex.app.data.providers

import com.streamflex.app.core.BaseProvider
import com.streamflex.app.data.extractors.HubCloudExtractor
import com.streamflex.app.data.models.Media
import com.streamflex.app.data.models.MediaDetails
import com.streamflex.app.data.models.MediaType
import com.streamflex.app.data.models.StreamLink
import com.streamflex.app.data.models.Episode
import org.jsoup.Jsoup

class HDHub4uProvider : BaseProvider() {
    override val name = "HDHub4u"
    override val mainUrl = "https://new1.hdhub4u.fo"

    override suspend fun search(query: String): List<Media> {
        val searchUrl = "$mainUrl/?s=$query"
        println("StreamFlex: Visiting -> $searchUrl")

        return try {
            val doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .get()

            val results = mutableListOf<Media>()

            // ⚡ THE WORKING SELECTOR ⚡
            val elements = doc.select("div#primary ul li, figure, article")

            println("StreamFlex: Found ${elements.size} items using Repo Logic")

            for (element in elements) {
                val linkElement = element.select("a").first()
                val imgElement = element.select("img").first()

                if (linkElement != null && imgElement != null) {
                    val url = linkElement.attr("href")
                    val poster = imgElement.attr("src")

                    val title = imgElement.attr("alt")
                        .replace("Download", "", true)
                        .replace("Free", "", true)
                        .trim()

                    if (title.length > 2 && !url.contains("/category/") && !url.contains("/tag/")) {
                        results.add(Media(
                            url = url,
                            title = title,
                            posterUrl = poster,
                            type = MediaType.Movie
                        ))
                    }
                }
            }

            val finalResults = results.distinctBy { it.url }
            println("StreamFlex: Final Count -> ${finalResults.size}")
            finalResults // Return the list

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // Return empty list on error
        }
    }

    override suspend fun loadHome(): List<Media> {
        println("StreamFlex: Loading Home Page...")
        return try {
            val doc = Jsoup.connect(mainUrl)
                .userAgent("Mozilla/5.0")
                .get()

            val elements = doc.select("div#primary ul li, figure, article")
            val results = mutableListOf<Media>()

            for (element in elements) {
                val link = element.select("a").first()
                val img = element.select("img").first()
                if (link != null && img != null) {
                    val title = img.attr("alt").replace("Download", "").trim()
                    if (title.isNotEmpty()) {
                        results.add(Media(link.attr("href"), title, img.attr("src"), MediaType.Movie))
                    }
                }
            }
            println("StreamFlex: Home Page found ${results.size} items")
            results.take(20)
        } catch (e: Exception) {
            println("StreamFlex: Home Load Failed, falling back to Search")
            e.printStackTrace()
            search("avengers")
        }
    }

    override suspend fun loadDetails(url: String): MediaDetails {
        println("StreamFlex: Loading Details for -> $url")
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(10000)
            .get()

        val title = doc.select("h1.entry-title").text()

        val poster = doc.select("figure.post-thumbnail img, div.poster img, img.attachment-post-thumbnail, .entry-content img").firstOrNull()?.attr("src") ?: ""

        // ⚡ FIX: Grab ALL text from the content div, not just paragraphs
        // This fixes the "Empty Description" issue
        var desc = doc.select("div.entry-content, div.post-content").text()

        // Cleanup: Remove common garbage text
        desc = desc.substringAfter("Storyline").substringBefore("Download")
            .substringBefore("Screenshots")
            .trim()

        val episodeList = mutableListOf<Episode>()

        // (Keep the Aggressive Link Selector from before)
        val potentialLinks = doc.select(
            "a:contains(Watch Online), " +  // ⚡ Priority 1: Streaming Link
                    "a[href*='hubcloud'], " +
                    "a.maxbutton, " +
                    "a.maxbutton-1, " +
                    "a.maxbutton-2, " +
                    "a.maxbutton-3, " +
                    "a.maxbutton-4, " +
                    "a.maxbutton-5, " +
                    "h4 a, " +
                    "p a[href^='http']"
        )

        potentialLinks.forEachIndexed { index, element ->
            val linkUrl = element.attr("href")
            val linkText = element.text()

            if (linkUrl.startsWith("http") &&
                !linkUrl.contains("wp-content") &&
                (linkUrl.contains("hubcloud") || linkText.contains("Download", true) || linkText.contains("720p") || linkText.contains("1080p"))
            ) {
                val cleanName = linkText.replace("Download", "", true).trim()
                val finalName = if (cleanName.isNotEmpty()) cleanName else "Link ${index + 1}"

                episodeList.add(Episode(
                    name = finalName,
                    url = linkUrl,
                    episodeNumber = 1,
                    seasonNumber = 1
                ))
            }
        }

        val uniqueEpisodes = episodeList.distinctBy { it.url }
        return MediaDetails(title, desc, poster, null, uniqueEpisodes, emptyList())
    }

    override suspend fun loadLinks(episodeUrl: String): List<StreamLink> {
        return HubCloudExtractor.extract(episodeUrl)
    }
}