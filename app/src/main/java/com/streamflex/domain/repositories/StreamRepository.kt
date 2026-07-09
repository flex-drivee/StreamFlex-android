package com.streamflex.domain.repositories

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.engine.stream.StreamEngine
import com.streamflex.engine.matcher.MovieMatcher

/**
 * High-level entry point for the streaming system.
 *
 * The UI should communicate only with this repository.
 *
 * Internally it coordinates:
 *
 * ProviderRepository
 *      ↓
 * StreamEngine
 *      ↓
 * FinalStreams
 */
class StreamRepository(

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
     * Search a single provider.
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
     * Resolve streams from a SearchResult.
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

    /**
     * Resolve a movie by title.
     *
     * Version 1:
     * - search
     * - first result
     * - load
     * - extract
     */
    suspend fun resolveMovie(
        title: String,
        year: Int? = null
    ): FinalStreams {

        val results =
            search(title)

        if (results.isEmpty()) {
            return FinalStreams.EMPTY
        }

        val selected = MovieMatcher.bestMatch(
            title = title,
            year = year,
            results = results
        ) ?: return FinalStreams.EMPTY
        return getStreams(selected)
    }

    /**
     * Resolve a TV episode.
     *
     * Matching logic will be improved later.
     */
    suspend fun resolveEpisode(
        title: String,
        season: Int,
        episode: Int,
        year: Int? = null
    ): FinalStreams {

        return resolveMovie(
            title = title,
            year = year
        )
    }
}