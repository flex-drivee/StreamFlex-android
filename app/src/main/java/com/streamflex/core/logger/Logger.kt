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
        if (enabled) runCatching { Log.v(tag, message) }.onFailure { println("VERBOSE [$tag] $message") }
    }

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) runCatching { Log.d(tag, message) }.onFailure { println("DEBUG [$tag] $message") }
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) runCatching { Log.i(tag, message) }.onFailure { println("INFO [$tag] $message") }
    }

    fun w(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) runCatching { Log.w(tag, message) }.onFailure { println("WARN [$tag] $message") }
    }

    fun e(message: String, tag: String = DEFAULT_TAG) {
        if (enabled) runCatching { Log.e(tag, message) }.onFailure { println("ERROR [$tag] $message") }
    }

    fun e(message: String, throwable: Throwable, tag: String = DEFAULT_TAG) {
        if (enabled) runCatching { Log.e(tag, message, throwable) }.onFailure { println("ERROR [$tag] $message (${throwable.message})") }
    }

    // ─── Programmatic level dispatch ─────────────────────────────────────────

    fun log(level: Level, message: String, tag: String = DEFAULT_TAG) {
        if (!enabled) return
        when (level) {
            Level.VERBOSE -> v(message, tag)
            Level.DEBUG   -> d(message, tag)
            Level.INFO    -> i(message, tag)
            Level.WARN    -> w(message, tag)
            Level.ERROR   -> e(message, tag)
        }
    }
}