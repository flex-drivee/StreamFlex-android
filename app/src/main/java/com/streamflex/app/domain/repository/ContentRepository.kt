package com.streamflex.app.domain.repository

import com.streamflex.app.domain.models.Movie
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.models.Show

interface ContentRepository {
    suspend fun search(query: String): List<SearchResult>
    suspend fun getPopularMovies(): List<SearchResult>
    suspend fun getPopularShows(): List<SearchResult>
    suspend fun getMovieDetails(id: String): Movie
    suspend fun getShowDetails(id: String): Show
}