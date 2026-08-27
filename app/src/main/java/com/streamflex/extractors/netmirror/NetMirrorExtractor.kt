package com.streamflex.extractors.netmirror

import android.net.Uri
import android.util.Base64
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

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

    private suspend fun resolveApiUrl(tHashT: String): String? {
        if (resolvedApiUrl.isNotBlank()) return resolvedApiUrl

        for (encoded in newTvDomains) {
            val base = String(Base64.decode(encoded, Base64.DEFAULT)).trimEnd('/')
            val request = RequestBuilder()
                .url("$base/checknewtv.php")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0")
                .header("Cookie", "t_hash_t=$tHashT")
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

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // source.url format: netmirror://player?id=$id&ott=$ott&base=$baseUrl&title=$title
        val uri = Uri.parse(source.url)
        val id = uri.getQueryParameter("id") ?: return emptyResult()
        val ott = uri.getQueryParameter("ott") ?: return emptyResult()
        
        val NATIVE_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0"
        
        return withContext(Dispatchers.IO) {
            // Step 1: Bypass Cloudflare entirely using the net52.cc POST backdoor!
            var tHashT = ""
            try {
                // net52.cc allows POST requests without Cloudflare Turnstile blocks!
                // Sending a fake g-recaptcha-response generates a valid t_hash_t cookie.
                val postBody = okhttp3.FormBody.Builder()
                    .add("g-recaptcha-response", UUID.randomUUID().toString())
                    .build()
                
                val req = okhttp3.Request.Builder()
                    .url("https://net52.cc/verify.php")
                    .post(postBody)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://net22.cc")
                    .header("Referer", "https://net22.cc/verify2")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
                    .build()

                val client = com.streamflex.core.network.StreamFlexHttpClient.okHttpClient
                    .newBuilder()
                    .followRedirects(false)
                    .build()
                    
                val res = client.newCall(req).execute()
                val cookies = res.headers("Set-Cookie")
                res.close()
                
                for (cookie in cookies) {
                    if (cookie.startsWith("t_hash_t=")) {
                        val value = cookie.substringAfter("t_hash_t=").substringBefore(";")
                        if (value.isNotBlank()) {
                            tHashT = value
                            com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "Successfully bypassed CF using net52.cc backdoor! t_hash_t: $tHashT")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to backdoor net52.cc: ${e.message}")
            }
            
            if (tHashT.isBlank()) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to acquire t_hash_t cookie via backdoor!")
                return@withContext emptyResult()
            }

            // Step 2: Resolve API URL
            val apiBase = resolveApiUrl(tHashT) ?: return@withContext emptyResult()
            
            // Step 3: Fetch Player API
            val request = RequestBuilder()
                .url("$apiBase/newtv/player.php?id=$id")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("X-Requested-With", "NetmirrorNewTV v1.0")
                .header("User-Agent", NATIVE_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Ott", ott)
                .header("Usertoken", "")
                .header("Cookie", "t_hash_t=$tHashT")
                .build()
                
            val response = HttpClient.execute(request)
            if (response !is NetworkResult.Success) return@withContext emptyResult()
            
            val json = response.data.body?.toString(Charsets.UTF_8) ?: return@withContext emptyResult()
            com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "newtv/player.php response: $json")
            
            val root = JsonParser.parse(json) ?: return@withContext emptyResult()
            val status = JsonParser.string(root, "status")
            val videoLink = JsonParser.string(root, "video_link")
            val referer = JsonParser.string(root, "referer") ?: apiBase
            
            if (videoLink.isNullOrBlank() || (status != "ok" && status != "otp")) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to extract from newtv API, status: $status")
                return@withContext emptyResult()
            }
            
            val streams = listOf(
                StreamLink(
                    name = "${source.provider} - Auto",
                    url = videoLink,
                    host = com.streamflex.domain.models.HostType.M3U8,
                    contentType = com.streamflex.core.network.detector.ContentType.M3U8,
                    headers = mapOf(
                        "Referer" to referer,
                        "Cookie" to "hd=on; t_hash_t=$tHashT",
                        "User-Agent" to NATIVE_USER_AGENT
                    )
                )
            )
            
            result(streams)
        }
    }
}
