package com.cinetheta.domain.repositories

import com.cinetheta.core.logger.Logger
import com.cinetheta.engine.matcher.EpisodeMatcher
import com.cinetheta.engine.matcher.MovieMatcher
import com.cinetheta.domain.models.FinalStreams
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.engine.stream.StreamEngine
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
        val sources = if (providerResult.success && providerResult.sources.isNotEmpty()) {
            providerResult.sources
        } else {
            emptyList()
        }
        return resolveSources(sources, onStreamFound)
    }

    suspend fun resolveSources(sources: List<com.cinetheta.domain.models.ProviderSource>, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams {
        if (sources.isEmpty()) return FinalStreams.EMPTY
        return streamEngine.resolve(sources, onStreamFound)
    }

    suspend fun resolveMovie(title: String, year: Int? = null, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams = coroutineScope {
        if (providerRepository.isNoneSelected) return@coroutineScope FinalStreams.EMPTY
        val cleanTitle = title.replace(Regex("[:\\-–—_.'!?()]+"), " ").replace(Regex("\\s+"), " ").trim()
        val baseResults = search(title)
        val cleanResults = if (cleanTitle.lowercase() != title.lowercase()) search(cleanTitle) else emptyList()
        val directResults = (baseResults + cleanResults).distinctBy { it.url }

        val results = if (directResults.isNotEmpty()) {
            directResults
        } else {
            val shortTitle = cleanTitle.split(" ").take(2).joinToString(" ")
            if (shortTitle.length > 3 && shortTitle.lowercase() != title.lowercase()) {
                search(shortTitle)
            } else {
                emptyList()
            }
        }.distinctBy { it.url }

        if (results.isEmpty()) return@coroutineScope FinalStreams.EMPTY

        val bestMatches = results.groupBy { it.providerName }.flatMap { entry ->
            val pId = entry.value.firstOrNull()?.providerId ?: entry.key
            val matches = com.cinetheta.engine.matcher.providers.MatcherDispatcher.matchMovie(pId, title, year, entry.value, limit = 1).take(1)
            if (matches.isNotEmpty()) {
                matches.forEach { match ->
                    Logger.d("Top movie match for ${entry.key}: ${match.title} | ${match.url}", "StreamRepository")
                }
                matches
            } else {
                Logger.d("No valid movie match for ${entry.key} for query: $title ($year)", "StreamRepository")
                emptyList()
            }
        }

        val prunedMatches = pruneNetMirrorMatches(bestMatches)
        Logger.d("Selected ${prunedMatches.size} provider(s) to load for movie $title: ${prunedMatches.map { "${it.providerName} (${it.title})" }}", "StreamRepository")

        val deferredResults = prunedMatches.map { selected ->
            async { loadContent(selected) }
        }

        val allSources = mutableListOf<com.cinetheta.domain.models.ProviderSource>()
        for (deferred in deferredResults) {
            val providerResult = deferred.await() ?: continue
            val sources = if (providerResult.success && providerResult.sources.isNotEmpty()) {
                providerResult.sources
            } else if (providerResult.success) {
                providerResult.seasons.firstOrNull()?.episodes?.firstOrNull()?.sources ?: emptyList()
            } else emptyList()
            allSources.addAll(sources)
        }

        if (allSources.isEmpty()) return@coroutineScope FinalStreams.EMPTY
        return@coroutineScope streamEngine.resolve(allSources, onStreamFound)
    }

    suspend fun resolveEpisode(title: String, season: Int, episode: Int, year: Int? = null, onStreamFound: suspend (FinalStreams) -> Unit = {}): FinalStreams = coroutineScope {
        if (providerRepository.isNoneSelected) return@coroutineScope FinalStreams.EMPTY
        Logger.d("resolveEpisode called: title=$title, season=$season, episode=$episode", "StreamRepository")
        
        val cleanTitle = title.replace(Regex("[:\\-–—_.'!?()]+"), " ").replace(Regex("\\s+"), " ").trim()
        val baseResults = search(title)
        val cleanResults = if (cleanTitle.lowercase() != title.lowercase()) search(cleanTitle) else emptyList()
        val seasonResults = search("$title Season $season")
        val cleanSeasonResults = if (cleanTitle.lowercase() != title.lowercase()) search("$cleanTitle Season $season") else emptyList()
        
        val directResults = (seasonResults + cleanSeasonResults + baseResults + cleanResults).distinctBy { it.url }
        val combinedResults = if (directResults.isNotEmpty()) {
            directResults
        } else {
            val shortTitle = cleanTitle.split(" ").take(2).joinToString(" ")
            if (shortTitle.length > 3 && shortTitle.lowercase() != title.lowercase()) {
                search(shortTitle)
            } else {
                emptyList()
            }
        }
        if (combinedResults.isEmpty()) {
            Logger.w("No search results found for query: $title", "StreamRepository")
            return@coroutineScope FinalStreams.EMPTY
        }

        val bestMatches = combinedResults.groupBy { it.providerName }.flatMap { entry ->
            val pId = entry.value.firstOrNull()?.providerId ?: entry.key
            val matches = com.cinetheta.engine.matcher.providers.MatcherDispatcher.matchEpisode(
                providerIdentifier = pId,
                expectedTitle = title,
                season = season,
                episode = episode,
                year = year,
                results = entry.value,
                limit = 1
            ).distinctBy { it.url }.take(1)
            if (matches.isNotEmpty()) {
                matches.forEach { match ->
                    Logger.d("Top match for ${entry.key}: ${match.title} | ${match.url}", "StreamRepository")
                }
                matches
            } else {
                Logger.d("No valid match for ${entry.key} for query: $title S${season}E${episode}", "StreamRepository")
                emptyList()
            }
        }

        val prunedMatches = pruneNetMirrorMatches(bestMatches)

        val deferredResults = prunedMatches.map { selected ->
            async { loadContent(selected) }
        }

        val allSources = mutableListOf<com.cinetheta.domain.models.ProviderSource>()
        for (deferred in deferredResults) {
            val providerResult = deferred.await()
            if (providerResult == null) {
                Logger.w("loadContent returned null", "StreamRepository")
                continue
            }
            
            val allProviderEpisodes = providerResult.seasons.flatMap { it.episodes }
            val targetSeason = providerResult.seasons.find { it.number == season }
            
            Logger.d("Provider ${providerResult.providerId} has ${providerResult.seasons.size} seasons, ${allProviderEpisodes.size} total episodes", "StreamRepository")
            
            // 1. Exact match in exact season
            var targetEpisode = targetSeason?.episodes?.find { it.number == episode }
            if (targetEpisode != null) Logger.d("Found via exact season match", "StreamRepository")
            
            // 2. Exact absolute match across all seasons
            if (targetEpisode == null) {
                targetEpisode = allProviderEpisodes.find { it.number == episode }
                if (targetEpisode != null) Logger.d("Found via absolute cross-season match", "StreamRepository")
            }
            
            // 3. Positional fallback
            if (targetEpisode == null && episode > 0 && episode <= allProviderEpisodes.size) {
                targetEpisode = allProviderEpisodes[episode - 1]
                Logger.d("Found via positional fallback: fallback episode is ${targetEpisode.title} (num: ${targetEpisode.number})", "StreamRepository")
            }

            if (targetEpisode == null) {
                Logger.w("Could not find episode $episode using any method", "StreamRepository")
            }

            val sources = if (targetEpisode != null && targetEpisode.sources.isNotEmpty()) {
                targetEpisode.sources
            } else if (providerResult.success && providerResult.sources.isNotEmpty()) {
                Logger.d("Using root sources from ProviderResult (Fallback for standalone episode/movie format)", "StreamRepository")
                providerResult.sources
            } else {
                emptyList()
            }
            allSources.addAll(sources)
        }

        Logger.d("All collected sources: ${allSources.size}", "StreamRepository")
        if (allSources.isEmpty()) return@coroutineScope FinalStreams.EMPTY
        
        return@coroutineScope streamEngine.resolve(allSources, onStreamFound)
    }

    private fun pruneNetMirrorMatches(matches: List<SearchResult>): List<SearchResult> {
        val netMirrorMatches = matches.filter { it.providerId == "netmirror" }
        if (netMirrorMatches.size <= 2) return matches

        val nonNetMirror = matches.filter { it.providerId != "netmirror" }
        val sortedNetMirror = netMirrorMatches.sortedByDescending { match ->
            when {
                match.providerName.contains("Netflix", ignoreCase = true) -> 400
                match.providerName.contains("Prime", ignoreCase = true) -> 300
                match.providerName.contains("Disney", ignoreCase = true) -> 200
                match.providerName.contains("Hotstar", ignoreCase = true) -> 100
                else -> 50
            }
        }.take(2)

        Logger.d("Pruned NetMirror sub-providers from ${netMirrorMatches.size} to ${sortedNetMirror.size}: ${sortedNetMirror.map { it.providerName }}", "StreamRepository")
        return nonNetMirror + sortedNetMirror
    }
}
