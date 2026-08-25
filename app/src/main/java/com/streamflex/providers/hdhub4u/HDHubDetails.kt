package com.streamflex.providers.hdhub4u

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.DetailParser
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.SourceParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * HDHub4U Detail Parser (Phase 1.3 & Phase 1.7).
 *
 * Handles both:
 *  - Movie pages  → extract all download links as ProviderSources
 *  - TV pages     → two modes:
 *      A. "All episodes" page: extract links, group by episode number found
 *         in link text (Ep.01, EP01, Episode 1, etc.), or treat as batch
 *         if no episode numbers found.
 *      B. Season index page: each row is a separate episode with its own
 *         download links.
 */
class HDHubDetails : DetailParser {

    companion object {
        private const val PROVIDER_ID = "hdhub4u"
        private const val PROVIDER_NAME = "HDHub4u"
        private const val TAG = "HDHubDetails"

        // Matches: EP01, Ep.01, EP.01, E01, Episode 1, Episode.1
        private val EPISODE_NUM_REGEX = Regex(
            """(?:ep(?:isode)?[.\s_-]?)(\d{1,3})""",
            RegexOption.IGNORE_CASE
        )
        // Matches season number in URL: season-1, s01, s1
        private val SEASON_URL_REGEX = Regex(
            """season[_-]?(\d{1,2})""",
            RegexOption.IGNORE_CASE
        )
    }

    private val sourceParser = HDHubSourceParser()

    // ─── Public API ───────────────────────────────────────────────────────────

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
        val isSeries = isTvSeries(document, detailUrl, title)
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

    // ─── Classification Helper ────────────────────────────────────────────────

