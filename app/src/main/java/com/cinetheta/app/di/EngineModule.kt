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
}