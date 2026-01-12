package com.streamflex.app.data.network

import okhttp3.OkHttpClient
import okhttp3.Request

object HttpClient {
    private val client = OkHttpClient()

    fun get(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
