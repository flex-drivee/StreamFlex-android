package com.streamflex.core.utils

import java.text.Normalizer
import java.util.Locale

object StringUtils {

    /**
     * Null-safe trim.
     */
    fun trim(text: String?): String {
        return text?.trim().orEmpty()
    }

    /**
     * Remove duplicate spaces.
     */
    fun normalizeSpaces(text: String): String {
        return text.replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * Remove accents.
     *
     * Café -> Cafe
     */
    fun removeAccents(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }

    /**
     * Lowercase using ROOT locale.
     */
    fun lowercase(text: String): String {
        return text.lowercase(Locale.ROOT)
    }

    /**
     * Case-insensitive contains.
     */
    fun contains(
        text: String,
        query: String
    ): Boolean {
        return lowercase(text).contains(lowercase(query))
    }

    /**
     * Remove every non-alphanumeric character.
     */
    fun alphanumeric(text: String): String {
        return text.replace("[^A-Za-z0-9]".toRegex(), "")
    }

    /**
     * Normalize a title for matching.
     *
     * Spider-Man: No Way Home
     * ->
     * spidermannowayhome
     */
    fun normalizeTitle(title: String): String {

        return lowercase(
            alphanumeric(
                removeAccents(title)
            )
        )
    }

    /**
     * Returns true if strings match after normalization.
     */
    fun equalsNormalized(
        first: String,
        second: String
    ): Boolean {

        return normalizeTitle(first) ==
                normalizeTitle(second)
    }

    /**
     * Returns true if string is numeric.
     */
    fun isNumeric(text: String): Boolean {
        return text.matches("\\d+".toRegex())
    }

    /**
     * Safe integer conversion.
     */
    fun toInt(text: String?): Int? {
        return text?.toIntOrNull()
    }

    /**
     * Safe long conversion.
     */
    fun toLong(text: String?): Long? {
        return text?.toLongOrNull()
    }

    /**
     * Safe double conversion.
     */
    fun toDouble(text: String?): Double? {
        return text?.toDoubleOrNull()
    }

    /**
     * Returns empty string if null.
     */
    fun emptyIfNull(text: String?): String {
        return text ?: ""
    }

    /**
     * Capitalize first letter.
     */
    fun capitalize(text: String): String {

        if (text.isBlank()) return text

        return text.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.ROOT)
            else
                it.toString()
        }
    }

    /**
     * Safe substring.
     */
    fun substring(
        text: String,
        start: Int,
        end: Int
    ): String {

        if (start >= text.length) return ""

        return text.substring(
            start,
            minOf(end, text.length)
        )
    }
}