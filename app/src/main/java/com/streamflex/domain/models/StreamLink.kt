package com.streamflex.domain.models

/**
 * Represents a single playable stream.
 * Produced by extractors and consumed by the StreamEngine.
 */
data class StreamLink(

    /** Display name shown to the user */
    val name: String,

    /** Direct playable URL */
    val url: String,

    /** Stream quality */
    val quality: Quality = Quality.UNKNOWN,

    /** Stream host */
    val host: HostType = HostType.UNKNOWN,

    /** Network content type */
    val contentType: com.streamflex.core.network.detector.ContentType =
        com.streamflex.core.network.detector.ContentType.UNKNOWN,

    /** Optional HTTP headers required for playback */
    val headers: Map<String, String> = emptyMap(),

    /** Optional cookies required for playback */
    val cookies: Map<String, String> = emptyMap(),

    /** Optional subtitles */
    val subtitles: List<Subtitle> = emptyList(),

    /** Optional audio tracks */
    val audioTracks: List<AudioTrack> = emptyList(),

    /** Estimated file size (bytes) */
    val fileSize: Long? = null,

    /** Whether this stream is adaptive (HLS/DASH) */
    val adaptive: Boolean = false,

    /** Whether login/account is required */
    val requiresAuth: Boolean = false,

    /** Optional referer */
    val referer: String? = null
)