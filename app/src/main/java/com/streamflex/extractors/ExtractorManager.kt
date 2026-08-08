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
import com.streamflex.extractors.dood.DoodExtractor
import com.streamflex.extractors.pixeldrain.PixelDrainExtractor
import com.streamflex.extractors.netmirror.NetMirrorExtractor
import com.streamflex.core.utils.StreamLogger
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

        GoogleVideoExtractor(),

        // Phase 3 — HDHub4U complete extractors
        DoodExtractor(),

        PixelDrainExtractor(),

        // Phase 3 — NetMirror complete extractors
        NetMirrorExtractor()

        // Future (Phase 3+ providers):
        // StreamTapeExtractor()  ← OTTMirror
        // VidStackExtractor()    ← VegaMovies
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

        StreamLogger.info(
            "ExtractorManager",
            "Starting extraction pipeline"
        )

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

            StreamLogger.debug(
                "ExtractorManager",
                "Processing ${current.hostType} -> ${current.url}"
            )

            if (!visited.add(current.url)) {

                StreamLogger.debug(
                    "ExtractorManager",
                    "Already visited. Skipping."
                )

                continue
            }

            val extractor =
                extractorMap[current.hostType]

            if (extractor == null) {

                StreamLogger.warn(
                    "ExtractorManager",
                    "No extractor registered for ${current.hostType}"
                )

                continue
            }

            StreamLogger.debug(
                "ExtractorManager",
                "Using ${extractor.javaClass.simpleName}"
            )

            try {

                val result = extractor.extract(current)

                StreamLogger.debug(
                    "ExtractorManager",
                    "Streams: ${result.streams.size}, Next Sources: ${result.sources.size}"
                )

                streams += result.streams

                result.sources
                    .filter {

                        it.url.isNotBlank() &&
                                it.hostType != HostType.UNKNOWN &&
                                it.url !in visited &&
                                queued.add(it.url)

                    }
                    .forEach {

                        StreamLogger.debug(
                            "ExtractorManager",
                            "Queued next source: ${it.hostType}"
                        )

                        queue.addLast(it)
                    }

            } catch (e: Exception) {

                StreamLogger.error(
                    "ExtractorManager",
                    "Extractor ${extractor.javaClass.simpleName} failed",
                    e
                )
            }
        }

        StreamLogger.info(
            "ExtractorManager",
            "Extraction finished. Total streams: ${streams.size}"
        )

        return streams.distinctBy { it.url }
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