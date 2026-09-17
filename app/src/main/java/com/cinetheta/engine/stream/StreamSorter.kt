package com.cinetheta.engine.stream

import com.cinetheta.core.network.detector.ContentType
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink

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

            { hostPriority(it.host) }

                .thenBy {

                    qualityPriority(it.quality)

                }

                .thenBy {

                    contentPriority(it)

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
            stream.adaptive || stream.contentType == ContentType.HLS || stream.contentType == ContentType.M3U8 || url.contains(".m3u8") -> 0
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
            else -> 10
        }
    }

    /**
     * Preferred hosting services.
     *
     * Strict User & Architecture Priority:
     * 1. StreamRuby (always 1st if available - multi-audio/quality)
     * 2. AWSStream (2nd option - master.m3u8 with multiple prints and audios in one link)
     * 3. Abyss (3rd option - separate quality links)
     * 4. GDMirrorBot and other fast mirrors
     * 5. Google Video / HubCloud / HubDrive (HDHub4u)
     * 6. Fallback extractors
     * 7. Vidmoly (unstable - last)
     */
    fun hostPriority(
        host: HostType
    ): Int {
        return when (host) {
            HostType.STREAMRUBY -> 0
            HostType.AWS_STREAM -> 1
            HostType.ABYSS -> 2
            HostType.GDMIRRORBOT -> 3
            HostType.CLOUDY -> 4
            HostType.TURBOVID -> 5
            HostType.STREAMUP -> 6
            HostType.XERVER -> 7
            HostType.BLAKITE -> 8

            HostType.GOOGLE_VIDEO -> 9
            HostType.HUBCLOUD -> 10
            HostType.HUBDRIVE -> 11
            HostType.HUBCDN -> 12
            HostType.HBLINKS -> 13
            HostType.PIXELDRAIN -> 14
            HostType.DIRECT -> 15
            HostType.M3U8 -> 16
            HostType.DASH -> 17
            HostType.STREAMTAPE -> 18
            HostType.FILEMOON -> 19
            HostType.MIXDROP -> 20
            HostType.DOOD -> 21
            HostType.HDSTREAM4U -> 22
            HostType.VIDSTACK -> 23

            HostType.VIDMOLY -> 80
            HostType.REDIRECT -> 90
            else -> 50
        }
    }
}