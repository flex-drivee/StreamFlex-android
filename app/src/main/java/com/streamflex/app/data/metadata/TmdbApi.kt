package com.streamflex.app.data.metadata

import com.streamflex.app.data.network.ApiRoutes
import com.streamflex.app.data.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class TmdbApi(
    private val apiKey: String
) {

    /**
     * Search both movies & TV using separate endpoints
     */
    suspend fun searchMovies(query: String): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_MOVIE_SEARCH}" +
                "?api_key=$apiKey&query=${query.trim()}"
        HttpClient.get(url)
    }

    suspend fun searchTv(query: String): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_TV_SEARCH}" +
                "?api_key=$apiKey&query=${query.trim()}"
        HttpClient.get(url)
    }

    /**
     * Details fetchers
     */
    suspend fun getMovieDetails(movieId: Int): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_MOVIE_DETAILS}$movieId" +
                "?api_key=$apiKey&append_to_response=videos,images"
        HttpClient.get(url)
    }

    suspend fun getTvDetails(tvId: Int): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_TV_DETAILS}$tvId" +
                "?api_key=$apiKey&append_to_response=videos,images"
        HttpClient.get(url)
    }

    /**
     * Episode-level metadata (optional but useful)
     */
    suspend fun getSeason(tvId: Int, season: Int): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_TV_DETAILS}$tvId/season/$season" +
                "?api_key=$apiKey"
        HttpClient.get(url)
    }

    suspend fun getEpisode(tvId: Int, season: Int, episode: Int): JSONObject? = withContext(Dispatchers.IO) {
        val url = "${ApiRoutes.TMDB_BASE_URL}${ApiRoutes.TMDB_TV_DETAILS}$tvId/season/$season/episode/$episode" +
                "?api_key=$apiKey"
        HttpClient.get(url)
    }

    /**
     * Helper for constructing image URL
     */
    fun buildImageUrl(path: String?, backdrop: Boolean = false): String? {
        if (path.isNullOrEmpty()) return null
        val size = if (backdrop) ApiRoutes.BACKDROP_SIZE else ApiRoutes.POSTER_SIZE
        return "${ApiRoutes.TMDB_IMAGE_BASE}$size$path"
    }
}
