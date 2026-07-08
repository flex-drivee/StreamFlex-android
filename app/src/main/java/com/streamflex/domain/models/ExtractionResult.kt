package com.streamflex.domain.models

/**
 * Result returned by every extractor.
 *
 * An extractor may:
 *
 * • Produce playable streams.
 * • Produce additional ProviderSources.
 * • Produce both.
 * • Produce neither.
 */
data class ExtractionResult(

    /**
     * Fully playable streams.
     */
    val streams: List<StreamLink> = emptyList(),

    /**
     * Additional sources requiring another extractor.
     */
    val sources: List<ProviderSource> = emptyList()

) {

    /**
     * Nothing extracted.
     */
    val isEmpty: Boolean
        get() = streams.isEmpty() && sources.isEmpty()

    /**
     * Playable streams exist.
     */
    val hasStreams: Boolean
        get() = streams.isNotEmpty()

    /**
     * More extraction work remains.
     */
    val hasSources: Boolean
        get() = sources.isNotEmpty()

    /**
     * Merge another ExtractionResult.
     */
    operator fun plus(
        other: ExtractionResult
    ): ExtractionResult {

        return ExtractionResult(

            streams =
                (streams + other.streams)
                    .distinctBy { it.url },

            sources =
                (sources + other.sources)
                    .distinctBy { it.url }

        )
    }

    companion object {

        val EMPTY = ExtractionResult()

        fun streams(
            vararg links: StreamLink
        ) = ExtractionResult(
            streams = links.toList()
        )

        fun sources(
            vararg providerSources: ProviderSource
        ) = ExtractionResult(
            sources = providerSources.toList()
        )
    }
}