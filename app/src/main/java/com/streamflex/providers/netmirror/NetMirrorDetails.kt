package com.streamflex.providers.netmirror

import android.net.Uri
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.*
import com.streamflex.extractors.netmirror.NetMirrorBypassManager

class NetMirrorDetails {

    companion object {
        private const val TAG = "NetMirrorDetails"
    }

    suspend fun load(
        result: SearchResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): ProviderResult {
        val id = result.url.substringAfterLast("/")
        val base = baseUrl.trimEnd('/')
        val postPath = when (ott) {
            NetMirrorConfig.OTT_PRIME -> "/mobile/pv/post.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/post.php"
            else -> "/mobile/post.php"
        }

        // Bypass security layer
        val bypassToken = NetMirrorBypassManager.getToken(base)
        if (bypassToken.isNullOrEmpty()) {
            StreamLogger.error(TAG, "Bypass failed for '$base'")
            return buildFallback(id, ott, base, providerName, result)
        }

        val unixTs    = System.currentTimeMillis() / 1000L
        val cookieStr = "t_hash_t=$bypassToken; ott=$ott; hd=on"
        val referer   = "$base/mobile/home?app=1"
        val postUrl   = "$base$postPath?id=$id&t=$unixTs"

        StreamLogger.debug(TAG, "GET $postUrl")

        return try {
            val response = HttpClient.execute(
                RequestBuilder()
                    .url(postUrl)
                    .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
                    .header("X-Requested-With", "app.netmirror.netmirrornew")
                    .header("Cookie", cookieStr)
                    .header("Referer", referer)
                    .header("Accept", "*/*")
                    .build()
            )

            if (response !is NetworkResult.Success) {
                StreamLogger.error(TAG, "Post load failed with non-success response")
                return buildFallback(id, ott, base, providerName, result)
            }

            val json = response.data.bodyAsString()
            val root = JsonParser.parse(json)

            val status = JsonParser.string(root, "status")
            if (status == "n") {
                StreamLogger.error(TAG, "API Error: ${JsonParser.string(root, "error")}")
                return buildFallback(id, ott, base, providerName, result)
            }

            val title   = JsonParser.string(root, "title") ?: result.title
            val imdb    = JsonParser.string(root, "match")
            val runtime = JsonParser.string(root, "runtime")
            val genre   = JsonParser.string(root, "genre")
            val castStr = JsonParser.string(root, "cast")

            val meta = buildMap<String, String> {
                if (imdb    != null) put("rating",  imdb)
                if (runtime != null) put("runtime", runtime)
                if (genre   != null) put("genres",  genre)
                if (castStr != null) put("cast",    castStr)
            }

            val episodesJson = JsonParser.array(root, "episodes")
            val allSources = mutableListOf<ProviderSource>()
            
            allSources.addAll(parseEpisodes(episodesJson, ott, base, providerName))

            val seasonsJson = JsonParser.array(root, "season")
            if (seasonsJson.size > 1) {
                var currentLoadedSeason: String? = null
                if (episodesJson.isNotEmpty()) {
                    val firstEp = episodesJson[0]
                    currentLoadedSeason = JsonParser.string(firstEp, "s")?.removePrefix("S")
                }
                StreamLogger.debug(TAG, "Current loaded season is: $currentLoadedSeason")

                val otherSeasonIds = mutableListOf<String>()
                for (s in seasonsJson) {
                    val sId = JsonParser.string(s, "id") ?: continue
                    val sNum = JsonParser.string(s, "s")?.removePrefix("S")
                    
                    if (sNum != currentLoadedSeason) {
                        otherSeasonIds.add(sId)
                    }
                }
                
                // Fetch other seasons sequentially to prevent "Invalid User" rate limits
                if (otherSeasonIds.isNotEmpty()) {
                    StreamLogger.debug(TAG, "Fetching ${otherSeasonIds.size} other seasons sequentially...")
                    var tsOffset = 1L
                    // Fetch episodes for all missing seasons using episodes.php
                    for (sId in otherSeasonIds) {
                        val eps = fetchSeasonEpisodes(sId, id, base, ott, unixTs + tsOffset, cookieStr, referer, providerName)
                        allSources.addAll(eps)
                        tsOffset++
                        StreamLogger.debug(TAG, "Added ${eps.size} episodes from season id $sId")
                        
                        if (otherSeasonIds.last() != sId) {
                            kotlinx.coroutines.delay(200)
                        }
                    }
                }
            }

            // Group into proper ProviderSeason and ProviderEpisode structures for TV Shows
            val providerSeasons = mutableListOf<ProviderSeason>()
            val isMovie = allSources.isEmpty() || (seasonsJson.isEmpty() && episodesJson.isEmpty()) // Treat as movie if no valid episodes were parsed

            if (!isMovie) {
                val bySeason = allSources.groupBy { it.metadata["season"]?.toIntOrNull() ?: 1 }
                for ((seasonNum, seasonSources) in bySeason) {
                    val byEpisode = seasonSources.groupBy { it.metadata["episode"]?.toIntOrNull() ?: 1 }
                    val providerEpisodes = byEpisode.map { (epNum, epSources) ->
                        ProviderEpisode(
                            number = epNum,
                            title = epSources.first().metadata["epTitle"] ?: "Episode $epNum",
                            thumbnail = epSources.first().metadata["poster"],
                            sources = epSources
                        )
                    }
                    providerSeasons.add(
                        ProviderSeason(
                            number = seasonNum,
                            title = "Season $seasonNum",
                            episodes = providerEpisodes.sortedBy { it.number }
                        )
                    )
                }
                providerSeasons.sortBy { it.number }
            }

            if (isMovie && allSources.isEmpty()) {
                allSources += createPlayerSource(
                    id           = id,
                    ott          = ott,
                    baseUrl      = base,
                    providerName = providerName,
                    title        = title
                )
            }

            ProviderResult(
                id        = id,
                providerId = providerId,
                title     = title,
                detailUrl = result.url,
                mediaType = result.mediaType,
                sources   = if (isMovie) allSources else emptyList(),
                seasons   = providerSeasons,
                year      = result.year,
                poster    = result.poster,
                overview  = null,
                metadata  = meta,
                success   = true
            )
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Post data parse error: ${e.message}")
            buildFallback(id, ott, base, providerName, result)
        }
    }

