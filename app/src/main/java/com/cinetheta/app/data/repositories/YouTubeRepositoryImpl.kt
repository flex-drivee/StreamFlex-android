package com.cinetheta.app.data.repositories

import com.cinetheta.app.domain.models.Movie
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.models.Show
import com.cinetheta.app.domain.models.Episode
import com.cinetheta.app.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.InfoItem
import com.cinetheta.providers.youtube.NewPipeDownloader

class YouTubeRepositoryImpl : ContentRepository {

    override suspend fun search(query: String): List<SearchResult> = emptyList()
    
    override suspend fun getPopularMovies(): List<SearchResult> = withContext(Dispatchers.IO) {
        try { NewPipeDownloader.init() } catch (e: Exception) {}
        getCategory(ServiceList.YouTube.kioskList.defaultKioskId, 1)
    }
    
    override suspend fun getPopularShows(): List<SearchResult> = emptyList()
    
    // We fetch a basic movie object bypassing TMDB when user clicks on a YouTube search result
    override suspend fun getMovieDetails(id: String): Movie = withContext(Dispatchers.IO) {
        try { NewPipeDownloader.init() } catch (e: Exception) {}
        val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$id")
        extractor.fetchPage()
        
        val info = StreamInfo.getInfo(extractor)
        
        Movie(
            id = id,
            title = info.name ?: "Unknown",
            overview = info.description?.content ?: "",
            poster = info.thumbnails?.lastOrNull()?.url ?: "",
            backdrop = info.thumbnails?.lastOrNull()?.url ?: "",
            year = null,
            rating = 0.0,
            runtime = (info.duration / 60).toInt(),
            genres = listOf(info.uploaderName ?: "YouTube")
        )
    }
    
    override suspend fun getShowDetails(id: String): Show = throw NotImplementedError()
    override suspend fun getSimilarContent(id: String, type: com.cinetheta.app.domain.models.ContentType): List<SearchResult> = emptyList()
    override suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<Episode> = emptyList()

    override suspend fun getCategory(categoryId: String, page: Int): List<SearchResult> = withContext(Dispatchers.IO) {
        if (page > 1) return@withContext emptyList() // Kiosks don't usually support pagination like TMDB
        
        try {
            try { NewPipeDownloader.init() } catch (e: Exception) {}
            
            // Use getExtractorById for all Kiosks in NewPipeExtractor
            val kioskExtractor = ServiceList.YouTube.kioskList.getExtractorById(categoryId, null)
            kioskExtractor.fetchPage()
            
            kioskExtractor.initialPage.items.mapNotNull { item: InfoItem ->
                if (item is StreamInfoItem) {
                    SearchResult(
                        id = item.url.substringAfter("?v="),
                        title = item.name,
                        poster = item.thumbnails?.lastOrNull()?.url ?: "",
                        type = com.cinetheta.app.domain.models.ContentType.MOVIE,
                        year = null // SearchResult year is Int? but NewPipe uploaderName is String
                    )
                } else null
            }
        } catch (e: Exception) {
            e.let { com.cinetheta.core.logger.Logger.e("Exception", it) }
            emptyList()
        }
    }

    override fun getSupportedCategories(): List<Pair<String, String>> {
        return try {
            try { NewPipeDownloader.init() } catch (e: Exception) {}
            val kiosks = ServiceList.YouTube.kioskList.availableKiosks
            kiosks.map { id -> 
                val title = id.split("_").joinToString(" ") { 
                    it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
                }
                id to title
            }
        } catch (e: Exception) {
            listOf(
                "Trending" to "Trending Now"
            )
        }
    }
}
