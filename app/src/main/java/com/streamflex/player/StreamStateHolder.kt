package com.streamflex.player

import com.streamflex.domain.models.StreamLink
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A singleton state holder to allow the background extraction in MovieDetailViewModel
 * to pass dynamically discovered streams to PlayerActivity without being limited by Intent sizes
 * or static arrays.
 */
object StreamStateHolder {
    val streams = MutableStateFlow<List<StreamLink>>(emptyList())
    
    fun clear() {
        streams.value = emptyList()
    }
}
