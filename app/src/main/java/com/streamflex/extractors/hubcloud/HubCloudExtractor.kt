package com.streamflex.extractors.hubcloud

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.Quality
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.network.detector.ContentTypeDetector
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorUtils
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.core.utils.StreamLogger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.network.NetworkResult
import java.net.URL

/**
 * Enhanced Extractor for HubCloud (Phase 1.7 / CloudStream 4KHDHub parity).
 *
 * Scrapes all streaming & download buttons on HubCloud gateway pages:
 * - FSL Server / FSLv2
 * - BuzzServer (resolving HX-Redirect headers)
 * - S3 Server
 * - PixelDrain / PixelServer (transformed to direct API stream endpoints)
 * - Mega Server / PDL Server
 * - 10Gbps Download
 * - Direct video files (.m3u8, .mp4, .mkv, Google Drive)
 * - Sub-extractors (HubDrive, HubCDN, MixDrop, StreamTape, etc.)
 */
class HubCloudExtractor : BaseExtractor() {

    override val hostType = HostType.HUBCLOUD

    companion object {
        private const val TAG = "HubCloudExtractor"

        private val SKIP_PATTERNS = listOf(
            "facebook", "twitter", "telegram", "discord", "imdb", "instagram",
            "youtube.com", "google.com/search", "/tg/", "tg://",
            "/category/", "/tag/", "/page/", "/sign", "/login", "/register",
            "/contact", "/about", "/privacy", "/terms", "/admin", "/drive/admin",
            "hubcloud.fans", "hubcloud.foo", "sample-page",
            "javascript:", "#"
        )
    }

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {
        StreamLogger.info(TAG, "Extracting HubCloud page: ${source.url}")

        if (!supports(source)) {
            StreamLogger.warn(TAG, "Unsupported source: ${source.hostType}")
            return emptyResult()
        }

        var document = ExtractorHelper.fetchDocument(source.url, source.headers)
        var currentUrl = source.url

        // Intermediate step handling (HubCloud timer / gateway page):
        if (!currentUrl.contains("hubcloud.php", ignoreCase = true)) {
            val downloadBtnHref = document.selectFirst("#download, a.btn, a[class*=btn]")?.attr("href")?.takeIf { 
                it.isNotBlank() && !it.startsWith("javascript:") && it != "#" 
            }
            val scriptUrlMatch = Regex("""(?:var\s+url\s*=\s*|location(?:\.href)?\s*=\s*)['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
                .find(document.html())?.groups?.get(1)?.value
            val nextHref = downloadBtnHref ?: scriptUrlMatch

            if (!nextHref.isNullOrBlank()) {
                val absoluteNext = if (nextHref.startsWith("http")) nextHref
                else {
                    val base = try {
                        val u = URL(currentUrl)
                        "${u.protocol}://${u.host}"
                    } catch (_: Exception) { currentUrl }
                    "$base/${nextHref.trimStart('/')}"
                }
                
                val lowerNext = absoluteNext.lowercase()
                if (SKIP_PATTERNS.none { lowerNext.contains(it) }) {
                    StreamLogger.info(TAG, "Following intermediate HubCloud step: $absoluteNext")
                    val nextHeaders = source.headers.toMutableMap().apply { 
                        put("Referer", currentUrl)
                        put("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
                    }
                    document = ExtractorHelper.fetchDocument(absoluteNext, nextHeaders)
                    currentUrl = absoluteNext
                }
            }
        }

        return parseHubCloud(source.copy(url = currentUrl), document)
    }

    private suspend fun parseHubCloud(
        source: ProviderSource,
        document: org.jsoup.nodes.Document
    ): ExtractionResult {
        val streams = mutableListOf<StreamLink>()
        val pendingSources = mutableListOf<ProviderSource>()
        val visitedUrls = mutableSetOf<String>()

        val currentUrl = source.url
        val baseHost = try {
            val u = URL(currentUrl)
            "${u.protocol}://${u.host}"
        } catch (_: Exception) { currentUrl }

        fun toAbsolute(urlStr: String): String {
            val trimmed = urlStr.trim()
            if (trimmed.startsWith("http")) return trimmed
            if (trimmed.startsWith("//")) return "https:$trimmed"
            if (trimmed.startsWith("/") || (!trimmed.startsWith("javascript:") && trimmed != "#")) {
                return "$baseHost/${trimmed.trimStart('/')}"
            }
            return ""
        }

        // Header info: resolution, codec, file size
        val headerElement = document.selectFirst("div.card-header")
        val headerText = headerElement?.text()?.trim() ?: ""
        val sizeElement = document.selectFirst("i#size, span#size, .badge")
        val sizeText = sizeElement?.text()?.trim() ?: ""
        
        val detectedQuality = if (headerText.isNotBlank()) {
            QualityDetector.detect(headerText)
        } else {
            source.quality.takeIf { it != Quality.UNKNOWN } ?: QualityDetector.detect(source.url)
        }

        val sizeSuffix = if (sizeText.isNotBlank()) " [$sizeText]" else ""

        // Process all download buttons & links
        val buttonElements = document.select("a.btn, a[class*=btn], button.btn, div.download-item a, div.entry-content a[href], a[href]")

        for (el in buttonElements) {
            val rawHref = el.absUrl("href").takeIf { it.isNotBlank() } ?: el.attr("href")
            val absUrl = toAbsolute(rawHref)
            if (absUrl.isBlank() || !visitedUrls.add(absUrl)) continue

            val lowerUrl = absUrl.lowercase()
            if (SKIP_PATTERNS.any { lowerUrl.contains(it) }) continue

            // Skip bare domain homepages
            val path = try { URL(absUrl).path } catch (_: Exception) { "" }
            if (path.isEmpty() || path == "/" || path == "/#") continue

            val text = (el.text() + " " + el.attr("title")).trim()
            val label = text.lowercase()

            val safeUrl = absUrl.replace(" ", "%20")
                .replace("[", "%5B")
                .replace("]", "%5D")

            val baseHeaders = mapOf(
                "User-Agent" to com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT,
                "Accept" to "*/*"
            )

            val lowerSafeUrl = safeUrl.lowercase()
            val isZipOrArchive = lowerSafeUrl.endsWith(".zip") || lowerSafeUrl.contains(".zip?") ||
                                 lowerSafeUrl.endsWith(".rar") || lowerSafeUrl.contains(".rar?") ||
                                 lowerSafeUrl.endsWith(".7z") || lowerSafeUrl.contains(".7z?") ||
                                 lowerSafeUrl.endsWith(".tar") || lowerSafeUrl.contains(".tar?")

            if (isZipOrArchive) {
                StreamLogger.info(TAG, "Skipping non-video archive: $safeUrl")
                continue
            }

            val isIntermediateHub = (lowerSafeUrl.contains("hubcloud.php") && (lowerSafeUrl.contains("id=") || lowerSafeUrl.contains("token="))) ||
                                    (lowerSafeUrl.contains("/drive/") && !lowerSafeUrl.endsWith("/drive/") && !lowerSafeUrl.contains("/admin")) ||
                                    (lowerSafeUrl.contains("/file/") && !lowerSafeUrl.endsWith("/file/")) ||
                                    (lowerSafeUrl.contains("greenmountmotors.com") && lowerSafeUrl.contains("?id=")) ||
                                    (lowerSafeUrl.contains("gdflix.") && lowerSafeUrl.contains("?id="))

            when {
                // 1. FSL Server / FSLv2
                label.contains("fsl server") || label.contains("fslv2") || label.contains("fsl") -> {
                    if (isIntermediateHub && !safeUrl.contains("cdn.") && !safeUrl.endsWith(".mp4") && !safeUrl.endsWith(".mkv")) {
                        StreamLogger.info(TAG, "Forwarding intermediate FSL page: $safeUrl")
                        pendingSources += source.copy(url = safeUrl, hostType = HostType.HUBCLOUD, quality = detectedQuality)
                    } else {
                        val serverName = if (label.contains("fslv2")) "[FSLv2]" else "[FSL Server]"
                        StreamLogger.info(TAG, "Found $serverName: $safeUrl")
                        streams += buildStreamLink(
                            source = source,
                            url = safeUrl,
                            quality = detectedQuality,
                            serverLabel = serverName + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 2. BuzzServer (Resolve HX-Redirect)
                label.contains("buzzserver") || label.contains("buzz") -> {
                    StreamLogger.info(TAG, "Found BuzzServer link: $absUrl, resolving redirect...")
                    val buzzStream = resolveBuzzServer(absUrl, currentUrl, source, detectedQuality, sizeSuffix)
                    if (buzzStream != null) {
                        streams += buzzStream
                    }
                }

                // 3. PixelDrain / PixelServer
                label.contains("pixeldra") || label.contains("pixelserver") || label.contains("pixel server") || lowerUrl.contains("pixeldrain.com") -> {
                    val fileId = absUrl.substringAfterLast("/").substringBefore("?")
                    val directPixelUrl = if (absUrl.contains("/api/file/")) absUrl else "https://pixeldrain.com/api/file/$fileId?download"
                    StreamLogger.info(TAG, "Found PixelDrain: $directPixelUrl")
                    streams += buildStreamLink(
                        source = source,
                        url = directPixelUrl,
                        quality = detectedQuality,
                        serverLabel = "Pixeldrain" + sizeSuffix,
                        headers = baseHeaders
                    )
                }

                // 4. S3 Server
                label.contains("s3 server") || label.contains("s3") -> {
                    if (isIntermediateHub && !safeUrl.contains("s3.") && !safeUrl.endsWith(".mp4") && !safeUrl.endsWith(".mkv")) {
                        pendingSources += source.copy(url = safeUrl, hostType = HostType.HUBCLOUD, quality = detectedQuality)
                    } else {
                        StreamLogger.info(TAG, "Found S3 Server: $safeUrl")
                        streams += buildStreamLink(
                            source = source,
                            url = safeUrl,
                            quality = detectedQuality,
                            serverLabel = "[S3 Server]" + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 5. Mega Server
                label.contains("mega server") || label.contains("mega") -> {
                    if (isIntermediateHub && !safeUrl.contains("mega.") && !safeUrl.endsWith(".mp4") && !safeUrl.endsWith(".mkv")) {
                        pendingSources += source.copy(url = safeUrl, hostType = HostType.HUBCLOUD, quality = detectedQuality)
                    } else {
                        StreamLogger.info(TAG, "Found Mega Server: $safeUrl")
                        streams += buildStreamLink(
                            source = source,
                            url = safeUrl,
                            quality = detectedQuality,
                            serverLabel = "[Mega Server]" + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 6. PDL Server
                label.contains("pdl server") || label.contains("pdl") -> {
                    if (isIntermediateHub && !safeUrl.endsWith(".mp4") && !safeUrl.endsWith(".mkv")) {
                        pendingSources += source.copy(url = safeUrl, hostType = HostType.HUBCLOUD, quality = detectedQuality)
                    } else {
                        StreamLogger.info(TAG, "Found PDL Server: $safeUrl")
                        streams += buildStreamLink(
                            source = source,
                            url = safeUrl,
                            quality = detectedQuality,
                            serverLabel = "[PDL Server]" + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 7. 10Gbps Download
                label.contains("10gbps") -> {
                    val direct10Gbps = if (absUrl.contains("link=")) absUrl.substringAfter("link=") else safeUrl
                    if (isIntermediateHub && !direct10Gbps.endsWith(".mp4") && !direct10Gbps.endsWith(".mkv")) {
                        pendingSources += source.copy(url = direct10Gbps, hostType = HostType.HUBCLOUD, quality = detectedQuality)
                    } else {
                        StreamLogger.info(TAG, "Found 10Gbps Download: $direct10Gbps")
                        streams += buildStreamLink(
                            source = source,
                            url = direct10Gbps,
                            quality = detectedQuality,
                            serverLabel = "10Gbps [Download]" + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 8. Download File / Fast Download / Generate Link
                label.contains("download file") || label.contains("fast cloud") || label.contains("direct download") || label.contains("generate link") -> {
                    if (isIntermediateHub) {
                        StreamLogger.info(TAG, "Forwarding intermediate gateway page: $safeUrl")
                        pendingSources += source.copy(
                            url = safeUrl,
                            hostType = HostType.HUBCLOUD,
                            quality = detectedQuality
                        )
                    } else {
                        StreamLogger.info(TAG, "Found Direct Download: $safeUrl")
                        streams += buildStreamLink(
                            source = source,
                            url = safeUrl,
                            quality = detectedQuality,
                            serverLabel = "[Download]" + sizeSuffix,
                            headers = baseHeaders
                        )
                    }
                }

                // 9. Google Drive direct links
                lowerUrl.contains("drive.google.com/uc") || lowerUrl.contains("docs.google.com/uc") -> {
                    StreamLogger.info(TAG, "Found Google Drive direct stream: $safeUrl")
                    streams += buildStreamLink(
                        source = source,
                        url = safeUrl,
                        quality = detectedQuality,
                        serverLabel = "Google Drive" + sizeSuffix,
                        headers = baseHeaders
                    )
                }

                // 10. Direct video URLs
                lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.contains(".m3u8") -> {
                    StreamLogger.info(TAG, "Found direct video URL: $safeUrl")
                    streams += buildStreamLink(
                        source = source,
                        url = safeUrl,
                        quality = detectedQuality,
                        serverLabel = "Direct Video" + sizeSuffix,
                        headers = baseHeaders
                    )
                }

                // 11. Other extractors / forwardable hosts
                else -> {
                    val type = HostDetector.detect(absUrl)
                    if (type != HostType.UNKNOWN && type != HostType.HUBCLOUD) {
                        StreamLogger.info(TAG, "Forwarding to extractor $type: $absUrl")
                        pendingSources += source.copy(
                            url = absUrl,
                            hostType = type,
                            quality = detectedQuality
                        )
                    } else if (isIntermediateHub && absUrl != source.url) {
                        StreamLogger.info(TAG, "Forwarding sub-hubcloud step: $absUrl")
                        pendingSources += source.copy(
                            url = absUrl,
                            hostType = HostType.HUBCLOUD,
                            quality = detectedQuality
                        )
                    }
                }
            }
        }

        // Also check JavaScript for redirects / direct streams (e.g. HubCDN / InventoryIdea / Googleusercontent)
        val scriptHtml = document.select("script").joinToString("\n") { it.data() }
        if (scriptHtml.isNotBlank()) {
            // 1. Script reurl / redirect variables
            val reurlMatches = Regex("""(?:var\s+reurl|reurl|var\s+url|location(?:\.href)?)\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
                .findAll(scriptHtml)
            for (m in reurlMatches) {
                val scriptUrl = m.groups[1]?.value?.trim() ?: continue
                if (scriptUrl.contains("?r=")) {
                    val b64Part = scriptUrl.substringAfter("?r=")
                    val decoded = decodeBase64Safe(b64Part)
                    if (!decoded.isNullOrBlank()) {
                        if (decoded.contains("link=")) {
                            val directLink = decoded.substringAfter("link=").trim()
                            if (directLink.startsWith("http") && visitedUrls.add(directLink)) {
                                StreamLogger.info(TAG, "Found decoded Google Video from script: $directLink")
                                streams += buildStreamLink(
                                    source = source,
                                    url = directLink,
                                    quality = detectedQuality,
                                    serverLabel = "Google Video$sizeSuffix",
                                    headers = emptyMap()
                                )
                            }
                        } else if (decoded.startsWith("http") && visitedUrls.add(decoded)) {
                            pendingSources += source.copy(url = decoded, hostType = HostDetector.detect(decoded), quality = detectedQuality)
                        }
                    }
                } else if (scriptUrl.startsWith("http") && visitedUrls.add(scriptUrl)) {
                    val type = HostDetector.detect(scriptUrl)
                    if (HostDetector.isDirect(type)) {
                        streams += buildStreamLink(
                            source = source,
                            url = scriptUrl,
                            quality = detectedQuality,
                            serverLabel = "Direct Video$sizeSuffix",
                            headers = emptyMap()
                        )
                    } else if (type != HostType.UNKNOWN && type != HostType.HUBCLOUD) {
                        pendingSources += source.copy(url = scriptUrl, hostType = type, quality = detectedQuality)
                    }
                }
            }

            // 2. Direct googleusercontent stream links in scripts
            val gVideoRegex = Regex("""https://video-downloads\.googleusercontent\.com/[^\s"'<>\\]+""")
            for (m in gVideoRegex.findAll(scriptHtml)) {
                val gUrl = m.value.trim()
                if (visitedUrls.add(gUrl)) {
                    StreamLogger.info(TAG, "Found direct Google Video URL in script: $gUrl")
                    streams += buildStreamLink(
                        source = source,
                        url = gUrl,
                        quality = detectedQuality,
                        serverLabel = "Google Video$sizeSuffix",
                        headers = emptyMap()
                    )
                }
            }
        }

        // Also check video & iframe tags
        document.select("video source[src], iframe[src]").forEach { el ->
            val src = toAbsolute(el.absUrl("src").takeIf { it.isNotBlank() } ?: el.attr("src"))
            if (src.isNotBlank() && visitedUrls.add(src)) {
                val lowerSrc = src.lowercase()
                if (SKIP_PATTERNS.none { lowerSrc.contains(it) }) {
                    val safeSrc = src.replace(" ", "%20").replace("[", "%5B").replace("]", "%5D")
                    val type = HostDetector.detect(src)
                    if (HostDetector.isDirect(type)) {
                        streams += buildStreamLink(
                            source = source,
                            url = safeSrc,
                            quality = detectedQuality,
                            serverLabel = "[Stream]",
                            headers = mapOf("Referer" to currentUrl)
                        )
                    } else if (type != HostType.UNKNOWN && type != HostType.HUBCLOUD) {
                        pendingSources += source.copy(url = src, hostType = type, quality = detectedQuality)
                    }
                }
            }
        }

        StreamLogger.info(TAG, "Extracted ${streams.size} stream(s) and ${pendingSources.size} pending source(s)")

        return result(
            streams = streams.distinctBy { it.url },
            sources = pendingSources.distinctBy { it.url }
        )
    }

    private fun decodeBase64Safe(input: String): String? {
        return try {
            val pad = (4 - (input.length % 4)) % 4
            val padded = if (pad > 0) input + "=".repeat(pad) else input
            val bytes = try {
                android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
            } catch (_: Exception) {
                java.util.Base64.getDecoder().decode(padded)
            }
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveBuzzServer(
        buzzUrl: String,
        referer: String,
        source: ProviderSource,
        quality: Quality,
        sizeSuffix: String
    ): StreamLink? {
        return try {
            val downloadUrl = if (buzzUrl.endsWith("/download")) buzzUrl else "$buzzUrl/download"
            val request = RequestBuilder()
                .url(downloadUrl)
                .referer(buzzUrl)
                .header("HX-Request", "true")
                .header("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
                .followRedirects(false)
                .build()

            val targetUrl = when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val hxRedirect = response.data.headers.entries.find { it.key.equals("hx-redirect", ignoreCase = true) }?.value?.firstOrNull()
                        ?: response.data.headers.entries.find { it.key.equals("location", ignoreCase = true) }?.value?.firstOrNull()
                    hxRedirect ?: if (response.data.url != downloadUrl) response.data.url else null
                }
                else -> null
            }

            val finalTarget = targetUrl
                ?: (if (buzzUrl.contains("/f/")) buzzUrl.replace("/f/", "/d/") else buzzUrl)

            val safeTarget = finalTarget.replace(" ", "%20").replace("[", "%5B").replace("]", "%5D")
            buildStreamLink(
                source = source,
                url = safeTarget,
                quality = quality,
                serverLabel = "[BuzzServer]$sizeSuffix",
                headers = mapOf("Referer" to buzzUrl)
            )
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Failed to resolve BuzzServer redirect: ${e.message}")
            val fallback = if (buzzUrl.contains("/f/")) buzzUrl.replace("/f/", "/d/") else buzzUrl
            buildStreamLink(
                source = source,
                url = fallback.replace(" ", "%20"),
                quality = quality,
                serverLabel = "[BuzzServer]$sizeSuffix",
                headers = mapOf("Referer" to referer)
            )
        }
    }

    private fun buildStreamLink(
        source: ProviderSource,
        url: String,
        quality: Quality,
        serverLabel: String,
        headers: Map<String, String>
    ): StreamLink {
        val contentType = ContentTypeDetector.detect(url)
        val name = buildString {
            append(source.provider)
            if (quality != Quality.UNKNOWN) {
                append(" • ")
                append(quality.label)
            }
            val codec = source.metadata["codec"]
            if (codec != null) {
                append(" • ")
                append(codec)
            }
            append(" • ")
            append(serverLabel)
        }

        return StreamLink(
            name = name,
            url = url,
            quality = quality,
            host = HostType.DIRECT,
            contentType = contentType,
            headers = headers,
            cookies = source.cookies,
            adaptive = ContentTypeDetector.isAdaptive(contentType),
            referer = source.referer
        )
    }
}