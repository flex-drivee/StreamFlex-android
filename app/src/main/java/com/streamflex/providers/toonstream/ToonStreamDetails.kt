package com.streamflex.providers.toonstream

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import kotlinx.coroutines.delay

class ToonStreamDetails {

    companion object {
        private const val TAG = "ToonStreamDetails"
    }

    suspend fun load(
        result  : SearchResult,
        baseUrl : String
    ): ProviderResult? = withContext(Dispatchers.IO) {

        val isTV = result.mediaType == MediaType.TV

        val detailHtml = fetchHtml(result.url, baseUrl) ?: return@withContext null
        val detailDoc  = HtmlParser.parse(detailHtml, baseUrl)

        if (isTV) {
            val seasonLinks = detailDoc.select("div.season-swiper-wrapper a[data-url]")
                .mapNotNull { it.attr("data-url").takeIf { u -> u.isNotBlank() } }
                .map { if (it.startsWith("http")) it else "$baseUrl/${it.trimStart('/')}" }
                .distinct()

            val allSeasons = if (seasonLinks.isNotEmpty()) {
                val fetchedSeasons = mutableListOf<ProviderSeason>()
                for ((sIndex, sUrl) in seasonLinks.withIndex()) {
                    val sNum = sIndex + 1
                    // Add a tiny delay to prevent triggering Toonstream's 502 rate limiter
                    if (sIndex > 0) delay(200) 
                    
                    val sHtml = fetchHtml(sUrl, baseUrl) ?: continue
                    val sDoc = HtmlParser.parse(sHtml, baseUrl)
                    fetchedSeasons.add(parseSeason(sDoc, sNum, baseUrl))
                }
                fetchedSeasons
            } else {
                // Single season fallback
                listOf(parseSeason(detailDoc, 1, baseUrl))
            }

            ProviderResult(
                id         = result.id,
                providerId = ToonStreamConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.TV,
                poster     = result.poster,
                seasons    = allSeasons
            )

        } else {
            val source = ToonStreamMapper.toProviderSource(
                iframeUrl = result.url,
                hostType  = HostType.TOONSTREAM,
                referer   = baseUrl,
                metadata  = mapOf("isMovie" to "true", "baseUrl" to baseUrl)
            )

            ProviderResult(
                id         = result.id,
                providerId = ToonStreamConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.MOVIE,
                poster     = result.poster,
                sources    = listOf(source)
            )
        }
    }

    private fun parseSeason(doc: Document, seasonNum: Int, baseUrl: String): ProviderSeason {
        val episodeLinks = doc.select("a[href]")
            .map { 
                it.attr("abs:href").takeIf { abs -> abs.isNotBlank() } ?: run {
                    val raw = it.attr("href")
                    if (raw.startsWith("http")) raw else "$baseUrl/${raw.trimStart('/')}"
                }
            }
            .filter { "episode" in it.lowercase() || "/ep-" in it.lowercase() }
            .distinct()

        val regexX = Regex("(\\d+)x(\\d+)")
        val regexEp = Regex("(?:ep|episode)-(\\d+)", RegexOption.IGNORE_CASE)
        
        val episodes = episodeLinks.mapIndexed { index, epUrl ->
            val matchX = regexX.find(epUrl)
            val matchEp = regexEp.find(epUrl)
            
            val parsedEpNum = matchX?.groupValues?.getOrNull(2)?.toIntOrNull()
                ?: matchEp?.groupValues?.getOrNull(1)?.toIntOrNull()
                
            val epNum = parsedEpNum ?: (index + 1)
            
            val source = ToonStreamMapper.toProviderSource(
                iframeUrl = epUrl,
                hostType  = HostType.TOONSTREAM,
                referer   = baseUrl,
                metadata  = mapOf("isMovie" to "false", "baseUrl" to baseUrl)
            )
            ProviderEpisode(
                number  = epNum,
                title   = "Episode $epNum",
                sources = listOf(source)
            )
        }
        
        return ProviderSeason(
            number = seasonNum,
            title = "Season $seasonNum",
            episodes = episodes
        )
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }
}
