package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.engine.matcher.SearchNormalizer
import com.cinetheta.engine.matcher.TitleMatcher

/**
 * Dedicated matcher for ToonStream.
 * Handles Hindi dub suffixes, anime/cartoon season conventions,
 * and multi-part naming.
 */
object ToonStreamMatcher : ProviderMatcher {

    override val providerId: String = "toonstream"

    override fun matchMovie(
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .map { it to scoreMovie(expectedTitle, year, it) }
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

    private fun scoreMovie(expectedTitle: String, year: Int?, result: SearchResult): Double {
        val cleanExpected = cleanAnimeTitle(expectedTitle)
        val cleanResult = cleanAnimeTitle(result.title)

        var similarity = TitleMatcher.similarity(cleanExpected, cleanResult)
        result.originalTitle?.let { orig ->
            similarity = maxOf(similarity, TitleMatcher.similarity(cleanExpected, cleanAnimeTitle(orig)))
        }

        return similarity
    }

    private fun scoreEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        result: SearchResult
    ): Int {
        val cleanExpected = cleanAnimeTitle(expectedTitle)
        val cleanResult = cleanAnimeTitle(result.title)

        var baseResult = cleanResult.replace(Regex("(?i)\\b(?:season|s)\\s*0*\\d+.*"), "")
        baseResult = baseResult.replace(Regex("(?i)\\b(?:episode|ep|e)\\s*0*\\d+.*"), "")
        baseResult = baseResult.trim()
        if (baseResult.isEmpty()) baseResult = cleanResult

        var sim = TitleMatcher.similarity(cleanExpected, baseResult)
        result.originalTitle?.let { orig ->
            sim = maxOf(sim, TitleMatcher.similarity(cleanExpected, cleanAnimeTitle(orig)))
        }

        if (cleanResult.contains(cleanExpected) || cleanExpected.contains(baseResult)) {
            sim = maxOf(sim, 0.85)
        }

        if (sim < 0.65) return -1

        var score = (sim * 80).toInt()

        // Season bonus
        val sStr = season.toString()
        val sPad = "%02d".format(season)
        val rawTitle = SearchNormalizer.normalize(result.title)

        if (rawTitle.contains("season $sStr") || rawTitle.contains("season $sPad") ||
            rawTitle.contains("s$sStr") || rawTitle.contains("s$sPad")) {
            score += 20
        }

        // Dub bonus if present
        if (rawTitle.contains("hindi") || rawTitle.contains("dual audio")) {
            score += 5
        }

        return score
    }

    private fun cleanAnimeTitle(title: String): String {
        var t = title.lowercase()
        // Strip common ToonStream suffixes
        t = t.replace(Regex("(?i)\\[?(?:hindi|dual audio|multi audio|dubbed|dub|sub)\\]?"), "")
        t = t.replace(Regex("[^a-zA-Z0-9 ]"), " ")
        t = t.replace(Regex("\\s+"), " ")
        return t.trim()
    }
}
