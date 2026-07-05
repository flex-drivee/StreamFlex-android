package com.streamflex.providers.hdhub4u

/**
 * HDHub4u configuration shared by all provider classes.
 */
object HDHubConfig {

    /**
     * Landing page.
     * Used only if dynamic domain detection fails.
     */
    const val LANDING_URL = "https://hdhub4u.med/"

    /**
     * Default full-site domain.
     * Updated whenever HDHub changes domains.
     */
    const val DEFAULT_DOMAIN = "https://new2.hdhub4u.cl"

    /**
     * Search API.
     */
    const val SEARCH_API =
        "https://search.pingora.fyi/collections/post/documents/search"

    /**
     * Remote domain configuration.
     */
    const val DOMAIN_CONFIG_URL =
        "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/domains.json"

    /**
     * Browser cookie.
     */
    const val COOKIE = "xla=s4t"

    /**
     * Browser User-Agent.
     */
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Safari/537.36"

    /**
     * Network timeout.
     */
    const val TIMEOUT = 15_000
}