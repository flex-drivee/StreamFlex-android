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
    
    // We must use an Android User-Agent with /OS.Gatu v3.0 so the CDN doesn't serve the harassment video.
    private val NATIVE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0"

    private suspend fun resolveApiUrl(tHashT: String): String? {
        if (resolvedApiUrl.isNotBlank()) return resolvedApiUrl

        for (encoded in newTvDomains) {
            val base = String(Base64.decode(encoded, Base64.DEFAULT)).trimEnd('/')
            val request = RequestBuilder()
                .url("$base/checknewtv.php")
                .header("User-Agent", NATIVE_USER_AGENT)
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
        val baseUrl = uri.getQueryParameter("base")?.trimEnd('/') ?: com.streamflex.providers.netmirror.NetMirrorConfig.DEFAULT_DOMAIN
        
        return withContext(Dispatchers.IO) {
            // Step 1: Get t_hash_t cookie using WebViewResolver
            // The net52.cc UUID POST backdoor is PATCHED by NetMirror (returns a fake token leading to harassment video).
            // We MUST solve the CAPTCHA legitimately using the WebViewResolver.
            var tHashT = ""
            val verifyUrl = "$baseUrl/verify.php"
            
            // Try to resolve using WebView. 
            // Note: WebViewResolver now uses the real Android User-Agent so Turnstile doesn't loop!
            val solved = com.streamflex.core.network.interceptor.WebViewResolver.resolveUsingWebView(
                com.streamflex.app.StreamFlexApplication.instance,
                verifyUrl,
                requiredCookie = "t_hash_t"
            )
            
            if (solved) {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(verifyUrl) ?: ""
                val match = Regex("t_hash_t=([^;]+)").find(cookies)
                if (match != null) {
                    tHashT = match.groupValues[1]
                    com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "WebView Bypass success, got t_hash_t: $tHashT")
                }
            }
            
            if (tHashT.isBlank()) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to acquire valid t_hash_t cookie via WebViewResolver!")
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
