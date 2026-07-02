package com.streamflex.core.logger

import android.util.Log

object Logger {

    private const val DEFAULT_TAG = "StreamFlex"

    var enabled = true

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) Log.d(tag, message)
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) Log.i(tag, message)
    }

    fun w(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) Log.w(tag, message)
    }

    fun e(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) Log.e(tag, message)
    }

    fun e(
        message: String,
        throwable: Throwable,
        tag: String = DEFAULT_TAG
    ) {
        if (enabled) Log.e(tag, message, throwable)
    }
}