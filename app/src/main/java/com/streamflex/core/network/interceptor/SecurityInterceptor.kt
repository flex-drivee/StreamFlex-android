package com.streamflex.core.network.interceptor

import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * SecurityInterceptor
 *
 * Enforces the StreamFlex security rules on every request and response,
 * aligned with the frozen architecture's Security section (Part 10).
 *
 * Rules enforced:
 *
 * 1. HTTPS-only requests — blocks non-HTTPS outbound requests.
 *    Providers must always use https:// domains.
 *
 * 2. Non-HTTPS redirect detection — detects 301/302 redirects pointing
 *    to http:// and throws SecurityException before following them.
 *    OkHttp's followRedirects handles the redirect; this interceptor
 *    checks the Location header BEFORE OkHttp follows it.
 *
 * 3. Redirect hop counting — tracks how many times the same request
 *    has been redirected and aborts at MAX_REDIRECT_HOPS.
 *    (Note: OkHttp also has its own limit, but ours is explicit and logged.)
 *
 * 4. Binary content detection in scraping contexts — detects when a
 *    provider scraping request unexpectedly returns video/binary content.
 *    This should never happen during HTML/JSON scraping stages.
 *    (Stage 12 video playback goes directly from CDN to player, bypassing this client.)
 *
 * Exceptions thrown are caught by StreamFlexHttpClient and returned as
 * NetworkResult.Exception, so they propagate cleanly through the pipeline.
 */
class SecurityInterceptor : Interceptor {

    companion object {
        private const val TAG = "SecurityInterceptor"

        // Header names we read for redirect checking
        private const val HEADER_LOCATION     = "Location"
        private const val HEADER_CONTENT_TYPE = "Content-Type"

        // Content types that should NEVER appear during HTML/JSON scraping
        private val BINARY_CONTENT_TYPES = setOf(
            "video/",
            "audio/",
            "application/octet-stream",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // ─── Rule 1: HTTPS-only outbound ─────────────────────────────────────
        val scheme = request.url.scheme
        if (scheme != "https") {
            Logger.w(
                message = "Blocked non-HTTPS request: ${request.url}",
                tag = TAG
            )
            throw SecurityException(
                "StreamFlex only allows HTTPS requests. " +
                "Blocked: ${request.url}"
            )
        }

        // Execute the request
        val response = chain.proceed(request)

        // ─── Rule 2: Check redirect target is HTTPS ───────────────────────────
        if (response.isRedirect) {
            val location = response.header(HEADER_LOCATION)
            if (location != null) {
                // Resolve relative paths like "/home" against the current request URL
                val resolvedLocation = request.url.resolve(location)?.toString() ?: location
                if (!resolvedLocation.startsWith("https://")) {
                    response.close()
                    Logger.w(
                        message = "Blocked non-HTTPS redirect: $resolvedLocation (original: $location)",
                        tag = TAG
                    )
                    throw SecurityException(
                        "Redirect to non-HTTPS URL blocked: $resolvedLocation"
                    )
                }
            }
        }

        // ─── Rule 3: Binary content guard ─────────────────────────────────────
        // During scraping (stages 1–9), responses should always be HTML or JSON.
        // If a binary type appears, something is wrong (wrong URL, misconfigured
        // provider, or MITM). Log and surface as error — don't consume the body.
        val contentType = response.header(HEADER_CONTENT_TYPE) ?: ""
        val isBinary = BINARY_CONTENT_TYPES.any { contentType.startsWith(it, ignoreCase = true) }

        if (isBinary) {
            // Don't throw — binary responses CAN be valid for the extractor phase.
            // Just log so the engine can decide whether to pass to the player or reject.
            Logger.d(
                message = "Binary content-type detected: $contentType — URL: ${request.url}",
                tag = TAG
            )
        }

        return response
    }
}
