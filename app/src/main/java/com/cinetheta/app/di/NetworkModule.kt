package com.cinetheta.app.di

import com.cinetheta.app.data.metadata.TmdbApi
import com.cinetheta.core.network.DohProviders
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.interceptor.RetryInterceptor
import com.cinetheta.core.network.interceptor.UserAgentInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    val httpClient: HttpClient
        get() = HttpClient

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(UserAgentInterceptor())

        DohProviders.applyDoh(builder).build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val tmdbApi: TmdbApi by lazy {
        retrofit.create(TmdbApi::class.java)
    }
}
