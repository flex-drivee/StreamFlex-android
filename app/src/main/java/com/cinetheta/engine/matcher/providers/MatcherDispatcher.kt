package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.SearchResult

/**
 * Routes content matching requests to dedicated provider-specific matchers.
 * Guarantees that each provider operates with its own accuracy formula and title heuristics,
 * completely eliminating cross-provider regressions.
 */
object MatcherDispatcher {

    private val providers = listOf(
        NetMirrorMatcher,
        MovieBoxMatcher,
        ToonStreamMatcher,
        AnimeDekhoMatcher
    )

    fun getMatcher(providerIdentifier: String): ProviderMatcher {
        val id = providerIdentifier.lowercase().trim()
        
        // Exact match
        providers.firstOrNull { it.providerId == id }?.let { return it }

        // NetMirror OTT family (netflixmirror, primevideomirror, hotstarmirror, disneymirror)
        if (id.contains("mirror")) {
            return NetMirrorMatcher
        }

        // Partial match
        if (id.contains("moviebox")) return MovieBoxMatcher
        if (id.contains("toonstream")) return ToonStreamMatcher
        if (id.contains("animedekho")) return AnimeDekhoMatcher

        return DefaultProviderMatcher
    }

    fun matchMovie(
        providerIdentifier: String,
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int = 1
    ): List<SearchResult> {
        val matcher = getMatcher(providerIdentifier)
        return matcher.matchMovie(expectedTitle, year, results, limit)
    }

    fun matchEpisode(
        providerIdentifier: String,
        expectedTitle: String,
        season: Int,
        episode: Int,
        year: Int?,
        results: List<SearchResult>,
        limit: Int = 1
    ): List<SearchResult> {
        val matcher = getMatcher(providerIdentifier)
        return matcher.matchEpisode(expectedTitle, season, episode, year, results, limit)
    }
}
