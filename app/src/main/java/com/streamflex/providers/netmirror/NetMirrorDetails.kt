package com.streamflex.providers.netmirror

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.*

class NetMirrorDetails {

    suspend fun load(
        result: SearchResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): ProviderResult? {
        
        // URL format from Search: netmirror://{ott}/{id}
        val id = result.url.substringAfterLast("/")
        
        val endpoint = when (ott) {
            NetMirrorConfig.OTT_NETFLIX -> "/mobile/post.php"
            NetMirrorConfig.OTT_PRIME -> "/mobile/pv/post.php"
            NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/post.php"
            else -> "/mobile/post.php"
        }

        val timestamp = System.currentTimeMillis() / 1000
        val postUrl = baseUrl.trimEnd('/') + "$endpoint?id=$id&t=$timestamp"

        val request = RequestBuilder()
            .url(postUrl)
            .header("Referer", "$baseUrl/home")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0")
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val jsonString = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext null
                    parseDetails(jsonString, id, result, baseUrl, ott, providerId, providerName)
                }
                else -> null
            }
        }
    }

    private fun parseDetails(
        jsonString: String,
        id: String,
        searchResult: SearchResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): ProviderResult? {
        val root = JsonParser.parse(jsonString) ?: return null

        val title = JsonParser.string(root, "title") ?: searchResult.title
        val desc = JsonParser.string(root, "desc")
        val year = JsonParser.string(root, "year")?.toIntOrNull()

        val episodesArray = JsonParser.array(root, "episodes")
        val isMovie = episodesArray.isEmpty() || !episodesArray.first().isJsonObject

        val sources = mutableListOf<ProviderSource>()
        val seasonsMap = mutableMapOf<Int, MutableList<ProviderEpisode>>()

        if (isMovie) {
            sources += createPlayerSource(id, ott, baseUrl, providerName, title)
        } else {
            for (epObj in episodesArray) {
                val epId = JsonParser.string(epObj, "id") ?: continue
                val epTitle = JsonParser.string(epObj, "t") ?: "Episode"
                
                val seasonStr = JsonParser.string(epObj, "s") ?: "S1"
                val seasonNum = seasonStr.replace("S", "").toIntOrNull() ?: 1
                
                val epStr = JsonParser.string(epObj, "ep") ?: "E1"
                val epNum = epStr.replace("E", "").toIntOrNull() ?: 1

                val epThumb = when (ott) {
                    NetMirrorConfig.OTT_PRIME -> "https://img.nfmirrorcdn.top/pvepimg/$epId.jpg"
                    NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "https://imgcdn.kim/hsepimg/$epId.jpg"
                    else -> "https://imgcdn.kim/poster/v/150/$epId.jpg"
                }

                val epSource = createPlayerSource(epId, ott, baseUrl, providerName, "$title $seasonStr$epStr")

                val episode = ProviderEpisode(
                    number = epNum,
                    title = epTitle,
                    thumbnail = epThumb,
                    sources = listOf(epSource)
                )

                seasonsMap.getOrPut(seasonNum) { mutableListOf() }.add(episode)
            }
            // Note: CloudStream reference implements pagination for episodes, 
            // but for simplicity in Phase 3 we parse the initial batch. 
        }

        val seasonsList = seasonsMap.map { (seasonNum, eps) ->
            ProviderSeason(
                number = seasonNum,
                episodes = eps.sortedBy { it.number }
            )
        }.sortedBy { it.number }

        return ProviderResult(
            id = id,
            providerId = providerId,
            title = title,
            detailUrl = searchResult.url,
            mediaType = if (isMovie) MediaType.MOVIE else MediaType.TV,
            sources = sources,
            seasons = seasonsList,
            year = year,
            poster = searchResult.poster,
            overview = desc,
            success = true
        )
    }

    private fun createPlayerSource(
        id: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        title: String
    ): ProviderSource {
        // Create a custom URI scheme so our Extractor can intercept it
        val encodedTitle = android.net.Uri.encode(title)
        val playerUri = "netmirror://player?id=$id&ott=$ott&base=${baseUrl}&title=$encodedTitle"
        
        return ProviderSource(
            provider = providerName,
            host = "NetMirror API",
            hostType = HostType.REDIRECT, // We'll map this to a custom extractor or use REDIRECT
            url = playerUri,
            quality = Quality.UNKNOWN
        )
    }
}
