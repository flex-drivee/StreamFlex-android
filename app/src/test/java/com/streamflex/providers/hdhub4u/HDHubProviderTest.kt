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
