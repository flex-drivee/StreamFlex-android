package com.cinetheta.engine.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.cinetheta.core.network.HttpClient
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.domain.models.Subtitle
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
 * Native HLS (.m3u8) Downloader with AES-128 decryption, demuxed audio downloading,
 * multi-track support, and hardware/software container muxing via Android MediaMuxer.
 */
class HlsDownloader(
    private val okHttpClient: OkHttpClient = HttpClient.getOkHttpClient()
) {

    sealed class Result {
        data class Success(
            val file: File,
            val totalBytes: Long,
            val extraSubtitles: List<Subtitle> = emptyList()
        ) : Result()
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
        val key: HlsKey?,
        val isInitSegment: Boolean = false
    )

    private data class HlsAudioTrack(
        val groupId: String,
        val name: String,
        val language: String,
        val isDefault: Boolean,
        val url: String
    )

    private data class MasterPlaylistInfo(
        val videoUrl: String,
        val audioTracks: List<HlsAudioTrack>,
        val subtitles: List<Subtitle>
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

            // 1. Resolve Master Playlist (extract video variant, audio tracks, subtitles)
            val masterInfo = parseMasterPlaylist(streamLink.url, streamLink.headers)
                ?: return@withContext Result.Error(IllegalStateException("Failed to load M3U8 master playlist"))

            // 2. Parse Video Playlist segments
            val videoSegments = parseMediaPlaylist(masterInfo.videoUrl, streamLink.headers)
            if (videoSegments.isEmpty()) {
                return@withContext Result.Error(IllegalStateException("No valid video segments found in M3U8 playlist"))
            }

            // 3. Resolve Demuxed Audio Tracks (if any)
            val selectedAudioTracks = selectAudioTracks(masterInfo.audioTracks)
            val audioSegmentsMap = mutableMapOf<HlsAudioTrack, List<HlsSegment>>()
            for (audioTrack in selectedAudioTracks) {
                val segs = parseMediaPlaylist(audioTrack.url, streamLink.headers)
                if (segs.isNotEmpty()) {
                    audioSegmentsMap[audioTrack] = segs
                }
            }

            // Shared AES-128 key cache across video and audio
            val keyCache = mutableMapOf<String, ByteArray>()

            var totalBytesDownloaded = 0L
            var lastProgressTime = System.currentTimeMillis()
            var lastDownloadedBytes = 0L

            val totalVideoSegs = videoSegments.size
            val totalAudioSegs = audioSegmentsMap.values.sumOf { it.size }
            val totalSegmentsCount = totalVideoSegs + totalAudioSegs

            val progressCallback: suspend (Long, Int) -> Unit = { segmentBytes, currentCompleted ->
                totalBytesDownloaded += segmentBytes
                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= 500L || currentCompleted == totalSegmentsCount) {
                    val timeDeltaSec = (now - lastProgressTime) / 1000.0
                    val bytesDelta = totalBytesDownloaded - lastDownloadedBytes
                    val speed = if (timeDeltaSec > 0) (bytesDelta / timeDeltaSec).toLong() else 0L

                    val estTotalBytes = if (currentCompleted > 0) {
                        ((totalBytesDownloaded.toDouble() / currentCompleted) * totalSegmentsCount).toLong()
                    } else {
                        totalBytesDownloaded
                    }

                    val remaining = (estTotalBytes - totalBytesDownloaded).coerceAtLeast(0L)
                    val eta = if (speed > 0) remaining / speed else 0L

                    lastProgressTime = now
                    lastDownloadedBytes = totalBytesDownloaded
                    onProgress(totalBytesDownloaded, estTotalBytes, speed, eta)
                }
            }

            // Case A: Pure Multiplexed HLS (No separate audio tracks)
            if (audioSegmentsMap.isEmpty()) {
                val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
                var segCounter = 0

                val downloadOk = downloadSegmentsToFile(
                    segments = videoSegments,
                    outputFile = partFile,
                    streamLink = streamLink,
                    keyCache = keyCache,
                    isPaused = isPaused,
                    isCancelled = isCancelled,
                    onSegmentDownloaded = { bytes ->
                        segCounter++
                        progressCallback(bytes, segCounter)
                    }
                )

                if (isCancelled()) {
                    partFile.delete()
                    return@withContext Result.Cancelled
                }
                if (isPaused() || !downloadOk) {
                    return@withContext Result.Paused
                }

                if (targetFile.exists()) targetFile.delete()
                partFile.renameTo(targetFile)
                onProgress(totalBytesDownloaded, totalBytesDownloaded, 0L, 0L)
                return@withContext Result.Success(targetFile, totalBytesDownloaded, masterInfo.subtitles)
            }

            // Case B: Demuxed HLS (Separate Video + Audio playlists, e.g. NetMirror)
            val tempVideoFile = File(targetFile.parentFile, "${targetFile.name}.temp_video.ts")
            val tempAudioFiles = mutableListOf<File>()
            var completedSegmentsCount = 0

            // 1. Download Video stream
            val videoOk = downloadSegmentsToFile(
                segments = videoSegments,
                outputFile = tempVideoFile,
                streamLink = streamLink,
                keyCache = keyCache,
                isPaused = isPaused,
                isCancelled = isCancelled,
                onSegmentDownloaded = { bytes ->
                    completedSegmentsCount++
                    progressCallback(bytes, completedSegmentsCount)
                }
            )

            if (isCancelled()) {
                tempVideoFile.delete()
                return@withContext Result.Cancelled
            }
            if (isPaused() || !videoOk) {
                return@withContext Result.Paused
            }

            // 2. Download each selected Audio stream
            for ((trackIndex, entry) in audioSegmentsMap.entries.withIndex()) {
                val audioFile = File(targetFile.parentFile, "${targetFile.name}.temp_audio_${trackIndex}.ts")
                tempAudioFiles.add(audioFile)

                val audioOk = downloadSegmentsToFile(
                    segments = entry.value,
                    outputFile = audioFile,
                    streamLink = streamLink,
                    keyCache = keyCache,
                    isPaused = isPaused,
                    isCancelled = isCancelled,
                    onSegmentDownloaded = { bytes ->
                        completedSegmentsCount++
                        progressCallback(bytes, completedSegmentsCount)
                    }
                )

                if (isCancelled()) {
                    tempVideoFile.delete()
                    tempAudioFiles.forEach { it.delete() }
                    return@withContext Result.Cancelled
                }
                if (isPaused() || !audioOk) {
                    return@withContext Result.Paused
                }
            }

            // 3. Mux Video + Audio(s) into final compliant MP4 container
            val muxOk = muxVideoAndAudio(tempVideoFile, tempAudioFiles, targetFile)

            if (!muxOk) {
                // If muxing failed, fallback to tempVideoFile (which contains full video in MPEG-TS)
                if (targetFile.exists()) targetFile.delete()
                if (tempVideoFile.exists() && tempVideoFile.length() > 0L) {
                    tempVideoFile.copyTo(targetFile, overwrite = true)
                } else {
                    tempVideoFile.delete()
                    tempAudioFiles.forEach { it.delete() }
                    return@withContext Result.Error(IllegalStateException("MediaMuxer failed to combine audio and video tracks"))
                }
            }

            // Clean up temporary elementary stream files
            tempVideoFile.delete()
            tempAudioFiles.forEach { it.delete() }

            onProgress(targetFile.length(), targetFile.length(), 0L, 0L)
            Result.Success(targetFile, targetFile.length(), masterInfo.subtitles)

        } catch (e: CancellationException) {
            Result.Cancelled
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun downloadSegmentsToFile(
        segments: List<HlsSegment>,
        outputFile: File,
        streamLink: StreamLink,
        keyCache: MutableMap<String, ByteArray>,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean,
        onSegmentDownloaded: suspend (Long) -> Unit
    ): Boolean {
        FileOutputStream(outputFile, true).use { outputStream ->
            for (segment in segments) {
                if (isCancelled() || isPaused()) return false

                val segmentBytes = fetchBytes(segment.url, streamLink.headers)
                    ?: throw IllegalStateException("Failed to fetch segment: ${segment.url}")

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
                onSegmentDownloaded(decryptedBytes.size.toLong())
            }
        }
        return true
    }

    private fun selectAudioTracks(audioTracks: List<HlsAudioTrack>): List<HlsAudioTrack> {
        if (audioTracks.isEmpty()) return emptyList()

        // Prioritize default audio tracks and distinct languages (up to 3 tracks to keep download fast and complete)
        val sorted = audioTracks.sortedWith(
            compareByDescending<HlsAudioTrack> { it.isDefault }
                .thenBy { it.language.lowercase() }
        )

        val selected = mutableListOf<HlsAudioTrack>()
        val seenLangs = mutableSetOf<String>()

        for (track in sorted) {
            val key = track.language.ifBlank { track.name }.lowercase()
            if (seenLangs.add(key)) {
                selected.add(track)
                if (selected.size >= 3) break
            }
        }

        return if (selected.isNotEmpty()) selected else listOf(audioTracks.first())
    }

    private fun parseMasterPlaylist(m3u8Url: String, headers: Map<String, String>): MasterPlaylistInfo? {
        val content = fetchString(m3u8Url, headers) ?: return null

        if (!content.contains("#EXT-X-STREAM-INF")) {
            return MasterPlaylistInfo(
                videoUrl = m3u8Url,
                audioTracks = emptyList(),
                subtitles = emptyList()
            )
        }

        val audioTracks = mutableListOf<HlsAudioTrack>()
        val subtitles = mutableListOf<Subtitle>()
        val videoVariants = mutableListOf<Pair<Long, String>>() // (bandwidth, url)
        var selectedAudioGroupId: String? = null

        val lines = content.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-MEDIA:")) {
                val typeMatch = Regex("""TYPE=([A-Z]+)""").find(line)
                val type = typeMatch?.groupValues?.get(1) ?: ""
                val groupMatch = Regex("""GROUP-ID="?([^",]+)"?""").find(line)
                val groupId = groupMatch?.groupValues?.get(1) ?: ""
                val nameMatch = Regex("""NAME="([^"]+)"""").find(line)
                val name = nameMatch?.groupValues?.get(1) ?: "Audio"
                val langMatch = Regex("""LANGUAGE="([^"]+)"""").find(line)
                val lang = langMatch?.groupValues?.get(1) ?: ""
                val defaultMatch = Regex("""DEFAULT=([A-Z]+)""").find(line)
                val isDefault = defaultMatch?.groupValues?.get(1)?.equals("YES", ignoreCase = true) ?: false
                val uriMatch = Regex("""URI="([^"]+)"""").find(line)
                val uri = uriMatch?.groupValues?.get(1)

                if (uri != null) {
                    val resolved = resolveUrl(m3u8Url, uri)
                    if (type == "AUDIO") {
                        audioTracks.add(
                            HlsAudioTrack(
                                groupId = groupId,
                                name = name,
                                language = lang,
                                isDefault = isDefault,
                                url = resolved
                            )
                        )
                    } else if (type == "SUBTITLES") {
                        subtitles.add(
                            Subtitle(
                                language = lang.ifBlank { "en" },
                                label = name,
                                url = resolved
                            )
                        )
                    }
                }
            } else if (line.startsWith("#EXT-X-STREAM-INF")) {
                val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                val bw = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val audioGroupMatch = Regex("""AUDIO="([^"]+)"""").find(line)
                val audioGrp = audioGroupMatch?.groupValues?.get(1)

                val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                if (nextLine.isNotBlank() && !nextLine.startsWith("#")) {
                    val resolvedUrl = resolveUrl(m3u8Url, nextLine)
                    videoVariants.add(bw to resolvedUrl)
                    if (audioGrp != null && selectedAudioGroupId == null) {
                        selectedAudioGroupId = audioGrp
                    }
                }
            }
        }

        val bestVideo = videoVariants.maxByOrNull { it.first }?.second ?: m3u8Url
        val relevantAudio = if (selectedAudioGroupId != null) {
            audioTracks.filter { it.groupId == selectedAudioGroupId }.ifEmpty { audioTracks }
        } else {
            audioTracks
        }

        return MasterPlaylistInfo(
            videoUrl = bestVideo,
            audioTracks = relevantAudio,
            subtitles = subtitles
        )
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
                trimmed.startsWith("#EXT-X-MAP:") -> {
                    val uriMatch = Regex("""URI="([^"]+)"""").find(trimmed)
                    val mapUri = uriMatch?.groupValues?.get(1)
                    if (mapUri != null) {
                        segments.add(
                            HlsSegment(
                                url = resolveUrl(playlistUrl, mapUri),
                                duration = 0f,
                                sequenceNumber = -1L,
                                key = currentKey,
                                isInitSegment = true
                            )
                        )
                    }
                }

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

    private fun muxVideoAndAudio(
        videoFile: File,
        audioFiles: List<File>,
        outputFile: File
    ): Boolean {
        var muxer: MediaMuxer? = null
        val videoExtractor = MediaExtractor()
        val audioExtractors = mutableListOf<MediaExtractor>()

        try {
            if (outputFile.exists()) outputFile.delete()

            videoExtractor.setDataSource(videoFile.absolutePath)
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    videoExtractor.selectTrack(i)
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                return false
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)

            // Add audio tracks
            val activeAudioTracks = mutableListOf<Pair<MediaExtractor, Int>>()
            for (audioFile in audioFiles) {
                if (!audioFile.exists() || audioFile.length() == 0L) continue
                try {
                    val aExtractor = MediaExtractor()
                    aExtractor.setDataSource(audioFile.absolutePath)
                    var aTrackIndex = -1
                    var aFormat: MediaFormat? = null

                    for (i in 0 until aExtractor.trackCount) {
                        val format = aExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            aTrackIndex = i
                            aFormat = format
                            aExtractor.selectTrack(i)
                            break
                        }
                    }

                    if (aTrackIndex != -1 && aFormat != null) {
                        val muxerAudioTrack = muxer.addTrack(aFormat)
                        audioExtractors.add(aExtractor)
                        activeAudioTracks.add(aExtractor to muxerAudioTrack)
                    } else {
                        aExtractor.release()
                    }
                } catch (_: Exception) {
                    // Non-fatal if a specific audio track cannot be added
                }
            }

            muxer.start()

            class TrackChannel(
                val extractor: MediaExtractor,
                val muxerTrackIndex: Int,
                val isVideo: Boolean,
                var hasStartedWithKeyFrame: Boolean = !isVideo,
                var trackBaseTimeUs: Long = -1L,
                var lastPtsUs: Long = -1L,
                var isDone: Boolean = false
            )

            val channels = mutableListOf<TrackChannel>()
            channels.add(TrackChannel(videoExtractor, muxerVideoTrack, isVideo = true))
            for ((aExtractor, muxerAudioTrack) in activeAudioTracks) {
                channels.add(TrackChannel(aExtractor, muxerAudioTrack, isVideo = false))
            }

            val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (channels.any { !it.isDone }) {
                var minChannel: TrackChannel? = null
                var minTime = Long.MAX_VALUE

                for (ch in channels) {
                    if (ch.isDone) continue

                    // Discard non-keyframes at the start of video to prevent decoder stalling
                    while (!ch.hasStartedWithKeyFrame) {
                        val flags = ch.extractor.sampleFlags
                        if ((flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            ch.hasStartedWithKeyFrame = true
                            break
                        }
                        if (!ch.extractor.advance()) {
                            ch.isDone = true
                            break
                        }
                    }
                    if (ch.isDone) continue

                    val st = ch.extractor.sampleTime
                    if (st < 0) {
                        ch.isDone = true
                        continue
                    }
                    if (st < minTime) {
                        minTime = st
                        minChannel = ch
                    }
                }

                if (minChannel == null) break

                bufferInfo.offset = 0
                bufferInfo.size = minChannel.extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    minChannel.isDone = true
                    continue
                }

                if (minChannel.trackBaseTimeUs < 0) {
                    minChannel.trackBaseTimeUs = minTime
                }

                var pts = (minTime - minChannel.trackBaseTimeUs).coerceAtLeast(0L)
                if (pts <= minChannel.lastPtsUs) {
                    pts = minChannel.lastPtsUs + 1000L
                }
                minChannel.lastPtsUs = pts

                val flags = minChannel.extractor.sampleFlags
                bufferInfo.presentationTimeUs = pts
                bufferInfo.flags = if ((flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                muxer.writeSampleData(minChannel.muxerTrackIndex, buffer, bufferInfo)
                if (!minChannel.extractor.advance()) {
                    minChannel.isDone = true
                }
            }

            muxer.stop()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { videoExtractor.release() } catch (_: Exception) {}
            for (aExtractor in audioExtractors) {
                try { aExtractor.release() } catch (_: Exception) {}
            }
            try { muxer?.release() } catch (_: Exception) {}
        }
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
            req.header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
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
            req.header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
        }
        return try {
            okHttpClient.newCall(req.build()).execute().use { it.body?.bytes() }
        } catch (_: Exception) {
            null
        }
    }
}
