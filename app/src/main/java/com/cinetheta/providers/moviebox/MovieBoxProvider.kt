package com.cinetheta.providers.moviebox

import com.cinetheta.core.cache.CacheManager
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.DomainResolver
import com.cinetheta.core.network.DomainResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider

class MovieBoxProvider(
    private val cacheManager : CacheManager = CacheManager(),
    private val resolver     : DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = MovieBoxConfig.PROVIDER_NAME.lowercase()
    override val name = MovieBoxConfig.PROVIDER_NAME

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: MovieBoxConfig.savedDomain ?: MovieBoxConfig.DEFAULT_DOMAIN

    private val searchImpl  = MovieBoxSearch()
    private val detailsImpl = MovieBoxDetails()

    companion object {
        private const val TAG = "MovieBoxProvider"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId   = MovieBoxConfig.PROVIDER_ID,
            hardcoded    = MovieBoxConfig.DEFAULT_DOMAIN,
            manifestPath = MovieBoxConfig.MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved  -> result.domain
            is DomainResult.Mirror    -> result.domain
            is DomainResult.Hardcoded -> result.domain
            is DomainResult.Offline   -> MovieBoxConfig.DEFAULT_DOMAIN
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(id)
        Logger.i("[$id] Domain reset — will re-resolve on next request", TAG)
    }

    override suspend fun search(query: String): List<SearchResult> = search(query, 1)

    override suspend fun search(query: String, page: Int): List<SearchResult> {
        ensureDomain()
        return runCatching {
            searchImpl.search(query = query, baseUrl = baseUrl, page = page)
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
