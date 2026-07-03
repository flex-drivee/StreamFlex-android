package com.streamflex.domain.models

/**
 * Represents a movie in StreamFlex.
 */
data class Movie(

    /** Internal ID (TMDB, IMDb, etc.) */
    override val id: String,

    /** Movie title */
    override val title: String,

    /** Original title */
    override val originalTitle: String? = null,

    /** Release year */
    override val year: Int? = null,

    /** Short description */
    override val overview: String? = null,

    /** Poster image */
    override val poster: String? = null,

    /** Backdrop image */
    override val backdrop: String? = null,

    /** Movie duration (minutes) */
    val duration: Int? = null,

    /** IMDb rating */
    override val rating: Double? = null,

    /** Genres */
    val genres: List<String> = emptyList(),

    /** Language */
    override val language: String? = null,

    /** Country */
    val country: String? = null,

    /** Whether adult content */
    val adult: Boolean = false,

    /** Content type */
    override val mediaType: MediaType = MediaType.MOVIE,

    /** IMDb ID */
    override val imdbId: String? = null,

    /** TMDB ID */
    override val tmdbId: Int? = null
) : MediaItem