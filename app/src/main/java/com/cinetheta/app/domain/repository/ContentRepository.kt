package com.cinetheta.app.domain.repository

import com.cinetheta.app.domain.models.Movie
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.models.Show

interface ContentRepository {
    suspend fun search(query: String): List<SearchResult>
    suspend fun getPopularMovies(): List<SearchResult>
    suspend fun getPopularShows(): List<SearchResult>
    suspend fun getMovieDetails(id: String): Movie
    suspend fun getShowDetails(id: String): Show
    suspend fun getSimilarContent(id: String, type: com.cinetheta.app.domain.models.ContentType): List<SearchResult>
    suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<com.cinetheta.app.domain.models.Episode>
    
    // Generic Category Fetch
    suspend fun getCategory(categoryId: String, page: Int = 1): List<SearchResult>
    fun getSupportedCategories(): List<Pair<String, String>>
}