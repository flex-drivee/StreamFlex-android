package com.cinetheta.player.vlc

import android.content.Context
import com.cinetheta.player.core.StreamPlayer

object VlcPlayerFactory {
    fun create(context: Context): StreamPlayer {
        return VlcPlayer(context)
    }
}
