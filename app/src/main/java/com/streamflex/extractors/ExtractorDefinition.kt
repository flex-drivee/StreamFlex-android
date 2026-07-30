package com.streamflex.extractors

import org.json.JSONObject

/**
 * ExtractorDefinition
 *
 * Kotlin data model representing an extractor definition in streamflex-providers
 * (streamflex-providers/extractors/registry.json).
 *
 * All fields match the frozen v1.json / registry.json schema.
 * Built-in Android [JSONObject] is used for parsing — no external JSON library needed.
 */
data class ExtractorDefinition(
    val id              : String              = "",
    val name            : String              = "",
    val priority        : Int                 = 50,
    val status          : String              = "active",       // active | pending | deprecated | disabled
    val domains         : List<String>        = emptyList(),    // Domain patterns handled by this extractor
    val outputFormats   : List<String>        = emptyList(),    // mp4 | m3u8 | dash
    val requiresReferer : Boolean             = false,
    val headers         : Map<String, String> = emptyMap(),     // e.g. Referer, Origin
    val notes           : String              = "",
    val androidClass    : String              = "",
    val webTs           : String              = ""
) {
    /**
     * True if [domainOrUrl] matches any domain pattern in [domains].
     */
    fun matchesDomain(domainOrUrl: String): Boolean {
        val lower = domainOrUrl.lowercase()
        return domains.any { d ->
            lower.contains(d.lowercase())
        }
    }
}

/**
 * RegistryMeta
 *
 * Metadata for the extractor registry manifest.
 */
data class RegistryMeta(
    val description : String = "",
    val version     : String = "1.0.0",
    val updatedAt   : String = ""
)

/**
 * ExtractorRegistryManifest
 *
 * Top-level container representing the full extractors/registry.json file.
 */
data class ExtractorRegistryManifest(
    val meta                 : RegistryMeta                     = RegistryMeta(),
    val extractors           : List<ExtractorDefinition>        = emptyList(),
    val qualityPatterns      : Map<String, List<String>>        = emptyMap(),
    val defaultFallbackOrder : List<String>                     = emptyList()
) {
    companion object {
        /**
         * Parses a JSON string from registry.json into an [ExtractorRegistryManifest].
         * Returns null if parsing fails.
         */
        fun parse(jsonString: String): ExtractorRegistryManifest? {
            return try {
                val root = JSONObject(jsonString)

                // _meta
                val metaObj = root.optJSONObject("_meta")
                val meta = RegistryMeta(
                    description = metaObj?.optString("description", "").orEmpty(),
                    version     = metaObj?.optString("version", "1.0.0") ?: "1.0.0",
                    updatedAt   = metaObj?.optString("updatedAt", "").orEmpty()
                )

                // extractors
                val extractorsArr = root.optJSONArray("extractors")
                val extractorsList = buildList {
                    extractorsArr?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue

                            val domainsArr = item.optJSONArray("domains")
                            val doms = buildList {
                                domainsArr?.let { dArr ->
                                    for (d in 0 until dArr.length()) {
                                        val dStr = dArr.optString(d)
                                        if (dStr.isNotBlank()) add(dStr)
                                    }
                                }
                            }

                            val formatsArr = item.optJSONArray("outputFormats")
                            val formats = buildList {
                                formatsArr?.let { fArr ->
                                    for (f in 0 until fArr.length()) {
                                        val fStr = fArr.optString(f)
                                        if (fStr.isNotBlank()) add(fStr)
                                    }
                                }
                            }

                            val headersObj = item.optJSONObject("headers")
                            val headersMap = buildMap {
                                headersObj?.let { hObj ->
                                    val keys = hObj.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val valStr = hObj.optString(key)
                                        if (valStr.isNotBlank()) {
                                            put(key, valStr)
                                        }
                                    }
                                }
                            }

                            add(
                                ExtractorDefinition(
                                    id              = item.optString("id", ""),
                                    name            = item.optString("name", ""),
                                    priority        = item.optInt("priority", 50),
                                    status          = item.optString("status", "active"),
                                    domains         = doms,
                                    outputFormats   = formats,
                                    requiresReferer = item.optBoolean("requiresReferer", false),
                                    headers         = headersMap,
                                    notes           = item.optString("notes", ""),
                                    androidClass    = item.optString("androidClass", ""),
                                    webTs           = item.optString("webTs", "")
                                )
                            )
                        }
                    }
                }

                // qualityPatterns
                val qualObj = root.optJSONObject("qualityPatterns")
                val qualityMap = buildMap {
                    qualObj?.let { qObj ->
                        val keys = qObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val arr = qObj.optJSONArray(key)
                            val patList = buildList {
                                arr?.let { a ->
                                    for (p in 0 until a.length()) {
                                        val pStr = a.optString(p)
                                        if (pStr.isNotBlank()) add(pStr)
                                    }
                                }
                            }
                            put(key, patList)
                        }
                    }
                }

                // defaultFallbackOrder
                val fbArr = root.optJSONArray("defaultFallbackOrder")
                val fbList = buildList {
                    fbArr?.let { f ->
                        for (i in 0 until f.length()) {
                            val fbStr = f.optString(i)
                            if (fbStr.isNotBlank()) add(fbStr)
                        }
                    }
                }

                ExtractorRegistryManifest(
                    meta                 = meta,
                    extractors           = extractorsList,
                    qualityPatterns      = qualityMap,
                    defaultFallbackOrder = fbList
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
