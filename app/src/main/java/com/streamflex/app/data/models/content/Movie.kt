package com.streamflex.app.data.models.content

data class Movie(
    val id: String,                 // TMDB/Cinemeta ID
    val title: String,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDate: String? = null,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val durationMinutes: Int? = null,
    val sources: List<ProviderSource> = emptyList()
)
