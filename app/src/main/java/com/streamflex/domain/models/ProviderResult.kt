package com.streamflex.domain.models

/**
 * Result returned after a provider loads a piece of content.
 *
 * This class does NOT contain playable streams.
 *
 * Movies:
 *     ProviderResult
 *         └── sources
 *
 * TV Shows:
 *     ProviderResult
 *         └── seasons
 *                 └── episodes
 *                         └── sources
 */
data class ProviderResult(

    /**
     * Provider-specific content ID.
     */
    val id: String,

    /**
     * Stable provider identifier.
     *
     * Examples:
     * - hdhub4u
     * - moviebox
     * - ottmirror
     */
    val providerId: String,

    /**
     * Content title.
     */
    val title: String,

    /**
     * Provider detail page.
     */
    val detailUrl: String,

    /**
     * Movie / TV / Anime.
     */
    val mediaType: MediaType,

    /**
     * Movie sources.
     *
     * Empty for TV shows.
     */
    val sources: List<ProviderSource> = emptyList(),

    /**
     * TV seasons.
     *
     * Empty for movies.
     */
    val seasons: List<ProviderSeason> = emptyList(),

    /**
     * Release year.
     */
    val year: Int? = null,

    /**
     * Poster image.
     */
    val poster: String? = null,

    /**
     * Overview.
     */
    val overview: String? = null,

    /**
     * Provider-specific metadata.
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * Whether loading succeeded.
     */
    val success: Boolean = true,

    /**
     * Error message when success == false.
     */
    val error: String? = null

) {

    /**
     * Convenience property.
     */
    val isMovie: Boolean
        get() = mediaType == MediaType.MOVIE

    /**
     * Convenience property.
     */
    val isShow: Boolean
        get() = mediaType != MediaType.MOVIE

    /**
     * True if at least one playable source exists.
     */
    val hasSources: Boolean
        get() = sources.isNotEmpty()

    /**
     * True if provider returned seasons.
     */
    val hasSeasons: Boolean
        get() = seasons.isNotEmpty()
}