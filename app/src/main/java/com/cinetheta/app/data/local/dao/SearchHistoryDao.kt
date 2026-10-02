package com.cinetheta.app.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.cinetheta.app.data.local.entities.SearchHistoryEntity

class SearchHistoryDao(private val db: SQLiteDatabase) {
    fun getSearchHistorySync(limit: Int): List<SearchHistoryEntity> {
        val list = mutableListOf<SearchHistoryEntity>()
        val cursor = db.query(
            "search_history_table", null, null, null, null, null, "timestamp DESC", limit.toString()
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(
                        SearchHistoryEntity(
                            query = it.getString(it.getColumnIndexOrThrow("query")),
                            timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun insertSearchQuery(item: SearchHistoryEntity) {
        val values = ContentValues().apply {
            put("query", item.query)
            put("timestamp", item.timestamp)
        }
        db.replace("search_history_table", null, values)
    }

    fun deleteSearchQuery(query: String) {
        db.delete("search_history_table", "query = ?", arrayOf(query))
    }

    fun clearHistory() {
        db.delete("search_history_table", null, null)
    }
}
