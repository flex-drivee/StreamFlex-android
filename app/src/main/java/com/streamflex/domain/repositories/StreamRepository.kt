package com.streamflex.domain.repositories

import com.streamflex.engine.matcher.EpisodeMatcher
import com.streamflex.engine.matcher.MovieMatcher
import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.engine.stream.StreamEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StreamRepository(
    private val providerRepository: ProviderRepository,
    private val streamEngine: StreamEngine
) {

    suspend fun search(query: String): List<SearchResult> {
        return providerRepository.search(query)
    }

    suspend fun searchProvider(providerId: String, query: String): List<SearchResult> {
        return providerRepository.provider(providerId)?.search(query) ?: emptyList()
    }

    suspend fun loadContent(item: SearchResult): ProviderResult? {
        return providerRepository.load(providerId = item.providerId, item = item)
    }

    suspend fun resolve(providerResult: ProviderResult, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams {
        val sources = if (providerResult.sources.isNotEmpty()) {
            providerResult.sources
        } else {
            providerResult.seasons.firstOrNull()?.episodes?.firstOrNull()?.sources ?: emptyList()
        }
        return streamEngine.resolve(sources, onStreamFound)
    }

    suspend fun resolveMovie(title: String, year: Int? = null, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams = coroutineScope {
        val results = search(title)
        if (results.isEmpty()) return@coroutineScope FinalStreams.EMPTY

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

    suspend fun resolveEpisode(title: String, season: Int, episode: Int, year: Int? = null, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams = coroutineScope {
        val baseResults = search(title)
        val seasonResults = search("$title Season $season")
        
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        val shortTitle = cleanTitle.split(" ").take(2).joinToString(" ")
        val shortResults = if (shortTitle.length > 3 && shortTitle.lowercase() != title.lowercase()) {
            search(shortTitle)
        } else {
            emptyList()
        }
        
        val combinedResults = (seasonResults + baseResults + shortResults).distinctBy { it.url }
        if (combinedResults.isEmpty()) return@coroutineScope FinalStreams.EMPTY

        val bestMatches = combinedResults.groupBy { it.providerName }.mapNotNull { entry ->
            EpisodeMatcher.bestMatch(title, season, episode, entry.value)
        }

        val deferredResults = bestMatches.map { selected ->
            async { loadContent(selected) }
        }

        val allSources = mutableListOf<com.streamflex.domain.models.ProviderSource>()
        for (deferred in deferredResults) {
            val providerResult = deferred.await() ?: continue
            
            val allProviderEpisodes = providerResult.seasons.flatMap { it.episodes }
            val targetSeason = providerResult.seasons.find { it.number == season }
            
            // 1. Exact match in exact season
            var targetEpisode = targetSeason?.episodes?.find { it.number == episode }
            
            // 2. Exact absolute match across all seasons
            if (targetEpisode == null) {
                targetEpisode = allProviderEpisodes.find { it.number == episode }
            }
            
            // 3. Positional fallback (flattens split-season relative episodes into a single absolute timeline)
            // Example: Jujutsu Kaisen Ep 25. Provider has S1 (24 eps) and S2 (23 eps). 
            // 25th episode in the flattened list corresponds to S2 E1!
            if (targetEpisode == null && episode > 0 && episode <= allProviderEpisodes.size) {
                targetEpisode = allProviderEpisodes[episode - 1]
            }

            val sources = if (targetEpisode != null && targetEpisode.sources.isNotEmpty()) {
                targetEpisode.sources
            } else {
                emptyList()
            }
            allSources.addAll(sources)
        }

        if (allSources.isEmpty()) return@coroutineScope FinalStreams.EMPTY
        return@coroutineScope streamEngine.resolve(allSources, onStreamFound)
    }
}
