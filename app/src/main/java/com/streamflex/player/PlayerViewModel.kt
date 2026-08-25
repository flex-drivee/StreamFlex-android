package com.streamflex.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.download.DownloadStatus
import com.streamflex.domain.repositories.DownloadRepository
import com.streamflex.domain.repositories.StreamRepository
import com.streamflex.player.episodes.PlayerEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlayerSession(
    val mediaId: String,
    val title: String,
    val year: Int,
    val isShow: Boolean,
    val episodes: List<PlayerEpisode>,
    val currentEpisode: PlayerEpisode?
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val session: PlayerSession? = null,
    val streams: List<StreamLink> = emptyList(),
    val isOffline: Boolean = false,
    val error: String? = null
)

class PlayerViewModel(
    private val streamRepository: StreamRepository,
    private val downloadRepository: DownloadRepository = com.streamflex.app.di.RepositoryModule.downloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initializeSession(session: PlayerSession) {
        if (_uiState.value.session == null) {
            _uiState.value = _uiState.value.copy(session = session)
            fetchStreamsForCurrentSession()
        }
    }

    private fun fetchStreamsForCurrentSession() {
        val session = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, streams = emptyList(), isOffline = false)
        
        viewModelScope.launch {
            try {
                // 1. Check if offline download exists on disk
                val sNum = if (session.isShow) session.currentEpisode?.seasonNumber else null
                val eNum = if (session.isShow) session.currentEpisode?.episodeNumber else null
                
                val downloadedItem = downloadRepository.getDownloadForMedia(session.mediaId, sNum, eNum)
                if (downloadedItem != null && downloadedItem.status == DownloadStatus.COMPLETED && !downloadedItem.localFilePath.isNullOrBlank()) {
                    val localFile = File(downloadedItem.localFilePath)
                    if (localFile.exists() && localFile.length() > 0) {
                        val offlineStream = StreamLink(
                            name = "Offline Download • ${downloadedItem.quality.label}",
                            url = localFile.absolutePath,
                            quality = downloadedItem.quality,
                            host = HostType.DIRECT
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            streams = listOf(offlineStream),
                            isOffline = true
                        )
                        return@launch
                    }
                }

                // 2. If not downloaded, resolve online streams
                if (session.isShow && session.currentEpisode != null) {
                    streamRepository.resolveEpisode(
                        title = session.title,
                        season = session.currentEpisode.seasonNumber,
                        episode = session.currentEpisode.episodeNumber,
                        year = session.year
                    ) { currentStreams ->
                        if (currentStreams.isPlayable) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = currentStreams.streams,
                                isOffline = false
                            )
                        }
                    }
                } else {
                    streamRepository.resolveMovie(
                        title = session.title,
                        year = session.year
                    ) { currentStreams ->
                        if (currentStreams.isPlayable) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = currentStreams.streams,
                                isOffline = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun playEpisode(episode: PlayerEpisode) {
        val session = _uiState.value.session ?: return
        val newSession = session.copy(currentEpisode = episode)
        _uiState.value = _uiState.value.copy(session = newSession)
        fetchStreamsForCurrentSession()
    }
    
    fun playNextEpisode() {
        val nextEp = getNextEpisode()
        if (nextEp != null) {
            playEpisode(nextEp)
        }
    }
    
    fun getNextEpisode(): PlayerEpisode? {
        val session = _uiState.value.session ?: return null
        val curr = session.currentEpisode ?: return null
        val index = session.episodes.indexOfFirst { it.id == curr.id }
        if (index != -1 && index + 1 < session.episodes.size) {
            return session.episodes[index + 1]
        }
        return null
    }
}

class PlayerViewModelFactory(
    private val streamRepository: StreamRepository,
    private val downloadRepository: DownloadRepository = com.streamflex.app.di.RepositoryModule.downloadRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(streamRepository, downloadRepository) as T
    }
}
