package com.streamflex.domain.models

/**
 * Represents a source returned by a provider.
 * This is NOT a playable stream.
 * It is passed to an extractor to resolve into StreamLinks.
 */
data class ProviderSource(

    /** Provider name (HDHub4u, MovieBox...) */
    val provider: String,

    /** Host display name (HubCloud, PixelDrain...) */
    val host: String,

    /** Host type */
    val hostType: HostType,

    /** URL to resolve */
    val url: String,

    /** Quality reported by the provider */
    val quality: Quality = Quality.UNKNOWN,

    /** Direct stream (no extractor required) */
    val isDirect: Boolean = false,

    /** Priority assigned by provider */
    val priority: Int = 0,

    /** Required request headers */
    val headers: Map<String, String> = emptyMap(),

    /** Required cookies */
    val cookies: Map<String, String> = emptyMap(),

    /** Referer if required */
    val referer: String? = null,

    /** Extra provider-specific metadata */
    val metadata: Map<String, String> = emptyMap()
)