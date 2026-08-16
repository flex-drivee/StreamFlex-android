package com.streamflex.player.resume

import android.content.Context
import android.content.SharedPreferences

class PlaybackProgressManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("streamflex_playback_progress", Context.MODE_PRIVATE)

    fun saveProgress(mediaId: String, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0 || mediaId.isBlank()) return
        
        val percentage = positionMs.toFloat() / durationMs.toFloat()
        
        // If watched more than 95%, mark as done and clear resume state
        if (percentage >= 0.95f) {
            prefs.edit().remove(mediaId).apply()
        } 
        // Only save if watched at least 10 seconds to prevent random junk records
        else if (positionMs > 10000L) {
            prefs.edit().putLong(mediaId, positionMs).apply()
        }
    }

    fun getProgress(mediaId: String): Long {
        if (mediaId.isBlank()) return 0L
        return prefs.getLong(mediaId, 0L)
    }
}
