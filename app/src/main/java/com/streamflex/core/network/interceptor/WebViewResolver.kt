package com.streamflex.core.network.interceptor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.streamflex.app.StreamFlexApplication
import com.streamflex.core.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object WebViewResolver {

    /**
     * Solves Cloudflare or custom anti-bot by loading the URL in a visible WebView Dialog.
     * Returns true if the anti-bot is solved (requiredCookie found) or if it times out.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun resolveUsingWebView(context: Context, url: String, requiredCookie: String = "cf_clearance"): Boolean {
        return kotlinx.coroutines.withTimeoutOrNull(25000L) {
            withContext(Dispatchers.Main) {
                kotlin.coroutines.suspendCoroutine { continuation ->
                    
                    val activity = StreamFlexApplication.topActivity ?: context
                    val webView = WebView(activity)
                    
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    // Build a beautiful dialog layout
                    val layout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.parseColor("#121212"))
                        setPadding(32, 32, 32, 32)
                    }
                    
                    val titleView = TextView(activity).apply {
                        text = "Bypassing Security Check..."
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 16)
                    }
                    
                    val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                        isIndeterminate = true
                        setPadding(0, 0, 0, 16)
                    }

                    // The WebView container
                    val webViewContainer = FrameLayout(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            600 // Fixed height for CAPTCHA to be visible
                        )
                        addView(webView)
                    }

                    layout.addView(titleView)
                    layout.addView(progressBar)
                    layout.addView(webViewContainer)

                    var dialog: Dialog? = null
                    if (activity is android.app.Activity && !activity.isFinishing) {
                        dialog = AlertDialog.Builder(activity)
                            .setView(layout)
                            .setCancelable(false) // Force user to solve it or wait
                            .create()
                            
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }

                    var isFinished = false

                    fun finishWithResult(success: Boolean) {
                        if (isFinished) return
                        isFinished = true
                        dialog?.dismiss()
                        
                        // Clean up WebView
                        webViewContainer.removeAllViews()
                        webView.destroy()
                        
                        continuation.resume(success)
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (isFinished) return
                            
                            val cookies = CookieManager.getInstance().getCookie(url)
                            if (cookies != null && cookies.contains(requiredCookie)) {
                                Logger.d("WebViewResolver", "Solved instantly on load!")
                                finishWithResult(true)
                            } else {
                                titleView.text = "Please solve the CAPTCHA..."
                                progressBar.visibility = android.view.View.GONE
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // Check periodically for required cookie on any resource load
                            val cookies = CookieManager.getInstance().getCookie(request?.url?.toString())
                            if (!isFinished && cookies != null && cookies.contains(requiredCookie)) {
                                Logger.d("WebViewResolver", "Solved via intercepted request!")
                                view?.post {
                                    finishWithResult(true)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // Pre-clean cookies for the domain to force a fresh token (important for NetMirror)
                    CookieManager.getInstance().setCookie(url, "$requiredCookie=; Max-Age=0")
                    CookieManager.getInstance().flush()

                    webView.loadUrl(url)
                }
            }
        } ?: false
    }
}
