package com.streamflex.providers.fourkhdhub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.SearchResult
import org.jsoup.nodes.Document

class FourKHDHubDetails {

    companion object {
        private const val PROVIDER_NAME = "4KHDHub"
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
                    
                    val title = result.title
                    val isTv = result.mediaType == com.streamflex.domain.models.MediaType.TV
                    
                    if (isTv) {
                        val seasons = parseSeasons(document, result.url)
                        ProviderResult(
                            id = result.id,
                            providerId = FourKHDHubConfig.PROVIDER_ID,
                            title = title,
                            detailUrl = result.url,
                            mediaType = com.streamflex.domain.models.MediaType.TV,
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
                            mediaType = com.streamflex.domain.models.MediaType.MOVIE,
                            poster = result.poster,
                            sources = sources
                        )
                    }
                }
                else -> null
            }
        }
    }

    private fun parseSeasons(document: Document, detailUrl: String): List<ProviderSeason> {
        val sources = sourceParser.parseDocument(document, detailUrl)
            .sortedBy { it.metadata["codec"] == "HEVC" }
        
        if (sources.isEmpty()) return emptyList()
        
        val ep = ProviderEpisode(
            number = 1,
            title = "Episode 1",
            sources = sources
        )
        return listOf(ProviderSeason(number = 1, title = "Season 1", episodes = listOf(ep)))
    }
}
