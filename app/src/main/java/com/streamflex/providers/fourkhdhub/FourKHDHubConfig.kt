package com.streamflex.providers.fourkhdhub

object FourKHDHubConfig {
    const val PROVIDER_ID = "fourkhdhub"
    const val DEFAULT_DOMAIN = "https://4khdhub.one"
    const val MANIFEST_PATH = "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/domains.json"
    
    // Fallback search config if they switch to Typesense in the future
    const val SEARCH_API = ""
    
    // We don't strictly need a cookie for 4KHDHub unless cloudflare challenges us
    const val COOKIE = ""
}
