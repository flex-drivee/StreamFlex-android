package com.streamflex.extractors.netmirror

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import java.net.URLDecoder
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

class NetMirrorExtractor : BaseExtractor() {
    override val hostType = HostType.NETMIRROR

    // Note: In CloudStream, these are base64 encoded. We decode them at runtime.
    private val newTvDomains = listOf(
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3RzLmNvbQ==",
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3QuYXBw",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmFydA==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNj",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lmluaw==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmxpdmU=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnBybw==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNob3A=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNpdGU="
    )

    private var resolvedApiUrl: String = ""

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // source.url format: netmirror://player?id=$id&ott=$ott&base=$baseUrl&title=$title
        val uri = Uri.parse(source.url)
        val id = uri.getQueryParameter("id") ?: return emptyResult()
        val ott = uri.getQueryParameter("ott") ?: return emptyResult()
        val baseUrl = uri.getQueryParameter("base") ?: com.streamflex.providers.netmirror.NetMirrorConfig.DEFAULT_DOMAIN
        val title = uri.getQueryParameter("title") ?: source.provider

        val playUrl = "$baseUrl/play.php"
        val playlistBaseUrl = "$baseUrl/playlist.php"
        
        val playRequest = RequestBuilder()
            .url(playUrl)
            .post("id=$id".toByteArray(Charsets.UTF_8))
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Origin", baseUrl)
            .header("Referer", "$baseUrl/home")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0")
            .build()
            
        return withContext(Dispatchers.IO) {
            val playResponse = HttpClient.execute(playRequest)
            if (playResponse !is NetworkResult.Success) return@withContext emptyResult()
            
            val playJson = playResponse.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyResult()
            val playRoot = JsonParser.parse(playJson) ?: return@withContext emptyResult()
            val h = JsonParser.string(playRoot, "h") ?: return@withContext emptyResult()
            
            val timestamp = System.currentTimeMillis() / 1000
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedH = java.net.URLEncoder.encode(h, "UTF-8")
            val playlistUrl = "$playlistBaseUrl?id=$id&t=$encodedTitle&tm=$timestamp&h=$encodedH"
            
            val playlistRequest = RequestBuilder()
                .url(playlistUrl)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Origin", baseUrl)
                .header("Referer", "$baseUrl/home")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0")
                .build()
                
            val playlistResp = HttpClient.execute(playlistRequest)
            if (playlistResp !is NetworkResult.Success) return@withContext emptyResult()
            
            val playlistJson = playlistResp.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyResult()
            val playlistArray = JsonParser.parse(playlistJson)?.takeIf { it.isJsonArray }?.asJsonArray ?: return@withContext emptyResult()
            if (playlistArray.isEmpty) return@withContext emptyResult()
            
            val item = playlistArray.get(0).asJsonObject
            val sourcesArray = item.getAsJsonArray("sources") ?: return@withContext emptyResult()
            
            val streams = mutableListOf<StreamLink>()
            for (sourceElement in sourcesArray) {
                val sourceObj = sourceElement.asJsonObject
                val file = sourceObj.get("file")?.asString ?: continue
                val label = sourceObj.get("label")?.asString ?: "Unknown"
                
                val streamUrl = if (file.startsWith("http")) file else baseUrl + file
                
                streams.add(
                    StreamLink(
                        name = "${source.provider} - $label",
                        url = streamUrl,
                        host = source.hostType,
                        headers = mapOf(
                            "Referer" to "$baseUrl/home",
                            "Cookie" to "hd=on"
                        )
                    )
                )
            }
            
            result(streams)
        }
    }

    private suspend fun resolveApiUrl(): String? {
        if (resolvedApiUrl.isNotBlank()) return resolvedApiUrl

        for (encoded in newTvDomains) {
            val base = String(Base64.getDecoder().decode(encoded)).trimEnd('/')
            val request = RequestBuilder()
                .url("$base/checknewtv.php")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0")
                .build()
                
            try {
                val response = HttpClient.execute(request)
                if (response is NetworkResult.Success) {
                    val json = response.data.body?.toString(Charsets.UTF_8) ?: continue
                    val root = JsonParser.parse(json) ?: continue
                    val tokenHash = JsonParser.string(root, "token_hash")
                    if (!tokenHash.isNullOrBlank()) {
                        resolvedApiUrl = String(Base64.getDecoder().decode(tokenHash)).trimEnd('/')
                        return resolvedApiUrl
                    }
                }
            } catch (_: Exception) {
                // Try next domain
            }
        }
        return null
    }
}
