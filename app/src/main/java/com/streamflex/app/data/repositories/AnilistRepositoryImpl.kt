package com.streamflex.app.data.repositories

import com.streamflex.app.data.metadata.AnilistApi
import com.streamflex.app.data.metadata.AnilistQueryRequest
import com.streamflex.app.domain.models.ContentType
import com.streamflex.app.domain.models.Episode
import com.streamflex.app.domain.models.Movie
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.models.Season
import com.streamflex.app.domain.models.Show
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnilistRepositoryImpl(
    private val anilistApi: AnilistApi
) : ContentRepository {

    private val SEARCH_QUERY = """
        query (${'$'}search: String, ${'$'}type: MediaType, ${'$'}sort: [MediaSort]) {
          Page(page: 1, perPage: 20) {
            media(search: ${'$'}search, type: ${'$'}type, sort: ${'$'}sort) {
              id
              title { english romaji }
              coverImage { large }
              format
              startDate { year }
            }
          }
        }
    """.trimIndent()
    
    private val DETAILS_QUERY = """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id) {
            id
            title { english romaji }
            description
            coverImage { large }
            bannerImage
            startDate { year }
            averageScore
            genres
            episodes
            nextAiringEpisode { episode }
          }
        }
    """.trimIndent()

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val req = AnilistQueryRequest(SEARCH_QUERY, mapOf("search" to query, "type" to "ANIME"))
        val res = anilistApi.query(req)
        res.data?.page?.media?.map { 
            SearchResult(
                id = it.id.toString(),
                title = it.title?.display ?: "",
                poster = it.coverImage?.large,
                type = if (it.format == "MOVIE") ContentType.MOVIE else ContentType.SHOW,
                year = it.startDate?.year
            )
        } ?: emptyList()
    }

    override suspend fun getPopularMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        val req = AnilistQueryRequest(SEARCH_QUERY, mapOf("type" to "ANIME", "sort" to listOf("TRENDING_DESC", "POPULARITY_DESC")))
        val res = anilistApi.query(req)
        // AniList trending returns both TV and MOVIE. We just filter for MOVIE.
        res.data?.page?.media?.filter { it.format == "MOVIE" }?.map { 
            SearchResult(
                id = it.id.toString(),
                title = it.title?.display ?: "",
                poster = it.coverImage?.large,
                type = ContentType.MOVIE,
                year = it.startDate?.year
            )
        } ?: emptyList()
    }

    override suspend fun getPopularShows(): List<SearchResult> = withContext(Dispatchers.IO) {
        val req = AnilistQueryRequest(SEARCH_QUERY, mapOf("type" to "ANIME", "sort" to listOf("TRENDING_DESC", "POPULARITY_DESC")))
        val res = anilistApi.query(req)
        res.data?.page?.media?.filter { it.format != "MOVIE" }?.map { 
            SearchResult(
                id = it.id.toString(),
                title = it.title?.display ?: "",
                poster = it.coverImage?.large,
                type = ContentType.SHOW,
                year = it.startDate?.year
            )
        } ?: emptyList()
    }

    override suspend fun getMovieDetails(id: String): Movie = withContext(Dispatchers.IO) {
        val req = AnilistQueryRequest(DETAILS_QUERY, mapOf("id" to id.toInt()))
        val res = anilistApi.query(req)
        val media = res.data?.media ?: throw Exception("Anime not found")
        Movie(
            id = media.id.toString(),
            title = media.title?.display ?: "",
            overview = media.description?.replace(Regex("<.*?>"), ""),
            poster = media.coverImage?.large,
            backdrop = media.bannerImage,
            year = media.startDate?.year,
            rating = media.averageScore?.div(10.0),
            genres = media.genres ?: emptyList(),
            providerSources = emptyList(), // Filled by StreamRepository later
            runtime = null
        )
    }

    override suspend fun getShowDetails(id: String): Show = withContext(Dispatchers.IO) {
        val req = AnilistQueryRequest(DETAILS_QUERY, mapOf("id" to id.toInt()))
        val res = anilistApi.query(req)
        val media = res.data?.media ?: throw Exception("Anime not found")
        
        // Generate giant Season 1
        val totalEps = media.episodes ?: (media.nextAiringEpisode?.episode?.minus(1) ?: 24)
        val episodes = (1..totalEps).map { ep ->
            Episode(
                id = "${media.id}_ep$ep",
                title = "Episode $ep",
                episodeNumber = ep,
                overview = "Episode $ep",
                airDate = null,
                runtime = null,
                stillPath = null,
                providerSources = emptyList(),
                streams = emptyList()
            )
        }
        
        Show(
            id = media.id.toString(),
            title = media.title?.display ?: "",
            overview = media.description?.replace(Regex("<.*?>"), ""),
            poster = media.coverImage?.large,
            backdrop = media.bannerImage,
            year = media.startDate?.year,
            rating = media.averageScore?.div(10.0),
            genres = media.genres ?: emptyList(),
            seasons = listOf(Season(1, episodes))
        )
    }

    override suspend fun getSimilarContent(id: String, type: ContentType): List<SearchResult> {
        // Just return popular for now as a stub
        return if (type == ContentType.MOVIE) getPopularMovies() else getPopularShows()
    }

    override suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<Episode> {
        return emptyList()
    }

    override suspend fun getCategory(categoryId: String, page: Int): List<SearchResult> {
        return getPopularShows()
    }
}
