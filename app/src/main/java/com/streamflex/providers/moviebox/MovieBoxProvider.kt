package com.streamflex.providers.moviebox

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.DomainResolver
import com.streamflex.core.network.DomainResult
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

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

        val domain = MovieBoxConfig.savedDomain ?: MovieBoxConfig.DEFAULT_DOMAIN
        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(id)
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
