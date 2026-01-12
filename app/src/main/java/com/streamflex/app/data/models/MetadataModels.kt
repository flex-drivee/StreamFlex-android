package com.streamflex.app.data.models

// Root response for movie or series
data class CinemetaResponse(
    val meta: Meta? = null,
    val streams: List<StreamItem>? = null
)

// ---- META SECTION ----
data class Meta(
    val id: String? = null,
    val type: String? = null,
    val name: String? = null,
    val imdbRating: Double? = null,
    val poster: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val year: Int? = null,
    val runtime: String? = null,
    val genres: List<String>? = null,
    val background: String? = null,
    val logo: String? = null,
    val trailerStreams: List<Trailer>? = null,
    val videos: List<Video>? = null,
    val episodes: List<MetaEpisode>? = null,   // ✔ Updated
    val season: Int? = null,
    val behaviorHints: BehaviorHints? = null,
)

// ---- STREAMS ----
data class StreamItem(
    val title: String? = null,
    val url: String? = null,
    val ytId: String? = null,
    val externalUrl: String? = null,
    val quality: String? = null,
    val file: String? = null
)

// ---- TRAILERS ----
data class Trailer(
    val id: String? = null,
    val source: String? = null
)

// ---- VIDEOS ----
data class Video(
    val id: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val streams: List<StreamItem>? = null
)

// ---- EPISODES ----
data class MetaEpisode(
    val id: String? = null,
    val title: String? = null,
    val overview: String? = null,
    val episode: Int? = null,
    val season: Int? = null,
    val image: String? = null,
    val releaseDate: String? = null
)

// ---- BEHAVIOR HINTS ----
data class BehaviorHints(
    val bingeGroup: String? = null,
    val countryAware: Boolean? = null
)
