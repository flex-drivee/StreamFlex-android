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
 * HDHub4U Detail Parser implementation (Phase 1.3 & Phase 1.7).
 *
 * Implements [DetailParser]:
 * - Scrapes movie/show metadata and returns canonical [ProviderResult]
 *   (with movie sources OR TV seasons/episodes).
 *
 * Consistent with Phase 1.7 roadmap rules:
 *   - Does NOT implement redirect unwrapping (e.g. WP ?id= decoding).
 *   - Does NOT resolve iframes or run extractors.
 *   - ResolverEngine & ExtractorManager own the entire resolution pipeline.
 */
class HDHubDetails : DetailParser {

    companion object {
        private const val PROVIDER_ID = "hdhub4u"
        private const val PROVIDER_NAME = "HDHub4u"
        private const val TAG = "HDHubDetails"
    }

    private val sourceParser = HDHubSourceParser()

    /**
     * Load detail page via network call and delegate to [parse].
     */
    suspend fun load(
        result: SearchResult
    ): ProviderResult {

        val pageUrl = normalizeUrl(result.url)
        StreamLogger.info(TAG, "Loading detail page: $pageUrl (${result.mediaType})")

        val request = RequestBuilder()
            .url(pageUrl)
            .header("Referer", HDHubConfig.DEFAULT_DOMAIN)
            .header("Cookie", HDHubConfig.COOKIE)
            .build()

        return when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val html = response.data.bodyAsString()
                StreamLogger.debug(TAG, "Downloaded HTML (${html.length} chars)")

                val transport = TransportResult.HtmlResponse(
                    html = html,
                    url = pageUrl
                )
                val parsed = parse(transport, pageUrl)

                val newTitle = if (parsed.title.isNotBlank() && parsed.title != pageUrl) parsed.title else result.title
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

            return HDHubMapper.toProviderResult(
                providerId = PROVIDER_ID,
                title = title,
                detailUrl = detailUrl,
                seasons = seasons,
                mediaType = MediaType.TV,
                poster = poster,
                overview = plot
            )
        }
    }

    // ─── TV Shows (Season & Episode) Parsing (inspired by CloudStream reference) ───

    private fun parseSeasons(
        document: Document,
        detailUrl: String
    ): List<ProviderSeason> {
        val epLinksMap = mutableMapOf<Int, MutableList<String>>()
        val episodeRegex = Regex("EPiSODE\\s*(\\d+)", RegexOption.IGNORE_CASE)

        document.select("h3, h4").forEach { element ->
            val epNum = episodeRegex.find(element.text())?.groupValues?.get(1)?.toIntOrNull()
            if (epNum != null) {
                val baseLinks = element.select("a[href]").mapNotNull {
                    it.attr("href").takeIf { u -> u.isNotBlank() && !sourceParser.shouldSkipUrl(u) }
                }
                val allLinks = mutableSetOf<String>()
                allLinks.addAll(baseLinks)

                if (element.tagName() == "h4" || element.tagName() == "h3") {
                    var nextElement: Element? = element.nextElementSibling()
                    while (nextElement != null && nextElement.tagName() != "hr" && nextElement.tagName() != "h3" && nextElement.tagName() != "h4") {
                        val siblingLinks = nextElement.select("a[href]").mapNotNull {
                            it.attr("href").takeIf { u -> u.isNotBlank() && !sourceParser.shouldSkipUrl(u) }
                        }
                        allLinks.addAll(siblingLinks)
                        nextElement = nextElement.nextElementSibling()
                    }
                }

                if (allLinks.isNotEmpty()) {
                    epLinksMap.getOrPut(epNum) { mutableListOf() }.addAll(allLinks.distinct())
                }
            }
        }

        if (epLinksMap.isEmpty()) return emptyList()

        val episodes = epLinksMap.toSortedMap().map { (epNum, links) ->
            val epSources = links.mapNotNull { url ->
                var hostType = HostDetector.detect(url)
                if (hostType == HostType.UNKNOWN && url.contains("?id=")) {
                    hostType = HostType.REDIRECT
                }
                if (hostType == HostType.UNKNOWN) null
                else {
                    HDHubMapper.toProviderSource(
                        provider = PROVIDER_NAME,
                        host = hostType.name,
                        hostType = hostType,
                        url = url,
                        quality = Quality.UNKNOWN,
                        referer = detailUrl
                    )
                }
            }
            ProviderEpisode(
                number = epNum,
                title = "Episode $epNum",
                sources = epSources
            )
        }

        return listOf(
            ProviderSeason(
                number = 1,
                title = "Season 1",
                episodes = episodes
            )
        )
    }

    // ─── Metadata Helpers ─────────────────────────────────────────────────────

    private fun extractTitle(document: Document, fallback: String): String {
        val titleEl = document.selectFirst(
            ".page-body h2[data-ved], h2[data-ved], h1.page-title span, h1.page-title"
        )
        val text = titleEl?.text()?.trim()
        return if (!text.isNullOrBlank()) text else fallback
    }

    private fun extractPoster(document: Document): String? {
        return document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: document.selectFirst("main.page-body img.aligncenter")?.attr("src")
    }

    private fun extractOverview(document: Document): String? {
        return document.selectFirst(".kno-rdesc, .page-body p, p")?.text()?.trim()
    }

    private fun isTvSeries(document: Document, detailUrl: String): Boolean {
        val titleText = document.selectFirst("h1.page-title span, h1.page-title")?.text() ?: ""
        if (titleText.contains("series", ignoreCase = true) || titleText.contains("season", ignoreCase = true)) {
            return true
        }
        if (detailUrl.contains("/series/") || detailUrl.contains("/web-series/") || detailUrl.contains("season")) {
            return true
        }
        return document.select("h3, h4").any { el ->
            el.text().contains("EPiSODE", ignoreCase = true)
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("http")) url
        else HDHubConfig.DEFAULT_DOMAIN.trimEnd('/') + "/" + url.trimStart('/')
    }
}