package com.streamflex.app.data.models.content

data class Season(
    val seasonNumber: Int,
    val title: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val episodeCount: Int = 0,
    val episodes: List<Episode> = emptyList()
)
