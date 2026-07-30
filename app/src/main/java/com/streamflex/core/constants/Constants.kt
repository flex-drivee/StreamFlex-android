package com.streamflex.core.constants

/**
 * StreamFlex global constants.
 *
 * All magic numbers live here — never scatter them across the codebase.
 * If a value ever needs to change, change it once, here.
 */
object Constants {

    // ─── Engine Version ──────────────────────────────────────────────────────
    const val ENGINE_VERSION = "1.0.0"

    // ─── User-Agent ───────────────────────────────────────────────────────────
    /**
     * Chrome 120 Desktop UA — the most broadly accepted UA across scraping targets.
     * Matches what CloudStream uses for provider compatibility.
     * Updated periodically; bump here only, nowhere else.
     */
    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

    /** Mobile UA — used for providers that serve different content to mobile browsers. */
    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Android 13; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0"

    // ─── Network Timeouts ─────────────────────────────────────────────────────
    /** Fast connect — fail quickly if the host is unreachable. */
    const val CONNECT_TIMEOUT_MS = 10_000L      // 10 seconds

    /** Allow streaming providers adequate time to respond (some are slow). */
    const val READ_TIMEOUT_MS    = 30_000L      // 30 seconds

    /** Write timeout for POST bodies (search queries, AJAX payloads). */
    const val WRITE_TIMEOUT_MS   = 15_000L      // 15 seconds

    // ─── Redirect Limits ──────────────────────────────────────────────────────
    /**
     * Maximum redirect hops before SecurityException.
     * CloudStream uses 5; we match that for compatibility.
     * HubCloud chains can be 3–4 hops deep; 8 is the safe ceiling.
     */
    const val MAX_REDIRECT_HOPS = 8

    // ─── Retry ────────────────────────────────────────────────────────────────
    /** Number of automatic retries on IO failure. Keep low — providers ban on excess. */
    const val MAX_RETRY_ATTEMPTS = 2

    /** Base delay between retries (exponential backoff: attempt * BASE_RETRY_DELAY_MS). */
    const val BASE_RETRY_DELAY_MS = 500L

    // ─── Rate Limiting ────────────────────────────────────────────────────────
    /**
     * Default maximum concurrent requests to any single provider.
     * Overridden per-provider by ProviderDefinition.rateLimit.maxConcurrent.
     */
    const val DEFAULT_MAX_CONCURRENT = 3

    /**
     * Default delay between requests when isSequential = true.
     * Overridden per-provider by ProviderDefinition.rateLimit.delayBetweenMs.
     */
    const val DEFAULT_SEQUENTIAL_DELAY_MS = 1_500L

    // ─── Cache TTLs (milliseconds) ────────────────────────────────────────────
    const val CACHE_TTL_PROVIDER_MANIFEST_MS = 6 * 3_600_000L   // 6 hours
    const val CACHE_TTL_DOMAIN_CONFIG_MS     = 6 * 3_600_000L   // 6 hours
    const val CACHE_TTL_TMDB_METADATA_MS     = 24 * 3_600_000L  // 24 hours
    const val CACHE_TTL_TMDB_SEARCH_MS       = 1 * 3_600_000L   // 1 hour
    const val CACHE_TTL_PROVIDER_SEARCH_MS   = 15 * 60_000L     // 15 minutes
    const val CACHE_TTL_DETAIL_MS            = 15 * 60_000L     // 15 minutes
    const val CACHE_TTL_EXTRACTOR_META_MS    = 24 * 3_600_000L  // 24 hours
    const val CACHE_TTL_ALT_TITLES_MS        = 7 * 24 * 3_600_000L // 7 days
    // STREAM URLS ARE NEVER CACHED — signed CDN URLs expire in 1–6h.

    // ─── LRU Cache Sizes ──────────────────────────────────────────────────────
    const val LRU_TMDB_SEARCH_SIZE    = 50
    const val LRU_PROVIDER_SEARCH_SIZE = 20
    const val LRU_DETAIL_SIZE          = 20

    // ─── Security ─────────────────────────────────────────────────────────────
    /** All scraped and resolved URLs must start with this scheme. */
    const val REQUIRED_URL_SCHEME = "https://"

    // ─── Providers Repository ─────────────────────────────────────────────────
    /**
     * Raw GitHub URL for provider manifests.
     * This is the remote source of truth for domains, capabilities and extractor IDs.
     */
    const val PROVIDERS_REPO_BASE =
        "https://raw.githubusercontent.com/flex-drivee/streamflex-providers/main"

    const val PROVIDERS_HEALTH_URL  = "$PROVIDERS_REPO_BASE/health/health.json"
    const val EXTRACTORS_REGISTRY_URL = "$PROVIDERS_REPO_BASE/extractors/registry.json"

    // ─── TitleMatcher ─────────────────────────────────────────────────────────
    /** Minimum confidence score (0.0–1.0) to accept a provider search result. */
    const val TITLE_MATCH_THRESHOLD = 0.65

    // ─── Domain Resolution ────────────────────────────────────────────────────
    /**
     * SharedPreferences key prefix for cached provider domains.
     * Full key: "domain_<provider_id>"
     */
    const val PREFS_DOMAIN_KEY_PREFIX = "domain_"
    const val PREFS_DOMAIN_TS_SUFFIX  = "_ts"

    // ─── SharedPreferences Names ─────────────────────────────────────────────
    const val PREFS_CONFIG   = "streamflex_config"
    const val PREFS_DOMAINS  = "streamflex_domains"
    const val PREFS_CACHE    = "streamflex_cache"
}