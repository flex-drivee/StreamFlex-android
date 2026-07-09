package com.streamflex.app.di

import com.streamflex.domain.repositories.StreamRepository
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.engine.stream.StreamEngine
import com.streamflex.extractors.ExtractorManager

/**
 * Root dependency container for StreamFlex.
 *
 * This is the single entry point used by the application.
 *
 * Other layers should depend on AppModule instead of
 * constructing repositories or engines directly.
 */
object AppModule {

    /**
     * Shared network layer.
     */
    val networkModule: NetworkModule
        get() = NetworkModule

    /**
     * Registered providers.
     */
    val providerModule: ProviderModule
        get() = ProviderModule

    /**
     * Registered extractors.
     */
    val extractorModule: ExtractorModule
        get() = ExtractorModule

    /**
     * Streaming engine.
     */
    val engineModule: EngineModule
        get() = EngineModule

    /**
     * Shared ProviderRepository.
     */
    val providerRepository: ProviderRepository
        get() = providerModule.repository

    /**
     * Shared ExtractorManager.
     */
    val extractorManager: ExtractorManager
        get() = extractorModule.manager

    /**
     * Shared StreamEngine.
     */
    val streamEngine: StreamEngine
        get() = engineModule.streamEngine

    /**
     * Main repository exposed to the UI.
     */
    val streamRepository: StreamRepository
        get() = engineModule.streamRepository
}