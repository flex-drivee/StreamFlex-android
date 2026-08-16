package com.streamflex.player.episodes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerEpisode(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int
)

class EpisodeNavigator {
    
    private val _episodes = MutableStateFlow<List<PlayerEpisode>>(emptyList())
    val episodes: StateFlow<List<PlayerEpisode>> = _episodes.asStateFlow()
    
    private val _currentEpisode = MutableStateFlow<PlayerEpisode?>(null)
    val currentEpisode: StateFlow<PlayerEpisode?> = _currentEpisode.asStateFlow()
    
    fun setEpisodes(episodeList: List<PlayerEpisode>, current: PlayerEpisode?) {
        _episodes.value = episodeList
        _currentEpisode.value = current
    }
    
    fun getNextEpisode(): PlayerEpisode? {
        val current = _currentEpisode.value ?: return null
        val list = _episodes.value
        val index = list.indexOfFirst { it.id == current.id }
        if (index != -1 && index + 1 < list.size) {
            return list[index + 1]
        }
        return null
    }
    
    fun getPreviousEpisode(): PlayerEpisode? {
        val current = _currentEpisode.value ?: return null
        val list = _episodes.value
        val index = list.indexOfFirst { it.id == current.id }
        if (index > 0) {
            return list[index - 1]
        }
        return null
    }
}
