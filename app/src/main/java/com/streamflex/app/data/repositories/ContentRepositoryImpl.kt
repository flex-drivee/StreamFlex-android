package com.streamflex.app.data.repositories

import com.streamflex.app.BuildConfig
import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.app.data.metadata.TmdbMapper
import com.streamflex.app.domain.models.ContentType // <--- ADDED THIS
import com.streamflex.app.domain.models.Episode     // <--- ADDED THIS
import com.streamflex.app.domain.models.Movie
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.models.Show
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepositoryImpl(
    private val tmdbApi: TmdbApi
) : ContentRepository {

    // SECURE: Reading from the generated BuildConfig
    private val apiKey: String = BuildConfig.TMDB_API_KEY

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val movies = tmdbApi.searchMovies(apiKey, query).results.map { TmdbMapper.toDomain(it) }
        val shows = tmdbApi.searchTvShows(apiKey, query).results.map { TmdbMapper.toDomain(it) }
        return@withContext movies + shows
    }

    override suspend fun getPopularMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        // Debug log to check if key is working
        android.util.Log.d("DEBUG_KEY", "Using Key: '$apiKey'")
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

    // Fixed: Types are now recognized because imports are added
    override suspend fun getSimilarContent(id: String, type: ContentType): List<SearchResult> = withContext(Dispatchers.IO) {
        val response = if (type == ContentType.MOVIE) {
            tmdbApi.getSimilarMovies(id.toInt(), apiKey)
        } else {
            tmdbApi.getSimilarTvShows(id.toInt(), apiKey)
        }
        return@withContext response.results.map { TmdbMapper.toDomain(it) }
    }

    // Fixed: Episode type is now recognized
    override suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<Episode> = withContext(Dispatchers.IO) {
        val seasonDetails = tmdbApi.getSeasonDetails(showId.toInt(), seasonNumber, apiKey)

        // Map TMDB Episodes to Domain Episodes manually here
        return@withContext seasonDetails.episodes?.map { tmdbEp ->
            Episode(
                id = tmdbEp.id.toString(),
                title = tmdbEp.title ?: "Episode ${tmdbEp.episodeNumber}",
                episodeNumber = tmdbEp.episodeNumber,
                overview = tmdbEp.overview,
                airDate = tmdbEp.airDate,
                runtime = tmdbEp.runtime,
                stillPath = tmdbEp.stillPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            )
        } ?: emptyList()
    }

    override suspend fun getKoreanDramas(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, originCountry = "KR").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getKoreanMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverMovies(apiKey, originCountry = "KR", originalLanguage = "ko").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getBollywoodMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverMovies(apiKey, originCountry = "IN", originalLanguage = "hi").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getIndianWebSeries(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, originCountry = "IN", originalLanguage = "hi").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getNetflixOriginals(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, networks = "213").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getPrimeOriginals(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, networks = "1024").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getAnimeMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverMovies(apiKey, genres = "16", originalLanguage = "ja").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getAnimeShows(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, genres = "16", originalLanguage = "ja").results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getTopAnimeMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverMovies(apiKey, genres = "16", originalLanguage = "ja", sortBy = "vote_average.desc", voteCountGte = 200).results.map { TmdbMapper.toDomain(it) }
    }

    override suspend fun getTopAnimeShows(): List<SearchResult> = withContext(Dispatchers.IO) {
        tmdbApi.discoverTvShows(apiKey, genres = "16", originalLanguage = "ja", sortBy = "vote_average.desc", voteCountGte = 200).results.map { TmdbMapper.toDomain(it) }
    }
}