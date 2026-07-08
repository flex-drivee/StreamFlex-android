package com.streamflex.engine.stream

import com.streamflex.core.network.detector.ContentDetector
import com.streamflex.domain.models.StreamLink

/**
 * Removes invalid or unsupported streams.
 *
 * This class intentionally performs only validation.
 */
object StreamFilter {

    /**
     * Filter unusable streams.
     */
    fun filter(
        streams: List<StreamLink>
    ): List<StreamLink> {

        return streams.filter(::isValid)
    }

    /**
     * Determines whether a stream should be kept.
     */
    private fun isValid(
        stream: StreamLink
    ): Boolean {

        val url = stream.url.trim()

        if (url.isBlank())
            return false

        if (!url.startsWith("http"))
            return false

        if (
            url.startsWith("javascript:", true) ||
            url.startsWith("mailto:", true) ||
            url.startsWith("about:", true)
        ) {
            return false
        }

        if (stream.requiresAuth) {
            return false
        }

        return ContentDetector.isVideo(
            stream.contentType
        )
    }
}