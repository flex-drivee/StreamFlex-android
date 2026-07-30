package com.streamflex.core.network.interceptor

import com.streamflex.core.logger.Logger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * LoggingInterceptor
 *
 * Logs all outgoing requests and incoming responses in debug builds.
 * Production builds should set isEnabled = false or use the no-op version.
 *
 * Logs:
 * - → [METHOD] URL (on request)
 * - ← [STATUS] URL (durationMs) (on response)
 * - ← [ERROR] URL message (on failure)
 *
 * Aligned with CloudStream's lightweight logging approach —
 * no body logging (avoids leaking HTML/JSON content to logcat in production).
 */
class LoggingInterceptor(
    private val isEnabled: Boolean = true
) : Interceptor {

    companion object {
        private const val TAG = "HTTP"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!isEnabled) return chain.proceed(request)

        val startMs = System.currentTimeMillis()

        Logger.d(
            message = "→ [${request.method}] ${request.url}",
            tag = TAG
        )

        return try {
            val response = chain.proceed(request)
            val durationMs = System.currentTimeMillis() - startMs

            val level = if (response.isSuccessful) Logger.Level.DEBUG else Logger.Level.WARN
            Logger.log(
                level = level,
                message = "← [${response.code}] ${request.url} (${durationMs}ms)",
                tag = TAG
            )

            response
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startMs
            Logger.e(
                message = "← [ERROR] ${request.url} (${durationMs}ms) — ${e.message}",
                tag = TAG
            )
            throw e
        }
    }
}