package com.streamflex.engine.stream

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.StreamLink

/**
 * Builds a smart failover chain.
 *
 * The first stream is the primary stream.
 * The remaining streams are ordered as fallback options.
 */
object StreamFailover {

    /**
     * Build a failover chain.
     */
    fun build(
        streams: List<StreamLink>
    ): List<StreamLink> {

        if (streams.isEmpty()) {
            return emptyList()
        }

        val primary = streams.first()

        val result = mutableListOf(primary)

        val usedHosts = mutableSetOf(primary.host)
        val usedUrls = mutableSetOf(primary.url)

        // STEP 1
        // Same quality, different host.
        streams.forEach { stream ->

            if (stream.url in usedUrls) return@forEach

            if (
                stream.host !in usedHosts &&
                stream.quality == primary.quality
            ) {

                result += stream

                usedHosts += stream.host
                usedUrls += stream.url
            }
        }

        // STEP 2
        // Remaining streams ordered by preference.
        streams
            .filter { it.url !in usedUrls }
            .sortedBy(::hostPriority)
            .forEach {

                result += it

                usedUrls += it.url
            }

        return result
    }

    /**
     * Primary stream.
     */
    fun primary(
        streams: List<StreamLink>
    ): StreamLink? {

        return build(streams).firstOrNull()
    }

    /**
     * Fallback stream.
     */
    fun fallback(
        streams: List<StreamLink>
    ): StreamLink? {

        return build(streams).drop(1).firstOrNull()
    }

    /**
     * Host priority.
     *
     * Lower value = higher priority.
     */
    private fun hostPriority(
        stream: StreamLink
    ): Int {
        val url = stream.url.lowercase()

        return when {
            url.endsWith(".m3u8") -> 0
            url.contains("googleusercontent.com") || stream.host == HostType.GOOGLE_VIDEO -> 1
            url.endsWith(".mp4") || url.endsWith(".mkv") -> 2
            url.endsWith(".mpd") -> 3
            url.contains("workers.dev") || stream.host == HostType.HUBCLOUD -> 4
            stream.host == HostType.HUBDRIVE -> 5
            stream.host == HostType.HUBCDN -> 6
            stream.host == HostType.HBLINKS -> 7
            stream.host == HostType.PIXELDRAIN || url.contains("pixeldrain") -> 8
            stream.host == HostType.STREAMTAPE -> 9
            stream.host == HostType.FILEMOON -> 10
            stream.host == HostType.MIXDROP -> 11
            else -> 20
        }
    }
}