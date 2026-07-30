package com.streamflex.engine.stream

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderSource
import com.streamflex.core.utils.StreamLogger

/**
 * Main stream resolution engine.
 *
 * This class coordinates the complete extraction pipeline.
 *
 * ProviderSources
 *      ↓
 * Extractors
 *      ↓
 * Raw StreamLinks
 *      ↓
 * Merge
 *      ↓
 * Remove duplicates
 *      ↓
 * Filter
 *      ↓
 * Sort
 *      ↓
 * Failover
 *      ↓
 * FinalStreams
 */
object StreamEngine {

    /**
     * Resolve every provider source into
     * player-ready streams.
     */
    suspend fun resolve(
        sources: List<ProviderSource>
    ): FinalStreams {

        StreamLogger.info(
            "StreamEngine",
            "Starting stream resolution"
        )

        StreamLogger.debug(
            "StreamEngine",
            "Received ${sources.size} provider source(s)"
        )

        if (sources.isEmpty()) {

            StreamLogger.warn(
                "StreamEngine",
                "No provider sources to resolve"
            )

            return FinalStreams.EMPTY
        }

        sources.forEachIndexed { index, source ->

            StreamLogger.debug(
                "StreamEngine",
                "Source ${index + 1}: ${source.provider} | ${source.hostType} | ${source.url}"
            )
        }

        val collectedStreams =
            StreamCollector.collect(sources)

        StreamLogger.debug(
            "StreamEngine",
            "Collected ${collectedStreams.size} raw stream(s)"
        )

        val finalStreams =
            FinalStreamBuilder.build(
                collectedStreams
            )

        StreamLogger.info(
            "StreamEngine",
            "Finished. Final playable streams: ${finalStreams.streamCount}"
        )

        if (finalStreams.isPlayable) {

            finalStreams.streams.forEachIndexed { index, stream ->

                StreamLogger.debug(
                    "StreamEngine",
                    "Playable ${index + 1}: ${stream.quality} | ${stream.host} | ${stream.url}"
                )

            }

        } else {

            StreamLogger.warn(
                "StreamEngine",
                "No playable streams produced"
            )
        }

        return finalStreams
    }

    /**
     * Convenience overload for one source.
     */
    suspend fun resolve(
        source: ProviderSource
    ): FinalStreams {

        return resolve(
            listOf(source)
        )
    }
}