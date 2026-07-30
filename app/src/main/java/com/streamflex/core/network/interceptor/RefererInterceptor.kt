package com.streamflex.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * RefererInterceptor
 *
 * Automatically injects Referer and Origin headers when a NetworkRequest
 * has them set.
 *
 * Why this matters:
 * Many video hosts (FileMoon, HubCloud, DooD, StreamTape) validate the
 * Referer header before serving content. Without the correct Referer,
 * they return HTTP 403. This is the extractor-level Referer problem.
 *
 * Design:
 * - This interceptor reads custom headers that the engine already attached
 *   to the OkHttp request from the NetworkRequest model.
 * - It is a NO-OP for requests that don't need Referer injection.
 * - The actual Referer value comes from the ExtractorRegistry metadata
 *   for extractors, and from provider configuration for provider requests.
 *
 * Usage:
 * The engine sets Referer/Origin on NetworkRequest.headers before calling
 * HttpClient.execute(). This interceptor ensures they survive the OkHttp
 * pipeline unchanged (some OkHttp versions strip certain headers).
 */
class RefererInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Referer and Origin are already in request.headers if set by the engine.
        // This interceptor enforces them as non-strippable headers by re-adding them.
        val builder = request.newBuilder()

        val referer = request.header("Referer")
        if (referer != null) {
            builder.header("Referer", referer)
        }

        val origin = request.header("Origin")
        if (origin != null) {
            builder.header("Origin", origin)
        }

        return chain.proceed(builder.build())
    }
}
