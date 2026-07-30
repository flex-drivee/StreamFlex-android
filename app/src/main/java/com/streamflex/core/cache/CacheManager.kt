package com.streamflex.core.cache

import android.content.SharedPreferences
import android.util.LruCache
import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger

/**
 * CacheManager
 *
 * Implements the exact cache TTL table from the frozen architecture (Part 9).
 *
 * ─── Cache Layers ─────────────────────────────────────────────────────────
 *
 * | Data                    | TTL        | Storage                          |
 * |-------------------------|------------|----------------------------------|
 * | Provider manifests      | 6 hours    | SharedPreferences (JSON string)  |
 * | Domain config           | 6 hours    | SharedPreferences                |
 * | Extractor metadata      | 24 hours   | SharedPreferences                |
 * | Alternate titles        | 7 days     | SharedPreferences                |
 * | TMDB metadata           | 24 hours   | SharedPreferences (JSON string)  |
 * | TMDB search results     | 1 hour     | In-memory LruCache (50 items)    |
 * | Provider search results | 15 minutes | In-memory LruCache (20 items)    |
 * | Detail page results     | 15 minutes | In-memory LruCache (20 items)    |
 * | Stream URLs (m3u8/mp4)  | NEVER      | NOT CACHED — CDN URLs expire     |
 * | Health status           | Session    | In-memory only (reset on restart)|
 *
 * ─── Key Design Rules ─────────────────────────────────────────────────────
 *
 * 1. STREAM URLS ARE NEVER CACHED.
 *    CDN hosts (FileMoon, HubCloud, PixelDrain) sign their delivery URLs
 *    with a timestamp expiry (1–6 hours). A cached URL returned 403 by
 *    the CDN is a broken playback experience. Re-run the full extraction
 *    pipeline on every play request.
 *    There is no method in this class for caching stream URLs.
 *    If you're ever tempted to add one: don't.
 *
 * 2. SharedPreferences is used for config-style data (low write frequency,
 *    needs to survive app restarts, small payloads).
 *    Not used for search results (too many unique keys → SP fragmentation).
 *
 * 3. LruCache is in-memory only. It's fast, bounded, and automatically
 *    evicts the least-recently-used entry. It does NOT survive app restarts,
 *    which is correct for 15-minute caches — they'd be stale anyway.
 *
 * 4. All TTL checks use System.currentTimeMillis() stored alongside the value.
 *    The pattern is: key → value, key_ts → timestamp.
 *
 * ─── Usage ────────────────────────────────────────────────────────────────
 * ```kotlin
 * // Inject via constructor (or use the shared instance from DI)
 * val cache = CacheManager(sharedPreferences)
 *
 * // Provider manifests
 * cache.putConfig("manifest_hdhub4u", jsonString)
 * val manifest = cache.getConfig("manifest_hdhub4u")  // null if expired
 *
 * // Domain config
 * cache.putDomain("hdhub4u", "https://new3.hdhub4u.cl")
 * val domain = cache.getDomain("hdhub4u")
 *
 * // Provider search results (in-memory, 15min)
 * cache.putProviderSearch("hdhub4u", "inception", listOf(...))
 * val results = cache.getProviderSearch("hdhub4u", "inception")
 * ```
 */
