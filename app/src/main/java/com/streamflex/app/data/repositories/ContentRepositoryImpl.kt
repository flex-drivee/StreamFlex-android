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
        try {
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
                    stillPath = tmdbEp.stillPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    rating = tmdbEp.voteAverage
                )
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("ContentRepository", "Failed to fetch season $seasonNumber for show $showId: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCategory(categoryId: String, page: Int): List<SearchResult> = withContext(Dispatchers.IO) {
        val response = when (categoryId) {
            "trending_cinema" -> tmdbApi.getNowPlayingMovies(apiKey, page)
            "south_indian" -> tmdbApi.discoverMovies(apiKey, originalLanguage = "ta|te|ml|kn", page = page)
            "top_series" -> tmdbApi.getTrendingTv(apiKey, page)
            "reality_tv" -> tmdbApi.discoverTvShows(apiKey, genres = "10764", page = page)
            "western_tv" -> tmdbApi.discoverTvShows(apiKey, genres = "10768", page = page)
            "turkish_drama" -> tmdbApi.discoverTvShows(apiKey, originalLanguage = "tr", page = page)
            "turkish_movies" -> tmdbApi.discoverMovies(apiKey, originalLanguage = "tr", page = page)
            "chinese_movies" -> tmdbApi.discoverMovies(apiKey, originalLanguage = "zh", page = page)
            "chinese_series" -> tmdbApi.discoverTvShows(apiKey, originalLanguage = "zh", page = page)
            "japan_movies" -> tmdbApi.discoverMovies(apiKey, originalLanguage = "ja", page = page)
            "japan_series" -> tmdbApi.discoverTvShows(apiKey, originalLanguage = "ja", page = page)
            "usa_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "US", page = page)
            "usa_series" -> tmdbApi.discoverTvShows(apiKey, originCountry = "US", page = page)
            "indian_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "IN", page = page)
            "indian_series" -> tmdbApi.discoverTvShows(apiKey, originCountry = "IN", page = page)
            "ph_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "PH", originalLanguage = "tl", page = page)
            "ph_series" -> tmdbApi.discoverTvShows(apiKey, originCountry = "PH", originalLanguage = "tl", page = page)
            "thai_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "TH", page = page)
            "thai_series" -> tmdbApi.discoverTvShows(apiKey, originCountry = "TH", page = page)
            "korean_dramas" -> tmdbApi.discoverTvShows(apiKey, originCountry = "KR", page = page)
            "korean_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "KR", originalLanguage = "ko", page = page)
            "action_movies" -> tmdbApi.discoverMovies(apiKey, genres = "28", page = page)
            "crime_movies" -> tmdbApi.discoverMovies(apiKey, genres = "80", page = page)
            "comedy_movies" -> tmdbApi.discoverMovies(apiKey, genres = "35", page = page)
            "comedy_series" -> tmdbApi.discoverTvShows(apiKey, genres = "35,10725", page = page)
            "romance_movies" -> tmdbApi.discoverMovies(apiKey, genres = "10749", page = page)
            "romance_series" -> tmdbApi.discoverTvShows(apiKey, genres = "10766", page = page)
            
            // Legacy/Existing ones for HomeViewModel
            "bollywood_movies" -> tmdbApi.discoverMovies(apiKey, originCountry = "IN", originalLanguage = "hi", page = page)
            "indian_web_series" -> tmdbApi.discoverTvShows(apiKey, originCountry = "IN", originalLanguage = "hi", page = page)
            "netflix_originals" -> tmdbApi.discoverTvShows(apiKey, networks = "213", page = page)
            "prime_originals" -> tmdbApi.discoverTvShows(apiKey, networks = "1024", page = page)
            "anime_movies" -> tmdbApi.discoverMovies(apiKey, genres = "16", originalLanguage = "ja", page = page)
            "anime_shows" -> tmdbApi.discoverTvShows(apiKey, genres = "16", originalLanguage = "ja", page = page)
            "top_anime_movies" -> tmdbApi.discoverMovies(apiKey, genres = "16", originalLanguage = "ja", sortBy = "vote_average.desc", voteCountGte = 200, page = page)
            "top_anime_shows" -> tmdbApi.discoverTvShows(apiKey, genres = "16", originalLanguage = "ja", sortBy = "vote_average.desc", voteCountGte = 200, page = page)
            
            else -> {
                if (categoryId.startsWith("genre_")) {
                    val genreId = categoryId.removePrefix("genre_")
                    tmdbApi.discoverMovies(apiKey, genres = genreId, page = page)
                } else {
                    tmdbApi.getPopularMovies(apiKey)
                }
            }
        }
        return@withContext response.results.map { TmdbMapper.toDomain(it) }
    }

    override fun getSupportedCategories(): List<Pair<String, String>> {
        return listOf(
            "trending_cinema" to "Trending in Cinema",
            "top_series" to "Top Series This Week",
            "netflix_originals" to "Netflix Originals",
            "prime_originals" to "Prime Originals",
            "korean_dramas" to "Top K-Dramas",
            "bollywood_movies" to "Bollywood Blockbusters",
            "indian_web_series" to "Indian Web Series",
            "south_indian" to "South Indian Hits",
            "anime_shows" to "Trending Anime Series",
            "anime_movies" to "Trending Anime Movies",
            "top_anime_shows" to "Top Rated Anime Series",
            "top_anime_movies" to "Top Rated Anime Movies",
            "action_movies" to "Action & Adventure",
            "comedy_movies" to "Comedy Movies",
            "romance_movies" to "Romance Movies",
            "crime_movies" to "Crime Movies",
            "turkish_drama" to "Turkish Dramas",
            "chinese_series" to "Chinese Series",
            "japan_series" to "Japanese Series",
            "thai_series" to "Thai Series",
            "ph_movies" to "Philippines Movies",
            "usa_movies" to "USA Movies"
        )
    }
}