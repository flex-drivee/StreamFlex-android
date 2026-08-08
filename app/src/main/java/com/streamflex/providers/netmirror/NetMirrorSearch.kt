package com.streamflex.providers.netmirror

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.parser.SearchResultParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.network.NetworkUtils
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult

class NetMirrorSearch : SearchResultParser {

    suspend fun search(
        query: String,
        baseUrl: String,
        ott: String
    ): List<SearchResult> {

        val endpoint = when (ott) {
            NetMirrorConfig.OTT_NETFLIX -> "/mobile/search.php"
            NetMirrorConfig.OTT_PRIME -> "/mobile/pv/search.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/search.php"
            else -> "/mobile/search.php"
        }

        val timestamp = System.currentTimeMillis() / 1000
        val searchUrl = baseUrl.trimEnd('/') + "$endpoint?s=${NetworkUtils.encode(query)}&t=$timestamp"

        val request = RequestBuilder()
            .url(searchUrl)
            .header("Referer", "$baseUrl/home")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0")
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val jsonString = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyList()
                    val rawTransport = TransportResult.TextResponse(text = jsonString, url = searchUrl)
                    parseResult(rawTransport, baseUrl, ott)
                }
                else -> emptyList()
            }
        }
    }

    override fun parse(raw: TransportResult): List<SearchResult> {
        return parseResult(raw, NetMirrorConfig.DEFAULT_DOMAIN, NetMirrorConfig.OTT_NETFLIX)
    }

    private fun parseResult(raw: TransportResult, baseUrl: String, ott: String): List<SearchResult> {
        val root = JsonParser.parse(raw.asString()) ?: return emptyList()
        val results = mutableListOf<SearchResult>()

        try {
            val searchResults = JsonParser.array(root, "searchResult")
            for (item in searchResults) {
                val id = JsonParser.string(item, "id") ?: continue
                val title = JsonParser.string(item, "t") ?: ""

                val poster = when (ott) {
                    NetMirrorConfig.OTT_PRIME -> "https://imgcdn.kim/pv/341/$id.jpg"
                    NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "https://imgcdn.kim/hs/v/$id.jpg"
                    else -> "https://imgcdn.kim/poster/v/150/$id.jpg"
                }

                // In StreamFlex, detailUrl often carries the ID or full URL to the detail page.
                // We'll pack the ID and OTT inside the detailUrl so NetMirrorDetails can extract it.
                val detailUrl = "netmirror://$ott/$id"

                results += SearchResult(
                    id = id,
                    title = title,
                    url = detailUrl,
                    poster = poster,
                    year = null,
                    mediaType = MediaType.MOVIE, // NetMirror returns mixed results, we resolve type in Details
                    providerId = "netmirror",
                    providerName = "NetMirror"
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return results
    }
}
