package com.streamflex.engine.stream

import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.ExtractorManager

/**
 * Collects playable streams from ProviderSources.
 *
 * This class is intentionally simple.
 *
 * It does NOT:
 * - sort streams
 * - remove duplicates
 * - filter streams
 * - rank quality
 *
 * It only asks ExtractorManager to resolve every source.
 */
object StreamCollector {

    private val extractorManager = ExtractorManager


    /**
     * Resolve a single ProviderSource.
     */
    suspend fun collect(
        source: ProviderSource
    ): List<StreamLink> {

        return extractorManager.extract(source)
    }

    /**
     * Resolve multiple ProviderSources.
     */
    suspend fun collect(
        sources: List<ProviderSource>
    ): List<StreamLink> {

        val streams = mutableListOf<StreamLink>()

        for (source in sources) {

            streams += extractorManager.extract(source)

        }

        return streams
    }
}