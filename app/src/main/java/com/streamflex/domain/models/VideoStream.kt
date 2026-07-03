package com.streamflex.domain.models

/**
 * Final stream package returned by StreamEngine.
 * Contains one or more playable links plus related metadata.
 */
data class VideoStream(

    /** Display title */
    val title: String,

    /** Movie or episode name */
    val name: String,

    /** Selected provider */
    val provider: String,

    /** Playable links */
    val streams: List<StreamLink>,

    /** Optional poster */
    val poster: String? = null,

    /** Optional description */
    val overview: String? = null,

    /** Release year */
    val year: Int? = null,

    /** Season number (TV only) */
    val season: Int? = null,

    /** Episode number (TV only) */
    val episode: Int? = null,

    /** Whether this is a movie */
    val isMovie: Boolean = true,

    /** Whether subtitles are available */
    val hasSubtitles: Boolean = streams.any { it.subtitles.isNotEmpty() },

    /** Whether multiple audio tracks exist */
    val hasMultipleAudio: Boolean = streams.any { it.audioTracks.size > 1 }
)