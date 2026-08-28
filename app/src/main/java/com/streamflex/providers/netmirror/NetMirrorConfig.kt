package com.streamflex.providers.netmirror

object NetMirrorConfig {
    const val PROVIDER_ID_NETFLIX = "netflixmirror"
    const val PROVIDER_ID_PRIME   = "primevideomirror"
    const val PROVIDER_ID_HOTSTAR = "hotstarmirror"
    const val PROVIDER_ID_DISNEY  = "disneymirror"

    // Fallback domain if dynamic resolution fails
    const val DEFAULT_DOMAIN = "https://net52.cc"

    // OTT identifiers (sent as Cookie: ott=<value>)
    const val OTT_NETFLIX = "nf"
    const val OTT_PRIME   = "pv"
    const val OTT_HOTSTAR = "hs"
    const val OTT_DISNEY  = "dp"

    // Session cookie key
    const val REQUIRED_COOKIE_KEY = "t_hash_t"

    // Timeouts
    const val SEARCH_TIMEOUT_MS = 30_000L   // increased to allow bypass time
    const val DETAIL_TIMEOUT_MS = 30_000L

    /**
     * Full list of domain resolver endpoints (Base64-encoded), sourced directly
     * from CNC Verse Mobile decompiled source (UtilsKt.newTvDomains).
     * These are probed via /checknewtv.php to find the active API base URL.
     */
    val RESOLVER_DOMAINS: List<String> = listOf(
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3RzLmNvbQ==",   // mobiledetects.com
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3QuYXBw",         // mobiledetect.app
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmFydA==",          // mobidetect.art
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNj",              // mobidetect.cc
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNsaWNr",          // mobidetect.click
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lmluaw==",          // mobidetect.ink
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmxpdmU=",          // mobidetect.live
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnBybw==",          // mobidetect.pro
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNob3A=",          // mobidetect.shop
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNpdGU=",          // mobidetect.site
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNwYWNl",          // mobidetect.space
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnN0b3Jl",          // mobidetect.store
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnZpcA==",           // mobidetect.vip
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lndpa2k=",           // mobidetect.wiki
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lnh5eg==",           // mobidetect.xyz
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5hcnQ=",           // mobidetects.art
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5jYw==",           // mobidetects.cc
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbmZv",           // mobidetects.info
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbms=",           // mobidetects.ink
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5saXZl",           // mobidetects.live
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5wcm8=",           // mobidetects.pro
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5zdG9yZQ==",       // mobidetects.store
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy50b3A=",           // mobidetects.top
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy54eXo="            // mobidetects.xyz
    )
}
