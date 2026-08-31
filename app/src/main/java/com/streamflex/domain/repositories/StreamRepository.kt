package com.streamflex.domain.repositories

import com.streamflex.engine.matcher.EpisodeMatcher
import com.streamflex.engine.matcher.MovieMatcher
import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.engine.stream.StreamEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * High-level repository that combines:
 * 1. Provider search
 * 2. Match selection
 * 3. Content loading
 * 4. Stream resolution
 */
class StreamRepository(
    private val providerRepository: ProviderRepository,
    private val streamEngine: StreamEngine
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
     * Directly resolve a loaded provider result.
     */
    suspend fun resolve(
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
    ): FinalStreams = coroutineScope {

        val results = search(title)

        if (results.isEmpty()) {
            return@coroutineScope FinalStreams.EMPTY
        }

        val bestMatches = results.groupBy { it.providerName }.mapNotNull { entry ->
            MovieMatcher.bestMatch(title, year, entry.value)
        }

        val deferredResults = bestMatches.map { selected ->
            async { loadContent(selected) }
        }

        val allSources = mutableListOf<com.streamflex.domain.models.ProviderSource>()
        for (deferred in deferredResults) {
            val providerResult = deferred.await() ?: continue
            val sources = if (providerResult.sources.isNotEmpty()) {
                providerResult.sources
            } else {
                providerResult.seasons.firstOrNull()?.episodes?.firstOrNull()?.sources ?: emptyList()
            }
            allSources.addAll(sources)
        }

        if (allSources.isEmpty()) return@coroutineScope FinalStreams.EMPTY

        return@coroutineScope streamEngine.resolve(allSources, onStreamFound)
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
    ): FinalStreams = coroutineScope {

        // Search both base title and season-specific query
        val baseResults = search(title)
        val seasonResults = search("$title Season $season")
        val combinedResults = (seasonResults + baseResults).distinctBy { it.url }

        if (combinedResults.isEmpty()) {
            return@coroutineScope FinalStreams.EMPTY
        }

        val bestMatches = combinedResults.groupBy { it.providerName }.mapNotNull { entry ->
            EpisodeMatcher.bestMatch(title, season, episode, entry.value) ?: entry.value.firstOrNull()
        }

        val deferredResults = bestMatches.map { selected ->
            async { loadContent(selected) }
        }

        val allSources = mutableListOf<com.streamflex.domain.models.ProviderSource>()
        for (deferred in deferredResults) {
            val providerResult = deferred.await() ?: continue
            val targetSeason = providerResult.seasons.find { it.number == season }
            val targetEpisode = targetSeason?.episodes?.find { it.number == episode }

            val sources = if (targetEpisode != null && targetEpisode.sources.isNotEmpty()) {
                targetEpisode.sources
            } else if (providerResult.sources.isNotEmpty() && providerResult.seasons.isEmpty()) {
                // Fallback for providers that just return raw sources without season mapping (e.g. movies)
                providerResult.sources
            } else {
                emptyList()
            }
            allSources.addAll(sources)
        }

        if (allSources.isEmpty()) return@coroutineScope FinalStreams.EMPTY

        return@coroutineScope streamEngine.resolve(allSources, onStreamFound)
    }
}
