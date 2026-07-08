package com.streamflex.extractors.shared

import android.util.Base64
import java.util.regex.Pattern

/**
 * Pure utility functions shared by all extractors.
 *
 * This file intentionally contains no networking code.
 */
object ExtractorUtils {

    /**
     * Decode Base64 safely.
     */
    fun decodeBase64(text: String): String {

        return runCatching {
            String(
                Base64.decode(
                    text,
                    Base64.DEFAULT
                )
            )
        }.getOrDefault("")
    }

    /**
     * Encode Base64.
     */
    fun encodeBase64(text: String): String {

        return Base64.encodeToString(
            text.toByteArray(),
            Base64.NO_WRAP
        )
    }

    /**
     * Returns the first regex match.
     */
    fun firstMatch(
        pattern: String,
        text: String
    ): String? {

        return Regex(pattern)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
    }

    /**
     * Returns every regex match.
     */
    fun allMatches(
        pattern: String,
        text: String
    ): List<String> {

        return Regex(pattern)
            .findAll(text)
            .mapNotNull {

                it.groupValues.getOrNull(1)

            }
            .toList()
    }

    /**
     * Returns true if text contains one of the keywords.
     */
    fun containsAny(
        text: String,
        vararg keywords: String
    ): Boolean {

        val value = text.lowercase()

        return keywords.any {

            value.contains(it.lowercase())

        }
    }

    /**
     * Remove duplicate URLs while preserving order.
     */
    fun uniqueUrls(
        urls: List<String>
    ): List<String> {

        return LinkedHashSet(urls)
            .toList()
    }

    /**
     * Returns true if URL looks like a video.
     */
    fun isVideoUrl(url: String): Boolean {

        val lower = url.lowercase()

        return lower.endsWith(".mp4")
                || lower.endsWith(".mkv")
                || lower.endsWith(".webm")
                || lower.endsWith(".mov")
                || lower.endsWith(".avi")
                || lower.endsWith(".m3u8")
                || lower.endsWith(".mpd")
                || lower.contains("googlevideo")
                || lower.contains("videoplayback")
                || lower.contains("googleusercontent")
    }

    /**
     * Returns true if URL looks like an iframe.
     */
    fun isIframe(
        url: String
    ): Boolean {

        val lower = url.lowercase()

        return containsAny(
            lower,
            "embed",
            "iframe",
            "/e/",
            "/v/"
        )
    }

    /**
     * Remove blank strings.
     */
    fun clean(
        values: List<String>
    ): List<String> {

        return values
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}