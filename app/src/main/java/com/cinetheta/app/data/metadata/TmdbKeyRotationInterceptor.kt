package com.cinetheta.app.data.metadata

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that automatically injects a rotated TMDB API key from [TmdbApiKeyPool]
 * into requests targeting api.themoviedb.org, and automatically retries with an alternate
 * healthy key if HTTP 429 (Too Many Requests), 401, or 403 is received.
 */
class TmdbKeyRotationInterceptor : Interceptor {

    companion object {
        private const val TAG = "TmdbKeyRotation"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        if (!originalUrl.host.contains("themoviedb.org")) {
            return chain.proceed(originalRequest)
        }

        var currentKey = originalUrl.queryParameter("api_key") ?: TmdbApiKeyPool.getNextKey()
        var newUrl = originalUrl.newBuilder().setQueryParameter("api_key", currentKey).build()
        var request = originalRequest.newBuilder().url(newUrl).build()

        var response = chain.proceed(request)
        var attempts = 0
        val maxAttempts = TmdbApiKeyPool.size

        while ((response.code == 429 || response.code == 401 || response.code == 403) && attempts < maxAttempts) {
            Log.w(TAG, "TMDB request failed with code ${response.code} on key ${currentKey.take(8)}..., rotating key (attempt ${attempts + 1}/$maxAttempts)")
            response.close()
            attempts++

            TmdbApiKeyPool.markCooldown(currentKey)
            currentKey = TmdbApiKeyPool.getNextKey()

            newUrl = originalUrl.newBuilder().setQueryParameter("api_key", currentKey).build()
            request = originalRequest.newBuilder().url(newUrl).build()
            response = chain.proceed(request)
        }

        return response
    }
}
