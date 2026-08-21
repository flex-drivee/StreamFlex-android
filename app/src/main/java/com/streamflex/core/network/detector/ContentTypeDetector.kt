package com.streamflex.core.network.detector

/**
 * Detects the content type of a stream from its URL
 * or MIME type.
 */
object ContentTypeDetector {

    /**
     * Detect from URL.
     */
    fun detect(
        url: String?
    ): ContentType {

        if (url.isNullOrBlank()) {
            return ContentType.UNKNOWN
        }

        val value = url.substringBefore('?').lowercase()

        return when {

            value.endsWith(".m3u8") ->
                ContentType.HLS

            value.endsWith(".mpd") ->
                ContentType.DASH

            value.endsWith(".mp4") ->
                ContentType.VIDEO

            value.endsWith(".mkv") ->
                ContentType.VIDEO

            value.endsWith(".webm") ->
                ContentType.VIDEO

            value.endsWith(".avi") ->
                ContentType.VIDEO

            value.endsWith(".mov") ->
                ContentType.VIDEO

            value.endsWith(".flv") ->
                ContentType.VIDEO

            value.endsWith(".ts") ->
                ContentType.HLS

            value.contains("googlevideo") ->
                ContentType.VIDEO

            else ->
                ContentType.UNKNOWN
        }
    }

    /**
     * Detect from MIME type.
     */
    fun detectMime(
        mime: String?
    ): ContentType {

        if (mime.isNullOrBlank()) {
            return ContentType.UNKNOWN
        }

        val value = mime.lowercase()

        return when {

            value.contains("application/vnd.apple.mpegurl") ->
                ContentType.HLS

            value.contains("application/x-mpegurl") ->
                ContentType.HLS

            value.contains("application/dash+xml") ->
                ContentType.DASH

            value.contains("video/mp4") ->
                ContentType.VIDEO

            value.contains("video/webm") ->
                ContentType.VIDEO

            value.contains("video/x-matroska") ->
                ContentType.VIDEO

            else ->
                ContentType.UNKNOWN
        }
    }

    /**
     * Returns true if the stream is adaptive.
     */
    fun isAdaptive(
        type: ContentType
    ): Boolean {

        return when (type) {

            ContentType.HLS,
            ContentType.DASH -> true

            else -> false
        }
    }
}