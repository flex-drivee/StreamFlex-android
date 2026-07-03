package com.streamflex.domain.models

/**
 * Represents a TV show or series.
 */
data class Show(

    /** Internal ID (TMDB, IMDb, etc.) */
    override val id: String,

    /** Show title */
    override val title: String,

    /** Original title */
    override val originalTitle: String? = null,

    /** First air year */
    override val year: Int? = null,

    /** Show description */
    override val overview: String? = null,

    /** Poster image */
    override val poster: String? = null,

    /** Backdrop image */
    override val backdrop: String? = null,

    /** IMDb rating */
    override val rating: Double? = null,

    /** Genres */
    val genres: List<String> = emptyList(),

    /** Original language */
    override val language: String? = null,

    /** Country of origin */
    val country: String? = null,

    /** Total seasons (if known) */
    val totalSeasons: Int? = null,

    /** Total episodes (if known) */
    val totalEpisodes: Int? = null,

    /** Whether currently airing */
    val isOngoing: Boolean = false,

    /** Adult content */
    val adult: Boolean = false,

    /** Media type */
    override val mediaType: MediaType = MediaType.TV,

    /** IMDb ID */
    override val imdbId: String? = null,

    /** TMDB ID */
    override val tmdbId: Int? = null
) : MediaItem