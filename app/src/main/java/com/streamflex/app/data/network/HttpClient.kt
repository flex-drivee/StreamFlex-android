package com.streamflex.app.data.network

import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object HttpClient {

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0"

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(logging)
            .build()
    }

    fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val request = Request.Builder()
            .url(url)
            .apply {
                header("User-Agent", DEFAULT_USER_AGENT)
                headers.forEach { (k, v) -> header(k, v) }
            }
            .build()

        return client.newCall(request).execute()
    }

    fun post(url: String, body: okhttp3.RequestBody, headers: Map<String, String> = emptyMap()): Response {
        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply {
                header("User-Agent", DEFAULT_USER_AGENT)
                headers.forEach { (k, v) -> header(k, v) }
            }
            .build()

        return client.newCall(request).execute()
    }

    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
    }
}
