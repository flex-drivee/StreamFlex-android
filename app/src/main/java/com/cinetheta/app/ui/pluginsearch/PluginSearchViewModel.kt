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
    val query: String = "",
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
        _uiState.value = _uiState.value.copy(
            providers = availableProviders,
            selectedProvider = availableProviders.firstOrNull()
        )
    }

    fun selectProvider(provider: Provider) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
        if (_uiState.value.query.isNotBlank()) {
            search(_uiState.value.query)
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        val provider = _uiState.value.selectedProvider ?: return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val results = provider.search(query)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
