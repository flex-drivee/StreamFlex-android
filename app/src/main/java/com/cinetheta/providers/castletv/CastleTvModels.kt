package com.cinetheta.providers.castletv

import kotlinx.serialization.Serializable

@Serializable
data class CastleTvBaseResponse(
    val code: Int,
    val msg: String? = null,
    val data: String? = null
)

@Serializable
data class CastleTvSearchResponse(
    val page: Int,
    val total: Int,
    val rows: List<CastleTvSearchItem>? = null
)

@Serializable
data class CastleTvSearchItem(
    val redirectId: Long,
    val title: String,
    val coverImage: String? = null,
    val publishTime: Long? = null,
    val movieType: Int? = null, // 1 for TV?, 2 for Movie?
    val languages: List<String>? = null
)
