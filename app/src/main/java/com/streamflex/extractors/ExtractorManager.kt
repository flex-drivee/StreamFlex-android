package com.streamflex.extractors

import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.redirect.RedirectExtractor
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.googlevideo.GoogleVideoExtractor
import com.streamflex.extractors.hblinks.HBLinksExtractor
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import com.streamflex.extractors.hubdrive.HubDriveExtractor
import java.util.ArrayDeque

/**
 * Central extraction engine.
 *
 * Handles recursive extraction automatically by processing
 * ProviderSources until no more work remains.
 */
object ExtractorManager {

    /**
     * Maximum number of sources processed during one extraction.
     * Prevents infinite redirect loops.
     */
    private const val MAX_DEPTH = 30

    /**
     * Registered extractors.
     */
    private val extractors: List<BaseExtractor> = listOf(

        HubCloudExtractor(),

        HubDriveExtractor(),

        HBLinksExtractor(),

        GoogleVideoExtractor(),

        RedirectExtractor()
        // PixelDrainExtractor()
        // StreamTapeExtractor()
        // FileMoonExtractor()
        // MixDropExtractor()
        // VidStackExtractor()
        // DoodExtractor()
    )

    /**
     * Resolve a ProviderSource into playable streams.
     */
    suspend fun extract(
        source: ProviderSource
    ): List<StreamLink> {

        val queue = ArrayDeque<ProviderSource>()

        val visited = mutableSetOf<String>()

        val streams = mutableListOf<StreamLink>()

        queue.add(source)

        while (
            queue.isNotEmpty() &&
            visited.size < MAX_DEPTH
        ) {

            val current = queue.removeFirst()

            if (!visited.add(current.url)) {
                continue
            }

            val extractor = extractors.firstOrNull {

                it.supports(current)

            } ?: continue

            val result = extractor.extract(current)

            streams += result.streams

            result.sources
                .filter {

                    it.url !in visited

                }
                .forEach(queue::addLast)
        }

        return streams
            .distinctBy { it.url }
    }

    /**
     * Returns true if an extractor exists.
     */
    fun supports(
        source: ProviderSource
    ): Boolean {

        return extractors.any {

            it.supports(source)

        }
    }

    /**
     * Returns the extractor for debugging/testing.
     */
    fun findExtractor(
        source: ProviderSource
    ): BaseExtractor? {

        return extractors.firstOrNull {

            it.supports(source)

        }
    }
}