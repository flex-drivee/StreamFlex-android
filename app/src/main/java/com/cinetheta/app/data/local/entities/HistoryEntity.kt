package com.cinetheta.app.data.local.entities

data class HistoryEntity(
    val id: String,
    val title: String,
    val type: String,
    val posterPath: String?,
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long,
    val episodeId: String?
)
