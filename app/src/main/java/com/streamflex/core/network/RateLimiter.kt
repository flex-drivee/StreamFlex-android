package com.streamflex.core.network

import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * RateLimiter
 *
 * Enforces provider-declared rate limits so StreamFlex never gets banned.
 *
 * ─── Architecture Context ──────────────────────────────────────────────────
 * This class implements the rate-limiting rules defined in the frozen
 * architecture (Part 10, Security) and the Provider Capability Matrix
 * (the isSequential + sequentialDelayMs fields from provider definitions).
 *
 * ─── Two Constraint Types ──────────────────────────────────────────────────
 *
 * 1. Concurrency limit [maxConcurrent]:
 *    Limits how many in-flight requests can be active simultaneously
 *    for a given provider. Implemented with a [Semaphore].
 *    Example: HDHub4u → maxConcurrent=3 means at most 3 parallel requests.
 *    (If the semaphore is full, the caller suspends until a permit is free.)
 *
 * 2. Sequential delay [delayBetweenMs]:
 *    When a provider sets isSequential=true, requests must not only be
 *    serialised (maxConcurrent=1) but also have a minimum gap between them.
 *    Example: A provider with sequentialDelayMs=1500 needs at least 1.5s
 *    between each request to avoid their anti-scraping detection.
 *
 * ─── Isolation ────────────────────────────────────────────────────────────
 * Each provider gets its own [Semaphore] and last-request timestamp.
 * A slow or sequential provider never blocks another provider's requests.
 *
 * ─── Usage ────────────────────────────────────────────────────────────────
 * ```kotlin
 * // Acquire before making the request:
 * rateLimiter.throttle(providerId, maxConcurrent = 3, delayBetweenMs = 0) {
 *     StreamFlexHttpClient.get(url)
 * }
 * ```
 *
 * The engine (StreamOrchestrator) calls this; providers never call it directly.
 *
 * ─── Inspired by CloudStream ──────────────────────────────────────────────
 * CloudStream doesn't have a central rate limiter — providers manage their own
 * delays individually, leading to inconsistent behaviour. StreamFlex fixes this
 * by centralising all rate-limit logic here.
 */
class RateLimiter {

    private val semaphores   = ConcurrentHashMap<String, Semaphore>()
    private val lastRequests = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "RateLimiter"
    }

    /**
     * Throttles a network operation for [providerId].
     *
     * 1. Acquires a semaphore permit (blocks if [maxConcurrent] is saturated).
     * 2. If [delayBetweenMs] > 0, waits until the minimum gap since the
     *    last request for this provider has elapsed.
     * 3. Executes [block] (the network call).
     * 4. Records the completion timestamp.
     * 5. Releases the semaphore permit.
     *
     * @param providerId     Unique provider ID (e.g. "hdhub4u").
     * @param maxConcurrent  Maximum parallel requests for this provider.
     * @param delayBetweenMs Minimum ms between consecutive requests.
     * @param block          The suspended operation to throttle.
     * @return               The result of [block].
     */
    suspend fun <T> throttle(
        providerId     : String,
        maxConcurrent  : Int  = Constants.DEFAULT_MAX_CONCURRENT,
        delayBetweenMs : Long = 0L,
        block          : suspend () -> T
    ): T {
        val semaphore = semaphores.getOrPut(providerId) {
            Semaphore(permits = maxConcurrent)
        }

        return semaphore.withPermit {

            // Enforce minimum delay between requests (for sequential providers)
            if (delayBetweenMs > 0) {
                val last    = lastRequests[providerId] ?: 0L
                val elapsed = System.currentTimeMillis() - last
                val waitMs  = delayBetweenMs - elapsed

                if (waitMs > 0) {
                    Logger.d(
                        message = "[$providerId] Rate limit: waiting ${waitMs}ms",
                        tag     = TAG
                    )
                    delay(waitMs)
                }
            }

            try {
                block()
            } finally {
                lastRequests[providerId] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Convenience overload that takes a [RateLimitConfig] directly.
     * Used by StreamOrchestrator when it has a parsed provider definition.
     */
    suspend fun <T> throttle(
        providerId : String,
        config     : RateLimitConfig,
        block      : suspend () -> T
    ): T = throttle(
        providerId     = providerId,
        maxConcurrent  = config.maxConcurrent,
        delayBetweenMs = config.delayBetweenMs,
        block          = block
    )

    /** Clears all state for a provider (useful for testing). */
    fun reset(providerId: String) {
        semaphores.remove(providerId)
        lastRequests.remove(providerId)
    }

    /** Clears all state. */
    fun resetAll() {
        semaphores.clear()
        lastRequests.clear()
    }
}

/**
 * Rate limit configuration for a provider.
 * Parsed directly from ProviderDefinition.rateLimit in the JSON manifest.
 */
data class RateLimitConfig(
    val maxConcurrent  : Int  = Constants.DEFAULT_MAX_CONCURRENT,
    val delayBetweenMs : Long = 0L
) {
    companion object {
        val DEFAULT = RateLimitConfig()

        /** Sequential provider — one request at a time with a 1.5s gap. */
        val SEQUENTIAL = RateLimitConfig(
            maxConcurrent  = 1,
            delayBetweenMs = Constants.DEFAULT_SEQUENTIAL_DELAY_MS
        )
    }
}
