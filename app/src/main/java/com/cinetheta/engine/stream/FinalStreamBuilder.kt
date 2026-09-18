package com.cinetheta.engine.stream

import com.cinetheta.domain.models.FinalStreams
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink

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
                .let(::pruneNetMirrorStreams)
                .let(StreamSorter::sort)
                .map(::normalizeName)
        )
        val mergedSubtitles = orderedStreams.flatMap { it.subtitles }.distinctBy { it.url }
        try { java.io.File("/sdcard/subtitle_debug.txt").appendText("FinalStreamBuilder merged ${mergedSubtitles.size} from ${orderedStreams.size}\n") } catch (e: Exception) {}
        com.cinetheta.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "FinalStreamBuilder merged ${mergedSubtitles.size} subtitles from ${orderedStreams.size} ordered streams")
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
     * Evaluates NetMirror streams across sub-providers (Netflix, Amazon Prime, Disney+, Hotstar)
     * and keeps at most 2 best sub-providers (or 1 if top is vastly superior) based on quality,
     * subtitles, and multi-audio support. Cancels redundant/inferior sub-providers to prevent rate-limits.
     */
    private fun pruneNetMirrorStreams(streams: List<StreamLink>): List<StreamLink> {
        val netMirrorStreams = streams.filter(::isNetMirrorStream)
        if (netMirrorStreams.isEmpty()) return streams

        val otherStreams = streams.filterNot(::isNetMirrorStream)
        val grouped = netMirrorStreams.groupBy(::getNetMirrorSubProvider)
        if (grouped.size <= 1) return streams

        val scored = grouped.map { (name, provStreams) ->
            Triple(name, scoreNetMirrorSubProvider(name, provStreams), provStreams)
        }.sortedByDescending { it.second }

        // Retain max 2 subproviders (or 1 if top is vastly superior to the rest)
        val maxQual0 = scored[0].third.map { it.quality }.maxOrNull() ?: com.cinetheta.domain.models.Quality.UNKNOWN
        val maxQual1 = scored.getOrNull(1)?.third?.map { it.quality }?.maxOrNull() ?: com.cinetheta.domain.models.Quality.UNKNOWN
        val isTopVastlySuperior = scored.size >= 2 && (
            (maxQual0 >= com.cinetheta.domain.models.Quality.P1080 && maxQual1 < com.cinetheta.domain.models.Quality.P1080) ||
            (scored[0].second >= 800 && scored[1].second < 400)
        )
        val retained = if (isTopVastlySuperior) {
            scored.take(1).flatMap { it.third }
        } else {
            scored.take(2).flatMap { it.third }
        }

        com.cinetheta.core.utils.StreamLogger.info(
            "FinalStreamBuilder",
            "Pruned NetMirror sub-providers from ${grouped.keys} down to ${retained.map(::getNetMirrorSubProvider).distinct()}"
        )

        return otherStreams + retained
    }

    private fun isNetMirrorStream(stream: StreamLink): Boolean {
        val cookie = stream.headers["Cookie"] ?: ""
        val ua = stream.headers["User-Agent"] ?: ""
        val url = stream.url
        return cookie.contains("ott=") ||
               cookie.contains("t_hash_t=") ||
               ua.contains("app.netmirror") ||
               ua.contains("/OS.Gatu") ||
               url.contains("net52.cc") ||
               url.contains("/mobile/hls/") ||
               stream.name.startsWith("Netflix -") ||
               stream.name.startsWith("Amazon Prime -") ||
               stream.name.startsWith("Disney+ -") ||
               stream.name.startsWith("Hotstar -")
    }

    private fun getNetMirrorSubProvider(stream: StreamLink): String {
        val cookie = stream.headers["Cookie"] ?: ""
        val ott = Regex("ott=([a-zA-Z0-9]+)").find(cookie)?.groupValues?.getOrNull(1)
        if (ott != null) {
            return when (ott.lowercase()) {
                "nf" -> "Netflix"
                "pv" -> "Amazon Prime"
                "dp" -> "Disney+"
                "hs" -> "Hotstar"
                else -> ott.uppercase()
            }
        }
        return when {
            stream.name.contains("Netflix", ignoreCase = true) -> "Netflix"
            stream.name.contains("Prime", ignoreCase = true) -> "Amazon Prime"
            stream.name.contains("Disney", ignoreCase = true) -> "Disney+"
            stream.name.contains("Hotstar", ignoreCase = true) -> "Hotstar"
            else -> "NetMirror"
        }
    }

    private fun scoreNetMirrorSubProvider(providerName: String, streams: List<StreamLink>): Int {
        var score = 0

        // 1. Resolution
        val maxQuality = streams.map { it.quality }.maxOrNull() ?: com.cinetheta.domain.models.Quality.UNKNOWN
        score += when (maxQuality) {
            com.cinetheta.domain.models.Quality.P2160 -> 1000
            com.cinetheta.domain.models.Quality.P1440 -> 800
            com.cinetheta.domain.models.Quality.P1080 -> 600
            com.cinetheta.domain.models.Quality.P720 -> 300
            com.cinetheta.domain.models.Quality.P480 -> 150
            else -> 50
        }

        // 2. Subtitles
        val totalSubs = streams.flatMap { it.subtitles }.distinctBy { it.url }.size
        score += when {
            totalSubs >= 10 -> 300
            totalSubs >= 5 -> 200
            totalSubs >= 1 -> 100
            else -> 0
        }

        // 3. Multi-Audio
        val hasMultiAudio = streams.any { it.audioTracks.size > 1 } ||
                streams.any { it.name.contains("Multi", ignoreCase = true) || it.name.contains("Dual", ignoreCase = true) }
        if (hasMultiAudio) {
            score += 200
        }

        // 4. OTT Baseline Priority (Netflix > Prime > Disney > Hotstar)
        score += when {
            providerName.contains("Netflix", ignoreCase = true) -> 150
            providerName.contains("Prime", ignoreCase = true) -> 120
            providerName.contains("Disney", ignoreCase = true) -> 80
            providerName.contains("Hotstar", ignoreCase = true) -> 40
            else -> 20
        }

        return score
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