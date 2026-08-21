package com.streamflex.player.resume

import android.content.Context
import android.content.SharedPreferences
import com.streamflex.app.domain.models.SearchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val prefs: SharedPreferences = context.getSharedPreferences("streamflex_playback_progress", Context.MODE_PRIVATE)

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
        
        // Raw progress for quick lookup (keyed by progressKey)
        if (percentage >= 0.95f) {
            prefs.edit().remove(progressKey).apply()
        } else if (positionMs > 10000L) {
            prefs.edit().putLong(progressKey, positionMs).apply()
        }

        // Keep History for Continue Watching if watched > 10s and not completely finished
        if (positionMs > 10000L && percentage < 0.95f) {
            val historyItem = HistoryItem(
                id = mediaId,
                title = title,
                type = type,
                posterPath = posterPath,
                positionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                episodeId = episodeId
            )
            saveToHistoryList(historyItem)
        } else if (percentage >= 0.95f) {
            removeFromHistory(mediaId)
        }
    }
    
    private fun saveToHistoryList(item: HistoryItem) {
        val currentList = getHistory().toMutableList()
        currentList.removeAll { it.id == item.id }
        currentList.add(0, item) // Add to top
        val limitedList = currentList.take(20) // Keep 20 recent items
        prefs.edit().putString("history_list", Json.encodeToString(limitedList)).apply()
    }

    private fun removeFromHistory(mediaId: String) {
        val currentList = getHistory().toMutableList()
        currentList.removeAll { it.id == mediaId }
        prefs.edit().putString("history_list", Json.encodeToString(currentList)).apply()
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history_list", "[]") ?: "[]"
        return try {
            Json.decodeFromString<List<HistoryItem>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getProgress(mediaId: String): Long {
        if (mediaId.isBlank()) return 0L
        return prefs.getLong(mediaId, 0L)
    }
}
