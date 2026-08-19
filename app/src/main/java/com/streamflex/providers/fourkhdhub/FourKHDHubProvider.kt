package com.streamflex.providers.fourkhdhub

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.DomainResolver
import com.streamflex.core.network.DomainResult
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

class FourKHDHubProvider(
    private val cacheManager : CacheManager = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = FourKHDHubConfig.PROVIDER_ID
    override val name = "4KHDHub"

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: FourKHDHubConfig.DEFAULT_DOMAIN

    private val searchImpl  = FourKHDHubSearch()
    private val detailsImpl = FourKHDHubDetails()

    companion object {
        private const val TAG = "FourKHDHubProvider"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = FourKHDHubConfig.PROVIDER_ID,
            hardcoded    = FourKHDHubConfig.DEFAULT_DOMAIN,
            manifestPath = FourKHDHubConfig.MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> result.domain
            is DomainResult.Mirror    -> result.domain
            is DomainResult.Hardcoded -> result.domain
            is DomainResult.Offline   -> FourKHDHubConfig.DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(FourKHDHubConfig.PROVIDER_ID)
        Logger.i("[$id] Domain reset", TAG)
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
            detailsImpl.load(
                result = searchResult,
                baseUrl = baseUrl
            )
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
            if (it.message?.contains("503") == true || it.message?.contains("404") == true) {
                resetDomain()
            }
        }.getOrNull()
    }
}
