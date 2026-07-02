package com.streamflex.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(
    private val maxRetries: Int = 2
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {

            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                lastException = e
                attempt++
            }
        }

        throw lastException ?: IOException("Unknown network error")
    }
}