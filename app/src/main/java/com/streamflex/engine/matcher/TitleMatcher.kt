package com.streamflex.engine.matcher

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

        return SearchNormalizer
            .normalize(title)
            .replace("&", "and")
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

        if (a.isBlank() || b.isBlank()) {
            return 0.0
        }

        if (a == b) {
            return 1.0
        }

        // Exact containment bonus
        val aNoSpace = a.replace(" ", "")
        val bNoSpace = b.replace(" ", "")
        
        // We use word boundaries on the original strings to prevent substring bleeding (e.g. reacher in preacher)
        val patternA = Regex("\\b${Regex.escape(a)}\\b")
        val patternB = Regex("\\b${Regex.escape(b)}\\b")
        if (
            (patternA.containsMatchIn(b) && b.length > 3) ||
            (patternB.containsMatchIn(a) && a.length > 3)
        ) {
            return 0.90
        }
        
        // Allow exact substring containment IF spaces were just omitted (e.g. spider man vs spiderman)
        // But only if the length is significant to avoid single letter matches
        if (
            (bNoSpace.contains(aNoSpace) && aNoSpace.length > 4) ||
            (aNoSpace.contains(bNoSpace) && bNoSpace.length > 4)
        ) {
            // We use 0.85 so it passes a 0.80 threshold
            return 0.85
        }

        val distance = levenshtein(a, b)
        val maxLength = max(a.length, b.length)
        var sim = 1.0 - distance.toDouble() / maxLength.toDouble()
        
        // If it didn't pass the containment checks above, and the first letter is different,
        // it's almost certainly a completely different word (e.g., Reacher vs Preacher) 
        // rather than a typo, so we heavily penalize it.
        val aCore = a.replace(Regex("^(the|a|an)\\s+"), "")
        val bCore = b.replace(Regex("^(the|a|an)\\s+"), "")
        if (aCore.isNotEmpty() && bCore.isNotEmpty() && aCore.first() != bCore.first()) {
            sim *= 0.5
        }
        
        return sim
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