package com.cinetheta.providers.toonstream

import com.cinetheta.domain.models.HostType

object ToonStreamConfig {

    const val PROVIDER_ID   = "toonstream"
    const val PROVIDER_NAME = "Anime 2"

    /**
     * Primary domain — no Cloudflare challenge on this mirror.
     * toon-stream.site uses Cloudflare Bot Management (plain 403 block).
     */
    const val DEFAULT_DOMAIN  = "https://toonstream.vip"

    /** Original domain — blocked by Cloudflare Bot Management on direct HTTP. */
    const val FALLBACK_DOMAIN = "https://toon-stream.site"

    /** Path in streamflex-providers repo for the remote domain manifest. */
    const val MANIFEST_PATH = "providers/toonstream.json"
}
