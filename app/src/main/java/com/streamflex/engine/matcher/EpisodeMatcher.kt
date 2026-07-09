package com.streamflex.engine.matcher

import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult
import kotlin.math.abs

/**
 * Selects the best TV episode/show match from provider search results.
 *
 * Version 1:
 * - Title similarity
 * - Year bonus
 * - Prefer TV content
 *
 * Future:
 * - Season matching
 * - Episode matching
 * - Multi-language titles
 */
object EpisodeMatcher {

    /**
     * Minimum confidence required.
     */
    private const val MIN_SCORE = 0.75

    /**
     * Returns the best matching search result.
     */
    fun bestMatch(
        title: String,
        season: Int,
        episode: Int,
        year: Int?,
        results: List<SearchResult>
    ): SearchResult? {

        if (results.isEmpty()) {
            return null
        }

        val best = results

            .map {

                it to score(
                    title = title,
                    season = season,
                    episode = episode,
                    year = year,
                    result = it
                )

            }

            .maxByOrNull {

                it.second

            }

        return if (
            best != null &&
            best.second >= MIN_SCORE
        ) {

            best.first

        } else {

            null

        }
    }

    /**
     * Score a search result.
     */
    private fun score(
        title: String,
        season: Int,
        episode: Int,
        year: Int?,
        result: SearchResult
    ): Double {

        var score = 0.0

        //--------------------------------------------------
        // Title similarity
        //--------------------------------------------------

        score += TitleMatcher.similarity(
            title,
            result.title
        )

        //--------------------------------------------------
        // Original title bonus
        //--------------------------------------------------

        result.originalTitle?.let {

            score = maxOf(
                score,
                TitleMatcher.similarity(
                    title,
                    it
                )
            )
        }

        //--------------------------------------------------
        // Year bonus
        //--------------------------------------------------

        if (
            year != null &&
            result.year != null
        ) {

            when {

                result.year == year ->
                    score += 0.40

                abs(result.year - year) == 1 ->
                    score += 0.20
            }
        }

        //--------------------------------------------------
        // Prefer TV content
        //--------------------------------------------------

        if (
            result.mediaType == MediaType.TV
        ) {

            score += 0.20
        }

        //--------------------------------------------------
        // Reserved for future episode parsing
        //--------------------------------------------------

        // season
        // episode

        return score
    }
}