package com.streamflex.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val isLoading: Boolean = true,
    val items: List<SearchResult> = emptyList(),
    val errorMessage: String? = null
)

class ExploreViewModel(
    private val repository: ContentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadCategory(0)
    }

    fun loadCategory(index: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val results = when(index) {
                    0 -> repository.getPopularMovies()
                    1 -> repository.getPopularShows()
                    2 -> repository.getAnimeShows()
                    else -> emptyList()
                }
                _uiState.value = ExploreUiState(isLoading = false, items = results)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }
}

class ExploreViewModelFactory(private val repository: ContentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExploreViewModel(repository) as T
    }
}
