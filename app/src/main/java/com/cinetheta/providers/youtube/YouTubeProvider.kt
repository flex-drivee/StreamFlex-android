package com.cinetheta.providers.youtube

import com.cinetheta.core.cache.CacheManager
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.DomainResolver
import com.cinetheta.core.network.DomainResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider

class YouTubeProvider(
    private val cacheManager: CacheManager = CacheManager(),
    private val resolver: DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id   = YouTubeConfig.PROVIDER_ID
    override val name = YouTubeConfig.PROVIDER_NAME
    override val enabled = true

    override val supportedMedia = setOf(
        MediaType.UNKNOWN,  // YouTube content doesn't fit MOVIE/TV cleanly
        MediaType.LIVE
    )

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: YouTubeConfig.BASE_URL

    private val searchImpl  = YouTubeSearch()
    private val detailsImpl = YouTubeDetails()

    companion object {
        private const val TAG = "YouTubeProvider"
        const val MANIFEST_PATH = "providers/youtube.json"
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId = id,
            hardcoded = YouTubeConfig.BASE_URL,
            manifestPath = MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved -> {
                Logger.i("[$id] Domain resolved (${result.step}): ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Mirror -> {
                Logger.i("[$id] Using mirror [${result.mirrorIndex}]: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Hardcoded -> {
                Logger.w("[$id] Using hardcoded fallback: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Offline -> {
                Logger.e("[$id] Provider OFFLINE — tried: ${result.tried}", TAG)
                YouTubeConfig.BASE_URL
            }
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(id)
    }

    override suspend fun search(query: String): List<SearchResult> = search(query, 1)

    override suspend fun search(query: String, page: Int): List<SearchResult> {
        ensureDomain()
        return runCatching {
            searchImpl.search(query, page)
        }.onFailure {
            Logger.e("[$id] Search failed for '$query': ${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        ensureDomain()
        return runCatching {
            detailsImpl.load(searchResult)
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
            resetDomain()
        }.getOrNull()
    }
}
