package com.cinetheta.app.di

import com.cinetheta.engine.stream.StreamEngine

/**
 * Dependency module for the streaming engine.
 *
 * Owns the stream resolution engine only.
 */
object EngineModule {

    /**
     * Shared StreamEngine.
     */
    val streamEngine: StreamEngine
        get() = StreamEngine

    /**
     * Shared DownloadEngine.
     */
    val downloadEngine: com.cinetheta.engine.download.DownloadEngine by lazy {
        com.cinetheta.engine.download.DownloadEngine(
            storageManager = RepositoryModule.downloadStorageManager
        )
    }

    /**
     * Shared DownloadQueueManager.
     */
    val downloadQueueManager: com.cinetheta.engine.download.DownloadQueueManager by lazy {
        com.cinetheta.engine.download.DownloadQueueManager(
            context = com.cinetheta.app.CineThetaApplication.instance,
            repository = RepositoryModule.downloadRepository,
            downloadEngine = downloadEngine
        )
    }
}