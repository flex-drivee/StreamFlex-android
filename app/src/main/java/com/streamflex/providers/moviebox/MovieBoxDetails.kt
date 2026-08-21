package com.streamflex.providers.moviebox

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.SearchResult
import com.streamflex.core.logger.Logger

private const val TAG = "MovieBoxDetails"

class MovieBoxDetails {

    /**
     * Load full details for a [SearchResult], returning a [ProviderResult].
     * - Movies: single play-info URL (now fetches all related subjectIds for multi-audio)
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
                        // For movies, fetch alternative subjectIds to get all languages
                        val baseTitle = cleanTitle(title)
                        val currentLang = JsonParser.string(data, "language") ?: ""
                        val relatedIds = fetchRelatedMovieIds(baseTitle, result.id, currentLang, baseUrl)
                        
                        val sources = relatedIds.map { (id, lang) ->
                            val source = MovieBoxMapper.toProviderSource(url = "$baseUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$id")
                            source.copy(metadata = mapOf("language" to lang))
                        }
                        
                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title      = title,
                            detailUrl  = result.url,
                            mediaType  = MediaType.MOVIE,
                            sources    = sources,
                            overview   = overview,
                            poster     = poster
                        )
                    } else {
                        val baseTitle = cleanTitle(title)
                        val currentLang = JsonParser.string(data, "language") ?: ""
                        val relatedIds = fetchRelatedMovieIds(baseTitle, result.id, currentLang, baseUrl)

                        val allSeasons = mutableListOf<ProviderSeason>()
                        for ((id, lang) in relatedIds) {
                            val seasonsForLang = fetchSeasons(
                                subjectId   = id,
                                baseUrl     = baseUrl,
                                basePlayUrl = "$baseUrl/wefeed-mobile-bff/subject-api/play-info",
                                lang        = lang
                            )
                            allSeasons.addAll(seasonsForLang)
                        }

                        // Merge seasons and episodes
                        val mergedSeasons = allSeasons.groupBy { it.number }.map { (seasonNumber, seasons) ->
                            val mergedEpisodes = seasons.flatMap { it.episodes }
                                .groupBy { it.number }
                                .map { (episodeNumber, episodes) ->
                                    ProviderEpisode(
                                        number = episodeNumber,
                                        title = episodes.first().title,
                                        sources = episodes.flatMap { it.sources }
                                    )
                                }
                            
                            ProviderSeason(
                                number = seasonNumber,
                                title = seasons.first().title,
                                episodes = mergedEpisodes
                            )
                        }

                        MovieBoxMapper.toProviderResult(
                            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
                            title      = title,
                            detailUrl  = result.url,
                            mediaType  = MediaType.TV,
                            seasons    = mergedSeasons,
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
    
    
    private fun cleanTitle(title: String): String {
        var clean = title.replace(Regex("\\[.*?\\]"), "")
        clean = clean.replace(Regex("\\(.*?\\)"), "")
        clean = clean.replace(Regex("(?i)\\bS\\d+(?:-S\\d+)?\\b"), "")
        clean = clean.replace(Regex("(?i)\\bSeason\\s*\\d+\\b"), "")
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    private suspend fun fetchRelatedMovieIds(title: String, currentId: String, currentLang: String, baseUrl: String): List<Pair<String, String>> {
        val searchUrl = "$baseUrl/wefeed-mobile-bff/subject-api/search/v2"
        val payload = """{"keyword":"$title","page":1,"perPage":20}"""
        
        val headers = MovieBoxCrypto.getHeaders(
            method = "POST",
            url = searchUrl,
            body = payload
        ).toMutableMap()
        headers["Content-Type"] = "application/json"
        
        val request = RequestBuilder()
            .url(searchUrl)
            .post(payload.toByteArray())
            .headers(headers)
            .build()
            
        val ids = mutableListOf(Pair(currentId, currentLang))
        
        try {
            when (val resp = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val json = resp.data.bodyAsString()
                    val root = JsonParser.parse(json)
                    val data = root?.let { JsonParser.objectOf(it, "data") }
                    val results = data?.let { JsonParser.array(it, "results") }
                    
                    if (results != null) {
                        for (resultItem in results) {
                            val subjects = JsonParser.array(resultItem, "subjects") ?: continue
                            for (subject in subjects) {
                                val subjectId = JsonParser.string(subject, "subjectId") ?: continue
                                val subjectTitle = JsonParser.string(subject, "title") ?: ""
                                
                                val baseSubjectTitle = cleanTitle(subjectTitle)
                                val lang = JsonParser.string(subject, "language") ?: ""
                                if (baseSubjectTitle.equals(title, ignoreCase = true) && !ids.any { it.first == subjectId }) {
                                    ids.add(Pair(subjectId, lang))
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ids
    }

    /**
     * Calls the `season-info` endpoint and maps each season/episode into [ProviderSeason].
     * Episode play-info URLs use the `&se=<se>&ep=<ep>` query params.
     */
    private suspend fun fetchSeasons(
        subjectId:   String,
        baseUrl:     String,
        basePlayUrl: String,
        lang:        String
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
                            val source = MovieBoxMapper.toProviderSource(url = episodePlayUrl)
                            val finalSource = source.copy(metadata = mapOf("language" to lang))
                            ProviderEpisode(
                                number  = ep,
                                title   = "Episode $ep",
                                sources = listOf(finalSource)
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

    // "?"? Helpers "?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?

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
