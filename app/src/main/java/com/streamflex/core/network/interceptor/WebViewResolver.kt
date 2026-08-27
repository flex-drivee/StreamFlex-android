package com.streamflex.core.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object WebViewResolver {

    /**
     * Solves Cloudflare or custom anti-bot by loading the URL in a headless WebView.
     * Returns true if the anti-bot is solved (requiredCookie found) or if it times out.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun resolveUsingWebView(context: Context, url: String, requiredCookie: String = "cf_clearance"): Boolean = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            var isFinished = false

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (isFinished) return
                    
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (cookies != null && cookies.contains(requiredCookie)) {
                        isFinished = true
                        webView.destroy()
                        continuation.resume(true)
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    // Check periodically for required cookie on any resource load
                    val cookies = CookieManager.getInstance().getCookie(request?.url?.toString())
                    if (!isFinished && cookies != null && cookies.contains(requiredCookie)) {
                        isFinished = true
                        view?.post {
                            view.destroy()
                            continuation.resume(true)
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            webView.loadUrl(url)

            // Timeout after 15 seconds
            webView.postDelayed({
                if (!isFinished) {
                    isFinished = true
                    webView.destroy()
                    continuation.resume(false)
                }
            }, 15000)
        }
    }
}
