package com.streamflex.app.data.metadata

import com.google.gson.annotations.SerializedName

data class AnilistQueryRequest(
    val query: String,
    val variables: Map<String, Any> = emptyMap()
)

data class AnilistQueryResponse(
    val data: AnilistData?
)

data class AnilistData(
    @SerializedName("Page") val page: AnilistPage?,
    @SerializedName("Media") val media: AnilistMedia?
)

data class AnilistPage(
    val media: List<AnilistMedia>?
)

data class AnilistMedia(
    val id: Int,
    val title: AnilistTitle?,
    val description: String?,
    val coverImage: AnilistCoverImage?,
    val bannerImage: String?,
    val format: String?,
    val startDate: AnilistDate?,
    val averageScore: Int?,
    val genres: List<String>?,
    val episodes: Int?,
    val nextAiringEpisode: AnilistAiringEpisode?
)

data class AnilistTitle(
    val english: String?,
    val romaji: String?
) {
    val display: String get() = english ?: romaji ?: "Unknown Anime"
}

data class AnilistCoverImage(
    val large: String?
)

data class AnilistDate(
    val year: Int?
)

data class AnilistAiringEpisode(
    val episode: Int?
)
