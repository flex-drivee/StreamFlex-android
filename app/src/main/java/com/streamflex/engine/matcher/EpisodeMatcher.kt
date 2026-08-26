package com.streamflex.engine.matcher

import com.streamflex.domain.models.SearchResult

/**
 * Finds the best episode match from provider search results.
 *
 * This class only compares titles.
 * It does not perform searching or loading.
 */
object EpisodeMatcher {

    /**
     * Returns the highest scoring episode.
     */
    fun bestMatch(
        title: String,
        season: Int,
        episode: Int,
        results: List<SearchResult>
    ): SearchResult? {

        return results
            .maxByOrNull {

                score(
                    title,
                    season,
                    episode,
                    it
                )
            }
            ?.takeIf {

                score(
                    title,
                    season,
                    episode,
                    it
                ) > 0
            }
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
        var score = 0
        val title = SearchNormalizer.normalize(result.title)
        val expected = SearchNormalizer.normalize(expectedTitle)

        // Exact / base title match
        if (title.contains(expected)) {
            score += 50
        }

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
                score -= 40 // Penalize results targeting another season
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