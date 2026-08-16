package com.streamflex.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.domain.models.*
import com.streamflex.app.domain.repository.ContentRepository
import com.streamflex.domain.repositories.StreamRepository
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

    fun fetchMovieStreams(
        onResult: (List<com.streamflex.domain.models.StreamLink>) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val movie = uiState.value.movie

                if (movie == null) {

                    onResult(emptyList())

                    return@launch

                }
                android.util.Log.d(
                    "MOVIE_DEBUG",
                    "Resolving movie: ${movie.title} (${movie.year})"
                )

                var firstStreamFired = false

                com.streamflex.player.StreamStateHolder.clear()

                val streams = streamRepository.resolveMovie(

                    title = movie.title,

                    year = movie.year

                ) { currentStreams ->
                    com.streamflex.player.StreamStateHolder.streams.value = currentStreams.streams
                    if (currentStreams.isPlayable && !firstStreamFired) {
                        firstStreamFired = true
                        onResult(currentStreams.streams)
                    }
                }
                
                android.util.Log.d(
                    "MOVIE_DEBUG",
                    "Playable = ${streams.isPlayable}"
                )

                android.util.Log.d(
                    "MOVIE_DEBUG",
                    "Stream count = ${streams.streamCount}"
                )

                streams.streams.forEachIndexed { index, stream ->

                    android.util.Log.d(
                        "MOVIE_DEBUG",
                        "[$index] ${stream.host} | ${stream.quality} | ${stream.url}"
                    )

                }
                android.util.Log.d(
                    "MOVIE_DEBUG",
                    "Returning ${streams.streams.size} URL(s) to UI"
                )
                
                // If it finished but never fired (e.g. only 1 stream total, or no streams)
                if (!firstStreamFired) {
                    onResult(
                        streams.streams
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                onResult(emptyList())

            }

        }
    }

    fun fetchEpisodeStreams(
        episode: Episode,
        onResult: (List<com.streamflex.domain.models.StreamLink>) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val show = uiState.value.show

                if (show == null) {

                    onResult(emptyList())

                    return@launch

                }

                var firstStreamFired = false

                com.streamflex.player.StreamStateHolder.clear()

                val streams = streamRepository.resolveEpisode(

                    title = show.title,

                    season = uiState.value.selectedSeason,

                    episode = episode.episodeNumber,

                    year = show.year

                ) { currentStreams ->
                    com.streamflex.player.StreamStateHolder.streams.value = currentStreams.streams
                    if (currentStreams.isPlayable && !firstStreamFired) {
                        firstStreamFired = true
                        onResult(currentStreams.streams)
                    }
                }

                if (!firstStreamFired) {
                    onResult(
                        streams.streams
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                onResult(emptyList())

            }

        }
    }

    fun loadSeason(seasonNumber: Int) {
        val showId = _uiState.value.show?.id ?: return

        viewModelScope.launch {

            val eps = contentRepository.getSeasonEpisodes(
                showId,
                seasonNumber
            )

            _uiState.value = _uiState.value.copy(
                selectedSeason = seasonNumber,
                episodes = eps
            )
        }
    }
}

class MovieDetailViewModelFactory(
    private val contentRepository: ContentRepository,
    private val streamRepository: StreamRepository,
    private val contentId: String,
    private val contentType: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MovieDetailViewModel(
            contentRepository = contentRepository,
            streamRepository = streamRepository,
            contentId = contentId,
            contentType = contentType
        ) as T
    }
}