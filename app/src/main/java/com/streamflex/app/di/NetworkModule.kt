package com.streamflex.app.di

import com.streamflex.core.network.HttpClient

/**
 * Provides networking components.
 *
 * Since HttpClient is currently implemented as a singleton object,
 * this module simply exposes it.
 *
 * If we migrate to Hilt/Koin later, this module can provide
 * OkHttpClient, interceptors, cookies, cache, etc.
 */
object NetworkModule {

    val httpClient: HttpClient
        get() = HttpClient

}