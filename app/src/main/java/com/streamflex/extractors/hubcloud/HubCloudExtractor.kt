package com.streamflex.extractors.hubcloud

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorUtils
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.core.utils.StreamLogger

/**
 * Extractor for HubCloud.
 *
 * HubCloud pages embed download links. We scrape them out, classify
 * them, and either emit a playable stream or forward for further resolution.
 *
 * Loop-protection: HubCloud pages frequently link back to other HubCloud
 * sub-pages. ExtractorManager already deduplicates by URL via `visited`
 * set, so we just need to NOT generate infinite pending sources by
 * limiting what we forward.
 */
class HubCloudExtractor
    : BaseExtractor() {

    override val hostType = HostType.HUBCLOUD

    // URL fragments we will always skip — avoids forwarding social links,
    // login pages, and same-domain roots back into the queue.
    private val SKIP_PATTERNS = listOf(
        "facebook", "twitter", "telegram", "discord", "imdb", "instagram",
        "youtube.com", "google.com/search", "/tg/", "tg://",
        "/category/", "/tag/", "/page/", "/sign", "/login", "/register",
        "/contact", "/about", "/privacy", "/terms",
        "javascript:", "#"
    )

    // Domains whose ROOT we allow through (they have useful content pages)
    // but whose bare domain home page we skip.
    private val SKIP_BARE_DOMAINS = listOf(
        ".tips", ".fans", ".cx", ".co", ".ist"
    )

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {
        StreamLogger.info("HubCloudExtractor", "Extracting HubCloud page")
        StreamLogger.debug("HubCloudExtractor", "URL: ${source.url}")

        if (!supports(source)) {
            StreamLogger.warn("HubCloudExtractor", "Unsupported source: ${source.hostType}")
            return emptyResult()
        }

        var document = ExtractorHelper.fetchDocument(source.url, source.headers)
        var currentUrl = source.url

        // Intermediate step handling (HubCloud timer / gateway page):
        // If the page has a #download button or `var url = '...'` script, follow to the actual download page
        if (!currentUrl.contains("hubcloud.php")) {
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
                        val u = java.net.URL(currentUrl)
                        "${u.protocol}://${u.host}"
                    } catch (_: Exception) { currentUrl }
                    "$base/${nextHref.trimStart('/')}"
                }
                
                // Ensure we don't accidentally fetch a telegram link or other skipped patterns
                val lowerNext = absoluteNext.lowercase()
                if (SKIP_PATTERNS.none { lowerNext.contains(it) }) {
                    StreamLogger.info("HubCloudExtractor", "Following intermediate HubCloud step: $absoluteNext")
                    val nextHeaders = source.headers.toMutableMap().apply { 
                        put("Referer", currentUrl)
                        put("User-Agent", com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT)
                    }
                    document = ExtractorHelper.fetchDocument(absoluteNext, nextHeaders)
                    currentUrl = absoluteNext
                } else {
                    StreamLogger.info("HubCloudExtractor", "Skipped intermediate HubCloud step (matches SKIP_PATTERNS): $absoluteNext")
                }
            }
        }

        StreamLogger.debug("HubCloudExtractor", "Document downloaded")
        return parseHubCloud(source.copy(url = currentUrl), document)
    }

    private suspend fun parseHubCloud(
        source: ProviderSource,
        document: org.jsoup.nodes.Document
    ): ExtractionResult {

        val candidates = linkedSetOf<String>()
        StreamLogger.debug("HubCloudExtractor", "Scanning page for candidate URLs...")

        val currentUrl = source.url
        val baseHost = try {
            val u = java.net.URL(currentUrl)
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

        // 1. <video> tags
        document.select("video source[src]")
            .map { it.absUrl("src").ifBlank { it.attr("src") } }
            .map { toAbsolute(it) }
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        // 2. <a href> download links and button tags
        document.select("a[href], a.btn, a[class*=btn], button.btn").forEach { el ->
            var href = el.absUrl("href").takeIf { it.isNotBlank() } ?: el.attr("href")
            val abs = toAbsolute(href)
            if (abs.isNotBlank()) {
                candidates.add(abs)
            }
            // Check onclick attribute (location.href = '...' or window.open('...'))
            val onclick = el.attr("onclick")
            if (onclick.isNotBlank()) {
                val onclickMatch = Regex("""(?:location(?:\.href)?|window\.open)\s*=\s*['"]([^'"]+)['"]|window\.open\s*\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(onclick)
                val onclickUrl = onclickMatch?.groups?.get(1)?.value ?: onclickMatch?.groups?.get(2)?.value
                if (!onclickUrl.isNullOrBlank()) {
                    val absOnclick = toAbsolute(onclickUrl)
                    if (absOnclick.isNotBlank()) {
                        candidates.add(absOnclick)
                    }
                }
            }
        }

        // 3. <iframe src>
        document.select("iframe[src]")
            .map { it.absUrl("src").ifBlank { it.attr("src") } }
            .map { toAbsolute(it) }
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        // 4. Script tag URLs
        document.select("script").forEach {
            val html = it.data()
            ExtractorUtils
                .allMatches("""https?:\/\/[^\s"'<>\\]+""", html)
                .forEach(candidates::add)
        }

        // 5. Google Drive uc?export=download links embedded in data- attrs or text
        val htmlText = document.html()
        Regex("""https://drive\.google\.com/uc\?[^\s"'<>\\]+""")
            .findAll(htmlText)
            .forEach { candidates.add(it.value) }

        if (candidates.isEmpty()) {
            StreamLogger.warn("HubCloudExtractor", "No candidate URLs found.")
            return emptyResult()
        }

        return buildCandidateStreams(source, candidates.toList())
    }

    private fun buildCandidateStreams(
        source: ProviderSource,
        urls: List<String>
    ): ExtractionResult {

        val streams = mutableListOf<StreamLink>()
        val pendingSources = mutableListOf<ProviderSource>()

        // Sort: prefer direct video files, then Google Video, then everything else
        val sorted = urls.distinct().sortedWith(
            compareBy<String> {
                when {
                    it.contains(".m3u8", true) -> 0
                    it.endsWith(".mp4", true) -> 1
                    it.endsWith(".mkv", true) -> 2
                    it.contains("googlevideo", true) ||
                            it.contains("googleusercontent", true) -> 3
                    it.contains("drive.google.com", true) -> 4
                    else -> 100
                }
            }
        )

        sorted.forEach { url ->
            val lower = url.lowercase()

            // Skip patterns
            if (SKIP_PATTERNS.any { lower.contains(it) }) return@forEach
            if (lower.isBlank()) return@forEach

            // Skip bare-domain root URLs (e.g. https://hubcloud.ist or https://hubdrive.tips)
            // but NOT URLs with a meaningful path like https://hubcloud.ist/drive/abc123
            val isBareRoot = SKIP_BARE_DOMAINS.any { ext ->
                lower.endsWith(ext) || lower.endsWith("$ext/")
            }
            if (isBareRoot) return@forEach

            // Skip self-referential
            if (lower == source.url.lowercase()) return@forEach

            val type = HostDetector.detect(url)

            when (type) {
                HostType.M3U8,
                HostType.DIRECT,
                HostType.GOOGLE_VIDEO,
                HostType.DASH -> {
                    StreamLogger.info("HubCloudExtractor", "Playable stream detected: $type → $url")
                    streams += createStream(source = source, url = url)
                }

                HostType.UNKNOWN -> {
                    // Don't forward UNKNOWN — these are usually ad trackers, fonts, images
                    // EXCEPTION: Google Drive direct download links
                    if (lower.contains("drive.google.com/uc") ||
                        lower.contains("docs.google.com/uc")) {
                        StreamLogger.info("HubCloudExtractor", "Google Drive direct link: $url")
                        streams += createStream(source = source, url = url)
                    }
                    // else: silently discard
                }

                else -> {
                    // Forward known extractable hosts (HubDrive, HubCDN, etc.)
                    // but NOT another HUBCLOUD to prevent infinite loops
                    if (type != HostType.HUBCLOUD) {
                        StreamLogger.info("HubCloudExtractor", "Forwarding to extractor: $type")
                        pendingSources += buildProviderSource(source, url)
                    } else {
                        // Only forward HubCloud sub-pages if path is different (prevents tight loops)
                        val sourceHost = try { java.net.URL(source.url).host } catch (_: Exception) { "" }
                        val targetHost = try { java.net.URL(url).host } catch (_: Exception) { "" }
                        val sourcePath = try { java.net.URL(source.url).path } catch (_: Exception) { "" }
                        val targetPath = try { java.net.URL(url).path } catch (_: Exception) { "" }

                        if (sourceHost == targetHost && sourcePath == targetPath) {
                            // exact same page — skip
                        } else {
                            StreamLogger.info("HubCloudExtractor", "Forwarding sub-HubCloud: $url")
                            pendingSources += buildProviderSource(source, url)
                        }
                    }
                }
            }
        }

        StreamLogger.info("HubCloudExtractor", "Returning ${streams.size} stream(s)")
        StreamLogger.info("HubCloudExtractor", "Forwarding ${pendingSources.size} source(s)")

        return result(
            streams = streams.distinctBy(StreamLink::url),
            sources = pendingSources.distinctBy { it.url }
        )
    }

    private fun buildProviderSource(source: ProviderSource, url: String): ProviderSource {
        return source.copy(url = url, hostType = HostDetector.detect(url))
    }
}