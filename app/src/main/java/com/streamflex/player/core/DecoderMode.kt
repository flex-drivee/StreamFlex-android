package com.streamflex.player.core

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FFmpegOnlyRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * Decoding preference mode for video and audio playback.
 *
 * Modeled after CloudStream's software decoding settings:
 * - AUTO: Automatic (Hardware accelerated with automatic FFmpeg fallback)
 * - HW_SW: Hardware preferred, fallback to software FFmpeg decoder
 * - SW_HW: Software FFmpeg preferred, fallback to hardware MediaCodec
 * - HW_ONLY: Hardware only (Native Android MediaCodec, no software decoding)
 * - SW_ONLY: Software only (Force Nextlib FFmpeg decoding)
 */
enum class DecoderMode(
    val key: String,
    val title: String,
    val description: String
) {
    AUTO(
        key = "AUTO",
        title = "Auto (Recommended)",
        description = "Hardware acceleration with automatic FFmpeg fallback for unsupported codecs"
    ),
    HW_SW(
        key = "HW_SW",
        title = "Hardware + Software (HW + SW)",
        description = "Prefer hardware decoders, fallback to Nextlib FFmpeg on failure"
    ),
    SW_HW(
        key = "SW_HW",
        title = "Software + Hardware (SW + HW)",
        description = "Prefer Nextlib FFmpeg software decoder, fallback to hardware"
    ),
    HW_ONLY(
        key = "HW_ONLY",
        title = "Hardware only (HW)",
        description = "Use device MediaCodec hardware only (Disable FFmpeg software decoding)"
    ),
    SW_ONLY(
        key = "SW_ONLY",
        title = "Software only (SW)",
        description = "Force Nextlib FFmpeg software decoding for all streams"
    );

    @OptIn(UnstableApi::class)
    fun createRenderersFactory(context: Context): RenderersFactory {
        return when (this) {
            AUTO, HW_SW -> {
                NextRenderersFactory(context).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                }
            }
            SW_HW -> {
                NextRenderersFactory(context).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                }
            }
            HW_ONLY -> {
                DefaultRenderersFactory(context).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                }
            }
            SW_ONLY -> {
                FFmpegOnlyRenderersFactory(context)
            }
        }
    }

    companion object {
        const val PREF_KEY = "decoder_mode"

        fun fromKey(key: String?): DecoderMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: AUTO
        }
    }
}
