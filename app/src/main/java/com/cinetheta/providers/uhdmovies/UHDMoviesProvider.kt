package com.cinetheta.providers.uhdmovies

import com.cinetheta.domain.models.*
import com.cinetheta.domain.provider.Provider
import org.jsoup.Jsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UHDMoviesProvider : Provider {
    override val name: String = "UHDMovies"
    override val id: String = "uhdmovies"
    override val baseUrl: String = "https://uhdmovies.my"
    override val supportedMedia: Set<MediaType> = setOf(MediaType.MOVIE, MediaType.TV)
    
    private val client = OkHttpClient.Builder().build()
    
    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val searchUrl = "$baseUrl/?s=${query.replace(" ", "+")}"
        
        try {
            val request = Request.Builder().url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val doc = Jsoup.parse(response.body?.string() ?: "")
            val results = mutableListOf<SearchResult>()
            
            val elements = doc.select("article.gridlove-post, article.latestPost, div.post")
            for (element in elements) {
                val a = element.selectFirst("a") ?: continue
                val title = element.selectFirst(".entry-title, .title, h2, .sanket")?.text() ?: a.attr("title")
                val url = a.attr("href")
                var img = element.selectFirst("img")?.attr("src") ?: ""
                
                if (img.isEmpty() || img.contains("data:image")) {
                    img = element.selectFirst("img")?.attr("data-src") ?: img
                }
                
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    val isTv = title.contains("Season", ignoreCase = true) ||
                               title.contains("Episode", ignoreCase = true) ||
                               title.contains("Series", ignoreCase = true) ||
                               title.contains("Complete", ignoreCase = true) ||
                               Regex("""(?i)\bS\d+\b""").containsMatchIn(title)

                    results.add(
                        SearchResult(
                            id = url,
                            url = url,
                            providerId = id,
                            providerName = name,
                            title = title,
                            poster = img,
                            mediaType = if (isTv) MediaType.TV else MediaType.MOVIE
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun determineHostType(href: String): HostType {
        return when {
            href.contains("thenaukriadda") -> HostType.THENAUKRIADDA
            href.contains("driveleech") -> HostType.DRIVELEECH
            href.contains("driveseed") -> HostType.DRIVESEED
            else -> HostType.THENAUKRIADDA
        }
    }

    private fun formatHostLabel(qualityText: String, quality: Quality): String {
        val qLabel = when (quality) {
            Quality.P2160 -> "4K (2160p)"
            Quality.P1080 -> "1080p FHD"
            Quality.P720 -> "720p HD"
            Quality.P480 -> "480p SD"
            else -> if (quality != Quality.UNKNOWN) quality.label else ""
        }

        val tags = mutableListOf<String>()
        if (qLabel.isNotEmpty()) tags.add(qLabel)

        if (qualityText.contains("remux", ignoreCase = true)) tags.add("REMUX")
        if (qualityText.contains("dovi", ignoreCase = true) || qualityText.contains("dv", ignoreCase = true)) tags.add("DV")
        if (qualityText.contains("hdr", ignoreCase = true)) tags.add("HDR")
        if (qualityText.contains("10bit", ignoreCase = true)) tags.add("10Bit")
        if (qualityText.contains("hevc", ignoreCase = true) || qualityText.contains("x265", ignoreCase = true)) tags.add("HEVC")

        val sizeMatch = Regex("""\[([\d.]+\s*(?:GB|MB)(?:/[Ee])?)\]""").find(qualityText)
        if (sizeMatch != null) {
            tags.add(sizeMatch.groupValues[1])
        }

        return if (tags.isNotEmpty()) tags.joinToString(" • ") else "UHDMovies"
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(searchResult.url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ProviderResult(
                    id = searchResult.id, providerId = id, title = searchResult.title,
                    detailUrl = searchResult.url, mediaType = searchResult.mediaType,
                    success = false, error = "HTTP ${response.code}"
                )
            }
            
            val doc = Jsoup.parse(response.body?.string() ?: "")
            val entryContent = doc.selectFirst("div.entry-content") ?: doc.body()
            
            // Map of (SeasonNumber, EpisodeNumber) -> List of ProviderSource
            val episodesMap = mutableMapOf<Pair<Int, Int>, MutableList<ProviderSource>>()
            
            var currentSeason = 1
            var currentQualityText = ""
            var currentQuality = Quality.UNKNOWN
            
            // Candidate elements in entry-content in document order
            val elements = entryContent.select("pre, p, h1, h2, h3, h4, h5, h6, a")
            for (el in elements) {
                val tag = el.tagName().lowercase()
                val text = el.text().trim()
                
                if (tag == "a") {
                    val href = el.attr("href").trim()
                    if (href.isBlank() || !href.startsWith("http")) continue
                    // Skip zip/pack/batch download links for individual episode scraping
                    if (text.contains("zip", ignoreCase = true) || 
                        text.contains("pack", ignoreCase = true) || 
                        text.contains("batch", ignoreCase = true)) {
                        continue
                    }
                    
                    // Match episode number: "Episode 1", "Episode 01", "Ep 1", "Ep. 2", "E03"
                    val epMatch = Regex("""(?i)\b(?:episode|ep\.?|e)\s*(\d{1,3})\b""").find(text)
                    if (epMatch != null) {
                        val epNum = epMatch.groupValues[1].toIntOrNull()
                        if (epNum != null && epNum in 1..999) {
                            val hostType = determineHostType(href)
                            val hostName = formatHostLabel(currentQualityText, currentQuality)
                            val source = ProviderSource(
                                provider = name,
                                host = hostName,
                                hostType = hostType,
                                url = href,
                                quality = currentQuality
                            )
                            val key = Pair(currentSeason, epNum)
                            episodesMap.getOrPut(key) { mutableListOf() }.add(source)
                        }
                    }
                } else {
                    // Check if element contains any episode links
                    val hasEpisodeLinks = el.select("a").any { a ->
                        Regex("""(?i)\b(?:episode|ep\.?|e)\s*(\d{1,3})\b""").containsMatchIn(a.text())
                    }
                    
                    if (!hasEpisodeLinks) {
                        // Check if it's a Season header: e.g. "Season 1", "Season 2", "S02"
                        val seasonMatch = Regex("""(?i)\b(?:season\s*|s)(\d{1,2})\b""").find(text)
                        if (seasonMatch != null) {
                            val parsedSeason = seasonMatch.groupValues[1].toIntOrNull()
                            if (parsedSeason != null && parsedSeason in 1..99) {
                                currentSeason = parsedSeason
                            }
                        }
                        
                        // Check if it's a Quality / Release info header
                        if (text.contains("2160", ignoreCase = true) || text.contains("4k", ignoreCase = true) ||
                            text.contains("1080", ignoreCase = true) || text.contains("720", ignoreCase = true) ||
                            text.contains("480", ignoreCase = true) || text.contains("hevc", ignoreCase = true) ||
                            text.contains("bluray", ignoreCase = true) || text.contains("web-dl", ignoreCase = true)) {
                            currentQualityText = text
                            currentQuality = Quality.fromLabel(text)
                        }
                    }
                }
            }

            val posterImg = doc.selectFirst("div.entry-content img, div.entry-image img")?.let { imgEl ->
                val src = imgEl.attr("src")
                if (src.isEmpty() || src.contains("data:image")) imgEl.attr("data-src") else src
            }?.takeIf { it.isNotBlank() } ?: searchResult.poster

            if (episodesMap.isNotEmpty()) {
                // TV SHOW PATH
                val seasons = episodesMap.keys.map { it.first }.distinct().sorted().map { seasonNum ->
                    val seasonEpisodes = episodesMap.filterKeys { it.first == seasonNum }
                        .toList()
                        .sortedBy { it.first.second }
                        .map { (key, sources) ->
                            ProviderEpisode(
                                number = key.second,
                                title = "Episode ${key.second}",
                                overview = null,
                                thumbnail = posterImg,
                                sources = sources.distinctBy { it.url }
                            )
                        }
                    ProviderSeason(
                        number = seasonNum,
                        title = "Season $seasonNum",
                        poster = posterImg,
                        episodes = seasonEpisodes
                    )
                }
                
                ProviderResult(
                    id = searchResult.id,
                    providerId = id,
                    title = searchResult.title,
                    detailUrl = searchResult.url,
                    mediaType = MediaType.TV,
                    seasons = seasons,
                    sources = emptyList(),
                    poster = posterImg,
                    success = true,
                    overview = doc.selectFirst("div.entry-content p")?.text()
                )
            } else {
                // MOVIE PATH (Fallback if no episodes parsed)
                val movieLinks = entryContent.select("a.maxbutton-download-g-drive, a[href*=thenaukriadda], a[href*=driveleech], a[href*=driveseed]")
                    .filter { a ->
                        val href = a.attr("href").trim()
                        val text = a.text().trim()
                        href.isNotBlank() && href.startsWith("http") &&
                        (a.hasClass("maxbutton") || text.contains("download", ignoreCase = true) || href.contains("driveleech") || href.contains("driveseed"))
                    }
                
                val sources = movieLinks.mapIndexed { index, el ->
                    val href = el.attr("href").trim()
                    val hostType = determineHostType(href)
                    
                    val rawQualityText = el.parents().firstOrNull { it.tagName() == "p" }?.previousElementSibling()?.text()
                        ?: el.parent()?.parent()?.previousElementSibling()?.text()
                        ?: "Link ${index + 1}"
                    
                    val quality = Quality.fromLabel(rawQualityText)
                    val hostName = formatHostLabel(rawQualityText, quality)
                    
                    ProviderSource(
                        provider = name,
                        host = hostName,
                        hostType = hostType,
                        url = href,
                        quality = quality
                    )
                }.distinctBy { it.url }
                
                ProviderResult(
                    id = searchResult.id,
                    providerId = id,
                    title = searchResult.title,
                    detailUrl = searchResult.url,
                    mediaType = searchResult.mediaType,
                    sources = sources,
                    seasons = emptyList(),
                    poster = posterImg,
                    success = true,
                    overview = doc.selectFirst("div.entry-content p")?.text()
                )
            }
        } catch (e: Exception) {
            ProviderResult(
                id = searchResult.id, providerId = id, title = searchResult.title,
                detailUrl = searchResult.url, mediaType = searchResult.mediaType,
                success = false, error = e.message
            )
        }
    }
}
