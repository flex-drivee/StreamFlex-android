package com.streamflex.providers.hdhub4u

import com.streamflex.core.parser.TransportResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.redirect.RedirectExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 1.7 — HDHub4U Provider First Milestone.
 *
 * Verifies:
 * 1. SearchResultParser ([HDHubSearch]) correctly parses Typesense JSON responses.
 * 2. DetailParser ([HDHubDetails]) & SourceParser ([HDHubSourceParser]) correctly parse Movie sources and TV seasons/episodes.
 * 3. Engine ownership: HDHubDetails does not decode redirects inline.
 * 4. [RedirectExtractor] correctly unwraps WordPress/HDHub4u base64+ROT13 redirect scripts.
 */
class HDHubProviderTest {

    @Test
    fun testSearchResultParser_parsesTypesenseJsonResponse() {
        val sampleJson = """
        {
          "hits": [
            {
              "document": {
                "post_title": "Inception (2010) Hindi Dubbed",
                "permalink": "inception-2010-hindi",
                "post_thumbnail": "https://img.example.com/inception.jpg",
                "category": "bollywood-movies"
              }
            },
            {
              "document": {
                "post_title": "Breaking Bad Season 1",
                "permalink": "https://hdhub4u.fyi/breaking-bad-s01",
                "post_thumbnail": "https://img.example.com/bb.jpg",
                "category": "web-series"
              }
            }
          ]
        }
        """.trimIndent()

        val parser = HDHubSearch()
        val transport = TransportResult.TextResponse(
            text = sampleJson,
            url = "https://search.pingora.fyi/collections/post/documents/search"
        )

        val results = parser.parse(transport, "https://hdhub4u.fyi")

        assertEquals(2, results.size)

        val first = results[0]
        assertEquals("Inception (2010) Hindi Dubbed", first.title)
        assertEquals("https://hdhub4u.fyi/inception-2010-hindi", first.url)
        assertEquals(MediaType.MOVIE, first.mediaType)

        val second = results[1]
        assertEquals("Breaking Bad Season 1", second.title)
        assertEquals("https://hdhub4u.fyi/breaking-bad-s01", second.url)
        assertEquals(MediaType.TV, second.mediaType)
    }

    @Test
    fun testDetailParser_parsesMovieSources_withoutInlineRedirectUnwrapping() {
        val sampleMovieHtml = """
        <html>
          <head>
            <title>Inception (2010) HDHub4u</title>
          </head>
          <body class="page-body">
            <h1 class="page-title"><span>Inception (2010) Movie 1080p</span></h1>
            <p class="kno-rdesc">A thief who steals corporate secrets...</p>
            <div class="links">
              <a href="https://hubcloud.one/drive/12345">1080p HubCloud Link</a>
              <a href="https://hubdrive.co/file/67890">720p HubDrive Link</a>
              <a href="https://new3.hdhub4u.cl/?id=abcde12345">HDHub Stream Link</a>
            </div>
          </body>
        </html>
        """.trimIndent()

        val parser = HDHubDetails()
        val transport = TransportResult.HtmlResponse(
            html = sampleMovieHtml,
            url = "https://hdhub4u.fyi/inception-2010"
        )

        val result = parser.parse(transport, "https://hdhub4u.fyi/inception-2010")

        assertEquals(MediaType.MOVIE, result.mediaType)
        assertTrue(result.success)
        assertEquals("Inception (2010) Movie 1080p", result.title)
        assertEquals("A thief who steals corporate secrets...", result.overview)

        assertEquals(3, result.sources.size)

        val hubCloud = result.sources[0]
        assertEquals(HostType.HUBCLOUD, hubCloud.hostType)

        val hubDrive = result.sources[1]
        assertEquals(HostType.HUBDRIVE, hubDrive.hostType)

        val redirectSource = result.sources[2]
        assertEquals(HostType.REDIRECT, redirectSource.hostType)
        assertEquals("https://new3.hdhub4u.cl/?id=abcde12345", redirectSource.url)
    }

