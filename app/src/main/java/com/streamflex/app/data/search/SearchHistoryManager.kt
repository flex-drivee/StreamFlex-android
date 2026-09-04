package com.streamflex.app.data.search

import android.content.Context
import com.streamflex.app.StreamFlexApplication
import org.json.JSONArray

object SearchHistoryManager {
    private const val PREFS_NAME = "search_history_prefs"
    private const val KEY_HISTORY = "search_history_list"
    private const val MAX_HISTORY = 10

    private val prefs by lazy {
        StreamFlexApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addSearchQuery(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val list = getHistory().toMutableList()
        // Remove it if it already exists so we can bump it to the top
        list.remove(q)
        list.add(0, q)
        
        if (list.size > MAX_HISTORY) {
            list.removeAt(list.size - 1)
        }
        saveHistory(list)
    }

    fun removeSearchQuery(query: String) {
        val list = getHistory().toMutableList()
        list.remove(query)
        saveHistory(list)
    }
    
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun getHistory(): List<String> {
        val jsonString = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveHistory(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }
}
