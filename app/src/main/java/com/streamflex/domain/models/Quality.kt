package com.streamflex.domain.models

/**
 * Standard video qualities used throughout StreamFlex.
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
    P2160("2160p", 2160);

    companion object {

        fun fromLabel(label: String?): Quality {

            if (label.isNullOrBlank()) return UNKNOWN

            val text = label.lowercase()

            return when {
                "2160" in text || "4k" in text -> P2160
                "1440" in text -> P1440
                "1080" in text -> P1080
                "720" in text -> P720
                "540" in text -> P540
                "480" in text -> P480
                "360" in text -> P360
                "240" in text -> P240
                "144" in text -> P144
                else -> UNKNOWN
            }
        }
    }
}