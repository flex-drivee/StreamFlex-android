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
    
    private val anilistRepository: com.streamflex.app.data.repositories.AnilistRepositoryImpl by lazy {
        com.streamflex.app.data.repositories.AnilistRepositoryImpl(
            anilistApi = NetworkModule.anilistApi
        )
    }

    /**
     * Metadata repository. Switches dynamically to AniList if an Anime provider is selected.
     */
    val contentRepository: com.streamflex.app.domain.repository.ContentRepository
        get() {
            val selected = ProviderModule.repository.selectedProviderId
            val isAnime = selected != null && (selected.contains("anime", ignoreCase = true) || selected == "gogoanime")
            
            return if (isAnime) {
                anilistRepository
            } else {
                tmdbRepository
            }
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