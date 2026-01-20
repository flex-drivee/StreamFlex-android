package com.streamflex.app.data.repositories

import com.streamflex.app.BuildConfig // Import this
import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.app.data.metadata.TmdbMapper
import com.streamflex.app.domain.models.Movie
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.models.Show
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepositoryImpl(
    private val tmdbApi: TmdbApi
) : ContentRepository {

    // SECURE: Now reading from the generated BuildConfig
  //  private val apiKey: String = BuildConfig.TMDB_API_KEY
    private val apiKey: String = "placeholder"

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val movies = tmdbApi.searchMovies(apiKey, query).results.map { TmdbMapper.toDomain(it) }
        val shows = tmdbApi.searchTvShows(apiKey, query).results.map { TmdbMapper.toDomain(it) }
        return@withContext movies + shows
    }

    override suspend fun getPopularMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        return@withContext tmdbApi.getPopularMovies(apiKey).results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getPopularShows(): List<SearchResult> = withContext(Dispatchers.IO) {
        return@withContext tmdbApi.getPopularTvShows(apiKey).results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getMovieDetails(id: String): Movie = withContext(Dispatchers.IO) {
        val tmdbMovie = tmdbApi.getMovieDetails(id.toInt(), apiKey)
        return@withContext TmdbMapper.toDomain(tmdbMovie)
    }

    override suspend fun getShowDetails(id: String): Show = withContext(Dispatchers.IO) {
        val tmdbShow = tmdbApi.getTvShowDetails(id.toInt(), apiKey)
        return@withContext TmdbMapper.toDomain(tmdbShow)
    }
}