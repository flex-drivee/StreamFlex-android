package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.SearchResult
import com.cinetheta.engine.matcher.TitleMatcher

/**
 * Dedicated matcher for AnimeDekho.
 * Supports Romanized (Romaji), English, and Japanese alternative titles.
 */
object AnimeDekhoMatcher : ProviderMatcher {

    override val providerId: String = "animedekho"

    override fun matchMovie(
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .map { it to scoreMovie(expectedTitle, it) }
            .filter { it.second >= 0.70 }
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
            .filter { it.second >= 40 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun scoreMovie(expectedTitle: String, result: SearchResult): Double {
        val cleanExpected = clean(expectedTitle)
        val cleanResult = clean(result.title)

        var similarity = TitleMatcher.similarity(cleanExpected, cleanResult)
        result.originalTitle?.let { orig ->
            similarity = maxOf(similarity, TitleMatcher.similarity(cleanExpected, clean(orig)))
        }

        if (cleanResult.contains(cleanExpected) || cleanExpected.contains(cleanResult)) {
            similarity = maxOf(similarity, 0.85)
        }

        return similarity
    }

    private fun scoreEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        result: SearchResult
    ): Int {
        val cleanExpected = clean(expectedTitle)
        var cleanResult = clean(result.title)

        var baseResult = cleanResult.replace(Regex("(?i)\\b(?:season|s)\\s*0*\\d+.*"), "")
        baseResult = baseResult.replace(Regex("(?i)\\b(?:episode|ep|e)\\s*0*\\d+.*"), "").trim()
        if (baseResult.isEmpty()) baseResult = cleanResult

        var sim = TitleMatcher.similarity(cleanExpected, baseResult)
        result.originalTitle?.let { orig ->
            sim = maxOf(sim, TitleMatcher.similarity(cleanExpected, clean(orig)))
        }

        if (cleanResult.contains(cleanExpected) || cleanExpected.contains(baseResult)) {
            sim = maxOf(sim, 0.85)
        }

        if (sim < 0.65) return -1

        var score = (sim * 80).toInt()

        val sStr = season.toString()
        val sPad = "%02d".format(season)
        if (cleanResult.contains("season $sStr") || cleanResult.contains("s$sStr") || cleanResult.contains("s$sPad")) {
            score += 20
        }

        return score
    }

    private fun clean(s: String): String {
        return s.lowercase()
            .replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
