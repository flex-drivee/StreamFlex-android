package com.streamflex.app.domain.models

import android.R
import com.streamflex.app.data.providers.hdhub4u.Hdhub4uProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)

@Serializable
data class SearchResult(
    val id: String,
    val title: String,
    val poster: String? = null,
    val type: ContentType,
    val year: Int? = null,
    val provider: String
)
