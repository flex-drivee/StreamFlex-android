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

        var score = 0.0

        //----------------------------------------------------
        // Title similarity (0.0 → 1.0)
        //----------------------------------------------------

        score += TitleMatcher.similarity(
            title,
            result.title
        )

        //----------------------------------------------------
        // Original title bonus
        //----------------------------------------------------

        result.originalTitle?.let {

            score = maxOf(

                score,

                TitleMatcher.similarity(
                    title,
                    it
                )

            )
        }

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