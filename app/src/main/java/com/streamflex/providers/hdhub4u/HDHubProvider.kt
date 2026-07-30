package com.streamflex.providers.hdhub4u

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.DomainResolver
import com.streamflex.core.network.DomainResult
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

/**
 * HDHub4u provider implementation.
 *
 * ─── Architecture role ────────────────────────────────────────────────────
 * This class is a thin orchestrator. It:
 * 1. Resolves the active domain via [DomainResolver] (5-step chain).
 * 2. Delegates search to [HDHubSearch].
 * 3. Delegates detail loading to [HDHubDetails].
 *
 * No scraping logic lives here. No hardcoded URLs. No runBlocking.
 * This is the pattern CloudStream providers should have used.
 *
 * ─── Domain resolution ────────────────────────────────────────────────────
 * Domain is resolved lazily on first use — not at construction time.
 * This avoids blocking app startup with a network call.
 *
 * After resolution, the domain is cached (session + SharedPreferences)
 * for 6 hours. All requests within that window use the cached value.
 *
 * If the resolved domain changes mid-session (provider detects errors),
 * the engine calls [resetDomain] and the next search triggers re-resolution.
 *
 * ─── CloudStream comparison ───────────────────────────────────────────────
 * CloudStream's HDhub4uProvider (reference lines 40-42):
 *   override var mainUrl = runBlocking { getDomains()?.HDHUB4u ?: "https://hdhub4u.rehab" }
 *
 * Problems with that approach:
 * - runBlocking blocks the calling thread during app init.
 * - Only 2 steps (remote JSON → single hardcoded URL). No mirrors.
 * - Caching is session-only (a field on a singleton), lost on restart.
 *
 * Our approach: lazy, coroutine-native, 5-step, persisted.
 */
class HDHubProvider(
    private val cacheManager : CacheManager = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = HDHubConfig.PROVIDER_ID
    override val name = "HDHub4u"

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    /**
     * Lazily resolved base URL. Null until first [ensureDomain] call.
     * @volatile ensures visibility across coroutines.
     */
    @Volatile
    private var resolvedDomain: String? = null

    /**
     * Returns the active base URL, resolving it if not yet done.
     * All provider methods call this before constructing any URL.
     *
     * If resolution fails completely (provider offline), returns the
     * hardcoded fallback so the caller can surface a meaningful error.
     */
    override val baseUrl: String
        get() = resolvedDomain ?: HDHubConfig.DEFAULT_DOMAIN

    private val searchImpl  = HDHubSearch()
    private val detailsImpl = HDHubDetails()

    companion object {
        private const val TAG = "HDHubProvider"
    }

    // ─── Domain Lifecycle ─────────────────────────────────────────────────────

    /**
     * Resolves and caches the active domain.
     * Must be called (and awaited) before any network request.
     *
     * Idempotent: safe to call multiple times — returns immediately
     * if domain is already resolved.
     *
     * @return The resolved domain, or the hardcoded fallback on failure.
     */
    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = HDHubConfig.PROVIDER_ID,
            hardcoded    = HDHubConfig.DEFAULT_DOMAIN,
            manifestPath = HDHubConfig.MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> {
                Logger.i("[$id] Domain resolved (${result.step}): ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Mirror    -> {
                Logger.i("[$id] Using mirror [${result.mirrorIndex}]: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Hardcoded -> {
                Logger.w("[$id] Using hardcoded fallback: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Offline   -> {
                Logger.e("[$id] Provider OFFLINE — tried: ${result.tried}", TAG)
                HDHubConfig.DEFAULT_DOMAIN // Return for error surfacing
            }
        }

        resolvedDomain = domain
        return domain
    }

    /**
     * Forces a domain re-resolution on the next request.
     * Call this when the engine detects repeated 5xx responses from the provider.
     */
    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(HDHubConfig.PROVIDER_ID)
        Logger.i("[$id] Domain reset — will re-resolve on next request", TAG)
    }

    // ─── Provider Operations ──────────────────────────────────────────────────

    /**
     * Search HDHub4u.
     *
     * Resolves the domain first, then delegates to [HDHubSearch].
     * HDHub4u uses the Typesense search API ([HDHubConfig.SEARCH_API]) — a
     * stable CDN-backed endpoint that doesn't require domain resolution itself.
     */
    override suspend fun search(query: String): List<SearchResult> {
        ensureDomain()
        return runCatching {
            searchImpl.search(query = query, baseUrl = baseUrl)
        }.onFailure {
            Logger.e("[$id] Search failed for '$query': ${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    /**
     * Load provider content for a [SearchResult].
     *
     * Returns:
     * - [ProviderResult.sources] for movies (list of stream gateway URLs).
     * - [ProviderResult.seasons] for TV shows (episode tree).
     *
     * Returns null on failure — the engine falls back to the next provider.
     */
    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        ensureDomain()
        return runCatching {
            detailsImpl.load(
                result = searchResult
            )
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)

            // If we get a 5xx-style failure, the domain may have changed — reset it
            if (it.message?.contains("503") == true || it.message?.contains("404") == true) {
                resetDomain()
            }
        }.getOrNull()
    }
}