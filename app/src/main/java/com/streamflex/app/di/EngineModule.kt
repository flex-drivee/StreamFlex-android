package com.streamflex.app.di

import com.streamflex.domain.repositories.ContentRepository
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.engine.stream.StreamEngine

/**
 * Dependency module for the streaming engine.
 */
object EngineModule {

    val providerRepository: ProviderRepository
        get() = ProviderModule.repository

    val streamEngine: StreamEngine
        get() = StreamEngine

    val contentRepository: ContentRepository by lazy {

        ContentRepository(

            providerRepository = providerRepository,

            streamEngine = streamEngine

        )

    }
}