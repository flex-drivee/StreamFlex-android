package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.SearchResult

/**
 * Contract for provider-specific content matching algorithms.
 * Each provider implements its own matching heuristics, title cleaning,
 * and accuracy scoring formulas to prevent cross-provider regressions.
 */
interface ProviderMatcher {
    val providerId: String

    fun matchMovie(
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int = 1
    ): List<SearchResult>

    fun matchEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        year: Int?,
        results: List<SearchResult>,
        limit: Int = 1
    ): List<SearchResult>
}
