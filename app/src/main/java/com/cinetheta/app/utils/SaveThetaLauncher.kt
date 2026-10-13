package com.cinetheta.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cinetheta.domain.models.StreamLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object SaveThetaLauncher {

    const val PACKAGE_NAME = "com.savetheta"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSaveTheta(context: Context, navigateTo: String = "downloads"): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)?.apply {
                putExtra("NAVIGATE_TO", navigateTo)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun launchDownload(
        context: Context,
        stream: StreamLink,
        title: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        onNotInstalled: () -> Unit
    ) {
        try {
            val encodedUrl = URLEncoder.encode(stream.url, "UTF-8")
            var uriString = "savetheta://download?url=$encodedUrl"

            val ref = stream.referer ?: stream.headers.entries.find { it.key.equals("Referer", ignoreCase = true) }?.value
            if (!ref.isNullOrBlank()) {
                uriString += "&referer=${URLEncoder.encode(ref, "UTF-8")}"
            }

            val ua = stream.headers.entries.find { it.key.equals("User-Agent", ignoreCase = true) }?.value
                ?: com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT
            if (!ua.isNullOrBlank()) {
                uriString += "&userAgent=${URLEncoder.encode(ua, "UTF-8")}"
            }

            val epStr = if (seasonNumber != null && episodeNumber != null) {
                " - S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
            } else if (episodeNumber != null) {
                " - E$episodeNumber"
            } else ""
            val fullTitle = "$title$epStr"
            uriString += "&title=${URLEncoder.encode(fullTitle, "UTF-8")}"

            val qualityStr = if (stream.quality != com.cinetheta.domain.models.Quality.UNKNOWN) {
                stream.quality.label
            } else if (stream.name.contains("2160", true) || stream.name.contains("4k", true)) {
                "2160p"
            } else if (stream.name.contains("1080", true)) {
                "1080p"
            } else if (stream.name.contains("720", true)) {
                "720p"
            } else {
                stream.quality.label
            }
            uriString += "&quality=${URLEncoder.encode(qualityStr, "UTF-8")}"

            try {
                val jsonHeaders = JSONObject()
                stream.headers.forEach { (key, value) -> jsonHeaders.put(key, value) }
                if (stream.cookies.isNotEmpty()) {
                    val cookieStr = stream.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    jsonHeaders.put("Cookie", cookieStr)
                }
                uriString += "&headers=${URLEncoder.encode(jsonHeaders.toString(), "UTF-8")}"

                if (stream.subtitles.isNotEmpty()) {
                    val subsJson = JSONArray()
                    stream.subtitles.forEach { sub ->
                        val subObj = JSONObject()
                        subObj.put("language", sub.language)
                        subObj.put("url", sub.url)
                        subObj.put("label", sub.label)
                        subsJson.put(subObj)
                    }
                    uriString += "&subtitles=${URLEncoder.encode(subsJson.toString(), "UTF-8")}"
                }
            } catch (_: Exception) {}

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uriString)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            onNotInstalled()
        }
    }
}
