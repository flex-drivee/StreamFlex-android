package com.streamflex.app.di

import com.streamflex.domain.repositories.StreamRepository
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

    val streamRepository: StreamRepository by lazy {

        StreamRepository(

            providerRepository = providerRepository,

            streamEngine = streamEngine

        )

    }
}