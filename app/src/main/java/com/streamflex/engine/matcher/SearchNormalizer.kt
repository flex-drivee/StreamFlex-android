package com.streamflex.engine.matcher

/**
 * Normalizes provider titles before matching.
 *
 * This improves search accuracy across providers that
 * use different separators and release formats.
 */
object SearchNormalizer {

    /**
     * Normalize a title for comparison.
     */
    fun normalize(
        text: String
    ): String {

        return text
            .lowercase()

            // Common separators
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')

            // Remove brackets
            .replace(Regex("[()\\[\\]{}]"), " ")

            // Collapse whitespace
            .replace(Regex("\\s+"), " ")

            .trim()
    }
}