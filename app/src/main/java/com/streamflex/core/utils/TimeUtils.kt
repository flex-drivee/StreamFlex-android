package com.streamflex.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Current timestamp in milliseconds.
     */
    fun now(): Long = System.currentTimeMillis()

    /**
     * Current timestamp in seconds.
     */
    fun nowSeconds(): Long = now() / 1000

    /**
     * Format timestamp.
     *
     * Example:
     * 2026-07-02 14:35:21
     */
    fun format(
        timestamp: Long,
        pattern: String = "yyyy-MM-dd HH:mm:ss"
    ): String {
        return SimpleDateFormat(
            pattern,
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    /**
     * Format duration.
     *
     * 65000 -> 01:05
     * 3665000 -> 01:01:05
     */
    fun formatDuration(milliseconds: Long): String {

        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

        return if (hours > 0) {
            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        } else {
            String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    /**
     * Returns elapsed milliseconds.
     */
    fun elapsed(startTime: Long): Long {
        return now() - startTime
    }

    /**
     * Returns true if duration has expired.
     */
    fun isExpired(
        timestamp: Long,
        durationMillis: Long
    ): Boolean {
        return now() - timestamp >= durationMillis
    }

    /**
     * Minutes to milliseconds.
     */
    fun minutes(value: Long): Long =
        TimeUnit.MINUTES.toMillis(value)

    /**
     * Hours to milliseconds.
     */
    fun hours(value: Long): Long =
        TimeUnit.HOURS.toMillis(value)

    /**
     * Days to milliseconds.
     */
    fun days(value: Long): Long =
        TimeUnit.DAYS.toMillis(value)

    /**
     * Sleep helper.
     * Used by retry manager.
     */
    fun sleep(milliseconds: Long) {
        Thread.sleep(milliseconds)
    }
}