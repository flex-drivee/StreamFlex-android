package com.streamflex.providers.hdhub4u

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.DetailParser
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.SearchResult
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HDHubDetails : DetailParser {

    companion object {
        private const val PROVIDER_ID = "hdhub4u"
        private const val PROVIDER_NAME = "HDHub4u"
        private const val TAG = "HDHubDetails"

        // Matches: EP01, Ep.01, EP.01, E01, Episode 1, Episode.1, S01E01, etc.
        private val EPISODE_NUM_REGEX = Regex(
            """(?:ep(?:isode)?[.\s_-]?|[eE]\s*0*)(\d{1,3})|(?:\b[sS]\d{1,2}[eE]0*(\d{1,3}))""",
            RegexOption.IGNORE_CASE
        )
        // Matches season number in URL: season-1, s01, s1
        private val SEASON_URL_REGEX = Regex(
            """season[_-]?(\d{1,2})""",
            RegexOption.IGNORE_CASE
        )

        private fun extractEpNumber(text: String): Int? {
            val match = EPISODE_NUM_REGEX.find(text) ?: return null
            return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull()
                ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        }
    }

    private val sourceParser = HDHubSourceParser()

    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult {
        val pageUrl = normalizeUrl(result.url, baseUrl)
        StreamLogger.info(TAG, "Loading detail page: $pageUrl (${result.mediaType})")

        val request = RequestBuilder()
            .url(pageUrl)
            .header("Referer", baseUrl)
            .header("Cookie", HDHubConfig.COOKIE)
            .build()

        return when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val html = response.data.bodyAsString()
                StreamLogger.debug(TAG, "Downloaded HTML (${html.length} chars)")

                val transport = TransportResult.HtmlResponse(html = html, url = pageUrl)
                val parsed = parse(transport, pageUrl)

                val newTitle = if (parsed.title.isNotBlank() && parsed.title != pageUrl)
                    parsed.title else result.title

                HDHubMapper.toProviderResult(
                    providerId = PROVIDER_ID,
                    title = newTitle,
                    detailUrl = pageUrl,
                    sources = parsed.sources,
                    mediaType = result.mediaType,
                    seasons = parsed.seasons,
                    year = parsed.year ?: result.year,
                    poster = parsed.poster ?: result.poster,
                    overview = parsed.overview,
                    success = parsed.success,
                    error = parsed.error
                )
            }
            else -> {
                StreamLogger.error(TAG, "Failed to load detail page: $pageUrl ($response)")
                HDHubMapper.toProviderResult(
                    providerId = PROVIDER_ID,
                    title = result.title,
                    detailUrl = pageUrl,
                    sources = emptyList(),
                    mediaType = result.mediaType,
                    success = false,
                    error = "HTTP request failed"
                )
            }
        }
    }

    override fun parse(raw: TransportResult, detailUrl: String): ProviderResult {
        val html = raw.asString()
        if (html.isBlank()) {
            return HDHubMapper.toProviderResult(
                providerId = PROVIDER_ID,
                title = detailUrl,
                detailUrl = detailUrl,
                mediaType = MediaType.MOVIE,
                success = false,
                error = "Empty HTML content"
            )
        }

        val document = HtmlParser.parse(html)
        val title = extractTitle(document, detailUrl)
        val poster = extractPoster(document)
        val plot = extractOverview(document)
        val isSeries = isTvSeries(document, detailUrl)
        val mediaType = if (isSeries) MediaType.TV else MediaType.MOVIE

        if (mediaType == MediaType.MOVIE) {
            val sources = sourceParser.parseDocument(document, detailUrl)
            StreamLogger.info(TAG, "Found ${sources.size} movie provider source(s)")
            return HDHubMapper.toProviderResult(
                providerId = PROVIDER_ID,
                title = title,
                detailUrl = detailUrl,
                sources = sources,
                mediaType = MediaType.MOVIE,
                poster = poster,
                overview = plot
            )
        } else {
            val seasons = parseSeasons(document, detailUrl)
            StreamLogger.info(TAG, "Found ${seasons.size} season(s) with TV episodes")

            val finalSeasons = if (seasons.isEmpty()) {
                StreamLogger.warn(TAG, "parseSeasons returned empty — falling back to batch mode")
                val allSources = sourceParser.parseDocument(document, detailUrl)
                if (allSources.isNotEmpty()) {
                    val seasonNum = extractSeasonNumber(detailUrl)
                    StreamLogger.info(TAG, "Batch fallback: ${allSources.size} source(s) → Season $seasonNum, Episode 1 (All Episodes)")
                    listOf(
                        ProviderSeason(
                            number = seasonNum,
                            title = "Season $seasonNum",
                            episodes = listOf(
                                ProviderEpisode(
                                    number = 1,
                                    title = "All Episodes",
                                    sources = allSources
                                )
                            )
                        )
                    )
                } else {
                    emptyList()
                }
            } else seasons

            return HDHubMapper.toProviderResult(
                providerId = PROVIDER_ID,
                title = title,
                detailUrl = detailUrl,
                seasons = finalSeasons,
                mediaType = MediaType.TV,
                poster = poster,
                overview = plot
            )
        }
    }

    private fun parseSeasons(document: Document, detailUrl: String): List<ProviderSeason> {
        val baseSeasonNum = extractSeasonNumber(detailUrl)
        // Map<SeasonNumber, Map<EpisodeNumber, List<ProviderSource>>>
        val seasonMap = mutableMapOf<Int, MutableMap<Int, MutableList<ProviderSource>>>()

        // 1. Check modern grid layout (div.episodes-list div.season-item)
        val seasonElements = document.select("div.episodes-list div.season-item")
        if (seasonElements.isNotEmpty()) {
            for (seasonEl in seasonElements) {
                val detectedSeason = extractSeasonNumber(seasonEl.text())
                val currentSeason = if (detectedSeason > 0) detectedSeason else baseSeasonNum

                val episodeItems = seasonEl.select("div.episode-download-item")
                for (epItem in episodeItems) {
                    val epBadgeText = epItem.select("div.episode-file-info span.badge-psa, span.badge").text()
                    val epNum = extractEpNumber(epBadgeText) ?: 1

                    val links = epItem.select("a[href]")
                    for (link in links) {
                        val href = link.absUrl("href").takeIf { it.isNotBlank() } ?: link.attr("href")
                        if (href.isBlank() || sourceParser.shouldSkipUrl(href)) continue

                        var hostType = HostDetector.detect(href)
                        if (hostType == HostType.UNKNOWN && href.contains("?id=")) hostType = HostType.REDIRECT
                        if (hostType != HostType.UNKNOWN) {
                            val quality = QualityDetector.detect(link.text() + " " + epItem.text())
                            val source = HDHubMapper.toProviderSource(
                                provider = PROVIDER_NAME,
                                host = hostType.name,
                                hostType = hostType,
                                url = href,
                                quality = quality,
                                referer = detailUrl,
                                headers = mapOf("Referer" to detailUrl, "Cookie" to HDHubConfig.COOKIE)
                            )
                            seasonMap.getOrPut(currentSeason) { mutableMapOf() }
                                .getOrPut(epNum) { mutableListOf() }
                                .add(source)
                        }
                    }
                }
            }
        }

        // 2. Sequential scanning over headings & paragraphs (classic HDHub4u layout)
        if (seasonMap.isEmpty()) {
            var currentSeason = baseSeasonNum
            var currentEpisode: Int? = null
            val elements = document.select("h1, h2, h3, h4, h5, h6, p, div, a[href]")

            for (el in elements) {
                val text = el.ownText().trim()
                val fullText = el.text().trim()
                val checkText = text.ifBlank { fullText }

                // Check for season headers (e.g., "Season 1", "Season 2", "S02")
                val seasonMatch = Regex("""\b(?:season|s)\s*0*(\d{1,2})\b""", RegexOption.IGNORE_CASE).find(checkText)
                if (seasonMatch != null && (el.tagName().startsWith("h") || el.tagName() in listOf("p", "strong", "b") || el.children().isEmpty())) {
                    val sNum = seasonMatch.groupValues[1].toIntOrNull()
                    if (sNum != null && sNum in 1..99) {
                        currentSeason = sNum
                    }
                }

                // Check for episode headers
                if (el.tagName().startsWith("h") || el.tagName() in listOf("p", "strong", "b") || el.children().isEmpty()) {
                    if (!checkText.contains("Single Episode", ignoreCase = true) && !checkText.contains("All Episodes", ignoreCase = true)) {
                        val num = extractEpNumber(checkText)
                        if (num != null && num in 1..999) {
                            currentEpisode = num
                        }
                    }
                }

                if (el.tagName() == "a") {
                    val url = el.attr("abs:href").ifBlank { el.attr("href") }
                    if (url.isNotBlank() && !sourceParser.shouldSkipUrl(url)) {
                        var hostType = HostDetector.detect(url)
                        if (hostType == HostType.UNKNOWN && url.contains("?id=")) hostType = HostType.REDIRECT
                        
                        if (hostType != HostType.UNKNOWN) {
                            val directSeason = extractSeasonNumber(el.text()) ?: extractSeasonNumber(el.attr("title")) ?: extractSeasonNumber(url)
                            val seasonToUse = if (directSeason > 0) directSeason else currentSeason

                            val directEp = extractEpNumber(el.text()) ?: extractEpNumber(el.attr("title")) ?: extractEpNumber(url)
                            val epToUse = directEp ?: currentEpisode
                            if (epToUse != null) {
                                val quality = QualityDetector.detect(el.text() + " " + (el.parent()?.text() ?: ""))
                                val source = HDHubMapper.toProviderSource(
                                    provider = PROVIDER_NAME,
                                    host = hostType.name,
                                    hostType = hostType,
                                    url = url,
                                    quality = quality,
                                    referer = detailUrl,
                                    headers = mapOf("Referer" to detailUrl, "Cookie" to HDHubConfig.COOKIE)
                                )
                                seasonMap.getOrPut(seasonToUse) { mutableMapOf() }
                                    .getOrPut(epToUse) { mutableListOf() }
                                    .add(source)
                            }
                        }
                    }
                }
            }
        }

        // 3. Fallback: Associating elements via DOM sibling scanning
        if (seasonMap.isEmpty()) {
            val allSources = sourceParser.parseDocument(document, detailUrl)
            val linkElements = document.select("a[href]").associateBy { it.attr("href").trim() }
            for (source in allSources) {
                val epNum = findEpisodeNumber(source, linkElements)
                if (epNum != null) {
                    val directSeason = extractSeasonNumber(source.url)
                    val seasonToUse = if (directSeason > 0) directSeason else baseSeasonNum
                    seasonMap.getOrPut(seasonToUse) { mutableMapOf() }
                        .getOrPut(epNum) { mutableListOf() }
                        .add(source)
                }
            }
        }

        if (seasonMap.isEmpty()) return emptyList()

        return seasonMap.entries.sortedBy { it.key }.map { (sNum, epMap) ->
            val episodes = epMap.entries.sortedBy { it.key }.map { (num, sources) ->
                ProviderEpisode(
                    number = num,
                    title = "Episode $num",
                    sources = sources
                )
            }
            ProviderSeason(
                number = sNum,
                title = "Season $sNum",
                episodes = episodes
            )
        }
    }

    private fun findEpisodeNumber(source: ProviderSource, linkElements: Map<String, Element>): Int? {
        val element = linkElements[source.url] ?: return null
        extractEpNumber(element.text())?.let { return it }
        element.parent()?.let { parent ->
            extractEpNumber(parent.text())?.let { return it }
        }

        var sibling: Element? = element.parent()?.previousElementSibling()
        var hops = 0
        while (sibling != null && hops < 8) {
            val tag = sibling.tagName()
            if (tag in listOf("h2", "h3", "h4", "p", "strong", "b")) {
                val text = sibling.text()
                if (!text.contains("Single Episode", ignoreCase = true) && !text.contains("All Episodes", ignoreCase = true)) {
                    extractEpNumber(text)?.let { return it }
                }
            }
            sibling = sibling.previousElementSibling()
            hops++
        }
        extractEpNumber(source.url)?.let { return it }
        return null
    }

    private fun extractSeasonNumber(urlOrText: String): Int {
        return SEASON_URL_REGEX.find(urlOrText)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun extractTitle(document: Document, fallback: String): String {
        val titleEl = document.selectFirst(
            "h1.page-title span, h1.page-title, h1, .entry-title"
        )
        val text = titleEl?.text()?.trim()
        return if (!text.isNullOrBlank()) text else fallback
    }

    private fun extractPoster(document: Document): String? {
        return document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: document.selectFirst("main.page-body img.aligncenter")?.attr("src")
            ?: document.selectFirst(".post-thumbnail img, img.wp-post-image")?.attr("src")
    }

    private fun extractOverview(document: Document): String? {
        return document.selectFirst(".kno-rdesc, .entry-content p, p")?.text()?.trim()
    }

    private fun isTvSeries(document: Document, detailUrl: String): Boolean {
        val titleText = document.selectFirst("h1.page-title span, h1.page-title, h1, .entry-title")?.text() ?: ""
        val combined = "$titleText $detailUrl".lowercase()

        // 1. Explicit Movie check
        if ((combined.contains("full movie") || combined.contains("full-movie") || combined.contains("movie")) &&
            !combined.contains("season") && !combined.contains("series") && !combined.contains("all-episodes")
        ) {
            return false
        }

        // 2. TV Series signals from URL and Title
        if (combined.contains("all-episodes") ||
            combined.contains("web-series") ||
            combined.contains("tv-series") ||
            combined.contains("/series/") ||
            combined.contains("/web-series/") ||
            Regex("""season[ _-]*\d+""", RegexOption.IGNORE_CASE).containsMatchIn(combined)
        ) {
            return true
        }

        // 3. Title-only episode patterns
        if (EPISODE_NUM_REGEX.containsMatchIn(titleText)) {
            return true
        }

        return false
    }

    private fun normalizeUrl(url: String, baseUrl: String): String {
        return if (url.startsWith("http")) url
        else baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
}