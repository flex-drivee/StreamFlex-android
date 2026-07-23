package com.streamflex.app.di

import com.streamflex.extractors.ExtractorManager

/**
 * Dependency module for the extraction system
 */
object ExtractorModule {

    val manager: ExtractorManager
        get() = ExtractorManager
}