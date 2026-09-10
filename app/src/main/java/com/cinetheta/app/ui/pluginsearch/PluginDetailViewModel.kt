package com.cinetheta.app.ui.pluginsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.repositories.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PluginDetailUiState(
    val isLoading: Boolean = true,
    val result: ProviderResult? = null,
    val error: String? = null
)

class PluginDetailViewModel(
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginDetailUiState())
    val uiState: StateFlow<PluginDetailUiState> = _uiState.asStateFlow()

    fun loadContent(searchResult: SearchResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = streamRepository.loadContent(searchResult)
                if (result != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, result = result)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load content from provider.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

class PluginDetailViewModelFactory(
    private val streamRepository: StreamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PluginDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PluginDetailViewModel(streamRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
