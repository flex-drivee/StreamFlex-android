package com.cinetheta.app.data.search

import com.cinetheta.app.CineThetaApplication
import com.cinetheta.app.data.local.AppDatabase
import com.cinetheta.app.data.local.entities.SearchHistoryEntity

object SearchHistoryManager {
    private const val MAX_HISTORY = 10
    private val db by lazy {
        AppDatabase.getDatabase(CineThetaApplication.instance)
    }
    private val searchDao by lazy {
        db.searchHistoryDao()
    }

    fun addSearchQuery(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        
        val entity = SearchHistoryEntity(query = q, timestamp = System.currentTimeMillis())
        searchDao.insertSearchQuery(entity)
    }

    fun removeSearchQuery(query: String) {
        searchDao.deleteSearchQuery(query)
    }
    
    fun clearHistory() {
        searchDao.clearHistory()
    }

    fun getHistory(): List<String> {
        return searchDao.getSearchHistorySync(MAX_HISTORY).map { it.query }
    }
}
