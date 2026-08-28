package com.streamflex.providers.netmirror

import android.net.Uri
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.*
import com.streamflex.extractors.netmirror.NetMirrorBypassManager

/**
 * NetMirrorDetails — v2 (Mobile API)
 *
 * Replaces the old stub that skipped the network request entirely.
 * Now fetches full metadata from /mobile/post.php using the authenticated
 * session token from NetMirrorBypassManager.
 *
 * Endpoint: GET /mobile/post.php?id=<id>&t=<unix_timestamp>
 *
 * Response JSON (PostData):
 * {
 *   "title": "...",
 *   "cast":  "Actor1, Actor2",
 *   "genre": "Action, Drama",
 *   "match": "IMDb 8.1",
 *   "runtime": "120",
 *   "suggest": [ { "id": "...", "t": "..." } ],
 *   "episodes": [ { "id": "...", "ep": "E01", "s": "S01", "t": "Title", "time": "45m" }, ... ]
 * }
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

        // URL format from Search: netmirror://{ott}/{id}
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

        // ── Fetch /mobile/post.php ─────────────────────────────────────────────
        val postUrl = "$base/mobile/post.php?id=$id&t=$unixTs"
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

        val json = response.data.body?.toString(Charsets.UTF_8) ?: return buildFallback(id, ott, base, providerName, result)
        StreamLogger.debug(TAG, "post.php response: $json")

        // ── Parse PostData ─────────────────────────────────────────────────────
        return try {
            val root    = JsonParser.parse(json)
            val title   = JsonParser.string(root, "title") ?: result.title
            val castStr = JsonParser.string(root, "cast")
            val genre   = JsonParser.string(root, "genre")
            val imdb    = JsonParser.string(root, "match")?.replace("IMDb ", "")?.toFloatOrNull()
            val runtime = JsonParser.string(root, "runtime")?.replace("m", "")?.toIntOrNull()

            // Parse episodes list to build seasons
            val episodesJson = JsonParser.array(root, "episodes")
            val sources      = mutableListOf<ProviderSource>()

            if (episodesJson.isEmpty()) {
                // Movie: single source pointing to episode ID = post ID
                sources += createPlayerSource(id, id, ott, base, providerName, title)
            } else {
                for (ep in episodesJson) {
                    val epId     = JsonParser.string(ep, "id")  ?: continue
                    val epNum    = JsonParser.string(ep, "ep")?.removePrefix("E")?.toIntOrNull()
                    val seasonN  = JsonParser.string(ep, "s")?.removePrefix("S")?.toIntOrNull()
                    val epTitle  = JsonParser.string(ep, "t")   ?: "Episode $epNum"
                    val epTime   = JsonParser.string(ep, "time")?.removeSuffix("m")?.toIntOrNull()
                    val posterKim = "https://imgcdn.kim/epimg/150/$epId.jpg"

                    sources += createPlayerSource(
                        id          = epId,
                        postId      = id,
                        ott         = ott,
                        baseUrl     = base,
                        providerName = providerName,
                        title       = epTitle,
                        episode     = epNum,
                        season      = seasonN,
                        runtime     = epTime,
                        poster      = posterKim
                    )
                }
            }

            ProviderResult(
                id          = id,
                providerId  = providerId,
                title       = title,
                detailUrl   = result.url,
                mediaType   = result.mediaType,
                sources     = sources,
                seasons     = emptyList(), // seasons resolved inline via sources
                year        = result.year,
                poster      = result.poster,
                overview    = null,
                rating      = imdb,
                runtime     = runtime,
                genres      = genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
                cast        = castStr?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
                success     = true
            )
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Post data parse error: ${e.message}")
            buildFallback(id, ott, base, providerName, result)
        }
    }

    /**
     * Fallback used when the API call fails: constructs a minimal ProviderResult
     * with a single player source so playback can still be attempted.
     */
    private fun buildFallback(
        id: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        result: SearchResult
    ): ProviderResult {
        StreamLogger.debug(TAG, "Building fallback result for id=$id")
        return ProviderResult(
            id          = id,
            providerId  = "",
            title       = result.title,
            detailUrl   = result.url,
            mediaType   = result.mediaType,
            sources     = listOf(createPlayerSource(id, id, ott, baseUrl, providerName, result.title)),
            seasons     = emptyList(),
            year        = result.year,
            poster      = result.poster,
            overview    = null,
            success     = false
        )
    }

    private fun createPlayerSource(
        id: String,
        postId: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        title: String,
        episode: Int?   = null,
        season: Int?    = null,
        runtime: Int?   = null,
        poster: String? = null
    ): ProviderSource {
        val encodedTitle = Uri.encode(title)
        val playerUri    = "netmirror://player?id=$id&ott=$ott&base=$baseUrl&title=$encodedTitle"

        return ProviderSource(
            provider  = providerName,
            host      = "NetMirror Mobile API",
            hostType  = HostType.NETMIRROR,
            url       = playerUri,
            quality   = Quality.UNKNOWN,
            episode   = episode,
            season    = season,
            runtime   = runtime,
            poster    = poster,
            title     = title
        )
    }
}
