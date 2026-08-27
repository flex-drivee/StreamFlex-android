package com.streamflex.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.di.EngineModule
import com.streamflex.app.di.RepositoryModule
import com.streamflex.app.domain.models.*
import com.streamflex.app.domain.repository.ContentRepository
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadStatus
import com.streamflex.domain.repositories.DownloadRepository
import com.streamflex.domain.repositories.StreamRepository
import com.streamflex.engine.download.DownloadQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val show: Show? = null,
    val similarContent: List<SearchResult> = emptyList(),
    val episodes: List<Episode> = emptyList(), // For Shows
    val selectedSeason: Int = 1,
    val isResolvingDownload: Boolean = false,
    val downloadStreamsAvailable: List<com.streamflex.domain.models.StreamLink>? = null,
    val pendingDownloadEpisode: Episode? = null,
    val errorMessage: String? = null
)

class MovieDetailViewModel(
    private val contentRepository: ContentRepository,
    private val streamRepository: StreamRepository,
    private val downloadRepository: DownloadRepository = RepositoryModule.downloadRepository,
    private val downloadQueueManager: DownloadQueueManager = EngineModule.downloadQueueManager,
    private val contentId: String,
    private val contentType: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    val allDownloads: StateFlow<List<DownloadItem>> = downloadRepository.allDownloads

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (contentType == ContentType.MOVIE.name) {
                    val movie = contentRepository.getMovieDetails(contentId)
                    val similar = contentRepository.getSimilarContent(contentId, ContentType.MOVIE)
                    _uiState.value = MovieDetailUiState(
                        isLoading = false,
                        movie = movie,
                        similarContent = similar
                    )
                } else {
                    val show = contentRepository.getShowDetails(contentId)
                    val similar = contentRepository.getSimilarContent(contentId, ContentType.SHOW)
                    val episodes = contentRepository.getSeasonEpisodes(contentId, 1) // Load Season 1 by default
                    _uiState.value = MovieDetailUiState(
                        isLoading = false,
                        show = show,
                        similarContent = similar,
                        episodes = episodes
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load content"
                )
            }
        }
    }

    fun loadSeason(seasonNumber: Int) {
        val showId = _uiState.value.show?.id ?: return

        _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber)

        viewModelScope.launch {
            try {
                val eps = contentRepository.getSeasonEpisodes(
                    showId,
                    seasonNumber
                )

                if (eps.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        selectedSeason = seasonNumber,
                        episodes = eps
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieDetailViewModel", "Failed to load season $seasonNumber: ${e.message}")
            }
        }
    }

    fun downloadMovie() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResolvingDownload = true, downloadStreamsAvailable = null, pendingDownloadEpisode = null)
            try {
                streamRepository.resolveMovie(
                    title = movie.title,
                    year = movie.year ?: 0
                ) { finalStreams ->
                    if (finalStreams.isPlayable && finalStreams.streams.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isResolvingDownload = false,
                            downloadStreamsAvailable = finalStreams.streams,
                            pendingDownloadEpisode = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isResolvingDownload = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isResolvingDownload = false)
            }
        }
    }

    fun downloadEpisode(episode: Episode) {
        val show = _uiState.value.show ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResolvingDownload = true, downloadStreamsAvailable = null, pendingDownloadEpisode = episode)
            try {
                streamRepository.resolveEpisode(
                    title = show.title,
                    season = _uiState.value.selectedSeason,
                    episode = episode.episodeNumber,
                    year = show.year ?: 0
                ) { finalStreams ->
                    if (finalStreams.isPlayable && finalStreams.streams.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isResolvingDownload = false,
                            downloadStreamsAvailable = finalStreams.streams
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isResolvingDownload = false, pendingDownloadEpisode = null)
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isResolvingDownload = false, pendingDownloadEpisode = null)
            }
        }
    }

    fun startSelectedDownload(selectedStream: com.streamflex.domain.models.StreamLink) {
        val state = _uiState.value
        val allStreams = state.downloadStreamsAvailable ?: return
        val fallbacks = allStreams.filter { it.url != selectedStream.url }

        val movie = state.movie
        val show = state.show
        val episode = state.pendingDownloadEpisode

        if (movie != null) {
            val downloadItem = DownloadItem(
                id = "${movie.id}_movie",
                mediaId = movie.id,
                title = movie.title,
                year = movie.year,
                isShow = false,
                posterUrl = movie.poster ?: movie.backdrop,
                quality = selectedStream.quality,
                streamLink = selectedStream,
                fallbackLinks = fallbacks,
                status = DownloadStatus.QUEUED
            )
            downloadQueueManager.enqueueDownload(downloadItem)
        } else if (show != null && episode != null) {
            val sNum = state.selectedSeason
            val sPad = sNum.toString().padStart(2, '0')
            val ePad = episode.episodeNumber.toString().padStart(2, '0')

            val downloadItem = DownloadItem(
                id = "${show.id}_s${sNum}_e${episode.episodeNumber}",
                mediaId = show.id,
                title = show.title,
                subtitle = "S$sPad E$ePad - ${episode.title}",
                year = show.year,
                isShow = true,
                seasonNumber = sNum,
                episodeNumber = episode.episodeNumber,
                posterUrl = episode.stillPath ?: show.poster,
                quality = selectedStream.quality,
                streamLink = selectedStream,
                fallbackLinks = fallbacks,
                status = DownloadStatus.QUEUED
            )
            downloadQueueManager.enqueueDownload(downloadItem)
        }

        // Clear dialog state
        _uiState.value = state.copy(downloadStreamsAvailable = null, pendingDownloadEpisode = null)
    }

    fun cancelDownloadDialog() {
        _uiState.value = _uiState.value.copy(downloadStreamsAvailable = null, pendingDownloadEpisode = null)
    }

    fun cancelDownload(id: String) {
        downloadQueueManager.cancelDownload(id)
    }

    fun getDownloadItem(mediaId: String, season: Int? = null, episode: Int? = null): DownloadItem? {
        return allDownloads.value.firstOrNull {
            it.mediaId == mediaId && it.seasonNumber == season && it.episodeNumber == episode
        }
    }
}

class MovieDetailViewModelFactory(
    private val contentRepository: ContentRepository,
    private val streamRepository: StreamRepository,
    private val downloadRepository: DownloadRepository = RepositoryModule.downloadRepository,
    private val downloadQueueManager: DownloadQueueManager = EngineModule.downloadQueueManager,
    private val contentId: String,
    private val contentType: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MovieDetailViewModel(
            contentRepository = contentRepository,
            streamRepository = streamRepository,
            downloadRepository = downloadRepository,
            downloadQueueManager = downloadQueueManager,
            contentId = contentId,
            contentType = contentType
        ) as T
    }
}