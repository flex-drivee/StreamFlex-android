package com.streamflex.app.di

import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.core.network.HttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Central networking module.
 *
 * Exposes:
 * - HttpClient (provider scraping)
 * - Retrofit
 * - TMDB API
 *
 * Future:
 * - Authentication
 * - Logging
 * - Cache
 * - Multiple APIs
 */
object NetworkModule {

    /**
     * Shared HttpClient used by providers.
     */
    val httpClient: HttpClient
        get() = HttpClient

    /**
     * Shared Retrofit instance.
     */
    val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    }

    /**
     * Shared TMDB API.
     */
    val tmdbApi: TmdbApi by lazy {

        retrofit.create(
            TmdbApi::class.java
        )

    }
    

    
    val anilistApi: com.streamflex.app.data.metadata.AnilistApi by lazy {
        anilistRetrofit.create(com.streamflex.app.data.metadata.AnilistApi::class.java)
    }
}