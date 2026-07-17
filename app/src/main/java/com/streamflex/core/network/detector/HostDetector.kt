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

        val lowerUrl = url.lowercase()

        // ----------------------------------------------------
        // Stage 1 : Direct media (highest priority)
        // ----------------------------------------------------

        when {

            lowerUrl.contains(".m3u8") ->
                return HostType.M3U8

            lowerUrl.contains(".mpd") ->
                return HostType.DASH

            lowerUrl.endsWith(".mp4") ||
                    lowerUrl.endsWith(".mkv") ||
                    lowerUrl.endsWith(".avi") ||
                    lowerUrl.endsWith(".mov") ||
                    lowerUrl.endsWith(".webm") ->
                return HostType.DIRECT
        }

        val host = try {
            URL(url).host.lowercase()
        } catch (_: Exception) {
            return HostType.UNKNOWN
        }

        // ----------------------------------------------------
        // Stage 2 : Known hosts
        // ----------------------------------------------------

        when {

            host.contains("googlevideo") ||
                    host.contains("googleusercontent") ->
                return HostType.GOOGLE_VIDEO

            host.contains("hubcloud") ->
                return HostType.HUBCLOUD

            host.contains("hubdrive") ->
                return HostType.HUBDRIVE

            host.contains("hubcdn") ->
                return HostType.HUBCDN

            host.contains("hblinks") ->
                return HostType.HBLINKS

            host.contains("hubstream") ->
                return HostType.HUBSTREAM

            host.contains("pixeldrain") ->
                return HostType.PIXELDRAIN

            host.contains("streamtape") ->
                return HostType.STREAMTAPE

            host.contains("mixdrop") ->
                return HostType.MIXDROP

            host.contains("filemoon") ->
                return HostType.FILEMOON

            host.contains("dood") ->
                return HostType.DOOD

            host.contains("vidstack") ->
                return HostType.VIDSTACK
        }

        // ----------------------------------------------------
        // Stage 3 : Redirect patterns
        // ----------------------------------------------------

        when {

            host.contains("gamerxyt") ->

                return HostType.REDIRECT

            lowerUrl.endsWith(".php") ->

                return HostType.REDIRECT

            lowerUrl.contains("/redirect") ->

                return HostType.REDIRECT

            lowerUrl.contains("/download") ->

                return HostType.REDIRECT

            lowerUrl.contains("?go=") ->

                return HostType.REDIRECT

            lowerUrl.contains("?url=") ->

                return HostType.REDIRECT

            lowerUrl.contains("?r=") ->

                return HostType.REDIRECT

            lowerUrl.contains("?id=") ->

                return HostType.REDIRECT
        }

        return HostType.UNKNOWN
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

    fun isRedirect(type: HostType): Boolean {

        return type == HostType.REDIRECT
    }


    /**
     * True if an extractor is required.
     */
    fun requiresExtractor(
        type: HostType
    ): Boolean {

        return when (type) {

            HostType.UNKNOWN,
            HostType.DIRECT,
            HostType.GOOGLE_VIDEO,
            HostType.M3U8,
            HostType.DASH -> false

            else -> true
        }
    }
}