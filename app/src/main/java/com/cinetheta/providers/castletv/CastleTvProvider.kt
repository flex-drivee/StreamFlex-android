package com.cinetheta.providers.castletv

import android.net.Uri
import com.cinetheta.domain.models.*
import com.cinetheta.domain.provider.Provider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class CastleTvProvider : Provider {
    override val id = "castletv"
    override val name = "Castle TV"
    override val baseUrl = "https://api.hlowb.com"
    override val supportedMedia = setOf(MediaType.MOVIE, MediaType.TV)
    
    override suspend fun search(query: String): List<SearchResult> {
        return search(query, 1)
    }
    
    override suspend fun search(query: String, page: Int): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/film-api/v1.1.0/movie/searchByKeyword?channel=IndiaA&clientType=1&clientType=1&keyword=$query"
            try {
                val decryptedObj = CastleTvCrypto.fetchAndDecrypt(url)
                val dataObj = decryptedObj["data"]?.jsonObject ?: return@withContext emptyList()
                val rows = dataObj["rows"]?.jsonArray ?: return@withContext emptyList()
                
                val results = mutableListOf<SearchResult>()
                for (row in rows) {
                    val item = row.jsonObject
                    val title = item["title"]?.jsonPrimitive?.content ?: continue
                    val idStr = item["id"]?.jsonPrimitive?.content ?: continue
                    val cover = item["coverVerticalImage"]?.jsonPrimitive?.content ?: item["coverHorizontalImage"]?.jsonPrimitive?.content
                    val movieTypeInt = item["movieType"]?.jsonPrimitive?.content?.toIntOrNull()
                    val mediaType = if (movieTypeInt == 1) MediaType.TV else MediaType.MOVIE
                    
                    results.add(
                        SearchResult(
                            id = idStr,
                            title = title,
                            url = "$baseUrl/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&clientType=1&lang=en-US&movieId=$idStr",
                            poster = cover,
                            mediaType = mediaType,
                            providerId = this@CastleTvProvider.id,
                            providerName = this@CastleTvProvider.name
                        )
                    )
                }
                results
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun extractSourcesFromEpisode(movieId: String, epObj: JsonObject): List<ProviderSource> {
        val sources = mutableListOf<ProviderSource>()
        val episodeId = epObj["id"]?.jsonPrimitive?.content ?: return emptyList()
        val tracks = epObj["tracks"]?.jsonArray
        
        if (tracks != null && tracks.isNotEmpty()) {
            for (track in tracks) {
                val tObj = track.jsonObject
                val langId = tObj["languageId"]?.jsonPrimitive?.content ?: continue
                val langName = tObj["languageName"]?.jsonPrimitive?.content ?: "Unknown"
                val existIndividual = tObj["existIndividualVideo"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                
                val playerUri = if (existIndividual) {
                    "castletv://player?movieId=$movieId&episodeId=$episodeId&languageId=$langId"
                } else {
                    "castletv://player?movieId=$movieId&episodeId=$episodeId"
                }
                
                sources.add(
                    ProviderSource(
                        provider = "${this@CastleTvProvider.name} - $langName",
                        host = "Castle",
                        hostType = HostType.CASTLETV,
                        url = playerUri,
                        quality = Quality.UNKNOWN
                    )
                )
            }
        } else {
            val playerUri = "castletv://player?movieId=$movieId&episodeId=$episodeId"
            sources.add(
                ProviderSource(
                    provider = this@CastleTvProvider.name,
                    host = "Castle",
                    hostType = HostType.CASTLETV,
                    url = playerUri,
                    quality = Quality.UNKNOWN
                )
            )
        }
        return sources.distinctBy { it.url } // Prevent duplicates if multiple languages share the default video
    }
    
    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        return withContext(Dispatchers.IO) {
            try {
                val isTvShow = searchResult.mediaType == MediaType.TV
                val detailsObj = CastleTvCrypto.fetchAndDecrypt(searchResult.url)
                val dataObj = detailsObj["data"]?.jsonObject ?: throw Exception("No data object")
                
                if (isTvShow) {
                    val seasonsArr = dataObj["seasons"]?.jsonArray
                    val providerSeasons = mutableListOf<ProviderSeason>()
                    
                    if (seasonsArr != null) {
                        val deferredSeasons = seasonsArr.map { seasonElement ->
                            async {
                                val sObj = seasonElement.jsonObject
                                val sMovieId = sObj["movieId"]?.jsonPrimitive?.content ?: return@async null
                                val sNumber = sObj["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@async null
                                val sDesc = sObj["description"]?.jsonPrimitive?.content ?: "Season $sNumber"
                                
                                val sUrl = "$baseUrl/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&clientType=1&lang=en-US&movieId=$sMovieId"
                                val sDetails = CastleTvCrypto.fetchAndDecrypt(sUrl)
                                val sData = sDetails["data"]?.jsonObject
                                val epsArr = sData?.get("episodes")?.jsonArray
                                
                                val providerEpisodes = mutableListOf<ProviderEpisode>()
                                if (epsArr != null) {
                                    for (epElement in epsArr) {
                                        val epObj = epElement.jsonObject
                                        val epNum = epObj["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
                                        val epTitle = epObj["title"]?.jsonPrimitive?.content ?: "Episode $epNum"
                                        val epThumb = epObj["coverImage"]?.jsonPrimitive?.content
                                        
                                        providerEpisodes.add(
                                            ProviderEpisode(
                                                number = epNum,
                                                title = epTitle,
                                                thumbnail = epThumb,
                                                sources = extractSourcesFromEpisode(sMovieId, epObj)
                                            )
                                        )
                                    }
                                }
                                
                                ProviderSeason(
                                    number = sNumber,
                                    title = sDesc,
                                    episodes = providerEpisodes.sortedBy { it.number }
                                )
                            }
                        }
                        
                        providerSeasons.addAll(deferredSeasons.awaitAll().filterNotNull().sortedBy { it.number })
                    }
                    
                    ProviderResult(
                        id = searchResult.id,
                        providerId = this@CastleTvProvider.id,
                        title = searchResult.title,
                        detailUrl = searchResult.url,
                        mediaType = searchResult.mediaType,
                        seasons = providerSeasons,
                        poster = searchResult.poster,
                        success = providerSeasons.isNotEmpty()
                    )
                } else {
                    val epObj = dataObj["episodes"]?.jsonArray?.firstOrNull()?.jsonObject
                    val sources = if (epObj != null) extractSourcesFromEpisode(searchResult.id, epObj) else emptyList()
                    
                    ProviderResult(
                        id = searchResult.id,
                        providerId = this@CastleTvProvider.id,
                        title = searchResult.title,
                        detailUrl = searchResult.url,
                        mediaType = searchResult.mediaType,
                        sources = sources,
                        poster = searchResult.poster,
                        success = sources.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ProviderResult(
                    id = searchResult.id,
                    providerId = this@CastleTvProvider.id,
                    title = searchResult.title,
                    detailUrl = searchResult.url,
                    mediaType = searchResult.mediaType,
                    success = false,
                    error = e.localizedMessage ?: "Failed to load"
                )
            }
        }
    }
}
