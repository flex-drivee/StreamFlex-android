package com.cinetheta.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.domain.repositories.StreamRepository
import com.cinetheta.player.episodes.PlayerEpisode
import kotlinx.coroutines.async
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
    val directStreamUrl: String? = null,
    val backupStreamUrls: List<String> = emptyList(),
    val streamHeaders: Map<String, String>? = null
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val session: PlayerSession? = null,
    val streams: List<StreamLink> = emptyList(),
    val isOffline: Boolean = false,
    val error: String? = null
)

class PlayerViewModel(
    private val streamRepository: StreamRepository
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
                // Direct stream URL (e.g. Live Sports / TV Channels)
                if (!session.directStreamUrl.isNullOrBlank()) {
                    val streamList = mutableListOf<StreamLink>()
                    streamList.add(
                        StreamLink(
                            name = if (session.backupStreamUrls.isNotEmpty()) "Server 1 (Primary)" else "Live Stream",
                            url = session.directStreamUrl,
                            quality = com.cinetheta.domain.models.Quality.P1080,
                            headers = session.streamHeaders ?: emptyMap(),
                            host = HostType.DIRECT,
                            adaptive = true,
                            contentType = com.cinetheta.core.network.detector.ContentType.HLS
                        )
                    )
                    session.backupStreamUrls.forEachIndexed { idx, backupUrl ->
                        streamList.add(
                            StreamLink(
                                name = "Server ${idx + 2} (Mirror)",
                                url = backupUrl,
                                quality = com.cinetheta.domain.models.Quality.P1080,
                                headers = session.streamHeaders ?: emptyMap(),
                                host = HostType.DIRECT,
                                adaptive = true,
                                contentType = com.cinetheta.core.network.detector.ContentType.HLS
                            )
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = streamList,
                        isOffline = false
                    )
                    return@launch
                }

                // 1. Check if offline file exists
                val filePath = session.localFilePath

                if (!filePath.isNullOrBlank()) {
                    if (filePath.startsWith("content://")) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOffline = true,
                            streams = listOf(
                                StreamLink(
                                    name = "Local Storage",
                                    url = filePath,
                                    host = HostType.DIRECT,
                                    adaptive = false
                                )
                            )
                        )
                        return@launch
                    }

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

                        val offlineStream = StreamLink(
                            name = "Local File",
                            url = localFile.absolutePath,
                            quality = com.cinetheta.domain.models.Quality.P1080,
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

                // Concurrently fetch subtitles for this movie/episode (OpenSubtitles v3)
                val subsDeferred = async {
                    try {
                        com.cinetheta.domain.repositories.SubtitleRepository.getSubtitles(
                            mediaId = session.mediaId,
                            title = session.title,
                            year = session.year,
                            season = session.currentEpisode?.seasonNumber,
                            episode = session.currentEpisode?.episodeNumber
                        )
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                // As soon as background subtitles resolve, attach them to the active stream list
                val subsJob = viewModelScope.launch {
                    val subs = subsDeferred.await()
                    if (subs.isNotEmpty()) {
                        val currentStreams = _uiState.value.streams
                        if (currentStreams.isNotEmpty() && currentStreams.none { it.subtitles.isNotEmpty() }) {
                            _uiState.value = _uiState.value.copy(
                                streams = currentStreams.map { it.copy(subtitles = subs) }
                            )
                        }
                    }
                }

                // 2. Check for Direct Plugin Sources
                val directSources = com.cinetheta.app.ui.pluginsearch.PluginSharedData.takeSources()
                if (directSources != null) {
                    val resolved = streamRepository.resolveSources(directSources) { currentStreams ->
                        if (currentStreams.isPlayable && currentStreams.streams.isNotEmpty()) {
                            val finalSubs = if (currentStreams.streams.none { it.subtitles.isNotEmpty() } && subsDeferred.isCompleted) {
                                try { subsDeferred.await() } catch (_: Exception) { emptyList() }
                            } else emptyList()
                            val enriched = if (finalSubs.isNotEmpty()) currentStreams.streams.map { it.copy(subtitles = finalSubs) } else currentStreams.streams
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = enriched
                            )
                        }
                    }
                    val streamsToUse = if (resolved.isPlayable && resolved.streams.isNotEmpty()) resolved.streams else _uiState.value.streams
                    if (streamsToUse.isNotEmpty()) {
                        val finalSubs = if (streamsToUse.none { it.subtitles.isNotEmpty() }) {
                            try { subsDeferred.await() } catch (_: Exception) { emptyList() }
                        } else emptyList()
                        val enriched = if (finalSubs.isNotEmpty()) streamsToUse.map { it.copy(subtitles = finalSubs) } else streamsToUse
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            streams = enriched
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            streams = emptyList(),
                            error = "Video not Available"
                        )
                    }
                    return@launch
                }

                // 3. If not downloaded, resolve online streams
                if (session.pluginProviderId == null && com.cinetheta.app.di.ProviderModule.repository.selectedProviderId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = emptyList(),
                        error = "NO_PROVIDER_SELECTED"
                    )
                    return@launch
                }

                val resolved = if (session.isShow && session.currentEpisode != null) {
                    streamRepository.resolveEpisode(
                        title = session.title,
                        season = session.currentEpisode.seasonNumber,
                        episode = session.currentEpisode.episodeNumber,
                        year = session.year,
                        pluginProviderId = session.pluginProviderId,
                        tmdbId = session.mediaId
                    ) { currentStreams ->
                        if (currentStreams.isPlayable && currentStreams.streams.isNotEmpty()) {
                            val finalSubs = if (currentStreams.streams.none { it.subtitles.isNotEmpty() } && subsDeferred.isCompleted) {
                                try { subsDeferred.await() } catch (_: Exception) { emptyList() }
                            } else emptyList()
                            val enriched = if (finalSubs.isNotEmpty()) currentStreams.streams.map { it.copy(subtitles = finalSubs) } else currentStreams.streams
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = enriched,
                                isOffline = false
                            )
                        }
                    }
                } else {
                    streamRepository.resolveMovie(
                        title = session.title,
                        year = session.year,
                        tmdbId = session.mediaId
                    ) { currentStreams ->
                        if (currentStreams.isPlayable && currentStreams.streams.isNotEmpty()) {
                            val finalSubs = if (currentStreams.streams.none { it.subtitles.isNotEmpty() } && subsDeferred.isCompleted) {
                                try { subsDeferred.await() } catch (_: Exception) { emptyList() }
                            } else emptyList()
                            val enriched = if (finalSubs.isNotEmpty()) currentStreams.streams.map { it.copy(subtitles = finalSubs) } else currentStreams.streams
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streams = enriched,
                                isOffline = false
                            )
                        }
                    }
                }

                val finalCandidateStreams = if (resolved.isPlayable && resolved.streams.isNotEmpty()) resolved.streams else _uiState.value.streams
                if (finalCandidateStreams.isNotEmpty()) {
                    val finalSubs = if (finalCandidateStreams.none { it.subtitles.isNotEmpty() }) {
                        try { subsDeferred.await() } catch (_: Exception) { emptyList() }
                    } else emptyList()
                    val enriched = if (finalSubs.isNotEmpty()) finalCandidateStreams.map { it.copy(subtitles = finalSubs) } else finalCandidateStreams
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = enriched,
                        isOffline = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streams = emptyList(),
                        error = "Video not Available"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streams = emptyList(),
                    error = e.message ?: "Video not Available"
                )
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = message
        )
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
    private val streamRepository: StreamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(streamRepository) as T
    }
}
