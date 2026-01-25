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
    val popularMovies: List<SearchResult> = emptyList(),
    val popularShows: List<SearchResult> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        testHdhub4uSearch() // ← temporary
    }


    private fun testHdhub4uSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val provider = Hdhub4uProvider()
                val results = provider.searchMovies("John Wick")

                Log.d("HDHUB4U_TEST", "Results Found: ${results.size}")
                results.forEach {
                    Log.d("HDHUB4U_TEST", "${it.title} | ${it.year} | ${it.url}")
                }
            } catch (e: Exception) {
                Log.e("HDHUB4U_TEST", "Error: ${e.localizedMessage}")
            }
        }
    }


    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Fetch both lists in parallel (optimization)
                val movies = repository.getPopularMovies()
                val shows = repository.getPopularShows()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    popularMovies = movies,
                    popularShows = shows
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
class HomeViewModelFactory(private val repository: ContentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}