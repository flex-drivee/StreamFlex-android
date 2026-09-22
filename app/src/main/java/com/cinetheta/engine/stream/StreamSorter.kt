package com.cinetheta.engine.stream

import com.cinetheta.core.network.detector.ContentType
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink

/**
 * Sorts streams by reliability, quota resilience, and quality.
 *
 * The goal is to place the best playable stream first while pushing
 * quota-limited streams (Google links, Buzzer links) to the very end
 * of the server list.
 */
object StreamSorter {

    fun isGoogleStream(stream: StreamLink): Boolean {
        val url = stream.url.lowercase()
        val name = stream.name.lowercase()
        return stream.host == HostType.GOOGLE_VIDEO ||
                url.contains("googleusercontent.com") ||
                url.contains("drive.google.com") ||
                url.contains("docs.google.com") ||
                url.contains("googlevideo.com") ||
                name.contains("google")
    }

    fun isBuzzStream(stream: StreamLink): Boolean {
        val url = stream.url.lowercase()
        val name = stream.name.lowercase()
        return stream.host == HostType.REDIRECT ||
                url.contains("buzzheavier.com") ||
                url.contains(".buzz/") ||
                url.contains("buzzserver") ||
                url.contains("buzzer") ||
                name.contains("buzz") ||
                name.contains("buzzer")
    }

    /**
     * Tier classification to ensure quota-limited servers come LAST:
     * 0 -> Normal high-speed / CDN streams (HubCloud, HubDrive, PixelDrain, StreamRuby, etc.)
     * 1 -> Google Video / Drive (strict download quotas)
     * 2 -> Buzzer / BuzzServer / Redirects (fast quota limit exhaustion)
     */
    fun quotaTier(stream: StreamLink): Int {
        return when {
            isBuzzStream(stream) -> 2
            isGoogleStream(stream) -> 1
            else -> 0
        }
    }

    fun sort(
        streams: List<StreamLink>
    ): List<StreamLink> {

        return streams.sortedWith(
            compareBy<StreamLink>
            { quotaTier(it) }
                .thenBy { hostPriority(it) }
                .thenBy { qualityPriority(it.quality) }
                .thenBy { contentPriority(it) }
                .thenBy { it.name.lowercase() }
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
            url.endsWith(".mp4") || url.endsWith(".mkv") -> 1
            stream.contentType == ContentType.DASH || url.endsWith(".mpd") -> 2
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
     * 5. HubCloud / HubDrive / HubCDN / HbLinks / PixelDrain (HDHub4u)
     * 6. Direct media (.mp4/.mkv)
     * 7. Fallback extractors
     * 8. Vidmoly (unstable - last among regular)
     * 9. Google & Buzzer (quota limited - sorted last via quotaTier)
     */
    fun hostPriority(
        stream: StreamLink
    ): Int {
        if (isBuzzStream(stream)) return 95
        if (isGoogleStream(stream)) return 90

        return hostPriority(stream.host)
    }

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
            HostType.GOOGLE_VIDEO -> 90
            HostType.REDIRECT -> 95
            else -> 50
        }
    }
}