    private fun isTvSeries(document: Document, url: String, title: String = ""): Boolean {
        val tvRegex = Regex("""(?:season[.\s_-]*\d+|s\d{1,2}\b|all\s+episodes|complete\s+series)""", RegexOption.IGNORE_CASE)
        if (tvRegex.containsMatchIn(url) || tvRegex.containsMatchIn(title)) return true

        if (document.select("div.episodes-list, div.season-item, div.episode-download-item").isNotEmpty()) return true

        val headings = document.select("h1, h2, h3, h4")
        for (h in headings) {
            val text = h.text()
            if (tvRegex.containsMatchIn(text) || text.contains("Single Episode", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    // ─── TV Season / Episode Parsing ─────────────────────────────────────────

    private fun parseSeasons(document: Document, detailUrl: String): List<ProviderSeason> {
        val seasonNum = extractSeasonNumber(detailUrl)
        val epLinksMap = mutableMapOf<Int, MutableList<ProviderSource>>()

        // 1. Check modern grid layout (div.episodes-list div.season-item)
        val seasonElements = document.select("div.episodes-list div.season-item")
        if (seasonElements.isNotEmpty()) {
            for (seasonEl in seasonElements) {
                val episodeItems = seasonEl.select("div.episode-download-item")
                for (epItem in episodeItems) {
                    val epBadgeText = epItem.select("div.episode-file-info span.badge-psa, span.badge").text()
                    val epNum = EPISODE_NUM_REGEX.find(epBadgeText)?.groupValues?.get(1)?.toIntOrNull()
                        ?: Regex("""Episode-0*([1-9][0-9]*)""").find(epBadgeText)?.groupValues?.get(1)?.toIntOrNull()
                        ?: 1

                    val links = epItem.select("a[href]")
                    for (link in links) {
                        var href = link.absUrl("href").takeIf { it.isNotBlank() } ?: link.attr("href")
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
                            epLinksMap.getOrPut(epNum) { mutableListOf() }.add(source)
                        }
                    }
                }
            }
        }

        // 2. Sequential scanning over headings & paragraphs (classic HDHub4u layout)
        if (epLinksMap.isEmpty()) {
            var currentEpisode: Int? = null
            val elements = document.select("h1, h2, h3, h4, h5, h6, p, div, a[href]")

            for (el in elements) {
            val text = el.ownText().trim()
            val fullText = el.text().trim()

            if (el.tagName().startsWith("h") || el.tagName() in listOf("p", "strong", "b") || el.children().isEmpty()) {
                val checkText = text.ifBlank { fullText }
                if (!checkText.contains("Single Episode", ignoreCase = true) && !checkText.contains("All Episodes", ignoreCase = true)) {
                    val epMatch = EPISODE_NUM_REGEX.find(checkText)
                    if (epMatch != null) {
                        val num = epMatch.groupValues[1].toIntOrNull()
                        if (num != null && num in 1..999) {
                            currentEpisode = num
                        }
                    }
                }
            }

            if (el.tagName() == "a" && currentEpisode != null) {
                val url = el.attr("abs:href").ifBlank { el.attr("href") }
                if (url.isNotBlank() && !sourceParser.shouldSkipUrl(url)) {
                    var hostType = HostDetector.detect(url)
                    if (hostType == HostType.UNKNOWN && url.contains("?id=")) hostType = HostType.REDIRECT
                    
                    if (hostType != HostType.UNKNOWN) {
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
                        epLinksMap.getOrPut(currentEpisode) { mutableListOf() }.add(source)
                    }
                }
            }
        }

        if (epLinksMap.isEmpty()) {
            val allSources = sourceParser.parseDocument(document, detailUrl)
            val linkElements = document.select("a[href]").associateBy { it.attr("href").trim() }
            for (source in allSources) {
                val epNum = findEpisodeNumber(source, linkElements)
                if (epNum != null) {
                    epLinksMap.getOrPut(epNum) { mutableListOf() }.add(source)
                }
            }
        }

        if (epLinksMap.isEmpty()) return emptyList()

        val episodes = epLinksMap.toSortedMap().map { (epNum, sources) ->
            ProviderEpisode(
                number = epNum,
                title = "Episode $epNum",
                sources = sources.distinctBy { it.url }
            )
        }

        return listOf(ProviderSeason(number = seasonNum, title = "Season $seasonNum", episodes = episodes))
    }

    private fun findEpisodeNumber(source: ProviderSource, linkElements: Map<String, Element>): Int? {
        val element = linkElements[source.url] ?: return null
        EPISODE_NUM_REGEX.find(element.text())?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        element.parent()?.let { parent ->
            EPISODE_NUM_REGEX.find(parent.text())?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        }

        var sibling: Element? = element.parent()?.previousElementSibling()
        var hops = 0
        while (sibling != null && hops < 8) {
            val tag = sibling.tagName()
            if (tag in listOf("h2", "h3", "h4", "p", "strong", "b")) {
                val text = sibling.text()
                if (!text.contains("Single Episode", ignoreCase = true) && !text.contains("All Episodes", ignoreCase = true)) {
                    EPISODE_NUM_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
                }
            }
            sibling = sibling.previousElementSibling()
            hops++
        }
        EPISODE_NUM_REGEX.find(source.url)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return null
    }

    /**
     * Extract season number from URL. E.g.
     * "stranger-things-season-2-..." → 2
     * "stranger-things-s03-..." → 3
     * Defaults to 1 if not found.
     */
    private fun extractSeasonNumber(url: String): Int {
        return SEASON_URL_REGEX.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    // ─── Metadata Helpers ─────────────────────────────────────────────────────

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

        // 1. Explicit Movie check: If it contains "full movie" or "movie" and does NOT mention "season" / "series" / "all-episodes", it is a Movie
        if ((combined.contains("full movie") || combined.contains("full-movie") || combined.contains("movie")) &&
            !combined.contains("season") && !combined.contains("series") && !combined.contains("all-episodes")
        ) {
            return false
        }

        // 2. TV Series signals from URL and Title (matching SkyStream inferIsSeries regex)
        if (combined.contains("all-episodes") ||
            combined.contains("web-series") ||
            combined.contains("tv-series") ||
            combined.contains("/series/") ||
            combined.contains("/web-series/") ||
            Regex("""season[ _-]*\d+""", RegexOption.IGNORE_CASE).containsMatchIn(combined)
        ) {
            return true
        }

        // 3. Title-only episode patterns (e.g. "Episode 1", "EP01")
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