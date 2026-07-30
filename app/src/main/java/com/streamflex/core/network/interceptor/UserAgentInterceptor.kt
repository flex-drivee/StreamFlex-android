package com.streamflex.core.network.interceptor

import com.streamflex.core.constants.Constants
import okhttp3.Interceptor
import okhttp3.Response

/**
 * UserAgentInterceptor
 *
 * Injects the StreamFlex default User-Agent on every outgoing request.
 *
 * Providers can override this per-request by setting the "User-Agent" header
 * explicitly in their NetworkRequest — this interceptor only sets it if not
 * already present (via addHeader, which is non-destructive).
 *
 * Inspired by CloudStream's approach: a single global UA that matches
 * real Chrome to avoid provider bot-detection.
 */
class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Only inject if not already set by the provider/extractor
        val modified = if (original.header("User-Agent") == null) {
            original.newBuilder()
                .addHeader("User-Agent", Constants.DEFAULT_USER_AGENT)
                .build()
        } else {
            original
        }

        return chain.proceed(modified)
    }
}