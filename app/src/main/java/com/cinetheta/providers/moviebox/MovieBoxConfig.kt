package com.cinetheta.providers.moviebox

import com.cinetheta.domain.models.HostType

object MovieBoxConfig {
    const val PROVIDER_NAME = "MovieBox"
    val HOST_TYPE = HostType.MOVIEBOX

    var savedDomain: String? = null

    val HOST_POOL = listOf(
        "https://api6.aoneroom.com",
        "https://api5.aoneroom.com",
        "https://api4.aoneroom.com",
        "https://api4sg.aoneroom.com",
        "https://api3.aoneroom.com"
    )

    val DEFAULT_DOMAIN = HOST_POOL[4]

    const val PROVIDER_ID = "moviebox"
    const val MANIFEST_PATH = "providers/moviebox.json"
    const val DOMAIN_CONFIG_URL =
        "https://raw.githubusercontent.com/flex-drivee/streamflex-providers/main/providers/moviebox.json"

    // Secret keys — verified identical to phisher v33 (MovieBoxProvider.cs3 version 33)
    // and cross-confirmed with MovieBox-Tui DEFAULT_SECRET_BYTES (0xefa891974eecd3148df63aa611602defd101259ba521022c57ae0566bd8e)
    const val SECRET_KEY_DEFAULT_B64 = "NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw=="
    const val SECRET_KEY_ALT_B64     = "WHFuMm5uTzQxL0w5Mm8xaXVYaFNMSFRiWHZZNFo1Wlo2Mm04bVNMQQ=="

    // ── App identity (updated to com.community.oneroom v4, per MovieBox-Tui 2.5k★ reference) ──
    const val PACKAGE_NAME  = "com.community.oneroom"
    const val VERSION_NAME  = "4.0.01.0813.03"

    /** Valid version codes (50020117–50020121) from MovieBox-Tui crypto.rs */
    val VERSION_CODES = listOf(50020117, 50020118, 50020119, 50020120, 50020121)

    /** sp_code required by the new API; verified via MovieBox-Tui reference */
    const val SP_CODE = "40401"

    /**
     * Referer that must accompany MovieBox CDN stream requests.
     * Note: https://sportslive.wine is blocked with 429 by CDN nginx;
     * https://movie-box.co succeeds with HTTP 200.
     */
    const val STREAM_REFERER = "https://movie-box.co"

    /** Web domains for the H5 play endpoint (phisher98 MovieBoxProvider reference) */
    val WEB_DOMAINS = listOf(
        "https://movie-box.co",
        "https://movieboxhd.net",
        "https://123moviesfree.club",
        "https://sflix.film",
        "https://movieboxonline.net",
        "https://netnaija.film",
        "https://h5-api.aoneroom.com"
    )

    const val WEB_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36"

    // Legacy brand/model pool (kept for fallback; prefer REDMI_MODELS for new requests)
    val BRAND_MODELS = mapOf(
        "Samsung" to listOf("SM-S918B", "SM-A528B", "SM-M336B"),
        "Xiaomi"  to listOf("2201117TI", "M2012K11AI", "Redmi Note 11"),
        "OnePlus" to listOf("LE2111", "CPH2449", "IN2023"),
        "Google"  to listOf("Pixel 6", "Pixel 7", "Pixel 8"),
        "Realme"  to listOf("RMX3085", "RMX3360", "RMX3551")
    )

    /** Redmi device pool from MovieBox-Tui crypto.rs `redmi_devices` */
    val REDMI_MODELS = listOf(
        Pair("23078RKD5C", "Redmi"),
        Pair("2201117TY",  "Redmi"),
        Pair("2201117TG",  "Redmi"),
        Pair("22101316G",  "Redmi"),
        Pair("21121210G",  "Redmi"),
        Pair("M2012K11AG", "Redmi"),
        Pair("M2007J20CG", "Redmi")
    )

    /** Android (version, build) pairs from MovieBox-Tui crypto.rs `android_versions` */
    val ANDROID_VERSIONS = listOf(
        Pair("9",  "PQ3A.190605.03081104"),
        Pair("10", "QP1A.191005.007.A3"),
        Pair("11", "RP1A.200720.011"),
        Pair("12", "S1B.220414.015"),
        Pair("13", "TQ2A.230405.003")
    )

    /** Indian IP prefixes for X-Forwarded-For spoofing (MovieBox-Tui reference) */
    val INDIAN_IP_PREFIXES = listOf(
        "103.241", "49.36", "117.195", "106.198", "122.162",
        "157.32",  "182.70", "103.58",  "27.60",   "59.90"
    )
}
