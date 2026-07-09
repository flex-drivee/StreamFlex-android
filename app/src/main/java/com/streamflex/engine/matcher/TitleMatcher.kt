package com.streamflex.engine.matcher

import java.util.Locale
import kotlin.math.max

/**
 * Utility for comparing movie and TV titles.
 *
 * Every matcher (MovieMatcher, EpisodeMatcher, etc.)
 * should use this class instead of comparing strings
 * directly.
 */
object TitleMatcher {

    /**
     * Normalize a title before comparison.
     */
    fun normalize(
        title: String
    ): String {

        return title
            .lowercase(Locale.ROOT)
            .replace("&", "and")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Returns true when two titles are considered equal.
     */
    fun matches(
        first: String,
        second: String,
        threshold: Double = 0.85
    ): Boolean {

        return similarity(first, second) >= threshold
    }

    /**
     * Returns similarity between 0.0 and 1.0.
     */
    fun similarity(
        first: String,
        second: String
    ): Double {

        val a = normalize(first)
        val b = normalize(second)

        if (a == b) {
            return 1.0
        }

        if (a.isBlank() || b.isBlank()) {
            return 0.0
        }

        val distance = levenshtein(a, b)

        val maxLength = max(a.length, b.length)

        return 1.0 - distance.toDouble() / maxLength.toDouble()
    }

    /**
     * Standard Levenshtein distance.
     */
    private fun levenshtein(
        first: String,
        second: String
    ): Int {

        val costs = IntArray(second.length + 1)

        for (j in costs.indices) {
            costs[j] = j
        }

        for (i in first.indices) {

            var previous = i

            costs[0] = i + 1

            for (j in second.indices) {

                val current = costs[j + 1]

                costs[j + 1] = minOf(
                    costs[j + 1] + 1,
                    costs[j] + 1,
                    previous + if (first[i] == second[j]) 0 else 1
                )

                previous = current
            }
        }

        return costs.last()
    }
}