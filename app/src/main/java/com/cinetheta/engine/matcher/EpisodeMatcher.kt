package com.cinetheta.engine.matcher

import com.cinetheta.domain.models.SearchResult

/**
 * Finds the best episode match from provider search results.
 *
 * This class only compares titles.
 * It does not perform searching or loading.
 */
object EpisodeMatcher {

    fun topMatches(
        title: String,
        season: Int,
        episode: Int,
        results: List<SearchResult>,
        limit: Int = 2
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()
        val providerId = results.first().providerId
        return com.cinetheta.engine.matcher.providers.MatcherDispatcher.matchEpisode(
            providerIdentifier = providerId,
            expectedTitle = title,
            season = season,
            episode = episode,
            year = null,
            results = results,
            limit = limit
        )
    }

    /**
     * Returns the highest scoring episode.
     */
    fun bestMatch(
        title: String,
        season: Int,
        episode: Int,
        results: List<SearchResult>
    ): SearchResult? {
        return topMatches(title, season, episode, results, limit = 1).firstOrNull()
    }

    /**
     * Calculates a confidence score.
     */
    private fun score(
        expectedTitle: String,
        season: Int,
        episode: Int,
        result: SearchResult
    ): Int {
        // Many anime providers omit the subtitle (e.g. "Re:ZERO" instead of "Re:ZERO -Starting Life in Another World-")
        // We take the first part before a hyphen if it's long enough.
        val mainExpectedTitle = expectedTitle.split(" -", "-").first().trim()
        val expectedToUse = if (mainExpectedTitle.length > 3) mainExpectedTitle else expectedTitle
        
        // Strip season and episode info from the provider's title to get the base title
        var baseResultTitle = result.title.replace(Regex("(?i)\\b(?:season|s)\\s*0*\\d+.*"), "")
        baseResultTitle = baseResultTitle.replace(Regex("(?i)\\b(?:episode|ep|e)\\s*0*\\d+.*"), "")
        baseResultTitle = baseResultTitle.replace(Regex("(?i)\\b\\d+x\\d+.*"), "")
        baseResultTitle = baseResultTitle.replace(Regex("(?i)hindi dub.*"), "")
        baseResultTitle = baseResultTitle.trim()
        
        // If the base title is empty after stripping, just use the original
        if (baseResultTitle.isEmpty()) baseResultTitle = result.title
        
        // Use the highest similarity between the full expected title and the main expected title
        var sim1 = TitleMatcher.similarity(expectedTitle, baseResultTitle)
        var sim2 = TitleMatcher.similarity(expectedToUse, baseResultTitle)
        result.originalTitle?.let { orig ->
            sim1 = maxOf(sim1, TitleMatcher.similarity(expectedTitle, orig))
            sim2 = maxOf(sim2, TitleMatcher.similarity(expectedToUse, orig))
        }
        val sim = maxOf(sim1, sim2)

        val containsMatch = (baseResultTitle.length >= 4 && expectedTitle.contains(baseResultTitle, ignoreCase = true)) ||
                            (expectedToUse.length >= 4 && baseResultTitle.contains(expectedToUse, ignoreCase = true)) ||
                            (expectedTitle.length >= 4 && baseResultTitle.contains(expectedTitle, ignoreCase = true))

        val effectiveSim = if (containsMatch) maxOf(sim, 0.85) else sim

        if (effectiveSim < 0.70) {
            return -1 // Title doesn't match closely enough, discard to prevent season/episode bonuses from overpowering it
        }

        var score = (effectiveSim * 80).toInt()

        // Bonus for explicit TV media type
        if (result.mediaType == com.cinetheta.domain.models.MediaType.TV) {
            score += 25
        }
        val title = SearchNormalizer.normalize(result.title)

        val seasonStr = season.toString()
        val seasonPadded = "%02d".format(season)
        val epStr = episode.toString()
        val epPadded = "%02d".format(episode)

        // Season matching (+60 if matching current season, -40 if matching a different season)
        val matchesSeason = title.contains("season $seasonStr") ||
                title.contains("season$seasonStr") ||
                title.contains("s$seasonPadded") ||
                title.contains("s$seasonStr") ||
                title.contains("season $seasonPadded")

        if (matchesSeason) {
            score += 60
        } else {
            val otherSeasonMatch = Regex("""\b(?:season|s)\s*0*(\d{1,2})\b""", RegexOption.IGNORE_CASE).findAll(title)
            val mentionedSeasons = otherSeasonMatch.mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
            if (mentionedSeasons.isNotEmpty() && !mentionedSeasons.contains(season)) {
                // FIX: If the provider mentions a season (e.g. "Season 2"), but we requested Season 1,
                // check if the EXPECTED TITLE actually contains that same number (e.g. AniList "Jujutsu Kaisen 2nd Season").
                // If it does, this is actually a PERFECT match for Anime sites that use Titles for seasons!
                val expectedLower = expectedTitle.lowercase()
                var shouldPenalize = true
                for (m in mentionedSeasons) {
                    if (expectedLower.contains(m.toString())) {
                        shouldPenalize = false
                        score += 60 // Treat as a direct match
                        break
                    }
                }
                
                if (shouldPenalize) {
                    score -= 40 // Penalize results targeting another season
                }
            }
        }

        // S01E01 / S1E1
        if (title.contains("s${seasonPadded}e${epPadded}".lowercase())) {
            score += 100
        }

        if (title.contains("s${seasonStr}e${epStr}".lowercase())) {
            score += 100
        }

        // 1x01
        if (title.contains("${seasonStr}x${epPadded}")) {
            score += 90
        }

        // Episode 1 / Ep 1
        if (title.contains("episode $episode") || title.contains("episode $epPadded")) {
            score += 40
        }

        if (title.contains("ep $episode") || title.contains("ep $epPadded")) {
            score += 35
        }

        return score
    }
}
