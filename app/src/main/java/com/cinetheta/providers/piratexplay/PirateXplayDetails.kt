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
import kotlinx.coroutines.withContext

class PirateXplayDetails {

    companion object {
        private const val TAG = "PirateXplayDetails"
    }

    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult? = withContext(Dispatchers.IO) {
        val isTV = result.mediaType == MediaType.TV
        
        if (isTV) {
            val detailHtml = fetchHtml(result.url, baseUrl) ?: return@withContext null
            val doc = HtmlParser.parse(detailHtml, baseUrl)
            
            val articles = doc.select("section.episodes article.episodes")
            
            if (articles.isEmpty()) {
                Logger.w("[$TAG] No episode articles found for ${result.url}")
                return@withContext null
            }
            
            val seasonMap = mutableMapOf<Int, MutableList<ProviderEpisode>>()
            
            for (article in articles) {
                val aElem = article.selectFirst("a.lnk-blk")
                var epUrl = aElem?.attr("abs:href").takeIf { !it.isNullOrBlank() } ?: aElem?.attr("href") ?: ""
                
                if (epUrl.isBlank()) continue
                if (epUrl.startsWith("/")) {
                    epUrl = "$baseUrl$epUrl"
                } else if (!epUrl.startsWith("http")) {
                    epUrl = "$baseUrl/$epUrl"
                }
                
                val numStr = article.selectFirst(".num-epi")?.text()?.trim() ?: ""
                val split = numStr.split("x")
                val seasonNum = split.getOrNull(0)?.toIntOrNull() ?: 1
                val epNum = split.getOrNull(1)?.toIntOrNull() ?: 1
                
                val title = article.selectFirst(".entry-title")?.text()?.trim() ?: "Episode $epNum"
                val thumb = article.selectFirst(".post-thumbnail img")?.attr("src")
                
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
            
            val seasons = seasonMap.entries.sortedBy { it.key }.map { (sNum, eps) ->
                ProviderSeason(
                    number = sNum,
                    title = "Season $sNum",
                    episodes = eps.sortedBy { it.number }
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
