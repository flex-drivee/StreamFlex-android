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
    val error: String? = null,
    val isResolvingDownload: Boolean = false,
    val downloadStreamsAvailable: List<com.cinetheta.domain.models.StreamLink>? = null,
    val pendingDownloadEpisode: com.cinetheta.domain.models.ProviderEpisode? = null
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

    fun downloadContent(episode: com.cinetheta.domain.models.ProviderEpisode? = null) {
        viewModelScope.launch {
            val result = _uiState.value.result ?: return@launch
            _uiState.value = _uiState.value.copy(
                isResolvingDownload = true,
                downloadStreamsAvailable = null,
                pendingDownloadEpisode = episode
            )

            try {
                val sourcesToUse = if (episode != null) episode.sources else result.sources
                val finalStreams = streamRepository.resolveSources(sourcesToUse)
                if (finalStreams.streams.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isResolvingDownload = false,
                        downloadStreamsAvailable = finalStreams.streams
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isResolvingDownload = false,
                        error = "No streams found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isResolvingDownload = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun cancelDownloadDialog() {
        _uiState.value = _uiState.value.copy(
            isResolvingDownload = false,
            downloadStreamsAvailable = null,
            pendingDownloadEpisode = null
        )
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
