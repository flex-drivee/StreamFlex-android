package com.streamflex.engine.stream

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.StreamLink

/**
 * Produces the final player-ready stream package.
 *
 * Pipeline:
 *
 * Raw Streams
 *      ↓
 * StreamMerger
 *      ↓
 * DuplicateRemover
 *      ↓
 * StreamFilter
 *      ↓
 * StreamSorter
 *      ↓
 * FinalStreams
 */
object FinalStreamBuilder {

    /**
     * Build the final stream package.
     */
    fun build(
        streams: List<StreamLink>
    ): FinalStreams {

        if (streams.isEmpty()) {
            return FinalStreams.EMPTY
        }

        val orderedStreams = StreamFailover.build(

            streams
                .let(StreamMerger::merge)
                .let(DuplicateRemover::remove)
                .let(StreamFilter::filter)
                .let(StreamSorter::sort)
                .map(::normalizeName)
        )
        val mergedSubtitles = orderedStreams.flatMap { it.subtitles }.distinctBy { it.url }
        try { java.io.File("/sdcard/subtitle_debug.txt").appendText("FinalStreamBuilder merged ${mergedSubtitles.size} from ${orderedStreams.size}\n") } catch (e: Exception) {}
        com.streamflex.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "FinalStreamBuilder merged ${mergedSubtitles.size} subtitles from ${orderedStreams.size} ordered streams")
        val augmentedStreams = orderedStreams.map { it.copy(subtitles = mergedSubtitles) }

        return FinalStreams(

            streams = augmentedStreams,

            defaultStream =
                StreamFailover.primary(augmentedStreams),

            fallbackStream =
                StreamFailover.fallback(augmentedStreams)
        )
    }

    /**
     * Stream used for autoplay.
     *
     * StreamSorter already placed the best stream first.
     */
    private fun chooseDefault(
        streams: List<StreamLink>
    ): StreamLink? {

        return streams.firstOrNull()
    }

    /**
     * Backup stream used if playback fails.
     *
     * Priority:
     *
     * MP4 / MKV
     * ↓
     * M3U8
     * ↓
     * DASH
     * ↓
     * Google Video
     * ↓
     * First available
     */
    private fun chooseFallback(
        streams: List<StreamLink>
    ): StreamLink? {

        return streams.firstOrNull {

            it.url.endsWith(".mp4", true) ||
                    it.url.endsWith(".mkv", true)

        }

            ?: streams.firstOrNull {

                it.host == HostType.M3U8

            }

            ?: streams.firstOrNull {

                it.host == HostType.DASH

            }

            ?: streams.firstOrNull {

                it.host == HostType.GOOGLE_VIDEO

            }

            ?: streams.firstOrNull()
    }

    /**
     * Ensure every stream has a readable name.
     */
    private fun normalizeName(
        stream: StreamLink
    ): StreamLink {

        if (stream.name.isNotBlank()) {
            return stream
        }

        val parts = mutableListOf<String>()

        parts += stream.host.name

        if (stream.quality.label.isNotBlank()) {
            parts += stream.quality.label
        }

        if (stream.contentType.name.isNotBlank()) {
            parts += stream.contentType.name
        }

        return stream.copy(
            name = parts.joinToString(" • ")
        )
    }

}