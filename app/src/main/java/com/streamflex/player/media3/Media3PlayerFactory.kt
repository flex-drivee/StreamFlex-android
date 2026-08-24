package com.streamflex.player.media3

import android.content.Context
import com.streamflex.player.core.DecoderMode
import com.streamflex.player.core.StreamPlayer

object Media3PlayerFactory {
    fun create(context: Context, mode: DecoderMode? = null): StreamPlayer {
        val selectedMode = mode ?: run {
            val prefs = context.getSharedPreferences("streamflex_settings", Context.MODE_PRIVATE)
            val key = prefs.getString(DecoderMode.PREF_KEY, DecoderMode.AUTO.key)
            DecoderMode.fromKey(key)
        }
        return Media3Player(context, selectedMode)
    }
}
