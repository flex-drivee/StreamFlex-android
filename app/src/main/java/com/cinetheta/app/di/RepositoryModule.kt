package com.cinetheta.app.di

import com.cinetheta.app.data.repositories.ContentRepositoryImpl
import com.cinetheta.domain.repositories.StreamRepository

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
    private val tmdbRepository: ContentRepositoryImpl by lazy {
        ContentRepositoryImpl(
            tmdbApi = NetworkModule.tmdbApi
        )
    }
    


    private val youtubeRepository: com.cinetheta.app.data.repositories.YouTubeRepositoryImpl by lazy {
        com.cinetheta.app.data.repositories.YouTubeRepositoryImpl()
    }

    /**
     * Dynamic Metadata repository. Switches automatically based on active provider!
     */
    val contentRepository: com.cinetheta.app.domain.repository.ContentRepository by lazy {
        com.cinetheta.app.data.repositories.DelegatingContentRepository(
            tmdbRepository = tmdbRepository,
            youtubeRepository = youtubeRepository
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

    /**
     * Download storage manager.
     */
    val downloadStorageManager: com.cinetheta.data.local.download.DownloadStorageManager by lazy {
        com.cinetheta.data.local.download.DownloadStorageManager(
            com.cinetheta.app.CineThetaApplication.instance
        )
    }

    /**
     * Download repository.
     */
    val downloadRepository: com.cinetheta.domain.repositories.DownloadRepository by lazy {
        com.cinetheta.data.local.download.JsonDownloadRepository(
            com.cinetheta.app.CineThetaApplication.instance
        )
    }
}