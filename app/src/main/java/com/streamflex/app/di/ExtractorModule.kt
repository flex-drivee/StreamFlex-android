package com.streamflex.app.di

import com.streamflex.extractors.ExtractorManager
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.googlevideo.GoogleVideoExtractor
import com.streamflex.extractors.hblinks.HBLinksExtractor
import com.streamflex.extractors.hubcdn.HubCDNExtractor
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import com.streamflex.extractors.hubdrive.HubDriveExtractor
import com.streamflex.extractors.redirect.RedirectExtractor

/**
 * Builds every extractor used by StreamFlex.
 *
 * This is the only place where extractors
 * are instantiated.
 */
object ExtractorModule {

    /**
     * Registered extractors.
     *
     * Order matters.
     */
    val extractors: List<BaseExtractor> by lazy {

        listOf(

            HubCloudExtractor(),

            HubDriveExtractor(),

            HubCDNExtractor(),

            HBLinksExtractor(),

            RedirectExtractor(),

            GoogleVideoExtractor()

        )
    }

    /**
     * Shared extraction manager.
     */
    val manager by lazy {

        ExtractorManager(
            extractors = extractors
        )
    }
}