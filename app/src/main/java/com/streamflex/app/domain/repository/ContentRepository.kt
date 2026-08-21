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
    // NEW:
    suspend fun getSimilarContent(id: String, type: com.streamflex.app.domain.models.ContentType): List<SearchResult>
    suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<com.streamflex.app.domain.models.Episode>
    
    // Custom Categories
    suspend fun getKoreanDramas(): List<SearchResult>
    suspend fun getKoreanMovies(): List<SearchResult>
    suspend fun getBollywoodMovies(): List<SearchResult>
    suspend fun getIndianWebSeries(): List<SearchResult>
    suspend fun getNetflixOriginals(): List<SearchResult>
    suspend fun getPrimeOriginals(): List<SearchResult>
    suspend fun getAnimeMovies(): List<SearchResult>
    suspend fun getAnimeShows(): List<SearchResult>
    suspend fun getTopAnimeMovies(): List<SearchResult>
    suspend fun getTopAnimeShows(): List<SearchResult>
}