package com.streamflex.domain.extractor

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.VideoStream

/**
 * Base contract for every host extractor.
 *
 * Examples:
 * - HubCloud
 * - HubDrive
 * - PixelDrain
 * - StreamTape
 * - FileMoon
 */
interface Extractor {

    /**
     * Extractor display name.
     */
    val name: String

    /**
     * Supported hosts.
     */
    val supportedHosts: Set<HostType>

    /**
     * Whether extractor is enabled.
     */
    val enabled: Boolean
        get() = true

    /**
     * Resolve a provider source into playable streams.
     */
    suspend fun extract(
        source: ProviderSource
    ): List<VideoStream>

}