package com.streamflex.app

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadProgress
import com.streamflex.domain.models.download.DownloadStatus
import org.junit.Assert.*
import org.junit.Test

class DownloadDomainTest {

    @Test
    fun testDownloadStatusFlags() {
        assertTrue(DownloadStatus.QUEUED.isActive)
        assertTrue(DownloadStatus.CONNECTING.isActive)
        assertTrue(DownloadStatus.DOWNLOADING.isActive)
        assertFalse(DownloadStatus.PAUSED.isActive)
        assertFalse(DownloadStatus.COMPLETED.isActive)

        assertTrue(DownloadStatus.PAUSED.isPaused)
        assertTrue(DownloadStatus.COMPLETED.isCompleted)
        assertTrue(DownloadStatus.FAILED.isFailed)
    }

    @Test
    fun testDownloadItemProgressAndFormatting() {
        val streamLink = StreamLink(
            name = "HDHub4u • 1080p • [FSL Server]",
            url = "https://fsl.hubcloud.club/file/test.mp4",
            quality = Quality.Q_1080P,
            host = HostType.DIRECT
        )

        val item = DownloadItem(
            id = "test-item-1",
            mediaId = "movie-12345",
            title = "Deadpool & Wolverine",
            year = 2024,
            quality = Quality.Q_1080P,
            streamLink = streamLink,
            downloadedBytes = 1_500_000_000L,
            totalBytes = 3_000_000_000L,
            speedBytesPerSec = 15_000_000L,
            etaSeconds = 100L,
            status = DownloadStatus.DOWNLOADING
        )

        assertEquals(0.5f, item.progress, 0.001f)
        assertEquals(50, item.progressPercent)
        assertEquals("2.8 GB", item.formattedSize) // 3 billion bytes ~ 2.79 GB
        assertEquals("1.40 GB", item.formattedDownloadedSize)
    }

    @Test
    fun testDownloadProgressSpeedAndEtaFormatting() {
        val p1 = DownloadProgress(
            id = "test-p1",
            downloadedBytes = 500_000_000L,
            totalBytes = 1_000_000_000L,
            progress = 0.5f,
            speedBytesPerSec = 5_242_880L, // 5.0 MB/s
            etaSeconds = 125L, // 2m 5s
            status = DownloadStatus.DOWNLOADING
        )

        assertEquals(50, p1.progressPercent)
        assertEquals("5.0 MB/s", p1.speedFormatted)
        assertEquals("2m 5s left", p1.etaFormatted)

        val p2 = DownloadProgress(
            id = "test-p2",
            downloadedBytes = 10_000L,
            totalBytes = 100_000L,
            progress = 0.1f,
            speedBytesPerSec = 512_000L, // 500 KB/s
            etaSeconds = 3665L, // 1h 1m 5s
            status = DownloadStatus.DOWNLOADING
        )

        assertEquals("500 KB/s", p2.speedFormatted)
        assertEquals("1h 1m left", p2.etaFormatted)
    }

    @Test
    fun testFilenameSanitizer() {
        val dirtyTitle = "Spider-Man: Across the Spider-Verse (2023) [4K / 1080p]*?<>|"
        val clean = dirtyTitle.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()

        assertFalse(clean.contains(":"))
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("?"))
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.contains("|"))
    }
}
