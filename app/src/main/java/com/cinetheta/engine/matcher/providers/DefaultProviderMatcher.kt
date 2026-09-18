package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.engine.matcher.SearchNormalizer
import com.cinetheta.engine.matcher.TitleMatcher
import kotlin.math.abs

/**
 * Default fallback matcher for general providers (e.g. HDHub4u, 4kHDHub).
 */
object DefaultProviderMatcher : ProviderMatcher {

    override val providerId: String = "default"

    override fun matchMovie(
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .map { it to scoreMovie(expectedTitle, year, it) }
            .filter { it.second >= 0.75 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    override fun matchEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        year: Int?,
        results: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .map { it to scoreEpisode(expectedTitle, season, episode, it) }
            .filter { it.second >= 45 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun scoreMovie(expectedTitle: String, year: Int?, result: SearchResult): Double {
        var similarity = TitleMatcher.similarity(expectedTitle, result.title)
        result.originalTitle?.let { orig ->
            similarity = maxOf(similarity, TitleMatcher.similarity(expectedTitle, orig))
        }

        var score = similarity
        if (year != null && result.year != null) {
            val diff = abs(year - result.year!!)
            when (diff) {
                0 -> score += 0.10
                1 -> score += 0.05
                2 -> score += 0.00
                else -> score -= 0.15
            }
        }

        when (result.mediaType) {
            MediaType.MOVIE -> score += 0.05
            MediaType.TV -> score -= 0.10
            else -> {}
        }

        return score
    }

    private fun scoreEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        result: SearchResult
    ): Int {
        val mainExpectedTitle = expectedTitle.split(" -", "-").first().trim()
        val expectedToUse = if (mainExpectedTitle.length > 3) mainExpectedTitle else expectedTitle

        var baseResultTitle = result.title.replace(Regex("(?i)\\b(?:season|s)\\s*0*\\d+.*"), "")
        baseResultTitle = baseResultTitle.replace(Regex("(?i)\\b(?:episode|ep|e)\\s*0*\\d+.*"), "")
        baseResultTitle = baseResultTitle.replace(Regex("(?i)\\b\\d+x\\d+.*"), "")
        baseResultTitle = baseResultTitle.trim()
        if (baseResultTitle.isEmpty()) baseResultTitle = result.title

        var sim1 = TitleMatcher.similarity(expectedTitle, baseResultTitle)
        var sim2 = TitleMatcher.similarity(expectedToUse, baseResultTitle)
        result.originalTitle?.let { orig ->
            sim1 = maxOf(sim1, TitleMatcher.similarity(expectedTitle, orig))
            sim2 = maxOf(sim2, TitleMatcher.similarity(expectedToUse, orig))
        }
        val sim = maxOf(sim1, sim2)

        val containsMatch = (baseResultTitle.length >= 4 && expectedTitle.contains(baseResultTitle, ignoreCase = true)) ||
                            (expectedToUse.length >= 4 && baseResultTitle.contains(expectedToUse, ignoreCase = true))

        val effectiveSim = if (containsMatch) maxOf(sim, 0.85) else sim
        if (effectiveSim < 0.70) return -1

        var score = (effectiveSim * 80).toInt()
        if (result.mediaType == MediaType.TV) score += 25

        val title = SearchNormalizer.normalize(result.title)
        val seasonStr = season.toString()
        val seasonPadded = "%02d".format(season)
        val epStr = episode.toString()
        val epPadded = "%02d".format(episode)

        if (title.contains("s$seasonPadded") || title.contains("season $seasonStr")) score += 20
        if (title.contains("e$epPadded") || title.contains("episode $epStr")) score += 20

        return score
    }
}
