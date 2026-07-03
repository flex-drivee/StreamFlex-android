package com.streamflex.domain.models

/**
 * Represents a search result displayed in the UI.
 * Can represent either a movie or a TV show.
 */
data class SearchResult(

    /** Internal ID */
    val id: String,

    /** Title displayed to the user */
    val title: String,

    /** Original title */
    val originalTitle: String? = null,

    /** Movie / TV / Anime */
    val mediaType: MediaType,

    /** Release year */
    val year: Int? = null,

    /** Poster image */
    val poster: String? = null,

    /** Backdrop image */
    val backdrop: String? = null,

    /** Short description */
    val overview: String? = null,

    /** Average rating */
    val rating: Double? = null,

    /** Genres */
    val genres: List<String> = emptyList(),

    /** Original language */
    val language: String? = null,

    /** IMDb ID */
    val imdbId: String? = null,

    /** TMDB ID */
    val tmdbId: Int? = null
)