package com.streamflex.app.data.network

object ApiRoutes {

    // Base URLs
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val CINEMETA_BASE_URL = "https://v3-cinemeta.strem.io/meta/"

    // Common Paths (TMDB)
    const val TMDB_MOVIE_DETAILS = "movie/"
    const val TMDB_TV_DETAILS = "tv/"
    const val TMDB_MOVIE_SEARCH = "search/movie"
    const val TMDB_TV_SEARCH = "search/tv"
    const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"

    // Image Sizes (can adjust later)
    const val POSTER_SIZE = "w500"
    const val BACKDROP_SIZE = "w780"

    // Cinemeta (TV metadata mostly)
    const val CINEMETA_MOVIE = "movie/"
    const val CINEMETA_SERIES = "series/"
}
