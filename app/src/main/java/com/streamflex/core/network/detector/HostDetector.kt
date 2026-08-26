package com.streamflex.core.network.detector

import com.streamflex.domain.models.HostType
import com.streamflex.extractors.ExtractorRegistry
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

        val host = try {
            URL(url).host.lowercase()
        } catch (_: Exception) {
            return HostType.UNKNOWN
        }

        // ----------------------------------------------------
        // Stage 1 : Known hosts (Check this first to catch things like Streamtape .mkv links)
        // ----------------------------------------------------

        when {

            host.contains("googlevideo") ||
                    host.contains("googleusercontent") ||
                    host.contains("drive.google.com") ->
                return HostType.GOOGLE_VIDEO

            host.contains("hubcloud") ||
                    host.contains("hubdrive") ||
                    host.contains("hubcdn") ||
                    (host.contains("gamerxyt") && lowerUrl.contains("hubcloud.php")) ->
                return HostType.HUBCLOUD

            host.contains("hblinks") ->
                return HostType.HBLINKS

            host.contains("hdstream4u") ||
                    host.contains("hdstream") ||
                    host.contains("vidhide") ||
                    host.contains("filelions") ->
                return HostType.HDSTREAM4U

            host.contains("hubstream") ->
                return HostType.HUBSTREAM

            host.contains("pixeldrain") || host.contains("pixeldra") ->
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

            host.contains("megaup") ->
                return HostType.UNKNOWN

            // AnimeDekho extractors
            host.contains("abyssplayer") || host.contains("playhydrax") ->
                return HostType.ABYSS

            host.contains("xerver") ->
                return HostType.XERVER

            host.contains("vidmoly") ->
                return HostType.VIDMOLY

            host.contains("rubystm") || host.contains("streamruby") ->
                return HostType.STREAMRUBY

            host.contains("gdmirrorbot") ->
                return HostType.GDMIRRORBOT

            host.contains("cloudy.upns") || host.contains("vidcloud.upns") ->
                return HostType.CLOUDY

            host.contains("turbovidhls") || host.contains("emturbovid") ->
                return HostType.TURBOVID

            host.contains("strmup.to") ->
                return HostType.STREAMUP
        }

        // File locker hosting pages that should not be queued as direct raw video streams
        val isFileLocker = host.contains("primeuploads") ||
                host.contains("pandafiles") ||
                host.contains("katfile") ||
                host.contains("dropgalaxy") ||
                host.contains("uploady") ||
                host.contains("clicknupload") ||
                host.contains("rapidgator") ||
                host.contains("turbobit") ||
                host.contains("nitroflare") ||
                host.contains("ddownload") ||
                host.contains("filehost") ||
                host.contains("usersdrive")

        if (isFileLocker) {
            return HostType.UNKNOWN
        }

        // Check dynamic domains in ExtractorRegistry (from remote registry.json / defaults)
        val registryType = ExtractorRegistry.getHostTypeForUrl(url)
        if (registryType != HostType.UNKNOWN) {
            return registryType
        }

        // ----------------------------------------------------
        // Stage 2 : Direct media
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

        // ----------------------------------------------------
        // Stage 3 : Redirect patterns
        // ----------------------------------------------------

        when {

            lowerUrl.endsWith(".php") && !lowerUrl.contains("gamerxyt.com") ->

                return HostType.REDIRECT

            lowerUrl.contains("/redirect") ->

                return HostType.REDIRECT

            lowerUrl.contains("/download") && !lowerUrl.contains("workers.dev") ->

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