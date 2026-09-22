package com.cinetheta.core.network.interceptor

import android.webkit.CookieManager
import com.cinetheta.app.CineThetaApplication
import com.cinetheta.core.logger.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

class CloudflareKiller : Interceptor {
    companion object {
        private const val TAG = "CloudflareKiller"
        private val ERROR_CODES = listOf(403, 503)
        private val CF_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
        
        // Cache to prevent infinite CAPTCHA loops when downloading video chunks
        private val savedCookies = ConcurrentHashMap<String, String>()
        
        // Mutex to prevent ExoPlayer from spawning 10 concurrent WebViews for different .ts segments
        private val mutex = Mutex()
    }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        var request = chain.request()
        val urlString = request.url.toString()
        // Helper to get root domain (e.g. s21.freecdn.top -> freecdn.top)
        val host = request.url.host
        val rootDomain = host.split(".").takeLast(2).joinToString(".")

        // Pre-apply saved cookies if we already solved CF for this host
        val knownCookies = savedCookies.entries.firstOrNull { rootDomain.endsWith(it.key) || it.key.endsWith(rootDomain) }?.value

        if (knownCookies != null) {
            val existingCookie = request.header("Cookie") ?: ""
            val mergedCookie = if (existingCookie.isNotEmpty()) {
                "$existingCookie; $knownCookies"
            } else {
                knownCookies
            }
            
            request = request.newBuilder()
                .header("Cookie", mergedCookie)
                .header("User-Agent", CF_USER_AGENT)
                .build()
        }

        var response = chain.proceed(request)

        // Do not attempt WebView challenge solving for HEAD probes, JSON APIs, or manifests
        if (request.method.equals("HEAD", ignoreCase = true) ||
            urlString.contains("/collections/post/documents/search") ||
            urlString.contains("raw.githubusercontent.com") ||
            urlString.endsWith(".json") ||
            request.header("Accept")?.contains("application/json") == true) {
            return@runBlocking response
        }

        // 1. Check if we hit a Cloudflare 403/503 block or 200 OK challenge page
        val isCloudflare = response.header("Server")?.contains("cloudflare", ignoreCase = true) == true
        
        var isChallenge = false
        if (isCloudflare) {
            if (response.code in ERROR_CODES) {
                try {
                    val bodyString = response.peekBody(1024 * 50).string()
                    isChallenge = bodyString.contains("cf-browser-verification") ||
                                  bodyString.contains("cf-turnstile") ||
                                  bodyString.contains("challenges.cloudflare.com") ||
                                  bodyString.contains("just a moment", ignoreCase = true) ||
                                  // Cloudflare Bot Management returns a plain 403 with no challenge body
                                  // (e.g. toon-stream.site). Treat any Cloudflare 403/503 as a challenge
                                  // so we can warm up the cookie via WebView.
                                  bodyString.length < 5000
                } catch (e: Exception) {
                    // If we can't read the body, assume it's a CF block
                    isChallenge = true
                }
            } else if (response.code == 200) {
                try {
                    val bodyString = response.peekBody(1024 * 30).string()
                    isChallenge = bodyString.contains("cf-turnstile") ||
                                  bodyString.contains("cf-browser-verification") ||
                                  (bodyString.contains("challenges.cloudflare.com") && bodyString.contains("turnstile")) ||
                                  (bodyString.contains("just a moment", ignoreCase = true) && bodyString.contains("cloudflare", ignoreCase = true))
                } catch (_: Exception) {
                    isChallenge = false
                }
            }
        }
        
        if (isChallenge) {
            response.close()
            
            // 2. Lock so that only ONE thread spawns a WebView at a time!
            mutex.withLock {
                // Check if another thread JUST solved it while we were waiting in the queue
                val newlySolvedCookies = savedCookies.entries.firstOrNull { rootDomain.endsWith(it.key) || it.key.endsWith(rootDomain) }?.value
                if (newlySolvedCookies != null) {
                    val existingCookie = request.header("Cookie") ?: ""
                    val mergedCookie = if (existingCookie.isNotEmpty()) {
                        "$existingCookie; $newlySolvedCookies"
                    } else {
                        newlySolvedCookies
                    }
                    
                    val newRequest = request.newBuilder()
                        .header("Cookie", mergedCookie)
                        .header("User-Agent", CF_USER_AGENT)
                        .build()
                    
                    return@runBlocking chain.proceed(newRequest)
                }

                // 3. Spawns WebView because we're the first thread to get blocked
                Logger.d(TAG, "Cloudflare protection detected at: $urlString. Attempting bypass...")
                
                val context = CineThetaApplication.instance
                
                val success = WebViewResolver.resolveUsingWebView(
                    context = context,
                    url = urlString
                )
                
                if (success) {
                    Logger.d(TAG, "Successfully bypassed Cloudflare for: $urlString")
                    
                    // 4. Get the solved cookies from the Android CookieManager
                    val solvedCookies = CookieManager.getInstance().getCookie(urlString) ?: ""
                    
                    // Save it for future requests to this host (to prevent CAPTCHA loops on video segments)
                    savedCookies[rootDomain] = solvedCookies
                    
                    // 5. Re-run the request with the new solved cookies and WebView User-Agent
                    val existingCookie = request.header("Cookie") ?: ""
                    val mergedCookie = if (existingCookie.isNotEmpty()) {
                        "$existingCookie; $solvedCookies"
                    } else {
                        solvedCookies
                    }
                    
                    val newRequest = request.newBuilder()
                        .header("Cookie", mergedCookie)
                        .header("User-Agent", CF_USER_AGENT)
                        .build()
                    
                    return@runBlocking chain.proceed(newRequest)
                }
            }
        }
        
        return@runBlocking response
    }
}
