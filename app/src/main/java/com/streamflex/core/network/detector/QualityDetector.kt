package com.streamflex.core.network.detector

import com.streamflex.domain.models.Quality

/**
 * Detects video quality from text or URLs.
 */
object QualityDetector {

    /**
     * Detect quality from any text.
     *
     * Examples:
     * "Movie 1080p WEB-DL"
     * "4K UHD"
     * "720p x264"
     */
    fun detect(text: String?): Quality {

        if (text.isNullOrBlank()) {
            return Quality.UNKNOWN
        }

        val value = text.lowercase()

        return when {

            value.contains("4320") ||
                    value.contains("8k") ->
                Quality.P4320

            value.contains("2160") ||
                    value.contains("4k") ||
                    value.contains("uhd") ->
                Quality.P2160

            value.contains("1440") ->
                Quality.P1440

            value.contains("1080") ->
                Quality.P1080

            value.contains("720") ->
                Quality.P720

            value.contains("540") ->
                Quality.P540

            value.contains("480") ->
                Quality.P480

            value.contains("360") ->
                Quality.P360

            value.contains("240") ->
                Quality.P240

            else ->
                Quality.UNKNOWN
        }
    }

    /**
     * Returns true if the quality is HD or higher.
     */
    fun isHd(quality: Quality): Boolean {

        return quality >= Quality.P720
    }

    /**
     * Returns true if quality is Full HD or better.
     */
    fun isFullHd(quality: Quality): Boolean {

        return quality >= Quality.P1080
    }

    /**
     * Returns the better of two qualities.
     */
    fun max(
        first: Quality,
        second: Quality
    ): Quality {

        return if (first >= second) first else second
    }
}