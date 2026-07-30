package com.streamflex.core.network

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * DomainResolver
 *
 * Resolves the active base URL for any StreamFlex provider using a
 * deterministic 5-step fallback chain.
 *
 * ─── Why This Exists ──────────────────────────────────────────────────────
 * Content providers like HDHub4u, VegaMovies, and NetMirror change their
 * domain every few weeks when their registrar suspends them.
 * CloudStream extensions hard-code a single fallback URL (line 41-42 in
 * HDhub4uProvider.kt: `"https://hdhub4u.rehab"`). When that URL goes down,
 * the provider breaks until the developer pushes a manual update.
 *
 * StreamFlex solves this with a 5-step resolution chain. Each step is tried
 * in order. The first successful step returns the domain and subsequent steps
 * are skipped. The resolved domain is cached locally for 6 hours.
 *
 * ─── The 5-Step Chain ─────────────────────────────────────────────────────
 *
 * Step 1 — In-memory session cache [FASTEST]
 *   The domain resolved this session is kept in a HashMap.
 *   Cost: HashMap lookup — nanoseconds.
 *   TTL: Until the process restarts (session-bound).
 *
 * Step 2 — Persistent SharedPreferences cache [FAST]
 *   The domain from the last successful resolution is persisted.
 *   Cost: SharedPreferences read — microseconds.
 *   TTL: 6 hours (Constants.CACHE_TTL_DOMAIN_CONFIG_MS).
 *   Why 6h? Providers rarely change domains more than once per day.
 *
 * Step 3 — GitHub provider manifest (remote source of truth) [NETWORK]
 *   Fetches the provider's JSON from streamflex-providers on GitHub.
 *   Parses the `domains.primary` field.
 *   This is the canonical source — if a maintainer updates the domain here,
 *   all apps pick it up within 6 hours.
 *   Cost: 1 HTTP GET to raw.githubusercontent.com (~50-200ms).
 *
 * Step 4 — Mirror fallback [NETWORK, SEQUENTIAL]
 *   If Step 3's primary domain is unreachable (HTTP HEAD check fails),
 *   tries each mirror URL from `domains.mirrors` in order.
 *   Cost: 1 HTTP HEAD per mirror until one responds 200/301.
 *
 * Step 5 — Hardcoded backup [LAST RESORT]
 *   Each provider has a hardcoded constant in its companion object.
 *   This was baked into the app at compile time.
 *   If this also fails, [DomainResult.Offline] is returned.
 *   The engine marks the provider as "offline" for this session.
 *
 * ─── Comparison with CloudStream ─────────────────────────────────────────
 *
 * | Aspect           | CloudStream             | StreamFlex              |
 * |------------------|-------------------------|-------------------------|
 * | Domain source    | Single hardcoded URL    | 5-step chain            |
 * | Remote update    | App update required     | Within 6 hours auto     |
 * | Mirror support   | Manual per-provider     | Unified, from JSON      |
 * | Concurrency      | None (runBlocking)      | Mutex per providerId    |
 * | Caching          | Session-only variable   | Session + SharedPrefs   |
 * | Observability    | None                    | Structured step logging |
 *
 * ─── Thread Safety ────────────────────────────────────────────────────────
 * Multiple coroutines calling [resolve] for the same providerId at startup
 * would trigger duplicate GitHub fetches. A per-provider Mutex ensures only
 * one coroutine performs the resolution at a time; others wait and then read
 * from the session cache (Step 1 hit on the second call).
 *
 * ─── Usage ────────────────────────────────────────────────────────────────
 * ```kotlin
 * val resolver = DomainResolver(cacheManager)
 *
 * // For a provider with a known hardcoded backup
 * val result = resolver.resolve(
 *     providerId   = "hdhub4u",
 *     hardcoded    = HDHubConfig.DEFAULT_DOMAIN,
 *     manifestPath = "providers/hdhub4u.json"
 * )
 *
 * when (result) {
 *     is DomainResult.Resolved -> baseUrl = result.domain
 *     is DomainResult.Mirror   -> baseUrl = result.domain
 *     is DomainResult.Hardcoded -> baseUrl = result.domain
 *     is DomainResult.Offline  -> markProviderOffline("hdhub4u")
 * }
 * ```
 */
