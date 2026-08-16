package com.streamflex.player

import com.streamflex.domain.models.StreamLink
import com.streamflex.player.episodes.PlayerEpisode
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A simple global holder to pass dynamically discovered streams to PlayerActivity without being limited by Intent sizes
 */
object StreamStateHolder {
    val streams = MutableStateFlow<List<StreamLink>>(emptyList())
    
    val episodes = MutableStateFlow<List<PlayerEpisode>>(emptyList())
    val currentEpisode = MutableStateFlow<PlayerEpisode?>(null)
    
    var onEpisodeSelected: ((PlayerEpisode) -> Unit)? = null

    fun getNextEpisode(): PlayerEpisode? {
        val curr = currentEpisode.value ?: return null
        val epsList = episodes.value
        val index = epsList.indexOfFirst { it.id == curr.id }
        if (index != -1 && index + 1 < epsList.size) {
            return epsList[index + 1]
        }
        return null
    }

    fun clear() {
        streams.value = emptyList()
        episodes.value = emptyList()
        currentEpisode.value = null
        onEpisodeSelected = null
    }
}
