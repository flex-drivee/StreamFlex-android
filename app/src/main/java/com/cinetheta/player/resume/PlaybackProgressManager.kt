package com.cinetheta.player.resume

import android.content.Context
import com.cinetheta.app.data.local.AppDatabase
import com.cinetheta.app.data.local.entities.HistoryEntity
import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val id: String,
    val title: String,
    val type: String,
    val posterPath: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val timestamp: Long = 0,
    val episodeId: String? = null
)

class PlaybackProgressManager(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val historyDao = db.historyDao()
    // Keeping SharedPreferences just for simple raw progress lookup optimization if needed,
    // or we can use DB. We will migrate to DB for simplicity and scale.
    private val prefs = context.getSharedPreferences("cinetheta_playback_progress", Context.MODE_PRIVATE)

    fun saveProgress(
        mediaId: String,
        progressKey: String,
        title: String, 
        type: String, 
        posterPath: String?, 
        positionMs: Long, 
        durationMs: Long,
        episodeId: String? = null
    ) {
        if (durationMs <= 0 || mediaId.isBlank()) return
        
        val percentage = positionMs.toFloat() / durationMs.toFloat()
        
        // Fast access progress for exact millisecond seeking
        if (percentage >= 0.95f) {
            prefs.edit().remove(progressKey).apply()
        } else if (positionMs > 10000L) {
            prefs.edit().putLong(progressKey, positionMs).apply()
        }

        // Save to Room Database for 'Continue Watching' Home Screen
        if (positionMs > 10000L && percentage < 0.95f) {
            val entity = HistoryEntity(
                id = mediaId,
                title = title,
                type = type,
                posterPath = posterPath,
                positionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                episodeId = episodeId
            )
            // Using runBlocking logic internally via allowMainThreadQueries for sync compatibility
            historyDao.insertHistoryItem(entity)
        } else if (percentage >= 0.95f) {
            removeFromHistory(mediaId)
        }
    }

        fun saveLastStream(progressKey: String, streamName: String) {
        prefs.edit().putString("stream_$progressKey", streamName).apply()
    }
    
    fun getLastStream(progressKey: String): String? {
        return prefs.getString("stream_$progressKey", null)
    }

    fun saveLastAudioTrack(progressKey: String, audioLangOrLabel: String) {
        prefs.edit().putString("audio_$progressKey", audioLangOrLabel).apply()
    }
    
    fun getLastAudioTrack(progressKey: String): String? {
        return prefs.getString("audio_$progressKey", null)
    }

    fun removeFromHistory(mediaId: String) {
        historyDao.deleteHistoryItem(mediaId)
    }

    fun getHistory(): List<HistoryItem> {
        val entities = historyDao.getAllHistorySync()
        return entities.map { 
            HistoryItem(
                id = it.id,
                title = it.title,
                type = it.type,
                posterPath = it.posterPath,
                positionMs = it.positionMs,
                durationMs = it.durationMs,
                timestamp = it.timestamp,
                episodeId = it.episodeId
            )
        }
    }

    fun getProgress(mediaId: String): Long {
        if (mediaId.isBlank()) return 0L
        return prefs.getLong(mediaId, 0L)
    }
}
