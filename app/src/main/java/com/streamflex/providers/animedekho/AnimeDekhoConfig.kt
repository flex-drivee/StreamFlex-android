package com.streamflex.providers.animedekho

import com.streamflex.domain.models.HostType

object AnimeDekhoConfig {

    const val PROVIDER_ID   = "animedekho"
    const val PROVIDER_NAME = "AnimeDekho"

    /** Hardcoded fallback domain — used if remote manifest cannot be fetched. */
    const val DEFAULT_DOMAIN = "https://animedekho.app"

    /** Mirror domain — no Cloudflare on the /?trdekho= stream endpoint. */
    const val MIRROR_DOMAIN  = "https://hindisubanime.co"

    /** Path in streamflex-providers repo for the remote domain manifest. */
    const val MANIFEST_PATH    = "providers/animedekho.json"
    const val DOMAIN_CONFIG_URL =
        "https://raw.githubusercontent.com/flex-drivee/streamflex-providers/main/providers/animedekho.json"

    /** Maximum server index to probe per episode (trdekho=1..MAX_SERVERS). */
    const val MAX_SERVERS = 10

    val HOST_TYPE = HostType.ABYSS
}
