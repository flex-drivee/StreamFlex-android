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

            stream.contentType == ContentType.M3U8 -> 0

            url.endsWith(".m3u8") -> 0

            url.endsWith(".mkv") -> 1

            url.endsWith(".mp4") -> 2

            stream.contentType == ContentType.DASH -> 3

            url.endsWith(".mpd") -> 3

            stream.host == HostType.GOOGLE_VIDEO -> 4

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

            HostType.HUBCLOUD -> 0

            HostType.HUBDRIVE -> 1

            HostType.HUBCDN -> 2

            HostType.HBLINKS -> 3

            HostType.GOOGLE_VIDEO -> 4

            HostType.DIRECT -> 5

            else -> 10
        }
    }
}