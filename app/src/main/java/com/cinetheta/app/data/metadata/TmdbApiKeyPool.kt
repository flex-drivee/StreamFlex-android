package com.cinetheta.app.data.metadata

import android.util.Log
import com.cinetheta.app.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe TMDB API Key Pool manager.
 *
 * Distributes requests evenly across multiple developer API keys using round-robin,
 * and handles cooldown tracking if any key encounters rate-limiting (HTTP 429)
 * or quota exhaustion.
 */
object TmdbApiKeyPool {

    private const val TAG = "TmdbApiKeyPool"
    private const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 minutes cooldown on 429

    // All available TMDB v3 developer API keys
    private val keys: List<String> = listOf(
        "938e6a6becd5cb517ec3267ea0e736fa",
        "eaa97563a1d87c75a203f24368b1e420",
        "f996a57ef8896650d793311e867d372e",
        "77fd2f3fa58f2d69ba7f815b580b3de7"
    ).let { list ->
        val buildKey = BuildConfig.TMDB_API_KEY.trim()
        if (buildKey.isNotEmpty() && !list.contains(buildKey)) {
            list + buildKey
        } else {
            list
        }
    }

    private val currentIndex = AtomicInteger(0)
    private val cooldownMap = ConcurrentHashMap<String, Long>()

    val size: Int get() = keys.size

    /**
     * Retrieves the next healthy API key in round-robin sequence.
     * Skips keys that are currently cooling down after a rate limit,
     * unless all keys are cooling down (in which case the oldest cooling key is used).
     */
    fun getNextKey(): String {
        if (keys.isEmpty()) return ""
        if (keys.size == 1) return keys[0]

        val now = System.currentTimeMillis()
        val startIndex = (currentIndex.getAndIncrement() and Int.MAX_VALUE) % keys.size

        // First pass: look for a key not in cooldown
        for (i in keys.indices) {
            val key = keys[(startIndex + i) % keys.size]
            val cooldownUntil = cooldownMap[key] ?: 0L
            if (now >= cooldownUntil) {
                return key
            }
        }

        // Fallback: If all keys are in cooldown, use the one that entered cooldown earliest
        Log.w(TAG, "All TMDB keys are in cooldown, using fallback key.")
        return keys[startIndex]
    }

    /**
     * Marks an API key for temporary cooldown following an HTTP 429 or auth error.
     */
    fun markCooldown(key: String, durationMs: Long = COOLDOWN_DURATION_MS) {
        val until = System.currentTimeMillis() + durationMs
        cooldownMap[key] = until
        Log.w(TAG, "TMDB key ${key.take(8)}... marked in cooldown for ${durationMs / 1000}s")
    }

    /**
     * Resets cooldown for a specific key or all keys.
     */
    fun resetCooldowns() {
        cooldownMap.clear()
    }
}
