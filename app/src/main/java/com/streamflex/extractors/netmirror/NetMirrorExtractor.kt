package com.streamflex.extractors.netmirror

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.ExtractorResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

class NetMirrorExtractor : BaseExtractor() {
    override val name = "NetMirror API"
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

    override suspend fun extract(source: ProviderSource): ExtractorResult {
        // source.url format: netmirror://player?id=$id&ott=$ott&base=$baseUrl
        val uri = Uri.parse(source.url)
        val id = uri.getQueryParameter("id") ?: return ExtractorResult()
        val ott = uri.getQueryParameter("ott") ?: return ExtractorResult()

        val apiBase = resolveApiUrl() ?: return ExtractorResult()
        
        val playerUrl = "$apiBase/newtv/player.php?id=$id"
        
        val request = RequestBuilder()
            .url(playerUrl)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .header("X-Requested-With", "NetmirrorNewTV v1.0")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0")
            .header("Accept", "application/json, text/plain, */*")
            .header("Ott", ott)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val json = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext ExtractorResult()
                    val root = JsonParser.parse(json) ?: return@withContext ExtractorResult()
                    
                    val status = JsonParser.string(root, "status")
                    if (status != "ok") return@withContext ExtractorResult()
                    
                    val videoLink = JsonParser.string(root, "video_link") ?: return@withContext ExtractorResult()
                    val referer = JsonParser.string(root, "referer") ?: apiBase

                    val stream = StreamLink(
                        provider = source.provider,
                        title = source.provider,
                        url = videoLink,
                        isM3U8 = videoLink.contains(".m3u8", ignoreCase = true),
                        headers = mapOf("Referer" to referer)
                    )

                    ExtractorResult(streams = listOf(stream))
                }
                else -> ExtractorResult()
            }
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
