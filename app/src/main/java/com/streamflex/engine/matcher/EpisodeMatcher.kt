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

        val title =
            SearchNormalizer.normalize(
                result.title
            )

        val expected =
            SearchNormalizer.normalize(
                expectedTitle
            )

        // Exact title

        if (title.contains(expected)) {

            score += 50

        }

        // S01E01

        if (title.contains("s%02de%02d".format(season, episode).lowercase())) {

            score += 100

        }

        // S1E1

        if (title.contains("s${season}e${episode}".lowercase())) {

            score += 100

        }

        // 1x01

        if (title.contains("${season}x%02d".format(episode))) {

            score += 90

        }

        // Season 1

        if (title.contains("season $season")) {

            score += 40

        }

        // Episode 1

        if (title.contains("episode $episode")) {

            score += 40

        }

        // Ep 1

        if (title.contains("ep $episode")) {

            score += 35

        }

        return score
    }
}