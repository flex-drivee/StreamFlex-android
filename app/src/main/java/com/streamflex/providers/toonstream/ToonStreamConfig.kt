package com.streamflex.providers.toonstream

import com.streamflex.domain.models.HostType

object ToonStreamConfig {

    const val PROVIDER_ID   = "toonstream"
    const val PROVIDER_NAME = "ToonStream"

    /** Hardcoded fallback domain */
    const val DEFAULT_DOMAIN = "https://toon-stream.site"

    /** Path in streamflex-providers repo for the remote domain manifest. */
    const val MANIFEST_PATH    = "providers/toonstream.json"
}
