package com.streamflex.core.network.interceptor

import com.streamflex.core.logger.Logger
import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        Logger.d(
            "➡ ${request.method} ${request.url}"
        )

        val response = chain.proceed(request)

        Logger.d(
            "⬅ ${response.code} ${request.url}"
        )

        return response
    }
}