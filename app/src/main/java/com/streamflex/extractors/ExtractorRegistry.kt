package com.streamflex.extractors

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.StreamFlexHttpClient
import com.streamflex.domain.models.HostType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ExtractorRegistry
 *
 * Centralized registry for extractor metadata (domains, required headers, priority,
 * output formats, and status) loaded from streamflex-providers/extractors/registry.json.
 *
 * ─── Why This Exists ──────────────────────────────────────────────────────────
 * CloudStream hardcodes extractor domains, priorities, and headers inside each Kotlin
 * extractor class. When a video hosting service changes its domain (e.g. hubcloud.tel
 * becomes hubcloud.xyz) or requires a new Referer header, CloudStream apps break
 * until an app update is released.
 *
 * StreamFlex solves this by treating registry.json as a remote source of truth:
 * 1. Domains, required headers (Referer/Origin), and fallback priorities are defined
 *    in registry.json and cached locally for 24 hours (Constants.CACHE_TTL_EXTRACTOR_META_MS).
 * 2. Complete compile-time fallback definitions are baked into this singleton so the
 *    app works offline without any network call.
 * 3. New mirror domains for existing extractors work immediately upon JSON update on GitHub,
 *    without any app recompilation.
 */
object ExtractorRegistry {

    private const val TAG = "ExtractorRegistry"

    private val mutex = Mutex()

