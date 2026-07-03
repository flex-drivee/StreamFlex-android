package com.streamflex.domain.models

/**
 * Result returned by a provider after locating a movie or episode.
 * This does NOT contain playable streams yet.
 */
data class ProviderResult(

    val provider: String,

    val title: String,

    val pageUrl: String,

    val sources: List<ProviderSource>,

    val mediaType: MediaType,

    val year: Int? = null,

    val season: Int? = null,

    val episode: Int? = null,

    val poster: String? = null,

    val overview: String? = null,

    val success: Boolean = true,

    val error: String? = null
)