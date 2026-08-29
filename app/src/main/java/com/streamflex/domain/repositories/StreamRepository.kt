package com.streamflex.domain.repositories

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.engine.stream.StreamEngine
import com.streamflex.engine.matcher.EpisodeMatcher
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
        item: SearchResult,
        onStreamFound: suspend (FinalStreams) -> Unit = {}
    ): FinalStreams {

        val providerResult =
            loadContent(item)
                ?: return FinalStreams.EMPTY

        return getStreams(providerResult, onStreamFound)
    }

    /**
     * Resolve streams from ProviderResult.
     */
    suspend fun getStreams(
        providerResult: ProviderResult,
        onStreamFound: suspend (FinalStreams) -> Unit = {}
    ): FinalStreams {
        val sources = if (providerResult.sources.isNotEmpty()) {
            providerResult.sources
        } else {
            providerResult.seasons.firstOrNull()?.episodes?.firstOrNull()?.sources ?: emptyList()
        }

        return streamEngine.resolve(
            sources,
            onStreamFound
        )
    }

    /**
     * Resolve a movie by title.
     */
    suspend fun resolveMovie(
        title: String,
        year: Int? = null,
        onStreamFound: suspend (FinalStreams) -> Unit = {}
    ): FinalStreams {

        val results = search(title)

        if (results.isEmpty()) {
            return FinalStreams.EMPTY
        }

        val selected = MovieMatcher.bestMatch(
            title = title,
            year = year,
            results = results
        ) ?: return FinalStreams.EMPTY

        val providerResult = loadContent(selected) ?: return FinalStreams.EMPTY

        val sources = if (providerResult.sources.isNotEmpty()) {
            providerResult.sources
        } else {
            providerResult.seasons.firstOrNull()?.episodes?.firstOrNull()?.sources ?: emptyList()
        }

        return streamEngine.resolve(sources, onStreamFound)
    }

    /**
     * Resolve a TV episode.
     */
    suspend fun resolveEpisode(
        title: String,
        season: Int,
        episode: Int,
        year: Int? = null,
        onStreamFound: suspend (FinalStreams) -> Unit = {}
    ): FinalStreams {

        // Search both base title and season-specific query
        val baseResults = search(title)
        val seasonResults = search("$title Season $season")
        val combinedResults = (seasonResults + baseResults).distinctBy { it.url }

        if (combinedResults.isEmpty()) {
            return FinalStreams.EMPTY
        }

        val selected = EpisodeMatcher.bestMatch(
            title = title,
            season = season,
            episode = episode,
            results = combinedResults
        ) ?: combinedResults.firstOrNull() ?: return FinalStreams.EMPTY

        val providerResult = loadContent(selected) ?: return FinalStreams.EMPTY

        // Find the matching season
        val targetSeason = providerResult.seasons.find { it.number == season }

        // Find the matching episode in that season
        val targetEpisode = targetSeason?.episodes?.find { it.number == episode }

        val sources = if (targetEpisode != null && targetEpisode.sources.isNotEmpty()) {
            targetEpisode.sources
        } else if (providerResult.sources.isNotEmpty() && providerResult.seasons.isEmpty()) {
            // Fallback for providers that just return raw sources without season mapping (e.g. movies)
            providerResult.sources
        } else {
            emptyList()
        }

        return streamEngine.resolve(sources, onStreamFound)
    }
}