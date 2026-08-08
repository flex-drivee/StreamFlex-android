package com.streamflex.providers.netmirror

object NetMirrorConfig {
    const val PROVIDER_ID_NETFLIX = "netflixmirror"
    const val PROVIDER_ID_PRIME = "primevideomirror"
    const val PROVIDER_ID_HOTSTAR = "hotstarmirror"
    const val PROVIDER_ID_DISNEY = "disneymirror"
    
    const val DEFAULT_DOMAIN = "https://net52.cc"

    // Types of OTT
    const val OTT_NETFLIX = "nf"
    const val OTT_PRIME = "pv"
    const val OTT_HOTSTAR = "hs"
    const val OTT_DISNEY = "dp"

    // Required cookies
    const val REQUIRED_COOKIE_KEY = "t_hash_t"
    
    // Timeouts
    const val SEARCH_TIMEOUT_MS = 15_000L
    const val DETAIL_TIMEOUT_MS = 20_000L
}
