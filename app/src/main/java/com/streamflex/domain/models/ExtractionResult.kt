package com.streamflex.domain.models

/**
 * Result returned by every extractor.
 *
 * An extractor may:
 *
 * 1. Produce playable streams.
 * 2. Produce additional ProviderSources that require
 *    another extractor.
 * 3. Produce both.
 * 4. Produce neither.
 */
data class ExtractionResult(

    /**
     * Fully playable streams.
     */
    val streams: List<StreamLink> = emptyList(),

    /**
     * Additional sources that require further extraction.
     */
    val sources: List<ProviderSource> = emptyList()

) {

    /**
     * True if nothing was extracted.
     */
    val isEmpty: Boolean
        get() = streams.isEmpty() && sources.isEmpty()

    /**
     * True if playable streams exist.
     */
    val hasStreams: Boolean
        get() = streams.isNotEmpty()

    /**
     * True if more extraction work remains.
     */
    val hasSources: Boolean
        get() = sources.isNotEmpty()

    companion object {

        /**
         * Empty result.
         */
        val EMPTY = ExtractionResult()

        /**
         * Convenience factory for streams.
         */
        fun streams(
            vararg links: StreamLink
        ): ExtractionResult {

            return ExtractionResult(
                streams = links.toList()
            )
        }

        /**
         * Convenience factory for sources.
         */
        fun sources(
            vararg providerSources: ProviderSource
        ): ExtractionResult {

            return ExtractionResult(
                sources = providerSources.toList()
            )
        }
    }
}