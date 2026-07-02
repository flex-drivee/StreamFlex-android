package com.streamflex.core.network.interceptor

import com.streamflex.core.constants.Constants
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
            .newBuilder()
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        return chain.proceed(request)
    }
}