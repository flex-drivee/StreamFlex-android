package com.streamflex.providers.fourkhdhub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
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

/**
 * 4KHDHub Detail Parser (with TV Series and Multi-Episode support).
 */
class FourKHDHubDetails {

    companion object {
        private const val PROVIDER_NAME = "4KHDHub"
        private const val TAG = "FourKHDHubDetails"

        private val EPISODE_NUM_REGEX = Regex(
            """(?:ep(?:isode)?[.\s_-]?)(\d{1,3})""",
            RegexOption.IGNORE_CASE
        )
        private val SEASON_NUM_REGEX = Regex(
            """S?([1-9][0-9]*)""",
            RegexOption.IGNORE_CASE
        )
    }

    private val sourceParser = FourKHDHubSourceParser()

    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult? {
        val request = RequestBuilder()
            .url(result.url)
            .header("Referer", baseUrl)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val htmlString = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext null
                    
                    val rawTransport = TransportResult.TextResponse(
                        text = htmlString,
                        url = request.url
                    )

                    val document = HtmlParser.parse(rawTransport.asString())
                    
                    val title = extractTitle(document, result.title)
                    val isTv = result.mediaType == MediaType.TV || isTvSeries(document, result.url)
                    
                    if (isTv) {
                        val seasons = parseSeasons(document, result.url)
                        ProviderResult(
                            id = result.id,
                            providerId = FourKHDHubConfig.PROVIDER_ID,
                            title = title,
                            detailUrl = result.url,
                            mediaType = MediaType.TV,
                            poster = result.poster,
                            seasons = seasons
                        )
                    } else {
                        val sources = sourceParser.parseDocument(document, result.url)
                            .sortedBy { it.metadata["codec"] == "HEVC" }
                        ProviderResult(
                            id = result.id,
                            providerId = FourKHDHubConfig.PROVIDER_ID,
                            title = title,
                            detailUrl = result.url,
                            mediaType = MediaType.MOVIE,
                            poster = result.poster,
                            sources = sources
                        )
                    }
                }
                else -> null
            }
        }
    }

    private fun extractTitle(document: Document, fallback: String): String {
        val pageTitle = document.selectFirst("h1.page-title, h1.entry-title, h1")?.text()?.trim()
        if (!pageTitle.isNullOrBlank()) {
            return pageTitle.substringBefore("(").trim().ifBlank { pageTitle }
        }
        return fallback
    }

    private fun isTvSeries(document: Document, url: String): Boolean {
        val tags = document.select("div.mt-2 span.badge, .badge").map { it.text().lowercase() }
        if (tags.any { it.contains("series") || it.contains("tv") || it.contains("episode") }) return true
        if (url.contains("series", ignoreCase = true) || url.contains("season", ignoreCase = true)) return true
        return document.select("div.episodes-list, div.season-item, div.episode-download-item").isNotEmpty()
    }

    private fun parseSeasons(document: Document, detailUrl: String): List<ProviderSeason> {
        val seasonsMap = mutableMapOf<Int, MutableMap<Int, MutableList<ProviderSource>>>()

        // 1. Check for Modern 4KHDHub Grid (div.episodes-list div.season-item)
        val seasonElements = document.select("div.episodes-list div.season-item")
        if (seasonElements.isNotEmpty()) {
            for (seasonEl in seasonElements) {
                val seasonText = seasonEl.select("div.episode-number").text()
                val seasonNum = SEASON_NUM_REGEX.find(seasonText)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val epMap = seasonsMap.getOrPut(seasonNum) { mutableMapOf() }

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
                        if (hostType == HostType.UNKNOWN && href.contains("drive", ignoreCase = true)) {
                            hostType = HostType.REDIRECT
                        }
                        if (hostType == HostType.UNKNOWN && href.contains("?id=")) {
                            hostType = HostType.REDIRECT
                        }
                        if (hostType == HostType.UNKNOWN) continue

                        val quality = QualityDetector.detect(link.text() + " " + epItem.text())
                        val source = FourKHDHubMapper.toProviderSource(
                            provider = PROVIDER_NAME,
                            host = hostType.name,
                            hostType = hostType,
                            url = href,
                            quality = quality,
                            referer = detailUrl,
                            headers = mapOf("Referer" to detailUrl)
                        )
                        epMap.getOrPut(epNum) { mutableListOf() }.add(source)
                    }
                }
            }
        }

        // 2. Fallback: Parse accordions or general download-items if grid wasn't present
        if (seasonsMap.isEmpty()) {
            val allSources = sourceParser.parseDocument(document, detailUrl)
            if (allSources.isNotEmpty()) {
                val ep = ProviderEpisode(
                    number = 1,
                    title = "All Episodes",
                    sources = allSources.distinctBy { it.url }
                )
                return listOf(ProviderSeason(number = 1, title = "Season 1", episodes = listOf(ep)))
            }
            return emptyList()
        }

        return seasonsMap.toSortedMap().map { (seasonNum, episodes) ->
            val providerEpisodes = episodes.toSortedMap().map { (epNum, sources) ->
                ProviderEpisode(
                    number = epNum,
                    title = "Episode $epNum",
                    sources = sources.distinctBy { it.url }
                )
            }
            ProviderSeason(
                number = seasonNum,
                title = "Season $seasonNum",
                episodes = providerEpisodes
            )
        }
    }
}
