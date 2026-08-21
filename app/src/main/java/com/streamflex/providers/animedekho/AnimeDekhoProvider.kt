package com.streamflex.providers.animedekho

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.DomainResolver
import com.streamflex.core.network.DomainResult
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

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

    override val baseUrl: String
        get() = resolvedDomain ?: AnimeDekhoConfig.DEFAULT_DOMAIN

    private val searchImpl  = AnimeDekhoSearch()
    private val detailsImpl = AnimeDekhoDetails()

    companion object {
        private const val TAG = "AnimeDekhoProvider"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = AnimeDekhoConfig.PROVIDER_ID,
            hardcoded    = AnimeDekhoConfig.DEFAULT_DOMAIN,
            manifestPath = AnimeDekhoConfig.MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> result.domain
            is DomainResult.Mirror    -> result.domain
            is DomainResult.Hardcoded -> result.domain
            is DomainResult.Offline   -> AnimeDekhoConfig.DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
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
