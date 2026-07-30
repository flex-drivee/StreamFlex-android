package com.streamflex.core.logger

import android.util.Log

/**
 * StreamFlex Logger
 *
 * Thin wrapper around Android's Log.
 * Set [enabled] = false in release builds to silence all output.
 */
object Logger {

    private const val DEFAULT_TAG = "StreamFlex"

    var enabled = true

    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    // ─── Convenience methods ─────────────────────────────────────────────────

    fun v(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) Log.v(tag, message)
    }

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

    fun e(message: String, throwable: Throwable, tag: String = DEFAULT_TAG) {
        if (enabled) Log.e(tag, message, throwable)
    }

    // ─── Programmatic level dispatch ─────────────────────────────────────────

    fun log(level: Level, message: String, tag: String = DEFAULT_TAG) {
        if (!enabled) return
        when (level) {
            Level.VERBOSE -> Log.v(tag, message)
            Level.DEBUG   -> Log.d(tag, message)
            Level.INFO    -> Log.i(tag, message)
            Level.WARN    -> Log.w(tag, message)
            Level.ERROR   -> Log.e(tag, message)
        }
    }
}