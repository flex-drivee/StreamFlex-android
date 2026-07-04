package com.streamflex.core.network.detector

import com.streamflex.domain.models.HostType
import java.net.URL

/**
 * Detects the hosting service from a URL.
 */
object HostDetector {

    /**
     * Detect host type from URL.
     */
    fun detect(url: String): HostType {

        val host = try {
            URL(url).host.lowercase()
        } catch (_: Exception) {
            return HostType.UNKNOWN
        }

        return when {

            // Direct video
            host.contains("googlevideo") ||
                    host.contains("googleusercontent") ->
                HostType.GOOGLE_VIDEO

            // Hub family
            host.contains("hubcloud") ->
                HostType.HUBCLOUD

            host.contains("hubdrive") ->
                HostType.HUBDRIVE

            host.contains("hubcdn") ->
                HostType.HUBCDN

            host.contains("hblinks") ->
                HostType.HBLINKS

            host.contains("hubstream") ->
                HostType.HUBSTREAM

            // Popular hosts
            host.contains("pixeldrain") ->
                HostType.PIXELDRAIN

            host.contains("streamtape") ->
                HostType.STREAMTAPE

            host.contains("mixdrop") ->
                HostType.MIXDROP

            host.contains("filemoon") ->
                HostType.FILEMOON

            host.contains("dood") ->
                HostType.DOOD

            host.contains("vidstack") ->
                HostType.VIDSTACK

            else ->
                HostType.UNKNOWN
        }
    }

    /**
     * True if this host can usually be played directly.
     */
    fun isDirect(type: HostType): Boolean {
        return when (type) {
            HostType.DIRECT,
            HostType.GOOGLE_VIDEO,
            HostType.M3U8,
            HostType.DASH -> true

            else -> false
        }
    }

    /**
     * True if an extractor is required.
     */
    fun requiresExtractor(type: HostType): Boolean {
        return !isDirect(type) &&
                type != HostType.UNKNOWN
    }
}