class DomainResolver(
    private val cache  : CacheManager = CacheManager(),
    private val http   : StreamFlexHttpClient = StreamFlexHttpClient
) {

    companion object {
        private const val TAG = "DomainResolver"

        // HEAD request timeout — short, we just want to know if the server responds
        private const val PROBE_TIMEOUT_MS = 6_000L
    }

    // Per-provider session cache (in-memory, fastest path)
    private val sessionDomains = HashMap<String, String>()

    // Per-provider mutex to prevent duplicate concurrent resolutions
    private val mutexes = HashMap<String, Mutex>()
    private val mutexLock = Any() // synchronize access to mutexes map itself

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Resolves the active domain for [providerId] using the 5-step chain.
     *
     * @param providerId    Provider ID (e.g. "hdhub4u"). Used as cache key.
     * @param hardcoded     Compile-time backup URL. Pass provider's constant.
     * @param manifestPath  Path in streamflex-providers repo (e.g. "providers/hdhub4u.json").
     *                      Combined with [Constants.PROVIDERS_REPO_BASE] to build the URL.
     * @param forceRefresh  If true, skip Steps 1–2 and re-fetch from GitHub.
     *
     * @return [DomainResult] describing which step succeeded and the resolved URL.
     */
    suspend fun resolve(
        providerId   : String,
        hardcoded    : String,
        manifestPath : String  = "providers/$providerId.json",
        forceRefresh : Boolean = false
    ): DomainResult {

        val mutex = getMutex(providerId)

        return mutex.withLock {
            resolveInternal(
                providerId   = providerId,
                hardcoded    = hardcoded,
                manifestPath = manifestPath,
                forceRefresh = forceRefresh
            )
        }
    }

    /**
     * Fetches and parses the full [ProviderDefinition] for a provider.
     * Returns null if the manifest cannot be fetched or parsed.
     *
     * This is separate from [resolve] because callers sometimes need the
     * full definition (capabilities, extractorIds, rateLimit) not just the domain.
     */
    suspend fun fetchDefinition(
        providerId   : String,
        manifestPath : String = "providers/$providerId.json",
        forceRefresh : Boolean = false
    ): ProviderDefinition? {

        // Try cache first
        if (!forceRefresh) {
            val cached = cache.getManifest(providerId)
            if (cached != null) {
                return parseDefinition(cached, providerId)
            }
        }

        // Fetch from GitHub
        val manifestUrl = "${Constants.PROVIDERS_REPO_BASE}/$manifestPath"
        val result = http.get(url = manifestUrl, timeout = Constants.CONNECT_TIMEOUT_MS)

        return when (result) {
            is NetworkResult.Success -> {
                val json = result.data.bodyAsString()
                if (json.isNotBlank()) {
                    cache.putManifest(providerId, json)
                    parseDefinition(json, providerId)
                } else {
                    Logger.w("[$providerId] Empty manifest JSON from GitHub", TAG)
                    null
                }
            }
            else -> {
                Logger.w("[$providerId] Failed to fetch manifest: $result", TAG)
                null
            }
        }
    }

    /**
     * Invalidates the cached domain for [providerId], forcing a fresh resolution
     * on the next [resolve] call. Call this when a provider returns 403 or 5xx
     * consistently — it indicates a domain change.
     */
    fun invalidate(providerId: String) {
        synchronized(sessionDomains) {
            sessionDomains.remove(providerId)
        }
        cache.clearDomains()
        Logger.i("[$providerId] Domain cache invalidated — will re-resolve on next call", TAG)
    }

    // ─── Internal Resolution Chain ────────────────────────────────────────────

    private suspend fun resolveInternal(
        providerId   : String,
        hardcoded    : String,
        manifestPath : String,
        forceRefresh : Boolean
    ): DomainResult {

        // ── Step 1: In-memory session cache ──────────────────────────────────
        if (!forceRefresh) {
            val session = synchronized(sessionDomains) { sessionDomains[providerId] }
            if (session != null) {
                Logger.v("[$providerId] Step 1 HIT — session domain: $session", TAG)
                return DomainResult.Resolved(
                    domain = session,
                    step   = ResolutionStep.SESSION_CACHE,
                    source = "in-memory"
                )
            }
        }

        // ── Step 2: Persistent SharedPreferences cache ───────────────────────
        if (!forceRefresh) {
            val persisted = cache.getDomain(providerId)
            if (persisted != null) {
                Logger.v("[$providerId] Step 2 HIT — persisted domain: $persisted", TAG)
                cacheSession(providerId, persisted)
                return DomainResult.Resolved(
                    domain = persisted,
                    step   = ResolutionStep.PREFS_CACHE,
                    source = "SharedPreferences"
                )
            }
        }

        Logger.d("[$providerId] Steps 1–2 missed — fetching manifest from GitHub", TAG)

        // ── Step 3: GitHub provider manifest (remote source of truth) ────────
        val definition = fetchDefinition(
            providerId   = providerId,
            manifestPath = manifestPath,
            forceRefresh = forceRefresh
        )

        if (definition != null) {
            val primary = definition.domains.primary
            if (primary.isNotBlank()) {

                // Probe the primary to confirm it's reachable
                if (probeUrl(primary)) {
                    Logger.i("[$providerId] Step 3 — primary domain live: $primary", TAG)
                    cacheAndPersist(providerId, primary)
                    return DomainResult.Resolved(
                        domain = primary,
                        step   = ResolutionStep.GITHUB_MANIFEST,
                        source = "manifest:primary"
                    )
                }

                Logger.w("[$providerId] Step 3 — primary unreachable: $primary", TAG)

                // ── Step 4: Mirror fallback ───────────────────────────────────
                val mirrors = definition.domains.mirrors
                for ((index, mirror) in mirrors.withIndex()) {
                    Logger.d("[$providerId] Step 4 — probing mirror $index: $mirror", TAG)
                    if (probeUrl(mirror)) {
                        Logger.i("[$providerId] Step 4 — mirror $index live: $mirror", TAG)
                        cacheAndPersist(providerId, mirror)
                        return DomainResult.Mirror(
                            domain      = mirror,
                            mirrorIndex = index,
                            primary     = primary
                        )
                    }
                }

                Logger.w("[$providerId] Step 4 — all mirrors unreachable (tried ${mirrors.size})", TAG)
            } else {
                Logger.w("[$providerId] Step 3 — manifest missing primary domain field", TAG)
            }
        }

        // ── Step 5: Hardcoded compile-time backup ─────────────────────────────
        Logger.d("[$providerId] Step 5 — probing hardcoded: $hardcoded", TAG)
        if (probeUrl(hardcoded)) {
            Logger.i("[$providerId] Step 5 — hardcoded URL live: $hardcoded", TAG)
            cacheAndPersist(providerId, hardcoded)
            return DomainResult.Hardcoded(domain = hardcoded)
        }

        // All 5 steps failed
        Logger.e("[$providerId] All 5 resolution steps failed — provider is OFFLINE", TAG)
        return DomainResult.Offline(
            providerId = providerId,
            tried      = buildList {
                if (definition != null) {
                    add(definition.domains.primary)
                    addAll(definition.domains.mirrors)
                }
                add(hardcoded)
            }
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Probes a URL with a HEAD request to check if it's reachable.
     * Returns true if the server responds with any HTTP status (even 3xx/4xx)
     * — what we're checking is network reachability, not content availability.
     * A 5xx or network error returns false.
     */
    private suspend fun probeUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val result = http.head(url = url, headers = emptyMap())
            when (result) {
                is NetworkResult.Success    -> result.data.code < 500
                is NetworkResult.Error      -> result.code < 500   // 4xx is ok, means server is up
                is NetworkResult.Timeout    -> false
                is NetworkResult.NetworkError -> false
                else                        -> false
            }
        } catch (e: Exception) {
            Logger.w("Probe failed for $url: ${e.message}", TAG)
            false
        }
    }

    /**
     * Parses a provider JSON manifest string into a [ProviderDefinition].
     * Uses Android's built-in [JSONObject] — no Gson/Moshi needed.
     */
    private fun parseDefinition(json: String, providerId: String): ProviderDefinition? {
        return try {
            val root = JSONObject(json)

            val domainsObj = root.optJSONObject("domains")
            val mirrorsArr = domainsObj?.optJSONArray("mirrors")
            val mirrorList = buildList {
                mirrorsArr?.let {
                    for (i in 0 until it.length()) {
                        val m = it.optString(i)
                        if (m.isNotBlank()) add(m)
                    }
                }
            }

            val capObj = root.optJSONObject("capabilities")
            val endpObj = root.optJSONObject("endpoints")
            val rlObj = root.optJSONObject("rateLimit")
            val extractorsArr = root.optJSONArray("extractorIds")
            val extractorList = buildList {
                extractorsArr?.let {
                    for (i in 0 until it.length()) {
                        val e = it.optString(i)
                        if (e.isNotBlank()) add(e)
                    }
                }
            }
            val fallbackArr = root.optJSONArray("fallbackTo")
            val fallbackList = buildList {
                fallbackArr?.let {
                    for (i in 0 until it.length()) {
                        val f = it.optString(i)
                        if (f.isNotBlank()) add(f)
                    }
                }
            }
            val maintObj = root.optJSONObject("maintainer")

            ProviderDefinition(
                schemaVersion        = root.optInt("schemaVersion", 1),
                providerVersion      = root.optInt("providerVersion", 1),
                minimumEngineVersion = root.optString("minimumEngineVersion", "1.0.0"),
                id                   = root.optString("id", providerId),
                name                 = root.optString("name", ""),
                description          = root.optString("description", ""),
                language             = root.optString("language", "en"),
                region               = root.optString("region", ""),
                lifecycle            = root.optString("lifecycle", "stable"),
                status               = root.optString("status", "online"),
                domains = DomainConfig(
                    primary   = domainsObj?.optString("primary", "").orEmpty(),
                    mirrors   = mirrorList,
                    search    = domainsObj?.optString("search"),
                    updatedAt = domainsObj?.optString("updatedAt", "").orEmpty()
                ),
                capabilities = Capabilities(
                    supportsSearch          = capObj?.optBoolean("supportsSearch", true) ?: true,
                    supportsMovies          = capObj?.optBoolean("supportsMovies", true) ?: true,
                    supportsTV              = capObj?.optBoolean("supportsTV", true) ?: true,
                    supportsAnime           = capObj?.optBoolean("supportsAnime", false) ?: false,
                    supportsDub             = capObj?.optBoolean("supportsDub", false) ?: false,
                    supportsSub             = capObj?.optBoolean("supportsSub", false) ?: false,
                    supportsHome            = capObj?.optBoolean("supportsHome", false) ?: false,
                    supportsMultipleServers = capObj?.optBoolean("supportsMultipleServers", false) ?: false,
                    supportsDirectLinks     = capObj?.optBoolean("supportsDirectLinks", false) ?: false,
                    supportsDownload        = capObj?.optBoolean("supportsDownload", false) ?: false,
                    supportsSkipIntro       = capObj?.optBoolean("supportsSkipIntro", false) ?: false,
                    supportsResume          = capObj?.optBoolean("supportsResume", false) ?: false,
                    isSequential            = capObj?.optBoolean("isSequential", false) ?: false,
                    sequentialDelayMs       = capObj?.optLong("sequentialDelayMs", 0L) ?: 0L
                ),
                endpoints = EndpointConfig(
                    searchType = endpObj?.optString("searchType", "HTML_PAGE") ?: "HTML_PAGE",
                    detailType = endpObj?.optString("detailType", "HTML_PAGE") ?: "HTML_PAGE",
                    sourceType = endpObj?.optString("sourceType", "HTML_EMBEDDED") ?: "HTML_EMBEDDED"
                ),
                extractorIds = extractorList,
                rateLimit = RateLimitDef(
                    maxConcurrent  = rlObj?.optInt("maxConcurrent", 3) ?: 3,
                    delayBetweenMs = rlObj?.optLong("delayBetweenMs", 0L) ?: 0L
                ),
                priority   = root.optInt("priority", 50),
                fallbackTo = fallbackList,
                maintainer = maintObj?.let {
                    MaintainerInfo(
                        github       = it.optString("github", ""),
                        addedAt      = it.optString("addedAt", ""),
                        lastVerified = it.optString("lastVerified", "")
                    )
                }
            )
        } catch (e: Exception) {
            Logger.e("[$providerId] Failed to parse manifest JSON: ${e.message}", TAG)
            null
        }
    }

    private fun cacheSession(providerId: String, domain: String) {
        synchronized(sessionDomains) { sessionDomains[providerId] = domain }
    }

    private fun cacheAndPersist(providerId: String, domain: String) {
        cacheSession(providerId, domain)
        cache.putDomain(providerId, domain)
    }

    private fun getMutex(providerId: String): Mutex =
        synchronized(mutexLock) {
            mutexes.getOrPut(providerId) { Mutex() }
        }
}

