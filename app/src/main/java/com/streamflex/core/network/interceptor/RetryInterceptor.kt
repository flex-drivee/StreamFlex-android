package com.streamflex.core.network.interceptor

import com.streamflex.core.constants.Constants
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * RetryInterceptor
 *
 * Automatically retries failed requests up to [maxRetries] times with
 * exponential backoff.
 *
 * Key design decisions (aligned with CloudStream's approach):
 * - Only retries on IOException (network failure, timeout, DNS).
 *   Does NOT retry on HTTP errors (4xx/5xx) — those are provider-logic failures,
 *   not transient network issues.
 * - Uses exponential backoff: attempt 1 → 500ms, attempt 2 → 1000ms.
 *   This reduces hammering overloaded provider servers.
 * - [maxRetries] is kept low (2) because providers may ban IPs on excess requests.
 */
class RetryInterceptor(
    private val maxRetries: Int = Constants.MAX_RETRY_ATTEMPTS,
    private val baseDelayMs: Long = Constants.BASE_RETRY_DELAY_MS
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

                if (attempt <= maxRetries) {
                    val backoffMs = baseDelayMs * attempt
                    Thread.sleep(backoffMs)
                }
            }
        }

        throw lastException ?: IOException("Unknown network error after $maxRetries retries")
    }
}