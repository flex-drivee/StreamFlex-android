package com.streamflex.extractors.netmirror

import android.util.Base64
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.utils.StreamLogger
import kotlinx.coroutines.delay
import kotlin.math.random

/**
 * NetMirrorBypassManager
 *
 * Implements a fully programmatic, no-UI bypass of the NetMirror session
 * verification. This replaces the legacy WebViewResolver approach.
 *
 * Verified bypass sequence (reverse-engineered from CNC Verse Mobile plugin):
 *   1. GET /mobile/home?app=1  -> extract `data-addhash` from <body>
 *   2. GET userver.net52.cc with the hash   -> triggers server-side timer
 *   3. Loop (max 7x, 10s delay each) POST /mobile/verify2.php
 *      until response contains `"statusup":"All Done"`
 *   4. Read `t_hash_t` cookie from the successful POST response
 *
 * Key spoofing requirement: X-Requested-With: app.netmirror.netmirrornew
 * This tricks the NetMirror CDN into treating us as their native Android app,
 * bypassing Cloudflare Turnstile entirely.
 *
 * Cookie TTL: The `t_hash_t` cookie is valid for ~15 hours (54,000,000 ms).
 * We cache it and reuse it until it expires.
 */
object NetMirrorBypassManager {

    private const val TAG = "NetMirrorBypassManager"

    // Matches the Mobile UA used in CNC Verse Mobile (RMX2117 build)
    const val NATIVE_UA =
        "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 " +
        "Mobile Safari/537.36 /OS.Gatu v3.0"

    // Max number of verify2.php poll iterations (7 × 10s = 70s max wait)
    private const val MAX_VERIFY_LOOPS = 7

    // Cookie validity window: 15 hours (same as native app, verified from source)
    private const val COOKIE_TTL_MS = 54_000_000L

    // In-memory cache: Pair<token, timestamp_acquired>
    @Volatile private var cachedToken: String = ""
    @Volatile private var cachedTokenTimestamp: Long = 0L

    /**
     * Returns a valid `t_hash_t` session token.
     * Uses cached token if still within TTL, otherwise runs full bypass.
     *
     * @param baseUrl  The active NetMirror base URL (e.g. "https://net52.cc")
     * @return         A valid t_hash_t string, or null on failure
     */
    suspend fun getToken(baseUrl: String): String? {
        // Return cached token if still valid
        val now = System.currentTimeMillis()
        if (cachedToken.isNotBlank() && (now - cachedTokenTimestamp) < COOKIE_TTL_MS) {
            StreamLogger.debug(TAG, "Using cached t_hash_t (age: ${now - cachedTokenTimestamp}ms)")
            return cachedToken
        }

        StreamLogger.debug(TAG, "No valid cached token. Starting silent bypass on $baseUrl ...")
        val token = runBypass(baseUrl)
        if (!token.isNullOrBlank()) {
            cachedToken = token
            cachedTokenTimestamp = System.currentTimeMillis()
            StreamLogger.debug(TAG, "Bypass successful. Token cached.")
        }
        return token
    }

    /**
     * Forces a fresh bypass, ignoring the cache.
     * Call this if a request returns 401/403 with a cached token.
     */
    suspend fun refreshToken(baseUrl: String): String? {
        cachedToken = ""
        cachedTokenTimestamp = 0L
        return getToken(baseUrl)
    }

    /**
     * Core bypass logic. Steps:
     *   1. GET /mobile/home?app=1 to get the session hash
     *   2. Ping the verification challenge server
     *   3. Poll /mobile/verify2.php until success
     */
    private suspend fun runBypass(baseUrl: String): String? {
        val base = baseUrl.trimEnd('/')

        // ── Step 1: Fetch /mobile/home?app=1 and extract data-addhash ────────
        val homeUrl = "$base/mobile/home?app=1"
        StreamLogger.debug(TAG, "GET $homeUrl")

        val homeResponse = try {
            HttpClient.execute(
                RequestBuilder()
                    .url(homeUrl)
                    .header("User-Agent", NATIVE_UA)
                    .header("X-Requested-With", "app.netmirror.netmirrornew")
                    .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                    .build()
            )
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Home fetch failed: ${e.message}")
            return null
        }

        val homeHtml = when (homeResponse) {
            is NetworkResult.Success -> homeResponse.data.body?.toString(Charsets.UTF_8) ?: ""
            else -> {
                StreamLogger.error(TAG, "Home request failed: $homeResponse")
                return null
            }
        }

        // Extract data-addhash="..." from the <body> tag
        val addHash = Regex("""data-addhash="([^"]+)"""").find(homeHtml)?.groupValues?.get(1)
        if (addHash.isNullOrBlank()) {
            StreamLogger.error(TAG, "data-addhash not found in home page HTML")
            return null
        }
        StreamLogger.debug(TAG, "Extracted hash: $addHash")

        // ── Step 2: Ping the challenge server (fire-and-forget, ignore result) ─
        val pingUrl = "https://userver.net52.cc/?hee5=$addHash&a=y&t=${Math.random()}"
        StreamLogger.debug(TAG, "Pinging challenge server: $pingUrl")
        try {
            HttpClient.execute(
                RequestBuilder()
                    .url(pingUrl)
                    .header("User-Agent", NATIVE_UA)
                    .build()
            )
        } catch (_: Exception) {
            // Intentionally ignored — ping failure doesn't stop the flow
        }

        // ── Step 3: Poll /mobile/verify2.php until "All Done" ────────────────
        val verifyUrl = "$base/mobile/verify2.php"
        StreamLogger.debug(TAG, "Starting verify poll loop (max $MAX_VERIFY_LOOPS × 10s)")

        for (loop in 1..MAX_VERIFY_LOOPS) {
            delay(10_000L) // 10-second mandatory wait between polls

            StreamLogger.debug(TAG, "Verify loop $loop/$MAX_VERIFY_LOOPS — POST $verifyUrl")

            val verifyResponse = try {
                HttpClient.execute(
                    RequestBuilder()
                        .url(verifyUrl)
                        .method("POST")
                        .formBody(mapOf("verify" to addHash))
                        .header("User-Agent", NATIVE_UA)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                )
            } catch (e: Exception) {
                StreamLogger.error(TAG, "Verify POST failed on loop $loop: ${e.message}")
                continue
            }

            if (verifyResponse !is NetworkResult.Success) continue

            val body = verifyResponse.data.body?.toString(Charsets.UTF_8) ?: continue
            StreamLogger.debug(TAG, "Verify response loop $loop: $body")

            if (body.contains("\"statusup\":\"All Done\"")) {
                // Extract t_hash_t from the Set-Cookie response header
                val tHashT = verifyResponse.data.headers["Set-Cookie"]
                    ?.let { Regex("t_hash_t=([^;]+)").find(it)?.groupValues?.get(1) }
                    ?: verifyResponse.data.cookies?.get("t_hash_t")

                if (!tHashT.isNullOrBlank()) {
                    StreamLogger.debug(TAG, "Got t_hash_t on loop $loop: $tHashT")
                    return tHashT
                }
                StreamLogger.error(TAG, "Got 'All Done' but no t_hash_t cookie in response!")
                return null
            }
        }

        StreamLogger.error(TAG, "Bypass failed after $MAX_VERIFY_LOOPS loops.")
        return null
    }
}
