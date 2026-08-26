package com.streamflex.engine.stream

import com.streamflex.core.network.detector.ContentType
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink

/**
 * Sorts streams by reliability and quality.
 *
 * The goal is to place the best playable stream first.
 */
object StreamSorter {

    fun sort(
        streams: List<StreamLink>
    ): List<StreamLink> {

        return streams.sortedWith(

            compareBy<StreamLink>

            { contentPriority(it) }

                .thenBy {

                    qualityPriority(it.quality)

                }

                .thenBy {

                    hostPriority(it.host)

                }

                .thenBy {

                    it.name.lowercase()

                }
        )
    }

    /**
     * Stream format priority.
     *
     * Lower number = higher priority.
     */
    private fun contentPriority(
        stream: StreamLink
    ): Int {
        val url = stream.url.lowercase()

        return when {
            stream.contentType == ContentType.M3U8 || url.endsWith(".m3u8") -> 0
            url.contains("googleusercontent.com") || stream.host == HostType.GOOGLE_VIDEO -> 1
            url.endsWith(".mp4") || url.endsWith(".mkv") -> 2
            stream.contentType == ContentType.DASH || url.endsWith(".mpd") -> 3
            else -> 5
        }
    }

    /**
     * Video quality priority.
     */
    private fun qualityPriority(
        quality: Quality
    ): Int {
        return when (quality) {
            Quality.P2160 -> 0
            Quality.P1440 -> 1
            Quality.P1080 -> 2
            Quality.P720 -> 3
            Quality.P480 -> 4
            Quality.P360 -> 5
            else -> 100
        }
    }

    /**
     * Preferred hosting services.
     */
    private fun hostPriority(
        host: HostType
    ): Int {
        return when (host) {
            HostType.GOOGLE_VIDEO -> 0
            HostType.HUBCLOUD -> 1
            HostType.HUBDRIVE -> 2
            HostType.HUBCDN -> 3
            HostType.DIRECT -> 4
            HostType.PIXELDRAIN -> 5
            HostType.HBLINKS -> 6
            HostType.STREAMTAPE -> 7
            HostType.MIXDROP -> 8
            else -> 10
        }
    }
}