package com.streamflex.providers.moviebox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.SearchResult
import org.json.JSONObject

class MovieBoxDetails {

    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult? {
        val detailUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=${result.id}"
        
        val headers = MovieBoxCrypto.getHeaders(
            method = "GET",
            url = detailUrl,
            body = null
        )

        val request = RequestBuilder()
            .url(detailUrl)
            .get()
            .headers(headers)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val json = response.data.bodyAsString()
                    val root = JsonParser.parse(json) ?: return@withContext null
                    val data = JsonParser.objectOf(root, "data") ?: return@withContext null

                    val title = JsonParser.string(data, "title") ?: result.title
                    val overview = JsonParser.string(data, "description")
                    
                    val coverObj = JsonParser.objectOf(data, "cover")
                    val poster = coverObj?.let { JsonParser.string(it, "url") }

                    if (result.mediaType == MediaType.MOVIE) {
                        val source = MovieBoxMapper.toProviderSource(
                            url = "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=${result.id}"
                        )
                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title = title,
                            detailUrl = result.url,
                            mediaType = MediaType.MOVIE,
                            sources = listOf(source),
                            overview = overview,
                            poster = poster
                        )
                    } else {
                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title = title,
                            detailUrl = result.url,
                            mediaType = MediaType.TV,
                            seasons = parseSeasons(data, "$baseUrl/wefeed-mobile-bff/subject-api/play-info", result.id),
                            overview = overview,
                            poster = poster
                        )
                    }
                }
                else -> null
            }
        }
    }
    
    private fun parseSeasons(data: com.google.gson.JsonElement, basePlayUrl: String, subjectId: String): List<ProviderSeason> {
        val seasons = mutableListOf<ProviderSeason>()
        val episodeList = JsonParser.array(data, "episodeMap") // Could also check "episodes"
        
        if (episodeList.size > 0) {
            val episodes = mutableListOf<ProviderEpisode>()
            for (ep in episodeList) {
                val epNum = JsonParser.int(ep, "episode") ?: continue
                val epTitle = JsonParser.string(ep, "title") ?: "Episode $epNum"
                episodes.add(
                    ProviderEpisode(
                        number = epNum,
                        title = epTitle,
                        sources = listOf(MovieBoxMapper.toProviderSource(url = "$basePlayUrl?subjectId=$subjectId&episode=$epNum"))
                    )
                )
            }
            seasons.add(ProviderSeason(number = 1, title = "Season 1", episodes = episodes))
        }
        return seasons
    }
}