    private fun buildFallback(
        id: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        result: SearchResult
    ): ProviderResult {
        return ProviderResult(
            id         = id,
            providerId = "",
            title      = result.title,
            detailUrl  = result.url,
            mediaType  = result.mediaType,
            sources    = listOf(createPlayerSource(id, ott, baseUrl, providerName, result.title)),
            seasons    = emptyList(),
            year       = result.year,
            poster     = result.poster,
            overview   = null,
            success    = false
        )
    }

    private fun createPlayerSource(
        id: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        title: String,
        metadata: Map<String, String> = emptyMap()
    ): ProviderSource {
        val encodedTitle = Uri.encode(title)
        val playerUri    = "netmirror://player?id=$id&ott=$ott&base=$baseUrl&title=$encodedTitle"

        return ProviderSource(
            provider = providerName,
            host     = "NetMirror Mobile API",
            hostType = HostType.NETMIRROR,
            url      = playerUri,
            quality  = Quality.UNKNOWN,
            metadata = metadata
        )
    }

    private fun parseEpisodes(
        episodesJson: List<com.google.gson.JsonElement>,
        ott: String,
        base: String,
        providerName: String
    ): List<ProviderSource> {
        val sources = mutableListOf<ProviderSource>()
        for (ep in episodesJson) {
            if (ep.isJsonNull) continue

            val epId    = JsonParser.string(ep, "id")  ?: continue
            val epNum   = JsonParser.string(ep, "ep")?.removePrefix("E")
            val seasonN = JsonParser.string(ep, "s")?.removePrefix("S")
            val epTitle = JsonParser.string(ep, "t")   ?: "Episode $epNum"
            val epTime  = JsonParser.string(ep, "time")
            val poster  = "https://imgcdn.kim/epimg/150/$epId.jpg"

            val epMeta = buildMap<String, String> {
                if (epNum   != null) put("episode", epNum)
                if (seasonN != null) put("season",  seasonN)
                if (epTime  != null) put("runtime", epTime)
                put("poster", poster)
                put("epTitle", epTitle)
            }

            sources += createPlayerSource(
                id           = epId,
                ott          = ott,
                baseUrl      = base,
                providerName = providerName,
                title        = epTitle,
                metadata     = epMeta
            )
        }
        return sources
    }

    private suspend fun fetchSeasonEpisodes(
        sId: String,
        seriesId: String,
        base: String,
        ott: String,
        unixTs: Long,
        cookieStr: String,
        referer: String,
        providerName: String
    ): List<ProviderSource> {
        val episodesPath = when (ott) {
            NetMirrorConfig.OTT_PRIME   -> "/mobile/pv/episodes.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/episodes.php"
            else                        -> "/mobile/episodes.php"
        }
        val postUrl = "$base$episodesPath?s=$sId&series=$seriesId&page=1&t=$unixTs"
        StreamLogger.debug(TAG, "fetchSeasonEpisodes: GET $postUrl")
        return try {
            val response = HttpClient.execute(
                RequestBuilder()
                    .url(postUrl)
                    .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
                    .header("X-Requested-With", "app.netmirror.netmirrornew")
                    .header("Cookie", cookieStr)
                    .header("Referer", referer)
                    .header("Accept", "*/*")
                    .build()
            )
            if (response is NetworkResult.Success) {
                val json = response.data.bodyAsString()
                val root = JsonParser.parse(json)
                val status = JsonParser.string(root, "status")
                if (status == "n") {
                    StreamLogger.error(TAG, "fetchSeasonEpisodes API Error: ${JsonParser.string(root, "error")}")
                    return emptyList()
                }
                val eps = JsonParser.array(root, "episodes")
                parseEpisodes(eps, ott, base, providerName)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            StreamLogger.error(TAG, "fetchSeasonEpisodes failed: ${e.message}")
            emptyList()
        }
    }
}
