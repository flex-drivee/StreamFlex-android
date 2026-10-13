package com.cinetheta.app.data.metadata

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ensures GET responses from TMDB (popular, trending, discover, details)
 * are cached locally so repeated requests by multiple screens / viewmodels
 * do not hit the network or burn API quota.
 */
class TmdbCacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.method.equals("GET", ignoreCase = true) && 
            request.url.host.contains("themoviedb.org") && 
            response.isSuccessful
        ) {
            // Cache successful TMDB queries for 2 hours (7200 seconds)
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=7200")
                .removeHeader("Pragma")
                .build()
        }

        return response
    }
}