    // ─── Compile-Time Hardcoded Fallback Defaults ─────────────────────────────
    /**
     * Default compile-time manifest matching streamflex-providers/extractors/registry.json.
     * Ensures 100% offline functionality if remote fetch fails or on clean install offline.
     */
    val DEFAULT_MANIFEST = ExtractorRegistryManifest(
        meta = RegistryMeta(
            description = "StreamFlex Extractor Registry default offline manifest",
            version     = "1.0.0",
            updatedAt   = "2026-07-30"
        ),
        extractors = listOf(
            ExtractorDefinition(
                id = "googlevideo",
                name = "Google Video",
                priority = 95,
                status = "active",
                domains = listOf("googlevideo.com", "googleusercontent.com", "rr1---sn"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = false,
                headers = emptyMap(),
                androidClass = "com.streamflex.extractors.googlevideo.GoogleVideoExtractor"
            ),
            ExtractorDefinition(
                id = "hubcloud",
                name = "HubCloud",
                priority = 90,
                status = "active",
                domains = listOf(
                    "hubcloud.tel", "hubcloud.lol", "hubcloud.art", "hubcloud.bond",
                    "hubcloud.mov", "hubcloud.life", "hubcloud.bar"
                ),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://hubcloud.tel/", "Origin" to "https://hubcloud.tel"),
                androidClass = "com.streamflex.extractors.hubcloud.HubCloudExtractor"
            ),
            ExtractorDefinition(
                id = "pixeldrain",
                name = "PixelDrain",
                priority = 85,
                status = "active",
                domains = listOf("pixeldrain.com"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = false,
                headers = emptyMap(),
                androidClass = "com.streamflex.extractors.pixeldrain.PixelDrainExtractor"
            ),
            ExtractorDefinition(
                id = "filemoon",
                name = "FileMoon",
                priority = 80,
                status = "active",
                domains = listOf("filemoon.sx", "filemoon.to", "filemoon.in", "filemoon.wf"),
                outputFormats = listOf("m3u8"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://filemoon.sx/", "Origin" to "https://filemoon.sx"),
                androidClass = "com.streamflex.extractors.shared.PackerExtractor"
            ),
            ExtractorDefinition(
                id = "hubdrive",
                name = "HubDrive",
                priority = 75,
                status = "active",
                domains = listOf("hubdrive.dad", "hubdrive.men", "hubdrive.me", "hubdrive.be"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://hubdrive.dad/"),
                androidClass = "com.streamflex.extractors.hubdrive.HubDriveExtractor"
            ),
            ExtractorDefinition(
                id = "hubcdn",
                name = "HubCDN",
                priority = 72,
                status = "active",
                domains = listOf("hubcdn.xyz", "hubcdn.store"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://hubcdn.xyz/"),
                androidClass = "com.streamflex.extractors.hubcdn.HubCDNExtractor"
            ),
            ExtractorDefinition(
                id = "dood",
                name = "DooD",
                priority = 70,
                status = "active",
                domains = listOf(
                    "dood.la", "dood.re", "dood.so", "dood.pm", "dood.sh", "dood.cx",
                    "dood.stream", "ds2play.com", "doods.pro", "do0od.com", "d000d.com", "dooood.com"
                ),
                outputFormats = listOf("mp4"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://dood.la/"),
                androidClass = "com.streamflex.extractors.dood.DoodExtractor"
            ),
            ExtractorDefinition(
                id = "hblinks",
                name = "HBLinks",
                priority = 68,
                status = "active",
                domains = listOf("hblinks.pro", "hblinksme.workers.dev"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://hblinks.pro/"),
                androidClass = "com.streamflex.extractors.hblinks.HBLinksExtractor"
            ),
            ExtractorDefinition(
                id = "streamtape",
                name = "StreamTape",
                priority = 65,
                status = "active",
                domains = listOf(
                    "streamtape.com", "streamtape.to", "streamtape.net",
                    "streamtape.xyz", "streamtape.cc", "strcloud.in", "shavetape.cash"
                ),
                outputFormats = listOf("mp4"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://streamtape.com/"),
                androidClass = "com.streamflex.extractors.streamtape.StreamTapeExtractor"
            ),
            ExtractorDefinition(
                id = "mixdrop",
                name = "MixDrop",
                priority = 55,
                status = "pending",
                domains = listOf("mixdrop.sb", "mixdrop.to", "mixdrop.ch", "mixdrop.co", "mixdrop.bz"),
                outputFormats = listOf("mp4"),
                requiresReferer = true,
                headers = mapOf("Referer" to "https://mixdrop.sb/"),
                androidClass = "com.streamflex.extractors.mixdrop.MixDropExtractor"
            ),
            ExtractorDefinition(
                id = "redirect",
                name = "Redirect Resolver",
                priority = 50,
                status = "active",
                domains = emptyList(),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = false,
                headers = emptyMap(),
                androidClass = "com.streamflex.extractors.redirect.RedirectExtractor"
            ),
            ExtractorDefinition(
                id = "netmirror",
                name = "NetMirror",
                priority = 98,
                status = "active",
                domains = listOf("netmirror"),
                outputFormats = listOf("mp4", "m3u8"),
                requiresReferer = false,
                headers = emptyMap(),
                androidClass = "com.streamflex.extractors.netmirror.NetMirrorExtractor"
            )
        ),
        qualityPatterns = mapOf(
            "2160p" to listOf("2160p", "4k", "uhd", "2160"),
            "1080p" to listOf("1080p", "1080", "fhd", "fullhd"),
            "720p"  to listOf("720p", "720", "hd"),
            "480p"  to listOf("480p", "480"),
            "360p"  to listOf("360p", "360")
        ),
        defaultFallbackOrder = listOf(
            "googlevideo", "hubcloud", "hubdrive", "hubcdn", "hblinks",
            "pixeldrain", "filemoon", "dood", "streamtape", "mixdrop", "redirect"
        )
    )

    @Volatile
    private var activeManifest: ExtractorRegistryManifest = DEFAULT_MANIFEST

    // ─── Loading & Caching Pipeline ───────────────────────────────────────────

    /**
     * Ensure extractor metadata is loaded.
     * Checks:
     * 1. In-memory session manifest (if already loaded)
     * 2. SharedPreferences cache ([CacheManager.getExtractorRegistry])
     * 3. Remote fetch from GitHub ([Constants.EXTRACTORS_REGISTRY_URL])
     * 4. [DEFAULT_MANIFEST] offline compile-time fallback
     */
    suspend fun ensureLoaded(
        cache        : CacheManager? = null,
        http         : StreamFlexHttpClient = StreamFlexHttpClient,
        forceRefresh : Boolean = false
    ): ExtractorRegistryManifest {
        // If already loaded and not forcing refresh, return in-memory active manifest
        if (!forceRefresh && activeManifest !== DEFAULT_MANIFEST) {
            return activeManifest
        }

        return mutex.withLock {
            if (!forceRefresh && activeManifest !== DEFAULT_MANIFEST) {
                return@withLock activeManifest
            }

            // Step 1: Check persistent SharedPreferences cache
            if (!forceRefresh && cache != null) {
                val cachedJson = cache.getExtractorRegistry()
                if (!cachedJson.isNullOrBlank()) {
                    val parsed = ExtractorRegistryManifest.parse(cachedJson)
                    if (parsed != null && parsed.extractors.isNotEmpty()) {
                        Logger.i("Loaded extractor registry from persistent cache (${parsed.extractors.size} extractors)", TAG)
                        activeManifest = parsed
                        return@withLock parsed
                    }
                }
            }

            // Step 2: Fetch remote source of truth from GitHub
            val result = http.get(url = Constants.EXTRACTORS_REGISTRY_URL, timeout = Constants.CONNECT_TIMEOUT_MS)
            when (result) {
                is NetworkResult.Success -> {
                    val json = result.data.bodyAsString()
                    if (json.isNotBlank()) {
                        val parsed = ExtractorRegistryManifest.parse(json)
                        if (parsed != null && parsed.extractors.isNotEmpty()) {
                            Logger.i("Fetched extractor registry from GitHub (${parsed.extractors.size} extractors, v=${parsed.meta.version})", TAG)
                            cache?.putExtractorRegistry(json)
                            activeManifest = parsed
                            return@withLock parsed
                        }
                    }
                }
                else -> {
                    Logger.w("Failed to fetch remote extractor registry: $result. Using offline defaults.", TAG)
                }
            }

            // Step 3: Fall back to compile-time default manifest
            Logger.i("Using compile-time default extractor registry (${DEFAULT_MANIFEST.extractors.size} extractors)", TAG)
            activeManifest = DEFAULT_MANIFEST
            DEFAULT_MANIFEST
        }
    }

    /**
     * Invalidate in-memory manifest to trigger reload on next [ensureLoaded] call.
     */
    fun invalidate() {
        activeManifest = DEFAULT_MANIFEST
    }

    // ─── Query Helpers ────────────────────────────────────────────────────────

    /**
     * Lookup extractor definition by ID (e.g. "hubcloud", "pixeldrain").
     */
    fun getExtractor(id: String): ExtractorDefinition? {
        val lowerId = id.lowercase()
        return activeManifest.extractors.firstOrNull { it.id.equals(lowerId, ignoreCase = true) }
    }

    /**
     * Lookup extractor definition matching a domain or URL.
     * Iterates extractors sorted by priority descending so higher-priority extractors match first.
     */
    fun getExtractorForUrl(url: String): ExtractorDefinition? {
        if (url.isBlank()) return null
        return activeManifest.extractors
            .filter { it.status == "active" || it.status == "pending" }
            .sortedByDescending { it.priority }
            .firstOrNull { it.matchesDomain(url) }
    }

    /**
     * Maps an extractor ID string to the corresponding Kotlin [HostType] enum.
     */
    fun getHostType(id: String): HostType {
        return when (id.lowercase()) {
            "googlevideo" -> HostType.GOOGLE_VIDEO
            "hubcloud"    -> HostType.HUBCLOUD
            "pixeldrain"  -> HostType.PIXELDRAIN
            "filemoon"    -> HostType.FILEMOON
            "hubdrive"    -> HostType.HUBDRIVE
            "hubcdn"      -> HostType.HUBCDN
            "dood"        -> HostType.DOOD
            "hblinks"     -> HostType.HBLINKS
            "streamtape"  -> HostType.STREAMTAPE
            "mixdrop"     -> HostType.MIXDROP
            "redirect"    -> HostType.REDIRECT
            "vidstack"    -> HostType.VIDSTACK
            "netmirror"   -> HostType.NETMIRROR
            else          -> HostType.UNKNOWN
        }
    }

    /**
     * Maps any URL to its [HostType] by checking the dynamic domain list in registry.json.
     */
    fun getHostTypeForUrl(url: String): HostType {
        val def = getExtractorForUrl(url) ?: return HostType.UNKNOWN
        return getHostType(def.id)
    }

    /**
     * Returns required headers (e.g. Referer, Origin) for a given extractor ID.
     */
    fun getHeaders(id: String): Map<String, String> {
        return getExtractor(id)?.headers ?: emptyMap()
    }

    /**
     * True if the extractor ID requires Referer validation.
     */
    fun requiresReferer(id: String): Boolean {
        return getExtractor(id)?.requiresReferer ?: false
    }

    /**
     * Returns the priority score (0–100) for an extractor ID.
     */
    fun getPriority(id: String): Int {
        return getExtractor(id)?.priority ?: 50
    }

    /**
     * Returns the priority score (0–100) for a [HostType].
     */
    fun getPriority(hostType: HostType): Int {
        val id = when (hostType) {
            HostType.GOOGLE_VIDEO -> "googlevideo"
            HostType.HUBCLOUD     -> "hubcloud"
            HostType.PIXELDRAIN   -> "pixeldrain"
            HostType.FILEMOON     -> "filemoon"
            HostType.HUBDRIVE     -> "hubdrive"
            HostType.HUBCDN       -> "hubcdn"
            HostType.DOOD         -> "dood"
            HostType.HBLINKS      -> "hblinks"
            HostType.STREAMTAPE   -> "streamtape"
            HostType.MIXDROP      -> "mixdrop"
            HostType.REDIRECT     -> "redirect"
            HostType.VIDSTACK     -> "vidstack"
            HostType.NETMIRROR    -> "netmirror"
            else                  -> return 50
        }
        return getPriority(id)
    }

    /**
     * Returns the default fallback order of extractor IDs.
     */
    fun getFallbackOrder(): List<String> {
        return activeManifest.defaultFallbackOrder.ifEmpty {
            DEFAULT_MANIFEST.defaultFallbackOrder
        }
    }

    /**
     * Returns quality pattern mappings from the registry.
     */
    fun getQualityPatterns(): Map<String, List<String>> {
        return activeManifest.qualityPatterns.ifEmpty {
            DEFAULT_MANIFEST.qualityPatterns
        }
    }
}
