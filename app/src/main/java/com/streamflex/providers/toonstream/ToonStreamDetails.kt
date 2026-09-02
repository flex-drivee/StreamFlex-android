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
            val episodeLinks = detailDoc
                .select("a[href]")
                .map { 
                    it.attr("abs:href").takeIf { abs -> abs.isNotBlank() } ?: run {
                        val raw = it.attr("href")
                        if (raw.startsWith("http")) raw else "$baseUrl/${raw.trimStart('/')}"
                    }
                }
                .filter { "episode" in it.lowercase() || "/ep-" in it.lowercase() }
                .distinct()

            val episodes = episodeLinks.mapIndexed { index, epUrl ->
                val epNum = index + 1
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

            val season = ProviderSeason(
                number   = 1,
                title    = "Season 1",
                episodes = episodes
            )

            ProviderResult(
                id         = result.id,
                providerId = ToonStreamConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.TV,
                poster     = result.poster,
                seasons    = listOf(season)
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
