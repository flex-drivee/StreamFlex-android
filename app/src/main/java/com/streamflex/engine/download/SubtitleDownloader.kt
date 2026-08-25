package com.streamflex.engine.download

import com.streamflex.core.network.HttpClient
import com.streamflex.data.local.download.DownloadStorageManager
import com.streamflex.domain.models.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads external subtitle files (.srt, .vtt) and writes them alongside downloaded video files.
 */
class SubtitleDownloader(
    private val okHttpClient: OkHttpClient = HttpClient.getOkHttpClient()
) {

    suspend fun downloadSubtitles(
        subtitles: List<Subtitle>,
        videoFile: File,
        storageManager: DownloadStorageManager
    ) = withContext(Dispatchers.IO) {
        if (subtitles.isEmpty() || !videoFile.exists()) return@withContext

        subtitles.forEach { sub ->
            try {
                if (sub.url.isNotBlank()) {
                    val req = Request.Builder()
                        .url(sub.url)
                        .header("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
                        .build()

                    okHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val text = resp.body?.string()
                            if (!text.isNullOrBlank()) {
                                val ext = if (sub.url.contains(".vtt", ignoreCase = true) || text.startsWith("WEBVTT")) "vtt" else "srt"
                                val lang = sub.language.ifBlank { "en" }.lowercase().take(3)
                                val subFile = storageManager.resolveSubtitleFile(videoFile, lang, ext)
                                subFile.writeText(text, Charsets.UTF_8)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Non-fatal if a subtitle download fails
            }
        }
    }
}
