package com.streamflex.domain.models

/**
 * Represents a single TV episode.
 */
data class Episode(

    /** Internal ID (TMDB, IMDb, etc.) */
    val id: String,

    /** Episode title */
    val title: String,

    /** Original title */
    val originalTitle: String? = null,

    /** Season number */
    val seasonNumber: Int,

    /** Episode number */
    val episodeNumber: Int,

    /** Episode description */
    val overview: String? = null,

    /** Episode thumbnail */
    val thumbnail: String? = null,

    /** Air date (yyyy-MM-dd) */
    val airDate: String? = null,

    /** Runtime in minutes */
    val duration: Int? = null,

    /** IMDb/TMDB rating */
    val rating: Double? = null,

    /** Whether the episode has aired */
    val aired: Boolean = true,

    /** IMDb ID (rarely available) */
    val imdbId: String? = null,

    /** TMDB ID */
    val tmdbId: Int? = null
)