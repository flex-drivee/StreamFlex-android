package com.cinetheta.app.data.sports

data class LiveSportItem(
    val id: String,
    val title: String,
    val category: String, // "football", "cricket", "basketball", etc.
    val date: Long?,
    val isLive: Boolean,
    val isAlwaysLive: Boolean = false,
    val poster: String?,
    val league: String? = null,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val status: String? = null,
    val viewers: Int? = null
)
