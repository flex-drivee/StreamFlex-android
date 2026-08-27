package com.streamflex.core.network.interceptor

import android.webkit.CookieManager
import com.streamflex.app.StreamFlexApplication
import com.streamflex.core.logger.Logger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class CloudflareKiller : Interceptor {

    companion object {
        const val TAG = "CloudflareKiller"
        private val CLOUDFLARE_SERVERS = listOf("cloudflare-nginx", "cloudflare")
        private val ERROR_CODES = listOf(403, 503)
    }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val urlString = request.url.toString()

        // 1. Let the request proceed normally first
        val response = chain.proceed(request)

        // 2. Check if it hit Cloudflare protection
        val serverHeader = response.header("Server") ?: ""
        val isCloudflare = CLOUDFLARE_SERVERS.any { serverHeader.contains(it, ignoreCase = true) }

        if (isCloudflare && response.code in ERROR_CODES) {
            Logger.d(TAG, "Cloudflare protection detected at: $urlString. Attempting bypass...")
            response.close()

            // 3. Open WebView to solve CAPTCHA/JS Challenge
            val success = WebViewResolver.resolveUsingWebView(StreamFlexApplication.instance, urlString)
            
            if (success) {
                Logger.d(TAG, "Successfully bypassed Cloudflare for: $urlString")
                
                // 4. Get the solved cookies from the Android CookieManager
                val solvedCookies = CookieManager.getInstance().getCookie(urlString) ?: ""
                
                // 5. Re-run the request with the new solved cookies and WebView User-Agent
                val newRequest = request.newBuilder()
                    .header("Cookie", solvedCookies)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0")
                    .build()
                
                return@runBlocking chain.proceed(newRequest)
            } else {
                Logger.w(TAG, "Failed to bypass Cloudflare for: $urlString")
                // Return original failed response if bypass fails
                return@runBlocking chain.proceed(request)
            }
        }

        // If not blocked by Cloudflare, just return normal response
        return@runBlocking response
    }
}
