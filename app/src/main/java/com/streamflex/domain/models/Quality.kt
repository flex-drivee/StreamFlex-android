package com.streamflex.domain.models

/**
 * Standard video qualities used throughout StreamFlex.
 *
 * The numeric order is used for sorting and comparing streams.
 */
enum class Quality(
    val label: String,
    val order: Int
) {

    UNKNOWN("Unknown", 0),

    P144("144p", 144),
    P240("240p", 240),
    P360("360p", 360),
    P480("480p", 480),
    P540("540p", 540),
    P720("720p", 720),
    P1080("1080p", 1080),
    P1440("1440p", 1440),
    P2160("2160p", 2160),
    P4320("4320p", 4320);

    /**
     * Returns true if quality is HD (720p+).
     */
    fun isHd(): Boolean =
        order >= P720.order

    /**
     * Returns true if quality is Full HD (1080p+).
     */
    fun isFullHd(): Boolean =
        order >= P1080.order

    /**
     * Returns true if quality is Ultra HD (4K+).
     */
    fun isUltraHd(): Boolean =
        order >= P2160.order

    /**
     * Returns true if this quality is better than another.
     */
    fun betterThan(other: Quality): Boolean =
        order > other.order

    /**
     * Returns true if this quality is worse than another.
     */
    fun worseThan(other: Quality): Boolean =
        order < other.order

    /**
     * Display label.
     */
    override fun toString(): String = label

    companion object {

        /**
         * Detect quality from any text.
         *
         * Examples:
         * 1080p
         * WEB-DL 4K
         * UHD BluRay
         */
        fun fromLabel(label: String?): Quality {

            if (label.isNullOrBlank()) {
                return UNKNOWN
            }

            val text = label.lowercase()

            return when {

                text.contains("4320") ||
                        text.contains("8k") ->
                    P4320

                text.contains("2160") ||
                        text.contains("4k") ||
                        text.contains("uhd") ->
                    P2160

                text.contains("1440") ->
                    P1440

                text.contains("1080") ->
                    P1080

                text.contains("720") ->
                    P720

                text.contains("540") ->
                    P540

                text.contains("480") ->
                    P480

                text.contains("360") ->
                    P360

                text.contains("240") ->
                    P240

                text.contains("144") ->
                    P144

                else ->
                    UNKNOWN
            }
        }

        /**
         * Returns the better quality.
         */
        fun max(first: Quality, second: Quality): Quality =
            if (first.order >= second.order) first else second

        /**
         * Returns the lower quality.
         */
        fun min(first: Quality, second: Quality): Quality =
            if (first.order <= second.order) first else second
    }
}