// ─── Result Types ─────────────────────────────────────────────────────────────

/**
 * The result of a domain resolution attempt.
 * All subclasses expose [domain] — the resolved URL — so callers can use it
 * regardless of which step succeeded. The step metadata is available for
 * tracing, monitoring, and diagnostics.
 */
sealed class DomainResult {

    abstract val domain: String

    /**
     * Steps 1, 2, or 3 succeeded — either from cache or GitHub primary.
     */
    data class Resolved(
        override val domain : String,
        val step            : ResolutionStep,
        val source          : String = ""
    ) : DomainResult()

    /**
     * Step 4 succeeded — a mirror was used because primary was unreachable.
     * The engine should consider re-probing the primary periodically.
     */
    data class Mirror(
        override val domain : String,
        val mirrorIndex     : Int,
        val primary         : String
    ) : DomainResult()

    /**
     * Step 5 — the hardcoded compile-time backup was used.
     * This means both the GitHub manifest AND all mirrors were unreachable.
     * The provider's domain may have changed — maintainer should update the JSON.
     */
    data class Hardcoded(
        override val domain: String
    ) : DomainResult()

    /**
     * All 5 steps failed. The provider is effectively offline.
     * The engine should mark it degraded and try the next provider in [fallbackTo].
     */
    data class Offline(
        val providerId      : String,
        val tried           : List<String>,
        override val domain : String = ""
    ) : DomainResult()
}

/**
 * Which step in the 5-step chain provided the resolved domain.
 * Used in structured logging and pipeline traces.
 */
enum class ResolutionStep {
    SESSION_CACHE,    // Step 1 — in-memory, this session
    PREFS_CACHE,      // Step 2 — SharedPreferences, previous session
    GITHUB_MANIFEST,  // Step 3 — fetched from GitHub repo
    MIRROR,           // Step 4 — a fallback mirror URL
    HARDCODED         // Step 5 — compile-time backup constant
}
