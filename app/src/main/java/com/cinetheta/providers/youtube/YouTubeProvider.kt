package com.cinetheta.providers.youtube

import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider

class YouTubeProvider : Provider {

    override val id   = YouTubeConfig.PROVIDER_ID
    override val name = YouTubeConfig.PROVIDER_NAME
    override val baseUrl = YouTubeConfig.BASE_URL
    override val enabled = true

    override val supportedMedia = setOf(
        MediaType.UNKNOWN,  // YouTube content doesn't fit MOVIE/TV cleanly
        MediaType.LIVE
    )

    private val searchImpl  = YouTubeSearch()
    private val detailsImpl = YouTubeDetails()

    companion object {
        private const val TAG = "YouTubeProvider"
    }

    override suspend fun search(query: String): List<SearchResult> = search(query, 1)

    override suspend fun search(query: String, page: Int): List<SearchResult> {
        return runCatching {
            searchImpl.search(query, page)
        }.onFailure {
            Logger.e("[$id] Search failed for '$query': ${it.message}", TAG)
        }.getOrDefault(emptyList())
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        return runCatching {
            detailsImpl.load(searchResult)
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
        }.getOrNull()
    }
}
