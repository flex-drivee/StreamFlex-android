package com.cinetheta.app.ui.pluginsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider
import com.cinetheta.domain.repositories.ProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PluginSearchUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val isLastPage: Boolean = false,
    val query: String = "",
    val submittedQuery: String = "",
    val providers: List<Provider> = emptyList(),
    val selectedProvider: Provider? = null,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null
)

class PluginSearchViewModel(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginSearchUiState())
    val uiState: StateFlow<PluginSearchUiState> = _uiState.asStateFlow()

    init {
        val availableProviders = providerRepository.enabledProviders()
        val globalSelectedId = providerRepository.selectedProviderId
        val initialProvider = if (globalSelectedId != null) {
            availableProviders.find { it.id == globalSelectedId } ?: availableProviders.firstOrNull()
        } else {
            availableProviders.firstOrNull()
        }
        
        _uiState.value = _uiState.value.copy(
            providers = availableProviders,
            selectedProvider = initialProvider
        )
    }

    fun selectProvider(provider: Provider) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
        if (_uiState.value.submittedQuery.isNotBlank()) {
            search()
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                submittedQuery = "",
                error = null
            )
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        
        _uiState.value = _uiState.value.copy(
            submittedQuery = query,
            currentPage = 1, 
            isLastPage = false, 
            results = emptyList(),
            isLoading = true, 
            error = null
        )
        val provider = _uiState.value.selectedProvider ?: return

        viewModelScope.launch {
            try {
                val results = provider.search(query, 1)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = results,
                    isLastPage = results.isEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || _uiState.value.isLastPage) return
        val provider = _uiState.value.selectedProvider ?: return
        val query = _uiState.value.submittedQuery
        if (query.isBlank()) return
        
        val nextPage = _uiState.value.currentPage + 1
        _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)

        viewModelScope.launch {
            try {
                val newResults = provider.search(query, nextPage)
                
                // If the provider doesn't support pagination, it might return the exact same page 1 results again.
                // We should filter out duplicates.
                val currentIds = _uiState.value.results.map { it.id }.toSet()
                val uniqueNewResults = newResults.filter { it.id !in currentIds }
                
                if (uniqueNewResults.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        isLastPage = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        currentPage = nextPage,
                        results = _uiState.value.results + uniqueNewResults
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
}

class PluginSearchViewModelFactory(
    private val providerRepository: ProviderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PluginSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PluginSearchViewModel(providerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
