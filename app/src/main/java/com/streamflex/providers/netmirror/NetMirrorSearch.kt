package com.streamflex.providers.netmirror

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.network.NetworkUtils
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.parser.SearchResultParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult
import com.streamflex.extractors.netmirror.NetMirrorBypassManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NetMirrorSearch — v2 (Mobile API)
 *
 * Uses the native /mobile/search.php endpoint instead of the old /search.php.
 * The mobile endpoint is behind a different Cloudflare policy and requires the
 * app spoof header (X-Requested-With: app.netmirror.netmirrornew) + t_hash_t cookie.
 *
 * Endpoint: GET /mobile/search.php?q=<query>&t=<unix_timestamp>
 *
 * Response JSON:
 * {
 *   "searchResult": [
 *     { "id": "123", "t": "Batman", "r": "Movie", "y": "2022" },
 *     ...
 *   ]
 * }
 */
class NetMirrorSearch : SearchResultParser {

    private companion object {
        const val TAG = "NetMirrorSearch"
    }

    suspend fun search(
        query: String,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): List<SearchResult> = withContext(Dispatchers.IO) {

        val base    = baseUrl.trimEnd('/')
        val unixTs  = System.currentTimeMillis() / 1000L

        // ── Acquire session token ─────────────────────────────────────────────
        val tHashT = NetMirrorBypassManager.getToken(base)
        if (tHashT.isNullOrBlank()) {
            StreamLogger.error(TAG, "Cannot search: no t_hash_t for $base")
            return@withContext emptyList()
        }

        val cookieStr = "t_hash_t=$tHashT; ott=$ott; hd=on"
        val referer   = "$base/mobile/home?app=1"

        // ── Build the mobile search URL ──────────────────────────────────────
        val searchPath = when (ott) {
            NetMirrorConfig.OTT_PRIME   -> "/mobile/pv/search.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/search.php"
            else                        -> "/mobile/search.php"
        }
        val searchUrl = "$base$searchPath?s=${NetworkUtils.encode(query)}&t=$unixTs"
        StreamLogger.debug(TAG, "GET $searchUrl")

        val request = RequestBuilder()
            .url(searchUrl)
            .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
            .header("X-Requested-With", "app.netmirror.netmirrornew")
            .header("Cookie", cookieStr)
            .header("Referer", referer)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8")
            .build()

        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val jsonStr = response.data.body?.toString(Charsets.UTF_8)
                if (jsonStr.isNullOrBlank()) {
                    StreamLogger.error(TAG, "Empty search response")
                    return@withContext emptyList()
                }
                val transport = TransportResult.TextResponse(text = jsonStr, url = searchUrl)
                parseResult(transport, base, ott, providerId, providerName)
            }
            else -> {
                StreamLogger.error(TAG, "Search request failed: $response")
                emptyList()
            }
        }
    }

    override fun parse(raw: TransportResult): List<SearchResult> =
        parseResult(raw, NetMirrorConfig.DEFAULT_DOMAIN, NetMirrorConfig.OTT_NETFLIX, "netflixmirror", "NetflixMirror")

    private fun parseResult(
        raw: TransportResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): List<SearchResult> {
        val root = JsonParser.parse(raw.asString()) ?: return emptyList()
        val results = mutableListOf<SearchResult>()

        return try {
            val searchResults = JsonParser.array(root, "searchResult")
            for (item in searchResults) {
                val id    = JsonParser.string(item, "id") ?: continue
                val title = JsonParser.string(item, "t")  ?: ""

                // Poster CDN differs per OTT type (same pattern as CNC Verse Mobile)
                val poster = when (ott) {
                    NetMirrorConfig.OTT_PRIME                                -> "https://imgcdn.kim/pv/341/$id.jpg"
                    NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY  -> "https://imgcdn.kim/hs/v/$id.jpg"
                    else                                                      -> "https://imgcdn.kim/poster/v/150/$id.jpg"
                }

                val rStr           = JsonParser.string(item, "r")
                val isSeries       = rStr?.equals("Series", ignoreCase = true) == true
                val resolvedMedia  = if (isSeries) MediaType.TV else MediaType.MOVIE

                // Custom URI scheme consumed by NetMirrorExtractor
                val detailUrl = "netmirror://$ott/$id"

                results += SearchResult(
                    id           = id,
                    title        = title,
                    url          = detailUrl,
                    poster       = poster,
                    year         = JsonParser.string(item, "y")?.toIntOrNull(),
                    mediaType    = resolvedMedia,
                    providerId   = providerId,
                    providerName = providerName
                )
            }
            StreamLogger.debug(TAG, "Parsed ${results.size} search results for ott=$ott")
            results
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Search parse error: ${e.message}")
            emptyList()
        }
    }
}
