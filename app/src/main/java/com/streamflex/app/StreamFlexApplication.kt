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

    override fun onCreate() {
        super.onCreate()

        initializeModules()
    }

    /**
     * Initialize dependency graph.
     */
    private fun initializeModules() {

        // Force creation of lazy modules.
        AppModule.streamRepository
    }
}