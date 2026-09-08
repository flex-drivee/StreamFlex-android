package com.cinetheta.app.di

import com.cinetheta.app.data.metadata.TmdbApi
import com.cinetheta.core.network.HttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    val httpClient: HttpClient
        get() = HttpClient

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val tmdbApi: TmdbApi by lazy {
        retrofit.create(TmdbApi::class.java)
    }
}
