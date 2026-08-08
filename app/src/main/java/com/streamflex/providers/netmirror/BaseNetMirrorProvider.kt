package com.streamflex.providers.netmirror

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

abstract class BaseNetMirrorProvider(
    private val cacheManager: CacheManager = CacheManager()
) : Provider {

    protected abstract val ottType: String

    override val supportedMedia = setOf(MediaType.MOVIE, MediaType.TV)
    
    // In Phase 3, we use a single hardcoded domain for simplicity since NetMirror domains
    // rarely change compared to HDHub4u, and if they do, the bypass logic handles it.
    override val baseUrl: String = NetMirrorConfig.DEFAULT_DOMAIN

    private val searchImpl = NetMirrorSearch()
    private val detailsImpl = NetMirrorDetails()

    companion object {
        private const val TAG = "BaseNetMirrorProvider"
    }

    override suspend fun search(query: String): List<SearchResult> {
        return runCatching {
            searchImpl.search(query = query, baseUrl = baseUrl, ott = ottType)
        }.onFailure {
            Logger.e("[$id] Search failed for '$query': ${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        return runCatching {
            detailsImpl.load(
                result = searchResult,
                baseUrl = baseUrl,
                ott = ottType,
                providerId = id,
                providerName = name
            )
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
        }.getOrNull()
    }
}
