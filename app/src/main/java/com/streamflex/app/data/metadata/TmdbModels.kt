package com.streamflex.app.data.metadata

import com.google.gson.annotations.SerializedName

// -----------------------------
// Search / Discover / Trending
// -----------------------------
data class TmdbSearchResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbSearchResult>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbSearchResult(
    @SerializedName("id") val id: Int,

    // Type differentiator: "movie" or "tv"
    @SerializedName("media_type") val mediaType: String?,

    // Movie fields
    @SerializedName("title") val movieTitle: String?,
    @SerializedName("release_date") val releaseDate: String?,

    // TV fields
    @SerializedName("name") val tvName: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,

    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

// -----------------------------
// Movie Details
// -----------------------------
data class TmdbMovieDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
)

// -----------------------------
// TV Show Details
// -----------------------------
data class TmdbShowDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("seasons") val seasons: List<TmdbSeason>?
)

// -----------------------------
// Seasons & Episodes
// -----------------------------
data class TmdbSeason(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("poster_path") val posterPath: String?
)

data class TmdbEpisodeDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val title: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("still_path") val stillPath: String?
)

// -----------------------------
// Genre
// -----------------------------
data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbSeasonDetails(
    @SerializedName("_id") val _id: String,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("id") val id: Int,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episodes") val episodes: List<TmdbEpisodeDetails>?
)