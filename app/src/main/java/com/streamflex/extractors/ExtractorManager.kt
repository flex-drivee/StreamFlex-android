package com.streamflex.extractors

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.googlevideo.GoogleVideoExtractor
import com.streamflex.extractors.hblinks.HBLinksExtractor
import com.streamflex.extractors.hubcdn.HubCDNExtractor
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import com.streamflex.extractors.hubdrive.HubDriveExtractor
import com.streamflex.extractors.redirect.RedirectExtractor
import java.util.ArrayDeque

/**
 * Central extraction engine.
 *
 * Providers return ProviderSources.
 * Extractors resolve those ProviderSources into:
 *
 * - StreamLinks
 * - More ProviderSources
 *
 * The manager continues processing until no more
 * ProviderSources remain.
 */
object ExtractorManager {

    /**
     * Prevent infinite redirect loops.
     */
    private const val MAX_SOURCES = 30

    /**
     * Registered extractors.
     */
    private val extractors: List<BaseExtractor> = listOf(

        HubCloudExtractor(),

        HubDriveExtractor(),

        HubCDNExtractor(),

        HBLinksExtractor(),

        RedirectExtractor(),

        GoogleVideoExtractor()

        // Future:
        // PixelDrainExtractor()
        // StreamTapeExtractor()
        // FileMoonExtractor()
        // MixDropExtractor()
        // VidStackExtractor()
        // DoodExtractor()
    )

    /**
     * Fast lookup by HostType.
     */
    private val extractorMap =
        extractors.associateBy { it.hostType }

    /**
     * Resolve a ProviderSource into playable streams.
     */
    suspend fun extract(
        source: ProviderSource
    ): List<StreamLink> {

        val queue = ArrayDeque<ProviderSource>()

        val queued = mutableSetOf<String>()

        val visited = mutableSetOf<String>()

        val streams = mutableListOf<StreamLink>()

        queue.add(source)
        queued.add(source.url)

        while (
            queue.isNotEmpty() &&
            visited.size < MAX_SOURCES
        ) {

            val current = queue.removeFirst()

            if (!visited.add(current.url)) {
                continue
            }

            val extractor =
                extractorMap[current.hostType]
                    ?: continue

            val result = extractor.extract(current)

            streams += result.streams

            result.sources
                .filter {

                    it.url.isNotBlank() &&
                            it.hostType != HostType.UNKNOWN &&
                            it.url !in visited &&
                            queued.add(it.url)

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

        return extractorMap.containsKey(
            source.hostType
        )
    }

    /**
     * Returns extractor for debugging/testing.
     */
    fun findExtractor(
        source: ProviderSource
    ): BaseExtractor? {

        return extractorMap[
            source.hostType
        ]
    }
}