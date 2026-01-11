package com.streamflex.app.data.models

enum class MediaType { Movie, TvSeries, Anime }

data class Media(
    val url: String,
    val title: String,
    val posterUrl: String,
    val type: MediaType
)

data class MediaDetails(
    val title: String,
    val description: String,
    val posterUrl: String,
    val bannerUrl: String? = null,
    val episodes: List<Episode> = emptyList(),
    val related: List<Media> = emptyList()
)

data class Episode(
    val name: String,
    val url: String,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null
)

data class StreamLink(
    val url: String,
    val name: String,
    val isM3u8: Boolean,
    val headers: Map<String, String> = emptyMap()
)