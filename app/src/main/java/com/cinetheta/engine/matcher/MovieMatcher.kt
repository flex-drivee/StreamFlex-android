package com.cinetheta.engine.matcher

import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult

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
    
    /**
     * Returns the highest ranked search results up to a limit.
     */
    fun topMatches(
        title: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int = 1
    ): List<SearchResult> {
        if (results.isEmpty()) {
            return emptyList()
        }

        return results
            .map { it to score(title, year, it) }
            .filter { it.second >= 0.80 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

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

            ?.takeIf { it.second >= 0.80 }?.first
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

        // Exact title match bonus
        if (result.title.equals(title, ignoreCase = true) || result.originalTitle?.equals(title, ignoreCase = true) == true) {
            score += 0.50
        }

        // Detect sequel numbers (e.g. query is "Spider-Man" but candidate is "Spider-Man 3" or "Spider-Man 2")
        val sequelRegex = Regex("""\b([2-9]|II|III|IV|V)\b""", RegexOption.IGNORE_CASE)
        val queryHasSequel = sequelRegex.containsMatchIn(title)
        val resultHasSequel = sequelRegex.containsMatchIn(result.title)
        if (!queryHasSequel && resultHasSequel) {
            score -= 0.60
        }

        //----------------------------------------------------
        // Year bonus / penalty
        //----------------------------------------------------
        if (year != null && result.year != null && result.year > 0) {
            val diff = kotlin.math.abs(result.year - year)
            when {
                diff == 0 -> score += 0.60
                diff == 1 -> score += 0.30
                diff > 1 -> score -= 0.60 // Penalize different years
            }
        }

        //----------------------------------------------------
        // Prefer movies
        //----------------------------------------------------
        if (result.mediaType == MediaType.MOVIE) {
            score += 0.20
        }

        return score
    }
}