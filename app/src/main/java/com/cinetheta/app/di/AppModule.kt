package com.cinetheta.app.di

/**
 * Root dependency container.
 *
 * Exposes every dependency module used by CineTheta.
 *
 * The modules themselves own their objects.
 */
object AppModule {

    val network
        get() = NetworkModule

    val provider
        get() = ProviderModule

    val extractor
        get() = ExtractorModule

    val engine
        get() = EngineModule

    val repository
        get() = RepositoryModule
}