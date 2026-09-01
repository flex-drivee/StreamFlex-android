package com.streamflex.app.data.metadata

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AnilistApi {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("/")
    suspend fun query(
        @Body request: AnilistQueryRequest
    ): AnilistQueryResponse

}
