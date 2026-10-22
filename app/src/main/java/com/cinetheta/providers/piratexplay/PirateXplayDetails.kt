package com.cinetheta.providers.piratexplay

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderEpisode
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.ProviderSeason
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document

class PirateXplayDetails {

    companion object {
        private const val TAG = "PirateXplayDetails"
    }

    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult? = withContext(Dispatchers.IO) {
        val detailHtml = fetchHtml(result.url, baseUrl) ?: return@withContext null
        val doc = HtmlParser.parse(detailHtml, baseUrl)

        // Check if there are season tabs/buttons (multi-season show)
        val seasonButtons = doc.select(".season-swiper a.season-btn, a.season-btn")
        val seasonLinks = mutableListOf<Pair<Int, String>>()

        for (btn in seasonButtons) {
            var sUrl = btn.attr("abs:href").takeIf { !it.isNullOrBlank() } ?: btn.attr("href") ?: ""
            if (sUrl.isBlank()) continue
            if (sUrl.startsWith("/")) {
                sUrl = "$baseUrl$sUrl"
            } else if (!sUrl.startsWith("http")) {
                sUrl = "$baseUrl/$sUrl"
            }

            val sNum = btn.attr("data-season").toIntOrNull()
                ?: Regex("(?i)season[-_\\s]*(\\d+)").find(sUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Regex("(?i)season[-_\\s]*(\\d+)").find(btn.text())?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: (seasonLinks.size + 1)

            if (seasonLinks.none { it.first == sNum }) {
                seasonLinks.add(sNum to sUrl)
            }
        }

        // Fetch documents for each season
        val seasonDocs: List<Pair<Int, Document>> = if (seasonLinks.isNotEmpty()) {
            coroutineScope {
                seasonLinks.map { (sNum, sUrl) ->
                    async {
                        val sDoc = if (normalizeUrl(sUrl) == normalizeUrl(result.url)) {
                            doc
                        } else {
                            fetchHtml(sUrl, baseUrl)?.let { HtmlParser.parse(it, baseUrl) }
                        }
                        if (sDoc != null) sNum to sDoc else null
                    }
                }.awaitAll().filterNotNull()
            }
        } else {
            listOf(1 to doc)
        }

        val seasonMap = mutableMapOf<Int, MutableList<ProviderEpisode>>()

        for ((defaultSeason, sDoc) in seasonDocs) {
            // Select only actual episode articles (exclude recommendation carousels / related series)
            val articles = sDoc.select("#episode_by_temp article, ul.post-lst article, article.episodes")

            for (article in articles) {
                // Strictly exclude recommendation/related carousels
                if (article.parents().any { it.hasClass("owl-carousel") || it.hasClass("related") }) {
                    continue
                }

                val aElem = article.selectFirst("a.lnk-blk") ?: article.selectFirst("a[href*='/episode/']")
                var epUrl = aElem?.attr("abs:href").takeIf { !it.isNullOrBlank() } ?: aElem?.attr("href") ?: ""
                if (epUrl.isBlank()) continue
                if (epUrl.startsWith("/")) {
                    epUrl = "$baseUrl$epUrl"
                } else if (!epUrl.startsWith("http")) {
                    epUrl = "$baseUrl/$epUrl"
                }

                val numElem = article.selectFirst(".num-epi")
                // A true episode MUST have .num-epi or contain /episode/ in its URL
                if (numElem == null && !epUrl.contains("/episode/")) {
                    continue
                }

                val numStr = numElem?.text()?.trim().orEmpty()
                val split = numStr.split("x", "X")
                val parsedSeason = split.getOrNull(0)?.trim()?.toIntOrNull()
                val seasonNum = parsedSeason ?: defaultSeason

                val epNum = split.getOrNull(1)?.trim()?.toIntOrNull()
                    ?: Regex("(?i)[-/](\\d+)x(\\d+)").find(epUrl)?.groupValues?.getOrNull(2)?.toIntOrNull()
                    ?: Regex("(?i)episode[-_](\\d+)").find(epUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: ((seasonMap[seasonNum]?.size ?: 0) + 1)

                val rawTitle = article.selectFirst(".entry-title")?.text()?.trim().orEmpty()
                val title = if (rawTitle.isNotBlank()) rawTitle else "Episode $epNum"
                val thumb = article.selectFirst(".post-thumbnail img")?.let { img ->
                    img.attr("src").takeIf { it.isNotBlank() } ?: img.attr("data-src")
                }

                val source = ProviderSource(
                    provider = PirateXplayConfig.PROVIDER_NAME,
                    host = "PirateXplay",
                    hostType = HostType.PIRATEXPLAY,
                    url = epUrl,
                    referer = baseUrl,
                    metadata = mapOf("baseUrl" to baseUrl)
                )

                seasonMap.getOrPut(seasonNum) { mutableListOf() }.add(
                    ProviderEpisode(
                        number = epNum,
                        title = title,
                        thumbnail = thumb,
                        sources = listOf(source)
                    )
                )
            }
        }

        val isTV = seasonMap.isNotEmpty() || result.mediaType == MediaType.TV

        if (isTV && seasonMap.isNotEmpty()) {
            val seasons = seasonMap.entries.sortedBy { it.key }.map { (sNum, eps) ->
                ProviderSeason(
                    number = sNum,
                    title = "Season $sNum",
                    episodes = eps.distinctBy { it.number }.sortedBy { it.number }
                )
            }

            ProviderResult(
                id = result.id,
                providerId = PirateXplayConfig.PROVIDER_ID,
                title = result.title,
                detailUrl = result.url,
                mediaType = MediaType.TV,
                poster = result.poster,
                seasons = seasons,
                success = true
            )
        } else {
            // Movie
            val source = ProviderSource(
                provider = PirateXplayConfig.PROVIDER_NAME,
                host = "PirateXplay",
                hostType = HostType.PIRATEXPLAY,
                url = result.url,
                referer = baseUrl,
                metadata = mapOf("baseUrl" to baseUrl)
            )

            ProviderResult(
                id = result.id,
                providerId = PirateXplayConfig.PROVIDER_ID,
                title = result.title,
                detailUrl = result.url,
                mediaType = MediaType.MOVIE,
                poster = result.poster,
                sources = listOf(source),
                success = true
            )
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().trimEnd('/')
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }
}
