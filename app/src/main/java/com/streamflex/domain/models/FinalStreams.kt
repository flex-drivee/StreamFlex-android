package com.streamflex.domain.models

/**
 * Final output produced by the Stream Engine.
 *
 * This object contains everything the player needs
 * after extraction, filtering, merging and sorting
 * have completed.
 */
data class FinalStreams(

    /**
     * Final playable streams.
     *
     * Already:
     * - merged
     * - filtered
     * - deduplicated
     * - sorted
     */
    val streams: List<StreamLink> = emptyList(),

    /**
     * Best stream for autoplay.
     */
    val defaultStream: StreamLink? = streams.firstOrNull(),

    /**
     * Fallback stream used if playback fails.
     */
    val fallbackStream: StreamLink? =
        streams.firstOrNull(),

    /**
     * Available qualities.
     */
    val qualities: List<Quality> =
        streams
            .map { it.quality }
            .distinct(),

    /**
     * Available hosts.
     */
    val hosts: List<HostType> =
        streams
            .map { it.host }
            .distinct(),

    /**
     * Available subtitle tracks.
     */
    val subtitles: List<Subtitle> =
        streams
            .flatMap { it.subtitles }
            .distinctBy { it.url },

    /**
     * Available audio tracks.
     */
    val audioTracks: List<AudioTrack> =
        streams
            .flatMap { it.audioTracks }
            .distinctBy {
                Triple(
                    it.language,
                    it.codec,
                    it.channels
                )
            }
) {

    /**
     * True if at least one stream exists.
     */
    val isPlayable: Boolean
        get() = streams.isNotEmpty()

    /**
     * Number of streams.
     */
    val streamCount: Int
        get() = streams.size

    /**
     * Adaptive streams.
     */
    val adaptiveStreams: List<StreamLink>
        get() = streams.filter { it.adaptive }

    /**
     * Direct streams.
     */
    val directStreams: List<StreamLink>
        get() = streams.filter { !it.adaptive }

    companion object {

        val EMPTY = FinalStreams()

    }

}