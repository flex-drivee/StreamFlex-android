package com.cinetheta.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.domain.models.download.DownloadStatus
import com.cinetheta.domain.repositories.DownloadRepository
import com.cinetheta.domain.repositories.StreamRepository
import com.cinetheta.player.episodes.PlayerEpisode
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
    val currentEpisode: PlayerEpisode?,
    val pluginProviderId: String? = null,
    val localFilePath: String? = null,
    val downloadItemId: String? = null
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
    private val downloadRepository: DownloadRepository = com.cinetheta.app.di.RepositoryModule.downloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var originalProviderId: String? = null

    fun initializeSession(session: PlayerSession) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            session = session,
            streams = emptyList(),
            isOffline = false,
            error = null
        )
        if (session.pluginProviderId != null) {
            originalProviderId = com.cinetheta.app.di.ProviderModule.repository.selectedProviderId
            com.cinetheta.app.di.ProviderModule.repository.selectedProviderId = session.pluginProviderId
        }
        fetchStreamsForCurrentSession()
    }
    private fun fetchStreamsForCurrentSession() {
        val session = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, streams = emptyList(), isOffline = false)
        
        viewModelScope.launch {
            try {
                // 1. Check if offline download exists on disk
                val sNum = if (session.isShow) session.currentEpisode?.seasonNumber else null
                val eNum = if (session.isShow) session.currentEpisode?.episodeNumber else null
                
                val downloadedItem = if (!session.downloadItemId.isNullOrBlank()) {
                    downloadRepository.getDownloadById(session.downloadItemId)
                } else {
                    downloadRepository.getDownloadForMedia(session.mediaId, sNum, eNum)
                }
                val filePath = if (downloadedItem != null && downloadedItem.status == DownloadStatus.COMPLETED && !downloadedItem.localFilePath.isNullOrBlank()) {
                    downloadedItem.localFilePath
                } else if (!session.localFilePath.isNullOrBlank()) {
                    session.localFilePath
                } else null

                if (!filePath.isNullOrBlank()) {
                    val localFile = File(filePath)
                    if (localFile.exists() && localFile.length() > 0) {
                        val localSubtitles = mutableListOf<com.cinetheta.domain.models.Subtitle>()
                        val parentDir = localFile.parentFile
                        if (parentDir != null && parentDir.exists()) {
                            val baseName = localFile.nameWithoutExtension
                            parentDir.listFiles()?.filter { f ->
                                f.isFile && f.name.startsWith(baseName) && (f.name.endsWith(".srt", ignoreCase = true) || f.name.endsWith(".vtt", ignoreCase = true))
                            }?.forEach { subFile ->
                                val subName = subFile.nameWithoutExtension.removePrefix(baseName).removePrefix(".")
                                val label = if (subName.isNotBlank()) subName.replaceFirstChar { it.uppercase() } else "Subtitles"
                                localSubtitles.add(
                                    com.cinetheta.domain.models.Subtitle(
                                        language = subName.lowercase().take(3),
                                        label = label,
                                        url = android.net.Uri.fromFile(subFile).toString()
                                    )
                                )
                            }
                        }

                        if (localSubtitles.isEmpty() && downloadedItem?.subtitles?.isNotEmpty() == true) {
                            localSubtitles.addAll(downloadedItem.subtitles)
                        }

                        val quality = downloadedItem?.quality ?: com.cinetheta.domain.models.Quality.P1080
                        val offlineStream = StreamLink(
                            name = "Offline Download • ${quality.label}",
                            url = localFile.absolutePath,
                            quality = quality,
                            host = HostType.DIRECT,
                            contentType = com.cinetheta.core.network.detector.ContentType.VIDEO,
                            subtitles = localSubtitles
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            streams = listOf(offlineStream),
                            isOffline = true
                        )
                        return@launch
                    }
                }

                // 2. Check for Direct Plugin Sources
                val directSources = com.cinetheta.app.ui.pluginsearch.PluginSharedData.takeSources()
                if (directSources != null) {
                    streamRepository.resolveSources(directSources) { currentStreams ->
                        if (currentStreams.isPlayable) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = currentStreams.streams
                            )
                        }
                    }
                    return@launch
                }

                // 3. If not downloaded, resolve online streams
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
    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.session?.pluginProviderId != null) {
            com.cinetheta.app.di.ProviderModule.repository.selectedProviderId = originalProviderId
        }
    }
}

class PlayerViewModelFactory(
    private val streamRepository: StreamRepository,
    private val downloadRepository: DownloadRepository = com.cinetheta.app.di.RepositoryModule.downloadRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(streamRepository, downloadRepository) as T
    }
}
