package com.streamflex.core.network.detector

import com.streamflex.domain.models.ContentType
import okhttp3.Headers

object ContentDetector {

    /**
     * Detect content type using response headers first,
     * then fallback to URL extension.
     */
    fun detect(
        url: String,
        headers: Headers
    ): ContentType {

        val contentType = headers["Content-Type"]
            ?.lowercase()
            ?.substringBefore(";")
            ?: ""

        return when {

            // ---------- Video ----------

            contentType.startsWith("video/") ->
                ContentType.VIDEO

            contentType == "application/vnd.apple.mpegurl" ->
                ContentType.M3U8

            contentType == "application/x-mpegurl" ->
                ContentType.M3U8

            contentType == "application/dash+xml" ->
                ContentType.DASH

            // ---------- HTML ----------

            contentType == "text/html" ->
                ContentType.HTML

            // ---------- JSON ----------

            contentType.contains("json") ->
                ContentType.JSON

            // ---------- JavaScript ----------

            contentType.contains("javascript") ->
                ContentType.JAVASCRIPT

            // ---------- XML ----------

            contentType.contains("xml") ->
                ContentType.XML

            // ---------- Image ----------

            contentType.startsWith("image/") ->
                ContentType.IMAGE

            // ---------- Subtitle ----------

            contentType.contains("vtt") ->
                ContentType.SUBTITLE

            contentType.contains("srt") ->
                ContentType.SUBTITLE

            // ---------- Binary ----------

            contentType == "application/octet-stream" ->
                detectFromExtension(url)

            else ->
                detectFromExtension(url)
        }
    }

    /**
     * Fallback detection using file extension.
     */
    private fun detectFromExtension(url: String): ContentType {

        val extension = url
            .substringAfterLast('.', "")
            .substringBefore('?')
            .lowercase()

        return when (extension) {

            "m3u8" ->
                ContentType.M3U8

            "mp4", "mkv", "avi", "mov", "webm" ->
                ContentType.VIDEO

            "mpd" ->
                ContentType.DASH

            "json" ->
                ContentType.JSON

            "html", "htm", "php" ->
                ContentType.HTML

            "js" ->
                ContentType.JAVASCRIPT

            "xml" ->
                ContentType.XML

            "jpg", "jpeg", "png", "gif", "webp" ->
                ContentType.IMAGE

            "vtt", "srt", "ass", "ssa" ->
                ContentType.SUBTITLE

            else ->
                ContentType.UNKNOWN
        }
    }

    fun isVideo(type: ContentType): Boolean {
        return type == ContentType.VIDEO ||
                type == ContentType.M3U8 ||
                type == ContentType.DASH
    }

    fun isHtml(type: ContentType): Boolean {
        return type == ContentType.HTML
    }

    fun isJson(type: ContentType): Boolean {
        return type == ContentType.JSON
    }
}