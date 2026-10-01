package com.cinetheta.app.data.bookmarks

import com.cinetheta.app.CineThetaApplication
import com.cinetheta.app.data.local.AppDatabase
import com.cinetheta.app.data.local.entities.BookmarkEntity

data class BookmarkItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val isShow: Boolean
)

object BookmarkManager {
    private val db by lazy {
        AppDatabase.getDatabase(CineThetaApplication.instance)
    }
    private val bookmarkDao by lazy {
        db.bookmarkDao()
    }

    fun addBookmark(item: BookmarkItem) {
        val entity = BookmarkEntity(
            id = item.id,
            title = item.title,
            type = if (item.isShow) "tv" else "movie",
            posterPath = item.posterUrl,
            timestamp = System.currentTimeMillis()
        )
        bookmarkDao.insertBookmark(entity)
    }

    fun removeBookmark(id: String) {
        bookmarkDao.deleteBookmark(id)
    }

    fun isBookmarked(id: String): Boolean {
        return bookmarkDao.getBookmark(id) != null
    }

    fun getBookmarks(): List<BookmarkItem> {
        val entities = bookmarkDao.getAllBookmarksSync()
        return entities.map {
            BookmarkItem(
                id = it.id,
                title = it.title,
                posterUrl = it.posterPath,
                isShow = it.type == "tv"
            )
        }
    }
}
