package com.streamflex.engine.stream

import com.streamflex.domain.models.FinalStreams
import com.streamflex.domain.models.ProviderSource

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

        if (sources.isEmpty()) {
            return FinalStreams.EMPTY
        }

        val collectedStreams =
            StreamCollector.collect(sources)

        return FinalStreamBuilder.build(
            collectedStreams
        )
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