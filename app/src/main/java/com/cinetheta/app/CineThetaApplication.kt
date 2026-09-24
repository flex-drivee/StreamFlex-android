package com.cinetheta.app

import android.app.Application

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cinetheta.extractors.netmirror.NetMirrorBypassManager

import coil.ImageLoader
import coil.ImageLoaderFactory
import com.cinetheta.app.di.AppModule
import com.cinetheta.app.di.NetworkModule

/**
 * Application entry point for CineTheta.
 *
 * Responsible for initializing global application
 * dependencies.
 */
class CineThetaApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: CineThetaApplication
            private set
            
        var topActivity: android.app.Activity? = null
            private set
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                NetworkModule.okHttpClient
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {
                topActivity = activity
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                if (topActivity == activity) topActivity = null
            }
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

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
        // Force lazy initialization
        AppModule.repository.contentRepository
        AppModule.repository.streamRepository

        // Background NetMirror Warm-up
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!NetMirrorBypassManager.hasValidToken()) {
                    android.util.Log.d("CineThetaApplication", "NetMirror token missing or expired. Pre-warming...")
                    // Will take ~37 seconds in the background
                    NetMirrorBypassManager.getToken("https://net52.cc")
                } else {
                    android.util.Log.d("CineThetaApplication", "NetMirror token already valid. Skipping warm-up.")
                }
            } catch (e: Exception) {
                android.util.Log.e("CineThetaApplication", "Error warming up NetMirror", e)
            }
        }
    }
}