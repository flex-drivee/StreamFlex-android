package com.streamflex.app.di

import com.streamflex.engine.stream.StreamEngine

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
    val downloadEngine: com.streamflex.engine.download.DownloadEngine by lazy {
        com.streamflex.engine.download.DownloadEngine(
            storageManager = RepositoryModule.downloadStorageManager
        )
    }
}