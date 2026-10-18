package com.cinetheta.providers.animedekho

import com.cinetheta.core.cache.CacheManager
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.DomainResolver
import com.cinetheta.core.network.DomainResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider

class AnimeDekhoProvider(
    private val cacheManager : CacheManager   = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = AnimeDekhoConfig.PROVIDER_ID
    override val name = AnimeDekhoConfig.PROVIDER_NAME

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    @Volatile
    private var resolvedDomain: String? = null

    private fun isBadDomain(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return true
        return domain.contains("hindisubanime", ignoreCase = true) ||
               domain.contains("animedekho.app", ignoreCase = true)
    }

    override val baseUrl: String
        get() {
            val d = resolvedDomain ?: AnimeDekhoConfig.DEFAULT_DOMAIN
            return if (isBadDomain(d)) AnimeDekhoConfig.DEFAULT_DOMAIN else d
        }

    private val searchImpl  = AnimeDekhoSearch()
    private val detailsImpl = AnimeDekhoDetails()

    companion object {
        private const val TAG = "AnimeDekhoProvider"
    }

    suspend fun ensureDomain(): String {
        val current = resolvedDomain
        if (current != null && !isBadDomain(current)) {
            return current
        }

        // Purge any stale/poisoned domains or manifests from previous runs
        val persisted = cacheManager.getDomain(AnimeDekhoConfig.PROVIDER_ID)
        val cachedManifest = cacheManager.getManifest(AnimeDekhoConfig.PROVIDER_ID)
        if ((persisted != null && isBadDomain(persisted)) ||
            (cachedManifest != null && (cachedManifest.contains("hindisubanime") || cachedManifest.contains("animedekho.app")))) {
            cacheManager.clearDomain(AnimeDekhoConfig.PROVIDER_ID)
            cacheManager.clearProviderManifests()
            resolver.invalidate(AnimeDekhoConfig.PROVIDER_ID)
        }

        var domain = if (AnimeDekhoConfig.MANIFEST_PATH.isNotBlank()) {
            val result = resolver.resolve(
                providerId   = AnimeDekhoConfig.PROVIDER_ID,
                hardcoded    = AnimeDekhoConfig.DEFAULT_DOMAIN,
                manifestPath = AnimeDekhoConfig.MANIFEST_PATH
            )

            when (result) {
                is DomainResult.Resolved  -> result.domain
                is DomainResult.Mirror    -> result.domain
                is DomainResult.Hardcoded -> result.domain
                is DomainResult.Offline   -> AnimeDekhoConfig.DEFAULT_DOMAIN
            }
        } else {
            AnimeDekhoConfig.DEFAULT_DOMAIN
        }

        // If resolved domain was somehow bad (e.g. from an old cache), force refresh from GitHub
        if (isBadDomain(domain)) {
            cacheManager.clearDomain(AnimeDekhoConfig.PROVIDER_ID)
            cacheManager.clearProviderManifests()
            resolver.invalidate(AnimeDekhoConfig.PROVIDER_ID)

            val freshResult = resolver.resolve(
                providerId   = AnimeDekhoConfig.PROVIDER_ID,
                hardcoded    = AnimeDekhoConfig.DEFAULT_DOMAIN,
                manifestPath = AnimeDekhoConfig.MANIFEST_PATH,
                forceRefresh = true
            )
            domain = when (freshResult) {
                is DomainResult.Resolved  -> freshResult.domain
                is DomainResult.Mirror    -> freshResult.domain
                is DomainResult.Hardcoded -> freshResult.domain
                is DomainResult.Offline   -> AnimeDekhoConfig.DEFAULT_DOMAIN
            }
        }

        val safeDomain = if (isBadDomain(domain)) AnimeDekhoConfig.DEFAULT_DOMAIN else domain
        resolvedDomain = safeDomain
        cacheManager.putDomain(AnimeDekhoConfig.PROVIDER_ID, safeDomain)
        return safeDomain
    }

    fun resetDomain() {
        resolvedDomain = null
        cacheManager.clearDomain(AnimeDekhoConfig.PROVIDER_ID)
        cacheManager.clearProviderManifests()
        resolver.invalidate(AnimeDekhoConfig.PROVIDER_ID)
        Logger.i("[$id] Domain reset — will re-resolve on next request", TAG)
    }

    override suspend fun search(query: String): List<SearchResult> {
        ensureDomain()
        return runCatching {
            searchImpl.search(query = query, baseUrl = baseUrl)
        }.onFailure {
            Logger.e("[$id] Search failed for '$query': ${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        ensureDomain()
        return runCatching {
            detailsImpl.load(result = searchResult, baseUrl = baseUrl)
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
            if (it.message?.contains("503") == true || it.message?.contains("404") == true) {
                resetDomain()
            }
        }.getOrNull()
    }
}
