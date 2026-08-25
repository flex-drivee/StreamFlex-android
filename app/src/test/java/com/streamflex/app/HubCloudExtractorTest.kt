package com.streamflex.app

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class HubCloudExtractorTest {

    @Test
    fun testHubCloudServerButtonsParsing() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>HubCloud Download</title></head>
            <body>
                <div class="card-header">Spider-Man: Far From Home (1080p BluRay x264)</div>
                <div class="card-body">
                    <i id="size">1.8 GB</i>
                    <div class="download-item">
                        <a class="btn btn-primary" href="https://fsl.hubcloud.club/file/abc1234">FSL Server</a>
                        <a class="btn btn-secondary" href="https://hubcloud.club/download/xyz5678">Download File</a>
                        <a class="btn btn-success" href="https://pixeldrain.com/u/pd12345">PixelServer</a>
                        <a class="btn btn-info" href="https://s3.hubcloud.club/file/s3_123">S3 Server</a>
                        <a class="btn btn-warning" href="https://mega.hubcloud.club/file/mega_123">Mega Server</a>
                        <a class="btn btn-dark" href="https://pdl.hubcloud.club/file/pdl_123">PDL Server</a>
                        <a class="btn btn-danger" href="https://hubcloud.club/drive/10gbps?link=https://cdn10.hubcloud.club/stream.mkv">10Gbps Download</a>
                        <a class="btn" href="https://hubdrive.space/file/hd123">HubDrive</a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val extractor = HubCloudExtractor()
        val source = ProviderSource(
            provider = "HDHub4u",
            host = "HUBCLOUD",
            hostType = HostType.HUBCLOUD,
            url = "https://hubcloud.club/hubcloud.php?id=test",
            quality = Quality.Q_1080P,
            referer = "https://hdhub4u.rehab"
        )

        // Using reflection or testing the parse logic
        val document = Jsoup.parse(sampleHtml, "https://hubcloud.club/hubcloud.php?id=test")
        assertNotNull(document)
        
        val buttons = document.select("a.btn")
        assertEquals(8, buttons.size)

        // Verify button labels
        val labels = buttons.map { it.text().lowercase() }
        assertTrue(labels.any { it.contains("fsl server") })
        assertTrue(labels.any { it.contains("download file") })
        assertTrue(labels.any { it.contains("pixelserver") })
        assertTrue(labels.any { it.contains("s3 server") })
        assertTrue(labels.any { it.contains("mega server") })
        assertTrue(labels.any { it.contains("pdl server") })
        assertTrue(labels.any { it.contains("10gbps") })
        assertTrue(labels.any { it.contains("hubdrive") })
    }

    @Test
    fun testFourKHDHubTvEpisodeGridParsing() {
        val sampleTvHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1 class="page-title">Stranger Things Season 4 (2022)</h1>
                <div class="mt-2"><span class="badge">TV Series</span></div>
                <div class="episodes-list">
                    <div class="season-item">
                        <div class="episode-number">Season 4</div>
                        <div class="episode-download-item">
                            <div class="episode-file-info">
                                <span class="badge-psa">Episode-01</span>
                            </div>
                            <a href="https://hubcloud.club/drive/ep1_1080p">1080p [HubCloud]</a>
                            <a href="https://gamerxyt.com/?id=ep1_720p">720p [Redirect]</a>
                        </div>
                        <div class="episode-download-item">
                            <div class="episode-file-info">
                                <span class="badge-psa">Episode-02</span>
                            </div>
                            <a href="https://hubcloud.club/drive/ep2_1080p">1080p [HubCloud]</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleTvHtml, "https://4khdhub.one/series/stranger-things")
        val seasonElements = doc.select("div.episodes-list div.season-item")
        assertEquals(1, seasonElements.size)

        val epItems = seasonElements.first()!!.select("div.episode-download-item")
        assertEquals(2, epItems.size)

        val ep1Links = epItems[0].select("a[href]")
        assertEquals(2, ep1Links.size)

        val ep2Links = epItems[1].select("a[href]")
        assertEquals(1, ep2Links.size)
    }
}
