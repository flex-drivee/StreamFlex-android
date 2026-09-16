package com.cinetheta.engine.download

import com.cinetheta.core.network.HttpClient
import com.cinetheta.data.local.download.DownloadStorageManager
import com.cinetheta.domain.models.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI

/**
 * Downloads external subtitle files (.srt, .vtt, .m3u8 VTT playlists) and writes them alongside downloaded video files.
 */
class SubtitleDownloader(
    private val okHttpClient: OkHttpClient = HttpClient.getOkHttpClient()
) {

    suspend fun downloadSubtitles(
        subtitles: List<Subtitle>,
        headers: Map<String, String> = emptyMap(),
        videoFile: File,
        storageManager: DownloadStorageManager
    ) = withContext(Dispatchers.IO) {
        if (subtitles.isEmpty() || !videoFile.exists()) return@withContext

        val downloadedLanguages = mutableSetOf<String>()

        subtitles.forEach { sub ->
            try {
                if (sub.url.isNotBlank()) {
                    val rawText = fetchSubtitleString(sub.url, headers)
                    if (!rawText.isNullOrBlank()) {
                        val isVttPlaylist = rawText.contains("#EXTINF") && (sub.url.contains(".m3u8", ignoreCase = true) || rawText.contains(".vtt"))
                        val finalText = if (isVttPlaylist) {
                            assembleHlsVtt(sub.url, rawText, headers) ?: rawText
                        } else {
                            rawText
                        }

                        if (finalText.isNotBlank()) {
                            val ext = if (sub.url.contains(".vtt", ignoreCase = true) || finalText.startsWith("WEBVTT") || isVttPlaylist) "vtt" else "srt"
                            val cleanLabel = storageManager.sanitizeFilename(sub.label.ifBlank { sub.language }.ifBlank { "en" })
                            
                            // Prevent overwriting identical language subtitles
                            val finalLabel = if (downloadedLanguages.add(cleanLabel.lowercase())) {
                                cleanLabel
                            } else {
                                "${cleanLabel}_${downloadedLanguages.size}"
                            }

                            val subFile = storageManager.resolveSubtitleFile(videoFile, finalLabel, ext)
                            subFile.writeText(finalText, Charsets.UTF_8)
                        }
                    }
                }
            } catch (_: Exception) {
                // Non-fatal if a subtitle download fails
            }
        }
    }

    private fun fetchSubtitleString(url: String, headers: Map<String, String>): String? {
        val req = Request.Builder().url(url)
        headers.forEach { (k, v) -> req.header(k, v) }
        if (!headers.containsKey("User-Agent")) {
            req.header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
        }
        return try {
            okHttpClient.newCall(req.build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun assembleHlsVtt(playlistUrl: String, playlistContent: String, headers: Map<String, String>): String? {
        return try {
            val lines = playlistContent.lines()
            val vttUrls = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    val resolved = try {
                        URI(playlistUrl).resolve(trimmed).toString()
                    } catch (_: Exception) {
                        trimmed
                    }
                    vttUrls.add(resolved)
                }
            }

            if (vttUrls.isEmpty()) return null

            val sb = StringBuilder()
            sb.append("WEBVTT\n\n")

            for ((index, vttUrl) in vttUrls.withIndex()) {
                val segText = fetchSubtitleString(vttUrl, headers) ?: continue
                val segLines = segText.lines()
                for (segLine in segLines) {
                    val trimmed = segLine.trim()
                    // Strip WEBVTT header and metadata from secondary segments
                    if (index > 0 && (trimmed.startsWith("WEBVTT") || trimmed.startsWith("X-TIMESTAMP-MAP"))) {
                        continue
                    }
                    if (index == 0 && (trimmed.startsWith("WEBVTT") || trimmed.startsWith("X-TIMESTAMP-MAP"))) {
                        continue
                    }
                    sb.append(segLine).append("\n")
                }
            }
            sb.toString()
        } catch (_: Exception) {
            null
        }
    }
}
