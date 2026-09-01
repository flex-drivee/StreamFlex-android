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
    private val tmdbRepository: ContentRepositoryImpl by lazy {
        ContentRepositoryImpl(
            tmdbApi = NetworkModule.tmdbApi
        )
    }
    


    /**
     * Metadata repository. Switches dynamically to AniList if an Anime provider is selected.
     */
    val contentRepository: com.streamflex.app.domain.repository.ContentRepository
        get() {
            // Reverting back to TMDB exclusively as per user request to restore episode metadata
            // and avoid AnimeDekho season splitting mismatches.
            return tmdbRepository
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
    val downloadStorageManager: com.streamflex.data.local.download.DownloadStorageManager by lazy {
        com.streamflex.data.local.download.DownloadStorageManager(
            com.streamflex.app.StreamFlexApplication.instance
        )
    }

    /**
     * Download repository.
     */
    val downloadRepository: com.streamflex.domain.repositories.DownloadRepository by lazy {
        com.streamflex.data.local.download.JsonDownloadRepository(
            com.streamflex.app.StreamFlexApplication.instance
        )
    }
}