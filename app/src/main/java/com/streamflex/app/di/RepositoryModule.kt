package com.streamflex.app.di

import com.streamflex.app.data.repositories.ContentRepositoryImpl
import com.streamflex.domain.repositories.StreamRepository

/**
 * Central repository dependency module.
 *
 * Exposes the repositories used by the application.
 *
 * Metadata:
 *  - TMDB
 *  - Movies
 *  - TV Shows
 *  - Seasons
 *
 * Streams:
 *  - Provider search
 *  - Matching
 *  - Extraction
 *  - Final streams
 */
object RepositoryModule {

    /**
     * Metadata repository.
     */
    val contentRepository: ContentRepositoryImpl by lazy {

        ContentRepositoryImpl(
            tmdbApi = NetworkModule.tmdbApi
        )

    }

    /**
     * Streaming repository.
     */
    val streamRepository: StreamRepository by lazy {

        StreamRepository(

            providerRepository = ProviderModule.repository,

            streamEngine = EngineModule.streamEngine

        )

    }
}