package com.streamflex.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.domain.models.*
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val show: Show? = null,
    val similarContent: List<SearchResult> = emptyList(),
    val episodes: List<Episode> = emptyList(), // For Shows
    val selectedSeason: Int = 1,
    val errorMessage: String? = null
)

class MovieDetailViewModel(
    private val repository: ContentRepository,
    private val contentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Try loading as a Movie first
                // Note: In a real app, you pass the 'Type' from the previous screen to know if it's a Movie or Show.
                // For this simple version, we assume ID might be either, but let's try Movie.
                // ideally your navigation should pass "type" along with "id".

                // Let's assume for now we can try fetching movie, if it fails, it might be a show (naive approach)
                // Better approach: Update your Screen.kt to pass `/{type}/{id}`

                // Fetching Movie Logic (Update this if you pass type properly)
                val movie = repository.getMovieDetails(contentId)
                val similar = repository.getSimilarContent(contentId, ContentType.MOVIE)

                _uiState.value = MovieDetailUiState(
                    isLoading = false,
                    movie = movie,
                    similarContent = similar
                )

            } catch (e: Exception) {
                // If movie failed, try Show
                try {
                    val show = repository.getShowDetails(contentId)
                    val similar = repository.getSimilarContent(contentId, ContentType.SHOW)
                    val episodes = repository.getSeasonEpisodes(contentId, 1) // Load Season 1 by default

                    _uiState.value = MovieDetailUiState(
                        isLoading = false,
                        show = show,
                        similarContent = similar,
                        episodes = episodes
                    )
                } catch (e2: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load content"
                    )
                }
            }
        }
    }

    fun loadSeason(seasonNumber: Int) {
        val showId = _uiState.value.show?.id ?: return
        viewModelScope.launch {
            val eps = repository.getSeasonEpisodes(showId, seasonNumber)
            _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber, episodes = eps)
        }
    }
}

class MovieDetailViewModelFactory(
    private val repository: ContentRepository,
    private val contentId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MovieDetailViewModel(repository, contentId) as T
    }
}