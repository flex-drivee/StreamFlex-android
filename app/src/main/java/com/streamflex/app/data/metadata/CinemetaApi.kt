package com.streamflex.app.data.metadata

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface CinemetaApi {

    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String, // "movie" or "series"
        @Path("id") id: String      // e.g., "tt1234567"
    ): CinemetaResponse
}

data class CinemetaResponse(
    @SerializedName("meta") val meta: CinemetaMeta?
)

data class CinemetaMeta(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("poster") val poster: String?,
    @SerializedName("background") val background: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("year") val year: String?, // Cinemeta often sends year as string e.g. "2023-2024"
    @SerializedName("imdbRating") val imdbRating: String?
)