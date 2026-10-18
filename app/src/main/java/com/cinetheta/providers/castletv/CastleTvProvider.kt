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

class CastleTvProvider(
    private val cacheManager: com.cinetheta.core.cache.CacheManager = com.cinetheta.core.cache.CacheManager(),
    private val resolver: com.cinetheta.core.network.DomainResolver = com.cinetheta.core.network.DomainResolver(cacheManager)
) : Provider {
    override val id = "castletv"
    override val name = "Castle TV (Use VLC for Multi Language)"

    companion object {
        const val DEFAULT_DOMAIN = "https://api.hlowb.com"
        const val MANIFEST_PATH = "providers/castletv.json"
    }

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: DEFAULT_DOMAIN

    override val supportedMedia = setOf(MediaType.MOVIE, MediaType.TV)

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId = id,
            hardcoded = DEFAULT_DOMAIN,
            manifestPath = MANIFEST_PATH
        )

        val domain = when (result) {
            is com.cinetheta.core.network.DomainResult.Resolved -> result.domain
            is com.cinetheta.core.network.DomainResult.Mirror -> result.domain
            is com.cinetheta.core.network.DomainResult.Hardcoded -> result.domain
            is com.cinetheta.core.network.DomainResult.Offline -> DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(id)
    }
    
    override suspend fun search(query: String): List<SearchResult> {
        return search(query, 1)
    }
    
    override suspend fun search(query: String, page: Int): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            ensureDomain()
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
            val langNames = tracks.mapNotNull { it.jsonObject["languageName"]?.jsonPrimitive?.content }.joinToString(",")
            val playerUri = "castletv://player?movieId=$movieId&episodeId=$episodeId&langs=${Uri.encode(langNames)}"
            
            sources.add(
                ProviderSource(
                    provider = this@CastleTvProvider.name,
                    host = "Castle",
                    hostType = HostType.CASTLETV,
                    url = playerUri,
                    quality = Quality.UNKNOWN
                )
            )
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
        return sources
    }
    
    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        return withContext(Dispatchers.IO) {
            try {
                ensureDomain()
                val loadUrl = if (searchResult.url.startsWith("http")) {
                    val uri = Uri.parse(searchResult.url)
                    val pathAndQuery = buildString {
                        append(uri.encodedPath ?: "")
                        if (!uri.encodedQuery.isNullOrBlank()) append("?${uri.encodedQuery}")
                    }
                    if (pathAndQuery.isNotBlank() && pathAndQuery != "/") "$baseUrl$pathAndQuery" else searchResult.url
                } else {
                    searchResult.url
                }
                val detailsObj = CastleTvCrypto.fetchAndDecrypt(loadUrl)
                val dataObj = detailsObj["data"]?.jsonObject ?: throw Exception("No data object")
                val seasonsArr = dataObj["seasons"]?.jsonArray
                val isTvShow = searchResult.mediaType == MediaType.TV || (!seasonsArr.isNullOrEmpty())
                
                if (isTvShow) {
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
                        mediaType = MediaType.TV,
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
