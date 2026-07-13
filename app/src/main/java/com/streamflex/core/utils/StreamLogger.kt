package com.streamflex.core.utils

import android.util.Log

object StreamLogger {

    private const val TAG = "STREAM_PIPELINE"

    fun info(stage: String, message: String) {
        Log.i(TAG, "[$stage] $message")
    }

    fun debug(stage: String, message: String) {
        Log.d(TAG, "[$stage] $message")
    }

    fun warn(stage: String, message: String) {
        Log.w(TAG, "[$stage] $message")
    }

    fun error(
        stage: String,
        message: String,
        throwable: Throwable? = null
    ) {
        Log.e(TAG, "[$stage] $message", throwable)
    }
}