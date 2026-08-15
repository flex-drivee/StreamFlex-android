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
import com.streamflex.core.logger.Logger

private const val TAG = "MovieBoxDetails"

class MovieBoxDetails {

    /**
     * Load full details for a [SearchResult], returning a [ProviderResult].
     * - Movies: single play-info URL
     * - TV Shows: calls season-info to enumerate seasons/episodes, then constructs
     *   per-episode play-info URLs using `&se=<season>&ep=<episode>` params.
     */
    suspend fun load(result: SearchResult, baseUrl: String): ProviderResult? {
        val detailUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=${result.id}"

        val headers = MovieBoxCrypto.getHeaders(
            method = "GET",
            url    = detailUrl
        )

        val request = RequestBuilder()
            .url(detailUrl)
            .get()
            .headers(headers)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    // Persist any updated x-user token from the response
                    response.data.header("x-user")?.let {
                        val token = parseToken(it)
                        if (token != null) MovieBoxCrypto.xUserToken = token
                    }

                    val json = response.data.bodyAsString()
                    val root = JsonParser.parse(json) ?: return@withContext null
                    val data = JsonParser.objectOf(root, "data") ?: return@withContext null

                    val title       = JsonParser.string(data, "title")       ?: result.title
                    val overview    = JsonParser.string(data, "description")
                    val subjectType = JsonParser.int(data, "subjectType")    ?: 1

                    val coverObj = JsonParser.objectOf(data, "cover")
                    val poster   = coverObj?.let { JsonParser.string(it, "url") }

                    val isTV = subjectType == 2 || result.mediaType == MediaType.TV

                    if (!isTV) {
                        // ── Movie ──────────────────────────────────────────────────────────
                        val playUrl = "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=${result.id}"
                        val source  = MovieBoxMapper.toProviderSource(url = playUrl)
                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title      = title,
                            detailUrl  = result.url,
                            mediaType  = MediaType.MOVIE,
                            sources    = listOf(source),
                            overview   = overview,
                            poster     = poster
                        )
                    } else {
                        // ── TV Show ────────────────────────────────────────────────────────
                        val seasons = fetchSeasons(
                            subjectId   = result.id,
                            baseUrl     = baseUrl,
                            basePlayUrl = "$baseUrl/wefeed-mobile-bff/subject-api/play-info"
                        )
                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title      = title,
                            detailUrl  = result.url,
                            mediaType  = MediaType.TV,
                            seasons    = seasons,
                            overview   = overview,
                            poster     = poster
                        )
                    }
                }
                else -> {
                    Logger.e("[${MovieBoxConfig.PROVIDER_NAME}] Detail request failed for id=${result.id}", TAG)
                    null
                }
            }
        }
    }

    /**
     * Calls the `season-info` endpoint and maps each season/episode into [ProviderSeason].
     * Episode play-info URLs use the `&se=<se>&ep=<ep>` query params.
     */
    private suspend fun fetchSeasons(
        subjectId:   String,
        baseUrl:     String,
        basePlayUrl: String
    ): List<ProviderSeason> {
        val seasonUrl = "$baseUrl/wefeed-mobile-bff/subject-api/season-info?subjectId=$subjectId"

        val headers = MovieBoxCrypto.getHeaders(
            method = "GET",
            url    = seasonUrl
        )
        val request = RequestBuilder()
            .url(seasonUrl)
            .get()
            .headers(headers)
            .build()

        return withContext(Dispatchers.IO) {
            when (val resp = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    // Persist token
                    resp.data.header("x-user")?.let { xu ->
                        parseToken(xu)?.let { MovieBoxCrypto.xUserToken = it }
                    }

                    val json  = resp.data.bodyAsString()
                    val root  = JsonParser.parse(json) ?: return@withContext emptyList()
                    val data  = JsonParser.objectOf(root, "data") ?: return@withContext emptyList()
                    val seasonsArray = JsonParser.array(data, "seasons")

                    if (seasonsArray.isEmpty()) {
                        Logger.w("[${MovieBoxConfig.PROVIDER_NAME}] season-info returned empty seasons for id=$subjectId", TAG)
                        return@withContext emptyList()
                    }

                    seasonsArray.mapNotNull { seasonEl ->
                        val se    = JsonParser.int(seasonEl, "se")    ?: return@mapNotNull null
                        val maxEp = JsonParser.int(seasonEl, "maxEp") ?: 0

                        if (maxEp <= 0) return@mapNotNull null

                        val episodes = (1..maxEp).map { ep ->
                            val episodePlayUrl = "$basePlayUrl?subjectId=$subjectId&se=$se&ep=$ep"
                            ProviderEpisode(
                                number  = ep,
                                title   = "Episode $ep",
                                sources = listOf(MovieBoxMapper.toProviderSource(url = episodePlayUrl))
                            )
                        }

                        ProviderSeason(
                            number   = se,
                            title    = "Season $se",
                            episodes = episodes
                        )
                    }
                }
                else -> {
                    Logger.e("[${MovieBoxConfig.PROVIDER_NAME}] season-info request failed for id=$subjectId", TAG)
                    emptyList()
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseToken(xUserHeader: String): String? {
        if (xUserHeader.isBlank()) return null
        return try {
            val root = JsonParser.parse(xUserHeader)
            JsonParser.string(root, "token")
        } catch (_: Exception) {
            null
        }
    }
}
