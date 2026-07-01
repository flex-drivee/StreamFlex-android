package com.streamflex.core.network.detector

import com.streamflex.domain.models.HostType
import okhttp3.Headers

object VideoDetector {

    fun detect(
        url: String,
        headers: Headers
    ): HostType {

        val lowerUrl = url.lowercase()

        val contentType = headers["Content-Type"]
            ?.lowercase()
            ?.substringBefore(";")
            ?: ""

        // ---------- MIME Types ----------

        if (contentType == "application/vnd.apple.mpegurl" ||
            contentType == "application/x-mpegurl") {
            return HostType.M3U8
        }

        if (contentType == "application/dash+xml") {
            return HostType.DASH
        }

        if (contentType.startsWith("video/")) {
            return HostType.DIRECT
        }

        // ---------- Extensions ----------

        when {

            lowerUrl.endsWith(".m3u8") ->
                return HostType.M3U8

            lowerUrl.endsWith(".mpd") ->
                return HostType.DASH

            lowerUrl.endsWith(".mp4") ||
                    lowerUrl.endsWith(".mkv") ||
                    lowerUrl.endsWith(".avi") ||
                    lowerUrl.endsWith(".mov") ||
                    lowerUrl.endsWith(".webm") ->
                return HostType.DIRECT
        }

        // ---------- Hosts ----------

        return when {

            "googlevideo.com" in lowerUrl ||
                    "googleusercontent.com" in lowerUrl ->
                HostType.GOOGLE_VIDEO

            "vidstack" in lowerUrl ->
                HostType.VIDSTACK

            "streamtape" in lowerUrl ->
                HostType.STREAMTAPE

            "pixeldrain" in lowerUrl ->
                HostType.PIXELDRAIN

            "hubcloud" in lowerUrl ->
                HostType.HUBCLOUD

            "hubdrive" in lowerUrl ->
                HostType.HUBDRIVE

            "hblinks" in lowerUrl ->
                HostType.HBLINKS

            "filemoon" in lowerUrl ->
                HostType.FILEMOON

            "mixdrop" in lowerUrl ->
                HostType.MIXDROP

            "dood" in lowerUrl ->
                HostType.DOOD

            else ->
                HostType.UNKNOWN
        }
    }

    fun isAdaptive(type: HostType): Boolean {
        return type == HostType.M3U8 ||
                type == HostType.DASH
    }

    fun isDirect(type: HostType): Boolean {
        return type == HostType.DIRECT ||
                type == HostType.GOOGLE_VIDEO
    }

    fun isHosted(type: HostType): Boolean {
        return when (type) {
            HostType.VIDSTACK,
            HostType.STREAMTAPE,
            HostType.PIXELDRAIN,
            HostType.HUBCLOUD,
            HostType.HUBDRIVE,
            HostType.HBLINKS,
            HostType.FILEMOON,
            HostType.MIXDROP,
            HostType.DOOD -> true

            else -> false
        }
    }
}