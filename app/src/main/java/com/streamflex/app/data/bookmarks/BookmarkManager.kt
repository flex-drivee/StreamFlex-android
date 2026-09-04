package com.streamflex.app.data.bookmarks

import android.content.Context
import com.streamflex.app.StreamFlexApplication
import org.json.JSONArray
import org.json.JSONObject

data class BookmarkItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val isShow: Boolean
)

object BookmarkManager {
    private const val PREFS_NAME = "bookmarks_prefs"
    private const val KEY_BOOKMARKS = "bookmarks_list"

    private val prefs by lazy {
        StreamFlexApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addBookmark(item: BookmarkItem) {
        val list = getBookmarks().toMutableList()
        if (list.none { it.id == item.id }) {
            list.add(0, item) // Add to top
            saveBookmarks(list)
        }
    }

    fun removeBookmark(id: String) {
        val list = getBookmarks().toMutableList()
        list.removeAll { it.id == id }
        saveBookmarks(list)
    }

    fun isBookmarked(id: String): Boolean {
        return getBookmarks().any { it.id == id }
    }

    fun getBookmarks(): List<BookmarkItem> {
        val jsonString = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val list = mutableListOf<BookmarkItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BookmarkItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        posterUrl = if (obj.has("posterUrl")) obj.getString("posterUrl") else null,
                        isShow = obj.getBoolean("isShow")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveBookmarks(list: List<BookmarkItem>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            if (it.posterUrl != null) obj.put("posterUrl", it.posterUrl)
            obj.put("isShow", it.isShow)
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }
}
