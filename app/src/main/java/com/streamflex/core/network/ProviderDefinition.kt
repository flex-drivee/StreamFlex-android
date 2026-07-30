package com.streamflex.core.network

/**
 * ProviderDefinition
 *
 * The Kotlin data model for a StreamFlex provider JSON manifest.
 * Maps directly to the frozen v1.json schema in streamflex-providers.
 *
 * Populated by [DomainResolver] when it fetches or reads a manifest.
 * All fields are optional-with-defaults to survive partial/malformed JSON.
 *
 * ─── Schema origin ────────────────────────────────────────────────────────
 * streamflex-providers/schemas/provider/v1.json (frozen, do not modify here)
 *
 * ─── DO NOT add non-schema fields here ────────────────────────────────────
 * This class is the contract. Adding extra fields here without updating
 * the schema breaks cross-platform consistency.
 */
data class ProviderDefinition(

    val schemaVersion   : Int    = 1,
    val providerVersion : Int    = 1,
    val minimumEngineVersion: String = "1.0.0",

    val id          : String = "",
    val name        : String = "",
    val description : String = "",
    val language    : String = "en",
    val region      : String = "",
    val lifecycle   : String = "stable",   // stable | beta | deprecated
    val status      : String = "online",   // online | degraded | offline

    val domains     : DomainConfig      = DomainConfig(),
    val capabilities: Capabilities      = Capabilities(),
    val endpoints   : EndpointConfig    = EndpointConfig(),
    val extractorIds: List<String>      = emptyList(),
    val rateLimit   : RateLimitDef      = RateLimitDef(),

    val priority    : Int               = 50,
    val fallbackTo  : List<String>      = emptyList(),

    val maintainer  : MaintainerInfo?   = null
)

data class DomainConfig(
    /** The current live domain. e.g. "https://new3.hdhub4u.cl" */
    val primary   : String        = "",
    /** Ordered mirror list — tried sequentially if primary fails. */
    val mirrors   : List<String>  = emptyList(),
    /** Separate search API endpoint (e.g. Typesense API URL). */
    val search    : String?       = null,
    /** ISO date string for when this domain config was last verified. */
    val updatedAt : String        = ""
)

data class Capabilities(
    val supportsSearch          : Boolean = true,
    val supportsMovies          : Boolean = true,
    val supportsTV              : Boolean = true,
    val supportsAnime           : Boolean = false,
    val supportsDub             : Boolean = false,
    val supportsSub             : Boolean = false,
    val supportsHome            : Boolean = false,
    val supportsMultipleServers : Boolean = false,
    val supportsDirectLinks     : Boolean = false,
    val supportsDownload        : Boolean = false,
    val supportsSkipIntro       : Boolean = false,
    val supportsResume          : Boolean = false,
    val isSequential            : Boolean = false,
    val sequentialDelayMs       : Long    = 0L
)

data class EndpointConfig(
    /** How to search this provider: TYPESENSE_API, HTML_PAGE, JSON_API */
    val searchType : String = "HTML_PAGE",
    /** How to load a detail page: HTML_PAGE, JSON_API */
    val detailType : String = "HTML_PAGE",
    /** How source links are embedded: HTML_EMBEDDED, IFRAME, JSON */
    val sourceType : String = "HTML_EMBEDDED"
)

data class RateLimitDef(
    val maxConcurrent  : Int  = 3,
    val delayBetweenMs : Long = 0L
)

data class MaintainerInfo(
    val github       : String = "",
    val addedAt      : String = "",
    val lastVerified : String = ""
)
