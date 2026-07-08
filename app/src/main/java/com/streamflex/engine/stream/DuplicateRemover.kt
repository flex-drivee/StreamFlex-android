package com.streamflex.engine.stream

import com.streamflex.domain.models.StreamLink

/**
 * Removes duplicate streams.
 *
 * Duplicate detection is intentionally conservative.
 * We primarily use the stream URL because two different
 * extraction paths often resolve to the exact same video.
 */
object DuplicateRemover {

    /**
     * Remove duplicate StreamLinks.
     */
    fun remove(
        streams: List<StreamLink>
    ): List<StreamLink> {

        return streams
            .filter { it.url.isNotBlank() }
            .distinctBy { normalizeUrl(it.url) }
    }

    /**
     * Normalize URLs before comparison.
     *
     * Removes fragments (#...)
     * while keeping query parameters intact because many
     * hosts require authentication tokens in the query.
     */
    private fun normalizeUrl(
        url: String
    ): String {

        return url
            .trim()
            .substringBefore("#")
    }
}