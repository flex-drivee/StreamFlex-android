package com.cinetheta.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cinetheta.app.domain.models.*
import com.cinetheta.app.domain.repository.ContentRepository
import com.cinetheta.domain.repositories.StreamRepository
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
    val downloadStreamsAvailable: List<com.cinetheta.domain.models.StreamLink>? = null,
    val pendingDownloadEpisode: Episode? = null,
    val showMovieBoxComingSoon: Boolean = false,
    val errorMessage: String? = null
)

class MovieDetailViewModel(
    private val contentRepository: ContentRepository,
    private val streamRepository: StreamRepository,
    private val contentId: String,
    private val contentType: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

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
                    _uiState.value = MovieDetailUiState(
                        isLoading = false,
                        show = show,
                        similarContent = similar
                    )
                    loadEpisodes(seasonNumber = 1)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load content"
                )
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        if (_uiState.value.selectedSeason == seasonNumber) return
        _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber)
        loadEpisodes(seasonNumber)
    }

    fun loadSeason(seasonNumber: Int) = selectSeason(seasonNumber)

    private fun loadEpisodes(seasonNumber: Int) {
        viewModelScope.launch {
            try {
                val episodes = contentRepository.getSeasonEpisodes(contentId, seasonNumber)
                _uiState.value = _uiState.value.copy(episodes = episodes)
            } catch (e: Exception) {
                // If loading episodes fails, retain UI state with empty episode list
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

    fun dismissMovieBoxNotice() {
        _uiState.value = _uiState.value.copy(showMovieBoxComingSoon = false)
    }

    fun cancelDownloadDialog() {
        _uiState.value = _uiState.value.copy(downloadStreamsAvailable = null, pendingDownloadEpisode = null)
    }
}

class MovieDetailViewModelFactory(
    private val contentRepository: ContentRepository,
    private val streamRepository: StreamRepository,
    private val contentId: String,
    private val contentType: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MovieDetailViewModel(
            contentRepository = contentRepository,
            streamRepository = streamRepository,
            contentId = contentId,
            contentType = contentType
        ) as T
    }
}