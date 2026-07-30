package com.streamflex.providers.hdhub4u

/**
 * HDHub4u compile-time constants.
 *
 * These are the values baked in at build time — used only as Step 5 fallbacks
 * in DomainResolver if all remote and cached sources fail.
 *
 * IMPORTANT: Do NOT update these frequently. They are the last-resort fallback
 * for offline/network-failure scenarios. The live domain is kept in:
 *   streamflex-providers/providers/hdhub4u.json → domains.primary
 *
 * DomainResolver reads that JSON at runtime and caches it — these constants
 * only activate if GitHub AND all mirrors are unreachable simultaneously.
 *
 * Last verified: 2026-07-30
 * Inspiration: CloudStream's HDhub4uProvider line 41-42 fallback pattern,
 * extended with full 5-step chain.
 */
object HDHubConfig {

    // ─── Provider Identity ────────────────────────────────────────────────────
    const val PROVIDER_ID   = "hdhub4u"
    const val MANIFEST_PATH = "providers/hdhub4u.json"

    // ─── Step 5 Hardcoded Fallback ────────────────────────────────────────────
    /**
     * Compile-time fallback — used ONLY if DomainResolver Steps 1-4 all fail.
     * Mirrors are ordered by reliability (most stable first).
     */
    const val DEFAULT_DOMAIN = "https://new3.hdhub4u.cl"
    const val MIRROR_1       = "https://hdhub4u.fyi"
    const val MIRROR_2       = "https://hdhub4u.mom"
    const val MIRROR_3       = "https://hdhub4u.med"

    // ─── Search API ───────────────────────────────────────────────────────────
    /**
     * Typesense search API — this is a stable API endpoint, NOT a scraping target,
     * so it doesn't need domain resolution. It's a proper CDN-backed API.
     * CloudStream reference: HDhub4uProvider.kt line 108.
     */
    const val SEARCH_API =
        "https://search.pingora.fyi/collections/post/documents/search"

    // ─── Request Headers ──────────────────────────────────────────────────────
    /**
     * Cookie required by HDHub4u to serve content.
     * Source: CloudStream HDhub4uProvider.kt line 68 `Cookie" to "xla=s4t"`.
     */
    const val REQUIRED_COOKIE = "xla=s4t"
    const val COOKIE = REQUIRED_COOKIE

    /** GitHub config URL alias */
    const val DOMAIN_CONFIG_URL =
        "https://raw.githubusercontent.com/flex-drivee/streamflex-providers/main/providers/hdhub4u.json"

    /**
     * HDHub4u checks UA for bot detection. Chrome 131 is what the reference extension uses.
     * Note: StreamFlexHttpClient injects DEFAULT_USER_AGENT globally;
     * this constant is for cases where HDHub4u requires a more specific UA.
     */
    const val REQUIRED_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/131.0.0.0 Safari/537.36"

    // ─── Timeouts ─────────────────────────────────────────────────────────────
    /** HDHub4u can be slow on the first page — slightly longer than default. */
    const val SEARCH_TIMEOUT_MS = 20_000L
    const val DETAIL_TIMEOUT_MS = 25_000L
}