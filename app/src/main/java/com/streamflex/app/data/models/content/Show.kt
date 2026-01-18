package com.streamflex.app.data.models.content

data class Show(
    val id: String,
    val title: String,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val totalSeasons: Int = 0,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val seasons: List<Season> = emptyList()
)
