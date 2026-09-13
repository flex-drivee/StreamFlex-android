package com.cinetheta.core.utils

import android.util.Log

object StreamLogger {

    private const val TAG = "STREAM_PIPELINE"
    
    var enabled = com.cinetheta.app.BuildConfig.DEBUG

    fun info(stage: String, message: String) {
        if (!enabled) return
        runCatching {
            Log.i(TAG, "[$stage] $message")
        }.onFailure {
            println("INFO [$TAG] [$stage] $message")
        }
    }

    fun debug(stage: String, message: String) {
        if (!enabled) return
        runCatching {
            Log.d(TAG, "[$stage] $message")
        }.onFailure {
            println("DEBUG [$TAG] [$stage] $message")
        }
    }

    fun warn(stage: String, message: String) {
        if (!enabled) return
        runCatching {
            Log.w(TAG, "[$stage] $message")
        }.onFailure {
            println("WARN [$TAG] [$stage] $message")
        }
    }

    fun error(
        stage: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (!enabled) return
        runCatching {
            Log.e(TAG, "[$stage] $message", throwable)
        }.onFailure {
            println("ERROR [$TAG] [$stage] $message (${throwable?.message})")
        }
    }
}