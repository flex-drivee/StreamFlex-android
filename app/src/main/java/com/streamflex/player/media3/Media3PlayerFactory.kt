package com.streamflex.player.media3

import android.content.Context
import com.streamflex.player.core.StreamPlayer

object Media3PlayerFactory {
    fun create(context: Context): StreamPlayer {
        return Media3Player(context)
    }
}