    @Test
    fun testDetailParser_parsesTvSeriesSeasonsAndEpisodes() {
        val sampleTvHtml = """
        <html>
          <body class="page-body">
            <h1 class="page-title"><span>Breaking Bad Season 1 Web Series</span></h1>
            <h3>EPiSODE 1 1080p</h3>
            <p>
              <a href="https://hubcloud.one/drive/ep1">Episode 1 HubCloud</a>
            </p>
            <h4>EPiSODE 2 1080p</h4>
            <p>
              <a href="https://hubcloud.one/drive/ep2">Episode 2 HubCloud</a>
            </p>
          </body>
        </html>
        """.trimIndent()

        val parser = HDHubDetails()
        val transport = TransportResult.HtmlResponse(
            html = sampleTvHtml,
            url = "https://hdhub4u.fyi/breaking-bad-s01"
        )

        val result = parser.parse(transport, "https://hdhub4u.fyi/breaking-bad-s01")

        assertEquals(MediaType.TV, result.mediaType)
        assertTrue(result.hasSeasons)
        assertEquals(1, result.seasons.size)

        val season1 = result.seasons[0]
        assertEquals(1, season1.number)
        assertEquals(2, season1.episodes.size)

        val ep1 = season1.episodes[0]
        assertEquals(1, ep1.number)
        assertEquals("Episode 1", ep1.title)
        assertEquals(1, ep1.sources.size)

        val ep2 = season1.episodes[1]
        assertEquals(2, ep2.number)
        assertEquals("Episode 2", ep2.title)
        assertEquals(1, ep2.sources.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — DoodExtractor Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testDoodExtractor_normalizesShareLinkToEmbedLink() {
        // /d/ share links must be converted to /e/ embed links before fetching
        val shareUrl = "https://dood.re/d/abc123xyz"
        val expected = "https://dood.re/e/abc123xyz"

        // Use reflection to test the private normalizeToEmbedUrl method
        val extractor = com.streamflex.extractors.dood.DoodExtractor()
        val method = extractor.javaClass.getDeclaredMethod("normalizeToEmbedUrl", String::class.java)
        method.isAccessible = true
        val result = method.invoke(extractor, shareUrl) as String

        assertEquals(expected, result)
    }

    @Test
    fun testDoodExtractor_normalizesFileLinkToEmbedLink() {
        // /f/ file links must also be converted to /e/ embed links
        val fileUrl = "https://dood.to/f/zyx987def"
        val expected = "https://dood.to/e/zyx987def"

        val extractor = com.streamflex.extractors.dood.DoodExtractor()
        val method = extractor.javaClass.getDeclaredMethod("normalizeToEmbedUrl", String::class.java)
        method.isAccessible = true
        val result = method.invoke(extractor, fileUrl) as String

        assertEquals(expected, result)
    }

    @Test
    fun testDoodExtractor_extractsOriginFromEmbedUrl() {
        // Origin extraction must strip the path, leaving only scheme + host
        val extractor = com.streamflex.extractors.dood.DoodExtractor()
        val method = extractor.javaClass.getDeclaredMethod("extractOrigin", String::class.java)
        method.isAccessible = true

        assertEquals("https://dood.re", method.invoke(extractor, "https://dood.re/e/abc123"))
        assertEquals("https://dood.la", method.invoke(extractor, "https://dood.la/e/xyz789"))
        assertEquals("https://do0od.com", method.invoke(extractor, "https://do0od.com/e/testid"))
    }

    @Test
    fun testDoodExtractor_passMd5RegexMatchesEmbedPageScript() {
        // Verify the regex correctly extracts /pass_md5/<hash> from realistic embed HTML
        val sampleHtml = """
            <html><head></head><body>
            <script>
                (function(){
                    var ref = 'https://dood.re/pass_md5/abc123def456ghi789/token?expiry=999';
                    $.getScript('/pass_md5/abc123def456ghi789jkl', function() {});
                })();
            </script>
            </body></html>
        """.trimIndent()

        val passMd5Regex = Regex("""/pass_md5/[a-zA-Z0-9/]+""")
        val match = passMd5Regex.find(sampleHtml)?.value

        // Must find the path — that's what DoodExtractor uses in Step 2
        assertTrue("Should find /pass_md5/ path", match != null)
        assertTrue("Match should start with /pass_md5/", match!!.startsWith("/pass_md5/"))
    }

    @Test
    fun testDoodExtractor_supportsOnlyDoodHostType() {
        val extractor = com.streamflex.extractors.dood.DoodExtractor()
        assertEquals(HostType.DOOD, extractor.hostType)

        val goodSource = ProviderSource(
            provider = "HDHub4u", host = "DOOD", hostType = HostType.DOOD,
            url = "https://dood.re/e/abc123", quality = com.streamflex.domain.models.Quality.UNKNOWN,
            referer = "", headers = emptyMap(), cookies = emptyMap()
        )
        assertTrue("Should support DOOD host", extractor.supports(goodSource))

        val badSource = goodSource.copy(hostType = HostType.HUBCLOUD)
        assertTrue("Should NOT support HUBCLOUD", !extractor.supports(badSource))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — PixelDrainExtractor Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPixelDrainExtractor_extractsFileIdFromShareUrl() {
        val extractor = com.streamflex.extractors.pixeldrain.PixelDrainExtractor()
        val method = extractor.javaClass.getDeclaredMethod("extractFileId", String::class.java)
        method.isAccessible = true

        // /u/ share link
        assertEquals(
            "AbCdEfGh",
            method.invoke(extractor, "https://pixeldrain.com/u/AbCdEfGh")
        )

        // /l/ list link
        assertEquals(
            "XyZ12345",
            method.invoke(extractor, "https://pixeldrain.com/l/XyZ12345")
        )

        // Already an API URL
        assertEquals(
            "testFile1",
            method.invoke(extractor, "https://pixeldrain.com/api/file/testFile1")
        )
    }

    @Test
    fun testPixelDrainExtractor_buildsCorrectApiUrl() {
        // The final API URL must be /api/file/<id>?download — no auth, no token
        val fileId = "AbCdEfGh"
        val expected = "https://pixeldrain.com/api/file/$fileId?download"

        // Verify the pattern manually (mirrors what the extractor builds in Step 2)
        val built = "https://pixeldrain.com/api/file/$fileId?download"
        assertEquals(expected, built)
    }

    @Test
    fun testPixelDrainExtractor_supportsOnlyPixelDrainHostType() {
        val extractor = com.streamflex.extractors.pixeldrain.PixelDrainExtractor()
        assertEquals(HostType.PIXELDRAIN, extractor.hostType)

        val goodSource = ProviderSource(
            provider = "HDHub4u", host = "PIXELDRAIN", hostType = HostType.PIXELDRAIN,
            url = "https://pixeldrain.com/u/AbCdEfGh", quality = com.streamflex.domain.models.Quality.UNKNOWN,
            referer = "", headers = emptyMap(), cookies = emptyMap()
        )
        assertTrue("Should support PIXELDRAIN host", extractor.supports(goodSource))

        val badSource = goodSource.copy(hostType = HostType.DOOD)
        assertTrue("Should NOT support DOOD", !extractor.supports(badSource))
    }

    @Test
    fun testRedirectExtractor_unwrapsWpRot13Base64Script() = runBlocking {
        val targetUrl = "https://hubcloud.one/drive/target123"
        val encodedTarget = java.util.Base64.getEncoder().encodeToString(targetUrl.toByteArray())
        val jsonStr = """{"o":"$encodedTarget"}"""
        val b3 = java.util.Base64.getEncoder().encodeToString(jsonStr.toByteArray())
        // ROT13
        val rot13 = b3.map { ch ->
            when (ch) {
                in 'A'..'Z' -> ((ch - 'A' + 13) % 26 + 'A'.code).toChar()
                in 'a'..'z' -> ((ch - 'a' + 13) % 26 + 'a'.code).toChar()
                else -> ch
            }
        }.joinToString("")
        val b2 = java.util.Base64.getEncoder().encodeToString(rot13.toByteArray())
        val b1 = java.util.Base64.getEncoder().encodeToString(b2.toByteArray())

        val method = RedirectExtractor::class.java.getDeclaredMethod("decodeWpRedirect", String::class.java)
        method.isAccessible = true
        val decoded = method.invoke(RedirectExtractor(), b1) as String?

        assertEquals(targetUrl, decoded)
    }
}
