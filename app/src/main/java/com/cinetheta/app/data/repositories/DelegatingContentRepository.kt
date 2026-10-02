package com.cinetheta.app.data.repositories

import com.cinetheta.app.domain.models.Movie
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.models.Show
import com.cinetheta.app.domain.models.Episode
import com.cinetheta.app.domain.repository.ContentRepository
import com.cinetheta.app.di.ProviderModule

class DelegatingContentRepository(
    private val tmdbRepository: ContentRepository,
    private val youtubeRepository: ContentRepository
) : ContentRepository {

    private val currentRepository: ContentRepository
        get() {
            return if (ProviderModule.repository.selectedProviderId == "youtube") {
                youtubeRepository
            } else {
                tmdbRepository
            }
        }

    private val otherRepository: ContentRepository
        get() {
            return if (ProviderModule.repository.selectedProviderId == "youtube") {
                tmdbRepository
            } else {
                youtubeRepository
            }
        }

    override suspend fun search(query: String): List<SearchResult> = currentRepository.search(query)
    
    override suspend fun getPopularMovies(): List<SearchResult> = currentRepository.getPopularMovies()
    
    override suspend fun getPopularShows(): List<SearchResult> = currentRepository.getPopularShows()
    
    override suspend fun getMovieDetails(id: String): Movie {
        return try {
            currentRepository.getMovieDetails(id)
        } catch (e: Throwable) {
            try {
                otherRepository.getMovieDetails(id)
            } catch (e2: Throwable) {
                throw e // throw original error if both fail
            }
        }
    }
    
    override suspend fun getShowDetails(id: String): Show {
        return try {
            currentRepository.getShowDetails(id)
        } catch (e: Throwable) {
            try {
                otherRepository.getShowDetails(id)
            } catch (e2: Throwable) {
                throw e
            }
        }
    }
    
    override suspend fun getSimilarContent(id: String, type: com.cinetheta.app.domain.models.ContentType): List<SearchResult> {
        return try {
            currentRepository.getSimilarContent(id, type)
        } catch (e: Throwable) {
            try {
                otherRepository.getSimilarContent(id, type)
            } catch (e2: Throwable) {
                throw e
            }
        }
    }
    
    override suspend fun getSeasonEpisodes(showId: String, seasonNumber: Int): List<Episode> {
        return try {
            currentRepository.getSeasonEpisodes(showId, seasonNumber)
        } catch (e: Throwable) {
            try {
                otherRepository.getSeasonEpisodes(showId, seasonNumber)
            } catch (e2: Throwable) {
                throw e
            }
        }
    }

    override suspend fun getCategory(categoryId: String, page: Int): List<SearchResult> = currentRepository.getCategory(categoryId, page)

    override fun getSupportedCategories(): List<Pair<String, String>> = currentRepository.getSupportedCategories()
}
