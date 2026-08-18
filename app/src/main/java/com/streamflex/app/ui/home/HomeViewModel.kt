package com.streamflex.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import com.streamflex.app.data.providers.hdhub4u.Hdhub4uProvider


data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<com.streamflex.player.resume.HistoryItem> = emptyList(),
    val popularMovies: List<SearchResult> = emptyList(),
    val popularShows: List<SearchResult> = emptyList(),
    val koreanDramas: List<SearchResult> = emptyList(),
    val bollywoodMovies: List<SearchResult> = emptyList(),
    val indianWebSeries: List<SearchResult> = emptyList(),
    val netflixOriginals: List<SearchResult> = emptyList(),
    val primeOriginals: List<SearchResult> = emptyList(),
    val animeMovies: List<SearchResult> = emptyList(),
    val animeShows: List<SearchResult> = emptyList(),
    val topAnimeMovies: List<SearchResult> = emptyList(),
    val topAnimeShows: List<SearchResult> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ContentRepository,
    private val progressManager: com.streamflex.player.resume.PlaybackProgressManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun reloadHistory() {
        _uiState.value = _uiState.value.copy(continueWatching = progressManager.getHistory())
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val history = progressManager.getHistory()
                val movies = repository.getPopularMovies()
                val shows = repository.getPopularShows()
                val kDramas = repository.getKoreanDramas()
                val bMovies = repository.getBollywoodMovies()
                val iSeries = repository.getIndianWebSeries()
                val netflix = repository.getNetflixOriginals()
                val prime = repository.getPrimeOriginals()
                val aMovies = repository.getAnimeMovies()
                val aShows = repository.getAnimeShows()
                val topAMovies = repository.getTopAnimeMovies()
                val topAShows = repository.getTopAnimeShows()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    continueWatching = history,
                    popularMovies = movies,
                    popularShows = shows,
                    koreanDramas = kDramas,
                    bollywoodMovies = bMovies,
                    indianWebSeries = iSeries,
                    netflixOriginals = netflix,
                    primeOriginals = prime,
                    animeMovies = aMovies,
                    animeShows = aShows,
                    topAnimeMovies = topAMovies,
                    topAnimeShows = topAShows
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load content: ${e.localizedMessage}"
                )
            }
        }
    }
}

// Factory to pass the Repository to the ViewModel
class HomeViewModelFactory(
    private val repository: ContentRepository,
    private val progressManager: com.streamflex.player.resume.PlaybackProgressManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, progressManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}