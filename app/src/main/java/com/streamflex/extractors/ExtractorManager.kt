package com.streamflex.extractors

import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.googlevideo.GoogleVideoExtractor
import com.streamflex.extractors.hblinks.HBLinksExtractor
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import com.streamflex.extractors.hubdrive.HubDriveExtractor

/**
 * Central registry for all StreamFlex extractors.
 *
 * Providers never talk directly to extractors.
 * They simply pass a ProviderSource to this manager.
 */
object ExtractorManager {

    /**
     * Registered extractors.
     */
    private val extractors: List<BaseExtractor> = listOf(

        HubCloudExtractor(),

        HubDriveExtractor(),

        HBLinksExtractor(),

        GoogleVideoExtractor()

        // Future extractors:
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

        val extractor = extractors.firstOrNull {

            it.supports(source)

        } ?: return emptyList()

        return extractor.extract(source)
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
}