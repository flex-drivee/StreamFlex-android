package com.cinetheta.app.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.cinetheta.app.data.local.entities.HistoryEntity

class HistoryDao(private val db: SQLiteDatabase) {
    fun getAllHistorySync(): List<HistoryEntity> {
        val list = mutableListOf<HistoryEntity>()
        val cursor = db.query(
            "history_table", null, null, null, null, null, "timestamp DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(
                        HistoryEntity(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            title = it.getString(it.getColumnIndexOrThrow("title")),
                            type = it.getString(it.getColumnIndexOrThrow("type")),
                            posterPath = it.getString(it.getColumnIndexOrThrow("posterPath")),
                            positionMs = it.getLong(it.getColumnIndexOrThrow("positionMs")),
                            durationMs = it.getLong(it.getColumnIndexOrThrow("durationMs")),
                            timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                            episodeId = it.getString(it.getColumnIndexOrThrow("episodeId"))
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getHistoryItem(id: String): HistoryEntity? {
        var item: HistoryEntity? = null
        val cursor = db.query(
            "history_table", null, "id = ?", arrayOf(id), null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                item = HistoryEntity(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    posterPath = it.getString(it.getColumnIndexOrThrow("posterPath")),
                    positionMs = it.getLong(it.getColumnIndexOrThrow("positionMs")),
                    durationMs = it.getLong(it.getColumnIndexOrThrow("durationMs")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                    episodeId = it.getString(it.getColumnIndexOrThrow("episodeId"))
                )
            }
        }
        return item
    }

    fun insertHistoryItem(item: HistoryEntity) {
        val values = ContentValues().apply {
            put("id", item.id)
            put("title", item.title)
            put("type", item.type)
            put("posterPath", item.posterPath)
            put("positionMs", item.positionMs)
            put("durationMs", item.durationMs)
            put("timestamp", item.timestamp)
            put("episodeId", item.episodeId)
        }
        db.replace("history_table", null, values)
    }

    fun deleteHistoryItem(id: String) {
        db.delete("history_table", "id = ?", arrayOf(id))
    }

    fun clearAll() {
        db.delete("history_table", null, null)
    }
}
