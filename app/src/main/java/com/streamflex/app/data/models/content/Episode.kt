package com.streamflex.app.data.models.content

data class Episode(
    val id: String,                   // TMDB Episode ID (or provider-specific)
    val episodeNumber: Int,
    val seasonNumber: Int,
    val title: String,
    val overview: String? = null,
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
    val stillUrl: String? = null,
    val sources: List<ProviderSource> = emptyList()
)
