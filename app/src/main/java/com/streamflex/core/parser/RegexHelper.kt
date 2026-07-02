package com.streamflex.core.parser

/**
 * Utility methods for regular expression operations.
 */
object RegexHelper {

    /**
     * Returns the first matched group.
     */
    fun find(
        pattern: String,
        input: String,
        group: Int = 1,
        ignoreCase: Boolean = true
    ): String? {

        return Regex(
            pattern,
            if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        )
            .find(input)
            ?.groups
            ?.get(group)
            ?.value
    }

    /**
     * Returns every match.
     */
    fun findAll(
        pattern: String,
        input: String,
        group: Int = 1,
        ignoreCase: Boolean = true
    ): List<String> {

        return Regex(
            pattern,
            if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        )
            .findAll(input)
            .mapNotNull { it.groups[group]?.value }
            .toList()
    }

    /**
     * Returns true if the regex matches.
     */
    fun matches(
        pattern: String,
        input: String,
        ignoreCase: Boolean = true
    ): Boolean {

        return Regex(
            pattern,
            if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        ).containsMatchIn(input)
    }

    /**
     * Replace regex matches.
     */
    fun replace(
        pattern: String,
        input: String,
        replacement: String,
        ignoreCase: Boolean = true
    ): String {

        return Regex(
            pattern,
            if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        ).replace(input, replacement)
    }

    /**
     * Removes all HTML tags.
     */
    fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
}