package com.streamflex.domain.models

/**
 * Represents a source returned by a provider.
 * This is NOT a playable stream.
 * It is passed to an extractor to resolve into StreamLinks.
 */
data class ProviderSource(

    /** Provider name (HDHub4u, MovieBox, etc.) */
    val provider: String,

    /** Host name (HubCloud, PixelDrain, VidStack, etc.) */
    val host: String,

    /** Host type */
    val hostType: HostType,

    /** URL to resolve */
    val url: String,

    /** Display quality if known */
    val quality: Quality = Quality.UNKNOWN,

    /** Required request headers */
    val headers: Map<String, String> = emptyMap(),

    /** Required cookies */
    val cookies: Map<String, String> = emptyMap(),

    /** Referer if required */
    val referer: String? = null,

    /** Extra metadata for providers/extractors */
    val metadata: Map<String, String> = emptyMap()
)