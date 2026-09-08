package com.cinetheta.app.di

import com.cinetheta.extractors.ExtractorManager

/**
 * Dependency module for the extraction system
 */
object ExtractorModule {

    val manager: ExtractorManager
        get() = ExtractorManager
}