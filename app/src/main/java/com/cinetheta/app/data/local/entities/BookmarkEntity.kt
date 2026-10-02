package com.cinetheta.app.data.local.entities

data class BookmarkEntity(
    val id: String,
    val title: String,
    val type: String,
    val posterPath: String?,
    val timestamp: Long
)
