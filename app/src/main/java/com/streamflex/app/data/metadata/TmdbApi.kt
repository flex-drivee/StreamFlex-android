package com.streamflex.app.data.metadata

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    // --- Search ---
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): TmdbSearchResponse

    // --- Popular ---
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String
    ): TmdbSearchResponse

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String
    ): TmdbSearchResponse

    // --- Details ---
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetails

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbShowDetails

    // --- Recommendations / Similar ---

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbSearchResponse

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbSearchResponse

    // --- Season Details ---

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String
    ): TmdbSeasonDetails
}