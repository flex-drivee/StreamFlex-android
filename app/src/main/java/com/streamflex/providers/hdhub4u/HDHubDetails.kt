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
        // Map<SeasonNumber, Map<EpisodeNumber, MutableList<ProviderSource>>>
        val seasonMap = mutableMapOf<Int, MutableMap<Int, MutableList<ProviderSource>>>()
        val visitedUrls = mutableSetOf<String>()

        // Scan all anchor links across the entire document
        val allAnchors = document.select("a[href]")
        for (a in allAnchors) {
            val href = a.absUrl("href").takeIf { it.isNotBlank() } ?: a.attr("href")
            if (href.isBlank() || sourceParser.shouldSkipUrl(href) || !visitedUrls.add(href)) continue

            var hostType = HostDetector.detect(href)
            if (hostType == HostType.UNKNOWN && href.contains("?id=")) hostType = HostType.REDIRECT
            if (hostType == HostType.UNKNOWN) continue

            val anchorText = a.text().trim()
            val anchorTitle = a.attr("title").trim()

            // Find episode number
            val epNum = findEpisodeForAnchor(a, href, anchorText, anchorTitle)

            if (epNum != null && epNum in 1..999) {
                // Find season number
                val seasonNum = findSeasonForAnchor(a, href, anchorText, baseSeasonNum)

                // Detect quality from anchor, parent, and container
                val contextText = buildString {
                    append(anchorText)
                    append(" ")
                    append(anchorTitle)
                    append(" ")
                    a.parent()?.let { append(it.text()) }
                    a.closest("div, p, tr, li")?.let { append(" ").append(it.text()) }
                }
                val quality = QualityDetector.detect(contextText)

                val source = HDHubMapper.toProviderSource(
                    provider = PROVIDER_NAME,
                    host = hostType.name,
                    hostType = hostType,
                    url = href,
                    quality = quality,
                    referer = detailUrl,
                    headers = mapOf("Referer" to detailUrl, "Cookie" to HDHubConfig.COOKIE)
                )

                seasonMap.getOrPut(seasonNum) { mutableMapOf() }
                    .getOrPut(epNum) { mutableListOf() }
                    .add(source)
            }
        }

        if (seasonMap.isEmpty()) return emptyList()

        return seasonMap.entries.sortedBy { it.key }.map { (sNum, epMap) ->
            val episodes = epMap.entries.sortedBy { it.key }.map { (num, sources) ->
                ProviderEpisode(
                    number = num,
                    title = "Episode $num",
                    sources = sources.distinctBy { it.url }
                )
            }
            ProviderSeason(
                number = sNum,
                title = "Season $sNum",
                episodes = episodes
            )
        }
    }

    private fun findEpisodeForAnchor(
        a: Element,
        href: String,
        anchorText: String,
        anchorTitle: String
    ): Int? {
        // 1. Direct match in anchor text, title, or href
        extractEpNumber(anchorText)?.let { return it }
        extractEpNumber(anchorTitle)?.let { return it }
        extractEpNumber(href)?.let { return it }

        // 2. Check previous siblings in the DOM (e.g. <strong>Episode 03 : </strong> or text node)
        var prev = a.previousSibling()
        var hops = 0
        while (prev != null && hops < 8) {
            val text = when (prev) {
                is org.jsoup.nodes.TextNode -> prev.text()
                is Element -> prev.text()
                else -> ""
            }.trim()

            if (text.isNotBlank()) {
                val ep = extractEpNumber(text)
                if (ep != null) return ep
            }
            prev = prev.previousSibling()
            hops++
        }

        // 3. Check enclosing container / badge (e.g. div.episode-download-item span.badge)
        val parentItem = a.closest("div.episode-download-item, div.episode-item, li, tr")
        if (parentItem != null) {
            val badgeText = parentItem.select("span.badge-psa, span.badge, .episode-file-info, .ep-title").text()
            extractEpNumber(badgeText)?.let { return it }
            extractEpNumber(parentItem.ownText())?.let { return it }
        }

        // 4. Check parent element text if it's a single episode line
        val parent = a.parent()
        if (parent != null) {
            val epMatches = Regex("""\b(?:ep(?:isode)?[.\s_-]?|e)\s*0*(\d{1,3})\b""", RegexOption.IGNORE_CASE)
                .findAll(parent.text()).toList()
            if (epMatches.size == 1) {
                return epMatches[0].groupValues[1].toIntOrNull()
            }
        }

        return null
    }

    private fun findSeasonForAnchor(
        a: Element,
        href: String,
        anchorText: String,
        fallbackSeason: Int
    ): Int {
        // 1. Direct match in anchor text or href
        val directS = extractSeasonNumber(anchorText).takeIf { it > 0 }
            ?: extractSeasonNumber(href).takeIf { it > 0 }
        if (directS != null) return directS

        // 2. Check previous sibling headings
        var prevEl = a.previousElementSibling()
        while (prevEl != null) {
            if (prevEl.tagName().startsWith("h")) {
                val s = extractSeasonNumber(prevEl.text())
                if (s > 0) return s
            }
            prevEl = prevEl.previousElementSibling()
        }

        // 3. Walk up the DOM to find closest preceding heading
        var parent = a.parent()
        while (parent != null && parent.tagName() != "body") {
            var sibling = parent.previousElementSibling()
            while (sibling != null) {
                if (sibling.tagName().startsWith("h")) {
                    val s = extractSeasonNumber(sibling.text())
                    if (s > 0) return s
                }
                sibling = sibling.previousElementSibling()
            }
            parent = parent.parent()
        }

        return fallbackSeason
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