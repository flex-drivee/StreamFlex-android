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
    val pluginProviderId: String? = null
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

    fun initializeSession(session: PlayerSession) {
        if (_uiState.value.session == null) {
            _uiState.value = _uiState.value.copy(session = session)
            if (session.pluginProviderId != null) {
                com.cinetheta.app.di.ProviderModule.repository.selectedProviderId = session.pluginProviderId
            }
    override fun onCleared() { super.onCleared(); com.cinetheta.app.di.ProviderModule.repository.selectedProviderId = null } }
