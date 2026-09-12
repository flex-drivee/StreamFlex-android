package com.cinetheta.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.repository.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",          // the last query that was actually searched
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel(
    private val repository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Called on every keystroke — only updates the text, does NOT trigger search. */
    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        // If user clears the field, also clear results
        if (newQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                submittedQuery = "",
                errorMessage = null
            )
        }
    }

    /** Called when user presses Enter / Search on keyboard — triggers actual search. */
    fun onSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            submittedQuery = query,
            errorMessage = null
        )
        try {
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                results = results
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Search failed: ${e.localizedMessage}"
            )
        }
    }
}

class SearchViewModelFactory(private val repository: ContentRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository) as T
    }
}