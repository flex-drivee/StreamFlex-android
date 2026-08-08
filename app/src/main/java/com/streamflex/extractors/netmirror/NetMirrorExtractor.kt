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
import android.util.Base64
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

        val ottPrefix = when (ott) {
            com.streamflex.providers.netmirror.NetMirrorConfig.OTT_NETFLIX -> ""
            com.streamflex.providers.netmirror.NetMirrorConfig.OTT_PRIME -> "/pv"
            com.streamflex.providers.netmirror.NetMirrorConfig.OTT_HOTSTAR, com.streamflex.providers.netmirror.NetMirrorConfig.OTT_DISNEY -> "/hs"
            else -> ""
        }
        
        val playUrl = baseUrl.trimEnd('/') + "$ottPrefix/play.php"
        val playlistBaseUrl = baseUrl.trimEnd('/') + "$ottPrefix/playlist.php"
        
        val NATIVE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0"
        
        val playRequest = RequestBuilder()
            .url(playUrl)
            .post("id=$id".toByteArray(Charsets.UTF_8))
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Origin", baseUrl)
            .header("Referer", "$baseUrl/home")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("User-Agent", NATIVE_USER_AGENT)
            .build()
            
        return withContext(Dispatchers.IO) {
            val playResponse = HttpClient.execute(playRequest)
            if (playResponse !is NetworkResult.Success) return@withContext emptyResult()
            
            val playJson = playResponse.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyResult()
            com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "play.php response: $playJson")
            
            val playRoot = JsonParser.parse(playJson) ?: return@withContext emptyResult()
            var h = JsonParser.string(playRoot, "h")
            
            var streamReferer = "https://net52.cc/"
            
            if (h == null) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Missing 'h' in play.php response")
                return@withContext emptyResult()
            }
            
            // Extract the domain from the 'h' URL to use as the Referer for the video stream
            // The CDN is very strict and requires this exact referer, otherwise it throws a 404
            try {
                val hUri = Uri.parse(h)
                if (hUri.scheme != null && hUri.host != null) {
                    streamReferer = "${hUri.scheme}://${hUri.host}/"
                }
            } catch (e: Exception) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to parse stream referer from h")
            }
            
            // If the backend returns a full URL to a dead domain (like net52.cc), extract the 'in=' part
            if (h.contains("in=")) {
                h = "in=" + h.substringAfter("in=")
                com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "Extracted token: $h")
            }
            
            val timestamp = System.currentTimeMillis() / 1000
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedH = java.net.URLEncoder.encode(h, "UTF-8")
            val playlistUrl = "$playlistBaseUrl?id=$id&t=$encodedTitle&tm=$timestamp&h=$encodedH"
            
            com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "Requesting playlist: $playlistUrl")
            val playlistRequest = RequestBuilder()
                .url(playlistUrl)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Origin", baseUrl)
                .header("Referer", "$baseUrl/home")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", NATIVE_USER_AGENT)
                .build()
                
            val playlistResp = HttpClient.execute(playlistRequest)
            if (playlistResp !is NetworkResult.Success) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "playlist.php failed with error")
                return@withContext emptyResult()
            }
            
            val playlistJson = playlistResp.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyResult()
            com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "playlist.php response: $playlistJson")
            
            val playlistArray = JsonParser.parse(playlistJson)?.takeIf { it.isJsonArray }?.asJsonArray
            if (playlistArray == null || playlistArray.isEmpty) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "playlist.php returned empty or invalid array")
                return@withContext emptyResult()
            }
            
            val item = playlistArray.get(0).asJsonObject
            val sourcesArray = item.getAsJsonArray("sources") ?: return@withContext emptyResult()
            
            val streams = mutableListOf<StreamLink>()
            for (sourceElement in sourcesArray) {
                val sourceObj = sourceElement.asJsonObject
                val file = sourceObj.get("file")?.asString ?: continue
                val label = sourceObj.get("label")?.asString ?: "Unknown"
                val type = sourceObj.get("type")?.asString ?: ""
                
                val streamUrl = if (file.startsWith("http")) file else baseUrl + file
                
                val isM3u8 = type.contains("mpegurl") || streamUrl.contains(".m3u8")
                val isDash = type.contains("dash") || streamUrl.contains(".mpd")
                
                val extractedHost = when {
                    isM3u8 -> com.streamflex.domain.models.HostType.M3U8
                    isDash -> com.streamflex.domain.models.HostType.DASH
                    else -> com.streamflex.domain.models.HostType.DIRECT
                }
                
                val extractedContentType = when {
                    isM3u8 -> com.streamflex.core.network.detector.ContentType.M3U8
                    isDash -> com.streamflex.core.network.detector.ContentType.DASH
                    else -> com.streamflex.core.network.detector.ContentType.VIDEO
                }
                
                streams.add(
                    StreamLink(
                        name = "${source.provider} - $label",
                        url = streamUrl,
                        host = extractedHost,
                        contentType = extractedContentType,
                        headers = mapOf(
                            "Referer" to streamReferer,
                            "Cookie" to "hd=on",
                            "User-Agent" to NATIVE_USER_AGENT
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
            val base = String(Base64.decode(encoded, Base64.DEFAULT)).trimEnd('/')
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
                        resolvedApiUrl = String(Base64.decode(tokenHash, Base64.DEFAULT)).trimEnd('/')
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
