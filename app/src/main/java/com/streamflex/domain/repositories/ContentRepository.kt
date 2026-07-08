package com.streamflex.domain.repositories

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.engine.stream.StreamEngine

/**
 * Main repository exposed to the UI.
 *
 * Hides all provider and extraction logic.
 */
class ContentRepository(

    private val providerRepository: ProviderRepository,

    private val streamEngine: StreamEngine = StreamEngine

) {

    /**
     * Search every enabled provider.
     */
    suspend fun search(
        query: String
    ): List<SearchResult> {

        return providerRepository.search(query)
    }

    /**
     * Search only one provider.
     */
    suspend fun searchProvider(
        providerId: String,
        query: String
    ): List<SearchResult> {

        return providerRepository
            .provider(providerId)
            ?.search(query)
            ?: emptyList()
    }

    /**
     * Load provider content.
     */
    suspend fun loadContent(
        item: SearchResult
    ): ProviderResult? {

        return providerRepository.load(
            providerId = item.providerId,
            item = item
        )
    }

    /**
     * Convenience overload.
     */
    suspend fun getStreams(
        item: SearchResult
    ): FinalStreams {

        val providerResult =
            loadContent(item)
                ?: return FinalStreams.EMPTY

        return getStreams(providerResult)
    }

    /**
     * Resolve streams from ProviderResult.
     */
    suspend fun getStreams(
        providerResult: ProviderResult
    ): FinalStreams {

        return streamEngine.resolve(
            providerResult.sources
        )
    }
}