class CacheManager(
    private val configPrefs : SharedPreferences,
    private val domainPrefs : SharedPreferences
) {

    companion object {
        private const val TAG = "CacheManager"
    }

    // ─── In-Memory LRU Caches ────────────────────────────────────────────────

    /**
     * TMDB search results. Key: query string.
     * TTL: 1 hour (soft — no eviction on time, just size).
     */
    private val tmdbSearchLru = LruCache<String, CachedEntry<String>>(
        Constants.LRU_TMDB_SEARCH_SIZE
    )

    /**
     * Provider search results. Key: "providerId:query".
     * TTL: 15 minutes (checked on read).
     */
    private val providerSearchLru = LruCache<String, CachedEntry<String>>(
        Constants.LRU_PROVIDER_SEARCH_SIZE
    )

    /**
     * Detail page results. Key: post URL.
     * TTL: 15 minutes (checked on read).
     */
    private val detailLru = LruCache<String, CachedEntry<String>>(
        Constants.LRU_DETAIL_SIZE
    )

    /**
     * Session-only health status. Key: providerId.
     * Not persisted — resets on app restart. That's intentional:
     * a provider that was offline last session may be online now.
     */
    private val sessionHealth = HashMap<String, String>()

    // ─── SharedPreferences Helpers ───────────────────────────────────────────

    private fun SharedPreferences.putWithTimestamp(key: String, value: String) {
        edit()
            .putString(key, value)
            .putLong("${key}_ts", System.currentTimeMillis())
            .apply()
    }

    private fun SharedPreferences.getIfFresh(key: String, ttlMs: Long): String? {
        val ts  = getLong("${key}_ts", 0L)
        val age = System.currentTimeMillis() - ts
        return if (age < ttlMs) getString(key, null) else null
    }

    // ─── Provider Manifests ──────────────────────────────────────────────────

    /**
     * Cache a provider manifest JSON string.
     * Called after a successful fetch from the streamflex-providers GitHub repo.
     */
    fun putManifest(providerId: String, json: String) {
        Logger.d(message = "Cache: put manifest [$providerId]", tag = TAG)
        configPrefs.putWithTimestamp("manifest_$providerId", json)
    }

    /**
     * Get a cached provider manifest. Returns null if not cached or expired (6h TTL).
     */
    fun getManifest(providerId: String): String? =
        configPrefs.getIfFresh("manifest_$providerId", Constants.CACHE_TTL_PROVIDER_MANIFEST_MS)

    // ─── Domain Config ───────────────────────────────────────────────────────

    /**
     * Cache the active domain for a provider (e.g. "https://new3.hdhub4u.cl").
     * This is Step 2 of the 5-step domain resolution chain.
     */
    fun putDomain(providerId: String, domain: String) {
        Logger.d(message = "Cache: put domain [$providerId] = $domain", tag = TAG)
        domainPrefs.putWithTimestamp("domain_$providerId", domain)
    }

    /**
     * Get the cached domain for a provider. Returns null if expired (6h TTL).
     * DomainResolver falls through to Step 3 (hardcoded backup) on null.
     */
    fun getDomain(providerId: String): String? =
        domainPrefs.getIfFresh("domain_$providerId", Constants.CACHE_TTL_DOMAIN_CONFIG_MS)

    // ─── Extractor Metadata ──────────────────────────────────────────────────

    /**
     * Cache the full extractor registry JSON.
     * Called once at startup and every 24h thereafter.
     */
    fun putExtractorRegistry(json: String) {
        Logger.d(message = "Cache: put extractor registry", tag = TAG)
        configPrefs.putWithTimestamp("extractor_registry", json)
    }

    fun getExtractorRegistry(): String? =
        configPrefs.getIfFresh("extractor_registry", Constants.CACHE_TTL_EXTRACTOR_META_MS)

    // ─── Alternate Titles ────────────────────────────────────────────────────

    /**
     * Cache the alternate-titles config JSON (7-day TTL).
     * Used by TitleMatcher to improve confidence scoring.
     */
    fun putAlternateTitles(json: String) {
        configPrefs.putWithTimestamp("alternate_titles", json)
    }

    fun getAlternateTitles(): String? =
        configPrefs.getIfFresh("alternate_titles", Constants.CACHE_TTL_ALT_TITLES_MS)

    // ─── TMDB Metadata ───────────────────────────────────────────────────────

    /**
     * Cache TMDB item metadata as a JSON string (24h TTL).
     * Key: tmdbId (e.g. "27205").
     */
    fun putTmdbItem(tmdbId: String, json: String) {
        configPrefs.putWithTimestamp("tmdb_$tmdbId", json)
    }

    fun getTmdbItem(tmdbId: String): String? =
        configPrefs.getIfFresh("tmdb_$tmdbId", Constants.CACHE_TTL_TMDB_METADATA_MS)

    // ─── TMDB Search ─────────────────────────────────────────────────────────

    /**
     * Cache TMDB search results in-memory (1h TTL via timestamp check).
     * Key: query string.
     */
    fun putTmdbSearch(query: String, json: String) {
        tmdbSearchLru.put(query, CachedEntry(json))
    }

    fun getTmdbSearch(query: String): String? =
        tmdbSearchLru[query]?.takeIfFresh(Constants.CACHE_TTL_TMDB_SEARCH_MS)?.value

    // ─── Provider Search Results ─────────────────────────────────────────────

    /**
     * Cache provider search results in-memory (15-min TTL).
     * Key: "providerId:query".
     *
     * @param json  Serialised List<ProviderMatch> as JSON string.
     */
    fun putProviderSearch(providerId: String, query: String, json: String) {
        val key = "$providerId:$query"
        providerSearchLru.put(key, CachedEntry(json))
    }

    fun getProviderSearch(providerId: String, query: String): String? {
        val key = "$providerId:$query"
        return providerSearchLru[key]?.takeIfFresh(Constants.CACHE_TTL_PROVIDER_SEARCH_MS)?.value
    }

    // ─── Detail Page Results ─────────────────────────────────────────────────

    /**
     * Cache a detail page result in-memory (15-min TTL).
     * Key: provider post URL.
     *
     * @param json  Serialised DetailResult as JSON string.
     */
    fun putDetail(postUrl: String, json: String) {
        detailLru.put(postUrl, CachedEntry(json))
    }

    fun getDetail(postUrl: String): String? =
        detailLru[postUrl]?.takeIfFresh(Constants.CACHE_TTL_DETAIL_MS)?.value

    // ─── Session Health ───────────────────────────────────────────────────────

    /**
     * Session-only provider health state (resets on app restart).
     * Status: "online", "degraded", "offline"
     */
    fun putSessionHealth(providerId: String, status: String) {
        sessionHealth[providerId] = status
    }

    fun getSessionHealth(providerId: String): String? = sessionHealth[providerId]

    fun getAllSessionHealth(): Map<String, String> = sessionHealth.toMap()

    // ─── Cache Clearing ───────────────────────────────────────────────────────

    fun clearProviderManifests() {
        configPrefs.edit().let { editor ->
            configPrefs.all.keys
                .filter { it.startsWith("manifest_") }
                .forEach { editor.remove(it).remove("${it}_ts") }
            editor.apply()
        }
        Logger.i(message = "Cache: cleared all provider manifests", tag = TAG)
    }

    fun clearDomains() {
        domainPrefs.edit().clear().apply()
        Logger.i(message = "Cache: cleared all domains", tag = TAG)
    }

    fun clearSearchCaches() {
        providerSearchLru.evictAll()
        tmdbSearchLru.evictAll()
        Logger.i(message = "Cache: cleared search LRU caches", tag = TAG)
    }

    fun clearAll() {
        clearProviderManifests()
        clearDomains()
        clearSearchCaches()
        detailLru.evictAll()
        sessionHealth.clear()
        Logger.i(message = "Cache: full clear", tag = TAG)
    }
}

// ─── Helper: Cached Entry with TTL ───────────────────────────────────────────

/**
 * Wraps a cached value with a creation timestamp for TTL checking.
 */
private data class CachedEntry<T>(
    val value    : T,
    val createdAt: Long = System.currentTimeMillis()
)

private fun <T> CachedEntry<T>.takeIfFresh(ttlMs: Long): CachedEntry<T>? {
    val age = System.currentTimeMillis() - createdAt
    return if (age < ttlMs) this else null
}
