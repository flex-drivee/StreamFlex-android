package com.streamflex.app.di

import com.streamflex.extractors.ExtractorManager

/**
 * Dependency module for the extraction system.
 *
 * Currently, ExtractorManager is implemented as a singleton object.
 * This module exposes it so the rest of the application depends on
 * the DI layer instead of directly referencing the implementation.
 *
 * If ExtractorManager is converted into a class in the future,
 * only this module will need to change.
 */
object ExtractorModule {

    /**
     * Shared extraction manager.
     */
    val manager: ExtractorManager
        get() = ExtractorManager
}