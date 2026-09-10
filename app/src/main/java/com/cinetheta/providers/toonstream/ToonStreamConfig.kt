package com.cinetheta.providers.toonstream

import com.cinetheta.domain.models.HostType

object ToonStreamConfig {

    const val PROVIDER_ID   = "toonstream"
    const val PROVIDER_NAME = "Anime 2"

    /** Hardcoded fallback domain */
    const val DEFAULT_DOMAIN = "https://toon-stream.site"

    /** Path in cinetheta-providers repo for the remote domain manifest. */
    const val MANIFEST_PATH    = "providers/toonstream.json"
}
