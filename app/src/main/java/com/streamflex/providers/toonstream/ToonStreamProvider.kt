package com.streamflex.providers.toonstream

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.DomainResolver
import com.streamflex.core.network.DomainResult
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

class ToonStreamProvider(
    private val cacheManager : CacheManager   = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = ToonStreamConfig.PROVIDER_ID
    override val name = ToonStreamConfig.PROVIDER_NAME

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: ToonStreamConfig.DEFAULT_DOMAIN

    private val searchImpl  = ToonStreamSearch()
    private val detailsImpl = ToonStreamDetails()

    companion object {
        private const val TAG = "ToonStreamProvider"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = ToonStreamConfig.PROVIDER_ID,
            hardcoded    = ToonStreamConfig.DEFAULT_DOMAIN,
            manifestPath = ToonStreamConfig.MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> result.domain
            is DomainResult.Mirror    -> result.domain
            is DomainResult.Hardcoded -> result.domain
            is DomainResult.Offline   -> ToonStreamConfig.DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(ToonStreamConfig.PROVIDER_ID)
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
