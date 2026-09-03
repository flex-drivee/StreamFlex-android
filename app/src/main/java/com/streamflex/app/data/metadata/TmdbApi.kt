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
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos"
    ): TmdbMovieDetails

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos"
    ): TmdbShowDetails

    // --- Recommendations / Similar ---

    @GET("movie/{movie_id}/recommendations")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbSearchResponse

    @GET("tv/{tv_id}/recommendations")
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

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    // --- Discover ---
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_origin_country") originCountry: String? = null,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("with_networks") networks: String? = null,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("api_key") apiKey: String,
        @Query("with_origin_country") originCountry: String? = null,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("with_networks") networks: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = null,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse
}