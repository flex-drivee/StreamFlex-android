package com.streamflex.providers.netmirror

import android.net.Uri
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult
import com.streamflex.extractors.netmirror.NetMirrorBypassManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * NetMirrorDetails — v2 (Mobile API)
 *
 * Fetches full metadata from /mobile/post.php using the authenticated
 * session token from NetMirrorBypassManager.
 *
 * Endpoint: GET /mobile/post.php?id=<id>&t=<unix_timestamp>
 *
 * Extra metadata (rating, runtime, genres, cast) is stored in
 * ProviderResult.metadata as strings since ProviderResult has no
 * dedicated fields for them.
 *
 * Episode-specific info (episode, season, runtime, poster) is stored
 * in ProviderSource.metadata as strings since ProviderSource has no
 * dedicated fields for them.
 */
class NetMirrorDetails {

    private companion object {
        const val TAG = "NetMirrorDetails"
    }

    suspend fun load(
        result: SearchResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): ProviderResult? {

        val id   = result.url.substringAfterLast("/")
        val base = baseUrl.trimEnd('/')

        // ── Acquire session token ─────────────────────────────────────────────
        val tHashT = NetMirrorBypassManager.getToken(base)
        if (tHashT.isNullOrBlank()) {
            StreamLogger.error(TAG, "Cannot load details: no t_hash_t for $base")
            return buildFallback(id, ott, base, providerName, result)
        }

        val cookieStr = "t_hash_t=$tHashT; ott=$ott; hd=on"
        val referer   = "$base/mobile/home?app=1"
        val unixTs    = System.currentTimeMillis() / 1000L

        // ── Fetch /mobile/post.php ────────────────────────────────────────────
        val postPath = when (ott) {
            NetMirrorConfig.OTT_PRIME   -> "/mobile/pv/post.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/post.php"
            else                        -> "/mobile/post.php"
        }
        val postUrl = "$base$postPath?id=$id&t=$unixTs"
        StreamLogger.debug(TAG, "GET $postUrl")

        val response = try {
            HttpClient.execute(
                RequestBuilder()
                    .url(postUrl)
                    .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
                    .header("X-Requested-With", "app.netmirror.netmirrornew")
                    .header("Cookie", cookieStr)
                    .header("Referer", referer)
                    .header("Accept", "*/*")
                    .build()
            )
        } catch (e: Exception) {
            StreamLogger.error(TAG, "post.php request failed: ${e.message}")
            return buildFallback(id, ott, base, providerName, result)
        }

        if (response !is NetworkResult.Success) {
            StreamLogger.error(TAG, "post.php non-success: $response")
            return buildFallback(id, ott, base, providerName, result)
        }

        val json = response.data.bodyAsString()
        StreamLogger.debug(TAG, "post.php response: $json")

        // ── Parse PostData ────────────────────────────────────────────────────
        return try {
            val root    = JsonParser.parse(json)
            val title   = JsonParser.string(root, "title") ?: result.title
            val castStr = JsonParser.string(root, "cast")
            val genre   = JsonParser.string(root, "genre")
            val imdb    = JsonParser.string(root, "match")?.replace("IMDb ", "")
            val runtime = JsonParser.string(root, "runtime")

            // Build extra metadata map (ProviderResult has no dedicated fields)
            val meta = buildMap<String, String> {
                if (imdb    != null) put("rating",  imdb)
                if (runtime != null) put("runtime", runtime)
                if (genre   != null) put("genres",  genre)
                if (castStr != null) put("cast",    castStr)
            }

            // Parse episodes list for the initially loaded season
            val episodesJson = JsonParser.array(root, "episodes")
            val sources = mutableListOf<ProviderSource>()
            
            sources.addAll(parseEpisodes(episodesJson, ott, base, providerName))

            // Fetch other seasons if they exist
            val seasonsJson = JsonParser.array(root, "season")
            if (seasonsJson.size > 1) {
                // Find which season we actually got episodes for in the initial response
                var currentLoadedSeason: String? = null
                if (episodesJson.isNotEmpty()) {
                    val firstEp = episodesJson[0]
                    currentLoadedSeason = JsonParser.string(firstEp, "s")?.removePrefix("S")
                }

                val otherSeasonIds = mutableListOf<String>()
                for (s in seasonsJson) {
                    val sId = JsonParser.string(s, "id") ?: continue
                    val sNum = JsonParser.string(s, "s")
                    
                    // Fetch if this season is not the one we already loaded
                    if (sNum != currentLoadedSeason) {
                        otherSeasonIds.add(sId)
                    }
                }
                
                // Fetch all other seasons concurrently
                if (otherSeasonIds.isNotEmpty()) {
                    kotlinx.coroutines.coroutineScope {
                        val deferred = otherSeasonIds.map { sId ->
                            async {
                                fetchSeasonEpisodes(sId, base, postPath, unixTs, cookieStr, referer, ott, providerName)
                            }
                        }
                        deferred.awaitAll().forEach { seasonSources ->
                            sources.addAll(seasonSources)
                        }
                    }
                }
            }

            // If it's a movie, episodes array is empty or contains [null]
            if (sources.isEmpty()) {
                sources += createPlayerSource(
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
                sources   = sources,
                seasons   = emptyList(),
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
        StreamLogger.debug(TAG, "Building fallback result for id=$id")
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
        base: String,
        postPath: String,
        unixTs: Long,
        cookieStr: String,
        referer: String,
        ott: String,
        providerName: String
    ): List<ProviderSource> {
        val postUrl = "$base$postPath?id=$sId&t=$unixTs"
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
