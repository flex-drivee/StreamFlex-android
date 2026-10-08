package com.cinetheta.providers.piratexplay

object PirateXplayConfig {
    const val PROVIDER_ID   = "piratexplay"
    const val PROVIDER_NAME = "PirateXplay"

    /** Hardcoded fallback domain */
    const val DEFAULT_DOMAIN = "https://piratexplay.cc"
    
    /** Used for domain resolution fallback/saving, though Piratexplay isn't in manifest yet */
    var savedDomain: String? = null
}
