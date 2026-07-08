package com.streamflex.engine.stream

import com.streamflex.domain.models.AudioTrack
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.Subtitle

/**
 * Merges StreamLinks that point to the same stream.
 *
 * This combines metadata gathered from multiple extractors
 * before duplicate removal.
 */
object StreamMerger {

    /**
     * Merge streams that share the same URL.
     */
    fun merge(
        streams: List<StreamLink>
    ): List<StreamLink> {

        if (streams.isEmpty()) {
            return emptyList()
        }

        return streams
            .groupBy { normalize(it.url) }
            .values
            .map(::mergeGroup)
    }

    /**
     * Merge one group of identical URLs.
     */
    private fun mergeGroup(
        group: List<StreamLink>
    ): StreamLink {

        val first = group.first()

        return first.copy(

            headers = mergeHeaders(group),

            cookies = mergeCookies(group),

            subtitles = mergeSubtitles(group),

            audioTracks = mergeAudioTracks(group),

            fileSize = group
                .mapNotNull { it.fileSize }
                .maxOrNull(),

            adaptive = group.any { it.adaptive },

            requiresAuth = group.any { it.requiresAuth }
        )
    }

    /**
     * Merge HTTP headers.
     *
     * Later values override earlier ones.
     */
    private fun mergeHeaders(
        group: List<StreamLink>
    ): Map<String, String> {

        return buildMap {

            group.forEach {

                putAll(it.headers)

            }
        }
    }

    /**
     * Merge cookies.
     */
    private fun mergeCookies(
        group: List<StreamLink>
    ): Map<String, String> {

        return buildMap {

            group.forEach {

                putAll(it.cookies)

            }
        }
    }

    /**
     * Merge subtitles.
     */
    private fun mergeSubtitles(
        group: List<StreamLink>
    ): List<Subtitle> {

        return group

            .flatMap { it.subtitles }

            .distinctBy {

                it.url.lowercase()

            }
    }

    /**
     * Merge audio tracks.
     */
    private fun mergeAudioTracks(
        group: List<StreamLink>
    ): List<AudioTrack> {

        return group

            .flatMap { it.audioTracks }

            .distinctBy {

                Triple(
                    it.language.lowercase(),
                    it.codec,
                    it.channels
                )

            }
    }

    /**
     * Normalize URL for grouping.
     *
     * We intentionally ignore fragments.
     */
    private fun normalize(
        url: String
    ): String {

        return url

            .substringBefore('#')

            .trim()
    }
}