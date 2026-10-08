package com.cinetheta.providers.piratexplay

import com.cinetheta.core.cache.CacheManager
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.DomainResolver
import com.cinetheta.core.network.DomainResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider

class PirateXplayProvider(
    private val cacheManager : CacheManager   = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = PirateXplayConfig.PROVIDER_ID
    override val name = PirateXplayConfig.PROVIDER_NAME

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: PirateXplayConfig.DEFAULT_DOMAIN

    private val searchImpl  = PirateXplaySearch()
    private val detailsImpl = PirateXplayDetails()

    companion object {
        private const val TAG = "PirateXplayProvider"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = PirateXplayConfig.PROVIDER_ID,
            hardcoded    = PirateXplayConfig.DEFAULT_DOMAIN,
            manifestPath = "providers/piratexplay.json"
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> result.domain
            is DomainResult.Mirror    -> result.domain
            is DomainResult.Hardcoded -> result.domain
            is DomainResult.Offline   -> PirateXplayConfig.DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(PirateXplayConfig.PROVIDER_ID)
        Logger.i("[$id] Domain reset — will re-resolve on next request", TAG)
    }

    override suspend fun search(query: String): List<SearchResult> {
        ensureDomain()
        return runCatching {
            searchImpl.search(query = query, baseUrl = baseUrl)
        }.onFailure {
            Logger.e("[$id] Search failed for '\$query': \${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        ensureDomain()
        return runCatching {
            detailsImpl.load(result = searchResult, baseUrl = baseUrl)
        }.onFailure {
            Logger.e("[$id] Load failed for '\${searchResult.title}': \${it.message}", TAG)
            if (it.message?.contains("503") == true || it.message?.contains("404") == true) {
                resetDomain()
            }
        }.getOrNull()
    }
}
