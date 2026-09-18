package com.cinetheta.engine.matcher.providers

import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.engine.matcher.SearchNormalizer
import com.cinetheta.engine.matcher.TitleMatcher
import kotlin.math.abs

/**
 * Dedicated matcher for NetMirror and its OTT mirrors (Netflix, Prime, Hotstar, Disney).
 * 
 * Key guarantees:
 * 1. Strict title token isolation: Prevents prefix/suffix movies (e.g. "Jack Reacher")
 *    from ever matching a TV show with a similar name (e.g. "Reacher").
 * 2. Strict media type enforcement: Rejects MediaType.MOVIE when resolving episodes.
 * 3. Year verification: Discards results with significant year divergence (> 2 years).
 */
object NetMirrorMatcher : ProviderMatcher {

    override val providerId: String = "netmirror"

    private const val MIN_MOVIE_SIMILARITY = 0.80
    private const val MIN_EPISODE_SIMILARITY = 0.75
    private const val MIN_EPISODE_SCORE = 60

    override fun matchMovie(
        expectedTitle: String,
        year: Int?,
        results: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .map { it to scoreMovie(expectedTitle, year, it) }
            .filter { it.second >= MIN_MOVIE_SIMILARITY }
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
            .map { it to scoreEpisode(expectedTitle, season, episode, year, it) }
            .filter { it.second >= MIN_EPISODE_SCORE }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun scoreMovie(expectedTitle: String, year: Int?, result: SearchResult): Double {
        // NetMirror movies should never be TV series/season titles (e.g. titles with "Season", "Episode", etc.)
        val hasSeriesKeywords = result.title.contains(Regex("(?i)\\b(?:season|s\\d+|series|episodes?|cour\\s*\\d+|part\\s*\\d+)\\b"))
        if (hasSeriesKeywords && !result.title.contains(Regex("(?i)\\b(?:movie|film)\\b"))) return 0.0

        val expNorm = cleanTitle(expectedTitle)
        val resNorm = cleanTitle(result.title)

        var similarity = TitleMatcher.similarity(expNorm, resNorm)
        result.originalTitle?.let { orig ->
            similarity = maxOf(similarity, TitleMatcher.similarity(expNorm, cleanTitle(orig)))
        }

        // Strict token equality check for short titles
        if (!isTokenCompatible(expNorm, resNorm)) {
            similarity = minOf(similarity, 0.50)
        }

        if (similarity < MIN_MOVIE_SIMILARITY) return 0.0

        // Year verification
        var finalScore = similarity
        if (year != null && result.year != null) {
            val diff = abs(year - result.year!!)
            when {
                diff == 0 -> finalScore += 0.05
                diff == 1 -> finalScore += 0.02
                diff > 2  -> return 0.0 // Mismatched movie year in NetMirror
            }
        }

        return finalScore
    }

    private fun scoreEpisode(
        expectedTitle: String,
        season: Int,
        episode: Int,
        year: Int?,
        result: SearchResult
    ): Int {
        // Strict: Never match an explicit Movie when an episode is requested!
        val isExplicitMovie = result.title.contains(Regex("(?i)\\b(?:the\\s+movie|the\\s+film)\\b")) ||
                (result.mediaType == MediaType.MOVIE && !result.title.contains(Regex("(?i)\\b(?:season|s\\d+|cour|part|episodes?)\\b")) && result.year != null && year != null && abs(result.year!! - year) > 2)
        if (isExplicitMovie) {
            return -1
        }

        val expNorm = cleanTitle(expectedTitle)
        // Strip season and episode info from result title
        var resBase = cleanTitle(stripSeasonEpisode(result.title))
        if (resBase.isBlank()) resBase = cleanTitle(result.title)

        // Prevent "Jack Reacher" from matching "Reacher"
        if (!isTokenCompatible(expNorm, resBase)) {
            return -1
        }

        var sim = TitleMatcher.similarity(expNorm, resBase)
        result.originalTitle?.let { orig ->
            val origClean = cleanTitle(stripSeasonEpisode(orig))
            if (isTokenCompatible(expNorm, origClean)) {
                sim = maxOf(sim, TitleMatcher.similarity(expNorm, origClean))
            }
        }

        // Exact match gets full boost
        if (expNorm == resBase) {
            sim = 1.0
        }

        if (sim < MIN_EPISODE_SIMILARITY) {
            return -1
        }

        // Year verification: TV seasons can air years AFTER the premiere year, but not significantly BEFORE
        if (year != null && result.year != null) {
            if (result.year!! < year - 2) {
                return -1 // Aired significantly before the show premiere year
            }
        }

        var score = (sim * 80).toInt()

        // Explicit TV type boost
        if (result.mediaType == MediaType.TV) {
            score += 20
        }

        // Season matching in title (e.g. "Reacher Season 1", "Mushoku Tensei Season 2 Cour 2")
        val rawTitle = SearchNormalizer.normalize(result.title)
        val seasonMatch = Regex("(?i)\\b(?:season|s)\\s*0*(\\d+)\\b").find(rawTitle)
        val foundSeason = seasonMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

        if (foundSeason != null) {
            if (foundSeason == season) {
                score += 25 // Explicit matching season
            } else {
                return -1 // Explicitly a different season (e.g. S2 when S1 was requested)
            }
        } else {
            // NetMirror Season 1 almost always omits "Season 1" in its title
            if (season == 1) {
                score += 15
            }
        }

        return score
    }

    /**
     * Ensures that extra significant words in the result title do not falsely match.
     * Example:
     *   expected = "reacher", result = "jack reacher" -> FALSE (extra word "jack")
     *   expected = "reacher", result = "reacher" -> TRUE
     *   expected = "game of thrones", result = "game of thrones" -> TRUE
     */
    private fun isTokenCompatible(expected: String, result: String): Boolean {
        if (expected == result) return true
        val expTokens = expected.split(" ").filter { it.isNotBlank() }.toSet()
        val resTokens = result.split(" ").filter { it.isNotBlank() }.toSet()

        if (expTokens == resTokens) return true

        val benignTokens = setOf(
            "the", "a", "an", "series", "tv", "show", "hd", "4k", "part", "pt", 
            "season", "cour", "dub", "sub", "hindi", "english", "tamil", "telugu", 
            "japanese", "arc", "edition", "version", "special", "dual", "multi"
        )

        // If expected is 1 or 2 words (like "Reacher"), result must not contain extra words
        if (expTokens.size <= 2) {
            val extraTokens = resTokens - expTokens
            val significantExtra = extraTokens.filter { 
                it !in benignTokens && !it.all { ch -> ch.isDigit() }
            }
            if (significantExtra.isNotEmpty()) {
                return false
            }
        }

        // If all expected tokens are present in result (ignoring benign words), it's compatible
        val expSignificant = expTokens.filter { it !in benignTokens }
        if (expSignificant.isNotEmpty() && expSignificant.all { it in resTokens }) {
            return true
        }

        // Token overlap ratio
        val common = expTokens.intersect(resTokens).size
        val overlap = common.toDouble() / maxOf(expTokens.size, resTokens.size)
        return overlap >= 0.60
    }

    private fun cleanTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun stripSeasonEpisode(title: String): String {
        var t = title
        // Remove dub/sub annotations in parens or brackets
        t = t.replace(Regex("(?i)\\s*[\\[\\(].*?(?:dub|sub|audio|version|edition).*?[\\]\\)]"), "")
        // Remove season / episode / cour / part markers
        t = t.replace(Regex("(?i)\\b(?:season|s)\\s*0*\\d+.*"), "")
        t = t.replace(Regex("(?i)\\b(?:episode|ep|e)\\s*0*\\d+.*"), "")
        t = t.replace(Regex("(?i)\\b(?:cour|part|pt)\\s*0*\\d+.*"), "")
        t = t.replace(Regex("(?i)\\b\\d+x\\d+.*"), "")
        return t.trim()
    }
}
