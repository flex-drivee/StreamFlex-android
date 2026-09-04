package com.streamflex.engine.matcher

import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult

/**
 * Selects the best movie match from provider search results.
 *
 * The score is based on:
 *
 * 1. Title similarity
 * 2. Release year
 * 3. Media type
 */
object MovieMatcher {

    /**
     * Returns the highest ranked search result.
     */
    fun bestMatch(
        title: String,
        year: Int?,
        results: List<SearchResult>
    ): SearchResult? {

        if (results.isEmpty()) {
            return null
        }

        return results

            .map {

                it to score(
                    title = title,
                    year = year,
                    result = it
                )

            }

            .maxByOrNull { it.second }

            ?.takeIf { it.second >= 0.45 }?.first
    }

    /**
     * Score a single search result.
     */
    private fun score(
        title: String,
        year: Int?,
        result: SearchResult
    ): Double {

        var titleSim = TitleMatcher.similarity(
            title,
            result.title
        )
        
        result.originalTitle?.let {
            titleSim = maxOf(titleSim, TitleMatcher.similarity(title, it))
        }
        
        // If the title is completely different (e.g., sim < 0.50), reject it entirely.
        // Otherwise, year=match (+0.4) and type=movie (+0.2) will cause random movies from the same year to pass the 0.45 threshold!
        if (titleSim < 0.50) {
            return 0.0
        }

        var score = titleSim



        //----------------------------------------------------
        // Year bonus
        //----------------------------------------------------

        if (
            year != null &&
            result.year != null
        ) {

            when {

                result.year == year ->
                    score += 0.40

                kotlin.math.abs(
                    result.year - year
                ) == 1 ->
                    score += 0.20
            }
        }

        //----------------------------------------------------
        // Prefer movies
        //----------------------------------------------------

        if (
            result.mediaType == MediaType.MOVIE
        ) {

            score += 0.20
        }

        return score
    }
}