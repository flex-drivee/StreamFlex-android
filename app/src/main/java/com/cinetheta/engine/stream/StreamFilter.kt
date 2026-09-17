package com.cinetheta.engine.stream

import com.cinetheta.core.network.detector.ContentDetector
import com.cinetheta.domain.models.StreamLink

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

        // Allow data: URIs (e.g. base64-encoded inline m3u8 playlists we generate ourselves)
        val isDataUri = url.startsWith("data:")
        if (!isDataUri && !url.startsWith("http"))
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

        if (stream.host == com.cinetheta.domain.models.HostType.REDIRECT) {
            return false
        }

        if (
            stream.contentType == com.cinetheta.core.network.detector.ContentType.HTML ||
            stream.contentType == com.cinetheta.core.network.detector.ContentType.IMAGE ||
            stream.contentType == com.cinetheta.core.network.detector.ContentType.JAVASCRIPT ||
            stream.contentType == com.cinetheta.core.network.detector.ContentType.JSON
        ) {
            return false
        }

        if (stream.host != com.cinetheta.domain.models.HostType.UNKNOWN) {
            return true
        }

        return ContentDetector.isVideo(stream.contentType) ||
                url.contains(".m3u8", ignoreCase = true) ||
                url.contains(".mp4", ignoreCase = true) ||
                url.contains(".mpd", ignoreCase = true) ||
                url.contains(".mkv", ignoreCase = true)
    }
}