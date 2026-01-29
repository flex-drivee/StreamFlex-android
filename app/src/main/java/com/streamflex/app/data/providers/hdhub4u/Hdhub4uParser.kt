package com.streamflex.app.data.providers.hdhub4u

import com.streamflex.app.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Hdhub4uParser {

    /**
     * TEMP SEARCH
     * This is only to verify UI + pipeline.
     * We will replace with real scraping later.
     */
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        listOf(
            SearchResult(
                id = "https://hdhub4u.fake/movie/1",
                title = "HDHub4u Test Movie",
                poster = null,
                type = ContentType.MOVIE,
                year = 2024,
                rating = null
            ),
            SearchResult(
                id = "https://hdhub4u.fake/show/1",
                title = "HDHub4u Test Show",
                poster = null,
                type = ContentType.SHOW,
                year = 2023,
                rating = null
            )
        )
    }

    /**
     * MOVIE DETAILS
     */
    suspend fun getMovieDetails(id: String): Movie = withContext(Dispatchers.IO) {
        Movie(
            id = id,
            title = "HDHub4u Sample Movie",
            overview = "This is a temporary movie description.",
            poster = null,
            backdrop = null,
            year = 2024,
            rating = null,
            runtime = 130
        )
    }

    /**
     * SHOW DETAILS (Seasons only, episodes loaded separately)
     */
    suspend fun getShowDetails(id: String): Show = withContext(Dispatchers.IO) {
        Show(
            id = id,
            title = "HDHub4u Sample Show",
            overview = "This is a temporary show description.",
            poster = null,
            backdrop = null,
            year = 2023,
            rating = null,
            seasons = listOf(
                Season(seasonNumber = 1),
                Season(seasonNumber = 2)
            )
        )
    }

    /**
     * EPISODES FOR A SEASON
     */
    suspend fun getSeasonEpisodes(
        showId: String,
        season: Int
    ): List<Episode> = withContext(Dispatchers.IO) {
        (1..12).map { ep ->
            Episode(
                id = "$showId/season/$season/episode/$ep",
                title = "Episode $ep",
                episodeNumber = ep,
                overview = "Episode $ep overview",
                runtime = 45
            )
        }
    }

    /**
     * SIMILAR CONTENT
     */
    suspend fun getSimilarContent(
        id: String,
        type: ContentType
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        listOf(
            SearchResult(
                id = "https://hdhub4u.fake/similar/1",
                title = "Similar Content 1",
                poster = null,
                type = type,
                year = 2022,
                rating = null
            )
        )
    }
}
