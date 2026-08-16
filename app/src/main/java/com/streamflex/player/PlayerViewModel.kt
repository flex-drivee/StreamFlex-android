package com.streamflex.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.repositories.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val streams: List<StreamLink> = emptyList(),
    val currentStreamIndex: Int = 0
)

class PlayerViewModel(
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initializeWithUrls(urls: List<String>, referers: List<String>) {
        val streamLinks = urls.mapIndexed { index, url ->
            StreamLink(
                name = "Stream ${index + 1}",
                url = url,
                quality = com.streamflex.domain.models.Quality.UNKNOWN,
                host = com.streamflex.domain.models.HostType.DIRECT,
                referer = referers.getOrNull(index) ?: ""
            )
        }
        _uiState.value = PlayerUiState(
            isLoading = false,
            streams = streamLinks,
            currentStreamIndex = 0
        )
    }

    fun startExtractionForMovie(title: String, year: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            streamRepository.resolveMovie(title = title, year = year) { currentStreams ->
                if (currentStreams.isPlayable) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = currentStreams.streams
                    )
                }
            }
        }
    }

    fun startExtractionForEpisode(title: String, season: Int, episode: Int, year: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            streamRepository.resolveEpisode(
                title = title,
                season = season,
                episode = episode,
                year = year
            ) { currentStreams ->
                if (currentStreams.isPlayable) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = currentStreams.streams
                    )
                }
            }
        }
    }
}

class PlayerViewModelFactory(
    private val streamRepository: StreamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayerViewModel(streamRepository) as T
    }
}
