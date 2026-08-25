package com.streamflex.app

import android.app.Application
import com.streamflex.app.di.AppModule

/**
 * Application entry point for StreamFlex.
 *
 * Responsible for initializing global application
 * dependencies.
 */
class StreamFlexApplication : Application() {

    companion object {
        lateinit var instance: StreamFlexApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeModules()
    }

    /**
     * Initialize dependency graph.
     */
    private fun initializeModules() {

        AppModule.network
        AppModule.provider
        AppModule.extractor
        AppModule.engine
        AppModule.repository

        // Force lazy initialization
        AppModule.repository.contentRepository
        AppModule.repository.streamRepository
    }
}