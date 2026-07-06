package com.streamflex.core.network.detector

enum class ContentType {

    // Web content
    HTML,
    JSON,
    XML,
    JAVASCRIPT,

    // Video
    VIDEO,
    HLS,
    M3U8,
    DASH,

    // Media
    IMAGE,
    SUBTITLE,
    AUDIO,

    // Files
    PDF,
    ZIP,
    APK,

    // Unknown
    UNKNOWN
}