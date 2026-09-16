package com.cinetheta.core.network.interceptor

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
import com.cinetheta.app.CineThetaApplication
import com.cinetheta.core.logger.Logger
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
                    val targetUrl = url
                    
                    val activity = CineThetaApplication.topActivity ?: context
                    val webView = WebView(activity)
                    
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        
                        // Use the real Android WebView User-Agent to prevent Cloudflare Turnstile from looping!
                        // If we spoof a Windows Firefox agent, Turnstile's JS fingerprinting detects the mismatch and loops forever.
                        val defaultAgent = WebSettings.getDefaultUserAgent(activity)
                        userAgentString = "$defaultAgent /OS.Gatu v3.0"
                        
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    // Build a beautiful dialog layout
                    val layout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.parseColor("#121212"))
                        setPadding(32, 32, 32, 32)
                    }

                    var isFinished = false
                    var dialog: Dialog? = null

                    val headerLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setPadding(0, 0, 0, 16)
                    }
                    
                    val titleView = TextView(activity).apply {
                        text = "Bypassing Security Check..."
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val closeButton = TextView(activity).apply {
                        text = "✕"
                        setTextColor(Color.parseColor("#BBBBBB"))
                        textSize = 20f
                        setPadding(16, 4, 16, 4)
                        isClickable = true
                        setOnClickListener {
                            if (!isFinished) {
                                isFinished = true
                                dialog?.dismiss()
                                continuation.resume(false)
                            }
                        }
                    }

                    headerLayout.addView(titleView)
                    headerLayout.addView(closeButton)
                    
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

                    layout.addView(headerLayout)
                    layout.addView(progressBar)
                    layout.addView(webViewContainer)

                    if (activity is android.app.Activity && !activity.isFinishing) {
                        dialog = AlertDialog.Builder(activity)
                            .setView(layout)
                            .setCancelable(true)
                            .setOnCancelListener {
                                if (!isFinished) {
                                    isFinished = true
                                    continuation.resume(false)
                                }
                            }
                            .create()
                            
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }

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
                            
                            val cookies = CookieManager.getInstance().getCookie(targetUrl)
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
                            // We must check the original url (urlToLoad) as well, because the page might have
                            // redirected to a different domain (like net77.cc) while setting the cookie on net52.cc.
                            val cookies = CookieManager.getInstance().getCookie(targetUrl)
                            if (!isFinished && cookies != null && cookies.contains(requiredCookie)) {
                                Logger.d("WebViewResolver", "Solved instantly on load!")
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
