package com.streamflex.core.utils

import android.util.Log

object StreamLogger {

    private const val TAG = "STREAM_PIPELINE"

    fun info(stage: String, message: String) {
        runCatching {
            Log.i(TAG, "[$stage] $message")
        }.onFailure {
            println("INFO [$TAG] [$stage] $message")
        }
    }

    fun debug(stage: String, message: String) {
        runCatching {
            Log.d(TAG, "[$stage] $message")
        }.onFailure {
            println("DEBUG [$TAG] [$stage] $message")
        }
    }

    fun warn(stage: String, message: String) {
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
        runCatching {
            Log.e(TAG, "[$stage] $message", throwable)
        }.onFailure {
            println("ERROR [$TAG] [$stage] $message (${throwable?.message})")
        }
    }
}