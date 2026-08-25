package com.streamflex.engine.download

import com.streamflex.core.network.HttpClient
import com.streamflex.domain.models.StreamLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Native HLS (.m3u8) Downloader with AES-128 cryptographic decryption and TS segment reassembly.
 */
class HlsDownloader(
    private val okHttpClient: OkHttpClient = HttpClient.getOkHttpClient()
) {

    sealed class Result {
        data class Success(val file: File, val totalBytes: Long) : Result()
        object Paused : Result()
        object Cancelled : Result()
        data class Error(val throwable: Throwable) : Result()
    }

    private data class HlsKey(
        val method: String,
        val keyUrl: String,
        val ivBytes: ByteArray?
    )

    private data class HlsSegment(
        val url: String,
        val duration: Float,
        val sequenceNumber: Long,
        val key: HlsKey?
    )

    suspend fun download(
        streamLink: StreamLink,
        targetFile: File,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long, eta: Long) -> Unit,
        isPaused: () -> Boolean = { false },
        isCancelled: () -> Boolean = { false }
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (isCancelled()) return@withContext Result.Cancelled
            if (isPaused()) return@withContext Result.Paused

            targetFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val partFile = File(targetFile.parentFile, "${targetFile.name}.part")

            // 1. Fetch Master / Media playlist
            val playlistUrl = resolveMediaPlaylistUrl(streamLink.url, streamLink.headers)
            val segments = parseMediaPlaylist(playlistUrl, streamLink.headers)

            if (segments.isEmpty()) {
                return@withContext Result.Error(IllegalStateException("No valid TS segments found in M3U8 playlist"))
            }

            // Key Cache (URI -> KeyBytes)
            val keyCache = mutableMapOf<String, ByteArray>()

            var downloadedBytes = 0L
            val totalSegments = segments.size
            var lastProgressTime = System.currentTimeMillis()
            var lastDownloadedBytes = 0L

            FileOutputStream(partFile, true).use { outputStream ->
                for ((index, segment) in segments.withIndex()) {
                    if (isCancelled()) {
                        partFile.delete()
                        return@withContext Result.Cancelled
                    }
                    if (isPaused()) {
                        return@withContext Result.Paused
                    }

                    // Download segment bytes
                    val segmentBytes = fetchBytes(segment.url, streamLink.headers)
                        ?: throw IllegalStateException("Failed to fetch segment ${index + 1}/$totalSegments")

                    // Decrypt if AES-128
                    val decryptedBytes = if (segment.key != null && segment.key.method.equals("AES-128", ignoreCase = true)) {
                        val keyBytes = keyCache.getOrPut(segment.key.keyUrl) {
                            fetchBytes(segment.key.keyUrl, streamLink.headers)
                                ?: throw IllegalStateException("Failed to fetch AES key: ${segment.key.keyUrl}")
                        }
                        decryptSegment(segmentBytes, keyBytes, segment.key.ivBytes, segment.sequenceNumber)
                    } else {
                        segmentBytes
                    }

                    outputStream.write(decryptedBytes)
                    downloadedBytes += decryptedBytes.size

                    // Progress reporting
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= 500L || index == totalSegments - 1) {
                        val timeDeltaSec = (now - lastProgressTime) / 1000.0
                        val bytesDelta = downloadedBytes - lastDownloadedBytes
                        val speed = if (timeDeltaSec > 0) (bytesDelta / timeDeltaSec).toLong() else 0L

                        // Estimate total bytes based on progress percentage
                        val estTotalBytes = ((downloadedBytes.toDouble() / (index + 1)) * totalSegments).toLong()
                        val remaining = (estTotalBytes - downloadedBytes).coerceAtLeast(0L)
                        val eta = if (speed > 0) remaining / speed else 0L

                        lastProgressTime = now
                        lastDownloadedBytes = downloadedBytes
                        onProgress(downloadedBytes, estTotalBytes, speed, eta)
                    }
                }
            }

            if (targetFile.exists()) targetFile.delete()
            partFile.renameTo(targetFile)
            onProgress(downloadedBytes, downloadedBytes, 0L, 0L)
            Result.Success(targetFile, downloadedBytes)

        } catch (e: CancellationException) {
            Result.Cancelled
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun resolveMediaPlaylistUrl(m3u8Url: String, headers: Map<String, String>): String {
        val content = fetchString(m3u8Url, headers) ?: return m3u8Url
        if (content.contains("#EXT-X-STREAM-INF")) {
            // Master playlist: find highest bandwidth variant
            val lines = content.lines()
            var highestBandwidth = -1L
            var bestUrl = m3u8Url

            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                    val bw = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                    if (nextLine.isNotBlank() && !nextLine.startsWith("#") && bw > highestBandwidth) {
                        highestBandwidth = bw
                        bestUrl = resolveUrl(m3u8Url, nextLine)
                    }
                }
            }
            return bestUrl
        }
        return m3u8Url
    }

    private fun parseMediaPlaylist(playlistUrl: String, headers: Map<String, String>): List<HlsSegment> {
        val content = fetchString(playlistUrl, headers) ?: return emptyList()
        val segments = mutableListOf<HlsSegment>()
        val lines = content.lines()

        var currentKey: HlsKey? = null
        var currentDuration = 0f
        var sequenceNumber = 0L

        val seqMatch = Regex("""#EXT-X-MEDIA-SEQUENCE:(\d+)""").find(content)
        if (seqMatch != null) {
            sequenceNumber = seqMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("#EXT-X-KEY:") -> {
                    val methodMatch = Regex("""METHOD=([^,]+)""").find(trimmed)
                    val uriMatch = Regex("""URI="([^"]+)"""").find(trimmed)
                    val ivMatch = Regex("""IV=0x([0-9a-fA-F]+)""").find(trimmed)

                    val method = methodMatch?.groupValues?.get(1) ?: "NONE"
                    val keyUri = uriMatch?.groupValues?.get(1)
                    val ivBytes = ivMatch?.groupValues?.get(1)?.let { hexToBytes(it) }

                    if (keyUri != null) {
                        currentKey = HlsKey(method, resolveUrl(playlistUrl, keyUri), ivBytes)
                    } else if (method == "NONE") {
                        currentKey = null
                    }
                }

                trimmed.startsWith("#EXTINF:") -> {
                    val durMatch = Regex("""#EXTINF:([0-9.]+)""").find(trimmed)
                    currentDuration = durMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                }

                !trimmed.startsWith("#") -> {
                    val segUrl = resolveUrl(playlistUrl, trimmed)
                    segments.add(
                        HlsSegment(
                            url = segUrl,
                            duration = currentDuration,
                            sequenceNumber = sequenceNumber++,
                            key = currentKey
                        )
                    )
                }
            }
        }
        return segments
    }

    private fun decryptSegment(
        encryptedBytes: ByteArray,
        keyBytes: ByteArray,
        ivBytes: ByteArray?,
        sequenceNumber: Long
    ): ByteArray {
        val iv = ivBytes ?: ByteBuffer.allocate(16).apply {
            putLong(8, sequenceNumber)
        }.array()

        val keySpec = SecretKeySpec(keyBytes, "AES")
        val paramSpec: AlgorithmParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
        return cipher.doFinal(encryptedBytes)
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            URI(baseUrl).resolve(relativeUrl).toString()
        } catch (_: Exception) {
            relativeUrl
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun fetchString(url: String, headers: Map<String, String>): String? {
        val req = Request.Builder().url(url)
        headers.forEach { (k, v) -> req.header(k, v) }
        if (!headers.containsKey("User-Agent")) {
            req.header("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
        }
        return try {
            okHttpClient.newCall(req.build()).execute().use { it.body?.string() }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchBytes(url: String, headers: Map<String, String>): ByteArray? {
        val req = Request.Builder().url(url)
        headers.forEach { (k, v) -> req.header(k, v) }
        if (!headers.containsKey("User-Agent")) {
            req.header("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
        }
        return try {
            okHttpClient.newCall(req.build()).execute().use { it.body?.bytes() }
        } catch (_: Exception) {
            null
        }
    }
}
