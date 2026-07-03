package com.streamflex.core.network.detector

enum class ContentType {

    // Web content
    HTML,
    JSON,
    XML,
    JAVASCRIPT,

    // Video
    VIDEO,
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