package com.cinetheta.app.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.cinetheta.app.data.local.entities.BookmarkEntity

class BookmarkDao(private val db: SQLiteDatabase) {
    fun getAllBookmarksSync(): List<BookmarkEntity> {
        val list = mutableListOf<BookmarkEntity>()
        val cursor = db.query(
            "bookmarks_table", null, null, null, null, null, "timestamp DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(
                        BookmarkEntity(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            title = it.getString(it.getColumnIndexOrThrow("title")),
                            type = it.getString(it.getColumnIndexOrThrow("type")),
                            posterPath = it.getString(it.getColumnIndexOrThrow("posterPath")),
                            timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getBookmark(id: String): BookmarkEntity? {
        var item: BookmarkEntity? = null
        val cursor = db.query(
            "bookmarks_table", null, "id = ?", arrayOf(id), null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                item = BookmarkEntity(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    posterPath = it.getString(it.getColumnIndexOrThrow("posterPath")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                )
            }
        }
        return item
    }

    fun insertBookmark(item: BookmarkEntity) {
        val values = ContentValues().apply {
            put("id", item.id)
            put("title", item.title)
            put("type", item.type)
            put("posterPath", item.posterPath)
            put("timestamp", item.timestamp)
        }
        db.replace("bookmarks_table", null, values)
    }

    fun deleteBookmark(id: String) {
        db.delete("bookmarks_table", "id = ?", arrayOf(id))
    }
}
