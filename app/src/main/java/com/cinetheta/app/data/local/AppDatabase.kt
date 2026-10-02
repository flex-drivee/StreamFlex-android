package com.cinetheta.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cinetheta.app.data.local.dao.BookmarkDao
import com.cinetheta.app.data.local.dao.HistoryDao
import com.cinetheta.app.data.local.dao.SearchHistoryDao

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(context, "cinetheta_database.db", null, 1) {

    private val db by lazy { writableDatabase }

    fun historyDao() = HistoryDao(db)
    fun bookmarkDao() = BookmarkDao(db)
    fun searchHistoryDao() = SearchHistoryDao(db)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS history_table (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                posterPath TEXT,
                positionMs INTEGER NOT NULL,
                durationMs INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                episodeId TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS bookmarks_table (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                posterPath TEXT,
                timestamp INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS search_history_table (
                query TEXT PRIMARY KEY NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history_table")
        db.execSQL("DROP TABLE IF EXISTS bookmarks_table")
        db.execSQL("DROP TABLE IF EXISTS search_history_table")
        onCreate(db)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
