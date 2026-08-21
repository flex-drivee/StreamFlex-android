package com.streamflex.providers.fourkhdhub

object FourKHDHubConfig {
    const val PROVIDER_ID = "fourkhdhub"
    const val DEFAULT_DOMAIN = "https://4khdhub.one"
    const val MANIFEST_PATH = "providers/fourkhdhub.json"
    
    // Fallback search config if they switch to Typesense in the future
    const val SEARCH_API = ""
    
    // We don't strictly need a cookie for 4KHDHub unless cloudflare challenges us
    const val COOKIE = ""

    const val DOMAIN_CONFIG_URL =
        "https://raw.githubusercontent.com/flex-drivee/streamflex-providers/main/providers/fourkhdhub.json"
}
