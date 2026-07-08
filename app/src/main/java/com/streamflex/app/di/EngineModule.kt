package com.streamflex.app.di

import com.streamflex.domain.repositories.ContentRepository
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.engine.stream.StreamCollector
import com.streamflex.engine.stream.StreamEngine

/**
 * Dependency module for the streaming engine.
 *
 * This module wires together:
 *
 * ProviderRepository
 *          ↓
 * ContentRepository
 *
 * StreamCollector
 *          ↓
 * StreamEngine
 *
 * UI layers should depend on this module instead of
 * constructing repositories or engines directly.
 */
object EngineModule {

    val providerRepository: ProviderRepository
        get() = ProviderModule.repository

    /**
     * Shared StreamCollector.
     */
    val streamCollector: StreamCollector by lazy {

        StreamCollector()

    }

    /**
     * Shared StreamEngine.
     *
     * Currently StreamEngine is implemented as a singleton object.
     * This property exists so the rest of the application depends
     * on the DI layer instead of directly referencing StreamEngine.
     */
    val streamEngine: StreamEngine
        get() = StreamEngine

    /**
     * Main repository exposed to the UI.
     */
    val contentRepository: ContentRepository by lazy {

        ContentRepository(

            providerRepository = providerRepository,

            streamEngine = streamEngine

        )

    }
}