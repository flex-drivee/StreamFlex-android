package com.cinetheta.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import com.cinetheta.app.data.providers.hdhub4u.Hdhub4uProvider


data class HomeSection(
    val id: String,
    val title: String,
    val items: List<SearchResult>
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<com.cinetheta.player.resume.HistoryItem> = emptyList(),
    val popularMovies: List<SearchResult> = emptyList(), // For hero section
    val sections: List<HomeSection> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ContentRepository,
    private val progressManager: com.cinetheta.player.resume.PlaybackProgressManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun reloadHistory() {
        _uiState.value = _uiState.value.copy(continueWatching = progressManager.getHistory())
    }

    fun removeHistoryItem(mediaId: String) {
        progressManager.removeFromHistory(mediaId)
        reloadHistory()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val history = progressManager.getHistory()
                val popMovies = repository.getPopularMovies()
                
                // Define the categories to fetch
                val categoriesToFetch = repository.getSupportedCategories()

                val loadedSections = mutableListOf<HomeSection>()
                for ((catId, catTitle) in categoriesToFetch) {
                    try {
                        val items = repository.getCategory(catId)
                        if (items.isNotEmpty()) {
                            loadedSections.add(HomeSection(catId, catTitle, items))
                        }
                    } catch (e: Exception) {
                    }
                }

                _uiState.value = HomeUiState(
                    isLoading = false,
                    continueWatching = history,
                    popularMovies = popMovies,
                    sections = loadedSections
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
    private val progressManager: com.cinetheta.player.resume.PlaybackProgressManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, progressManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}