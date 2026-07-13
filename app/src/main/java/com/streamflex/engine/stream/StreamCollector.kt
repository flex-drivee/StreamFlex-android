package com.streamflex.engine.stream

import com.streamflex.core.utils.StreamLogger
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

        StreamLogger.debug(
            "StreamCollector",
            "Resolving source: ${source.provider} | ${source.hostType}"
        )

        val streams = extractorManager.extract(source)

        StreamLogger.debug(
            "StreamCollector",
            "Extractor returned ${streams.size} stream(s)"
        )

        return streams
    }

    /**
     * Resolve multiple ProviderSources.
     */
    suspend fun collect(
        sources: List<ProviderSource>
    ): List<StreamLink> {

        StreamLogger.info(
            "StreamCollector",
            "Resolving ${sources.size} provider source(s)"
        )

        val streams = mutableListOf<StreamLink>()

        sources.forEachIndexed { index, source ->

            StreamLogger.debug(
                "StreamCollector",
                "Source ${index + 1}/${sources.size}"
            )

            StreamLogger.debug(
                "StreamCollector",
                "Provider: ${source.provider}"
            )

            StreamLogger.debug(
                "StreamCollector",
                "Host: ${source.hostType}"
            )

            StreamLogger.debug(
                "StreamCollector",
                "URL: ${source.url}"
            )

            val extracted = extractorManager.extract(source)

            StreamLogger.debug(
                "StreamCollector",
                "Extractor produced ${extracted.size} stream(s)"
            )

            streams += extracted
        }

        StreamLogger.info(
            "StreamCollector",
            "Collected ${streams.size} total stream(s)"
        )

        return streams
    }
}