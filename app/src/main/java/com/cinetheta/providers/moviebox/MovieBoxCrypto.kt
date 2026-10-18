package com.cinetheta.providers.moviebox

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MovieBoxCrypto {

    private val random = java.security.SecureRandom()
    val deviceId = generateDeviceId()

    // Session-persistent spoofed IP (Indian prefix, matches MovieBox-Tui random_spoofed_ip)
    val spoofedIp: String by lazy { randomSpoofedIp() }

    // Pick a persistent Android version + build for this session (from ANDROID_VERSIONS pool)
    private val currentAndroid: Pair<String, String> by lazy {
        MovieBoxConfig.ANDROID_VERSIONS.random(Random.Default)
    }

    // Pick a persistent Redmi device (model, brand) for this session
    private val currentRedmi: Pair<String, String> by lazy {
        MovieBoxConfig.REDMI_MODELS.random(Random.Default)
    }

    // Pick a persistent version_code for this session
    private val currentVersionCode: Int by lazy {
        MovieBoxConfig.VERSION_CODES.random(Random.Default)
    }

    // GAID — random UUID per session
    private val gaid: String by lazy { randomUuid() }

    /**
     * The User-Agent string, matching MovieBox-Tui crypto.rs format exactly:
     *   com.community.oneroom/<version_code> (Linux; U; Android <os>; en_US; <model>; Build/<build>; Cronet/135.0.7012.3)
     */
    val userAgent: String by lazy {
        val (osVer, build) = currentAndroid
        val (model, _) = currentRedmi
        "${MovieBoxConfig.PACKAGE_NAME}/$currentVersionCode " +
                "(Linux; U; Android $osVer; en_US; $model; Build/$build; Cronet/135.0.7012.3)"
    }

    /**
     * The x-client-info JSON blob matching MovieBox-Tui crypto.rs format:
     *   package_name = com.community.oneroom
     *   version_name = 4.0.01.0813.03
     *   install_ch   = "ps"
     *   install_store= "ps"
     *   region       = "US"
     *   sp_code      = "40401"
     *   X-Play-Mode  = "2"    ← new field required by updated API
     */
    val clientInfo: String by lazy {
        val (osVer, _)      = currentAndroid
        val (model, brand)  = currentRedmi
        buildString {
            append("{")
            append("\"package_name\":\"${MovieBoxConfig.PACKAGE_NAME}\",")
            append("\"version_name\":\"${MovieBoxConfig.VERSION_NAME}\",")
            append("\"version_code\":$currentVersionCode,")
            append("\"os\":\"android\",")
            append("\"os_version\":\"$osVer\",")
            append("\"install_ch\":\"ps\",")
            append("\"device_id\":\"$deviceId\",")
            append("\"install_store\":\"ps\",")
            append("\"gaid\":\"$gaid\",")
            append("\"brand\":\"$brand\",")
            append("\"model\":\"$model\",")
            append("\"system_language\":\"en\",")
            append("\"net\":\"NETWORK_WIFI\",")
            append("\"region\":\"US\",")
            append("\"timezone\":\"Asia/Kolkata\",")
            append("\"sp_code\":\"${MovieBoxConfig.SP_CODE}\",")
            append("\"X-Play-Mode\":\"2\"")
            append("}")
        }
    }

    private fun generateDeviceId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun randomSpoofedIp(): String {
        val prefix = MovieBoxConfig.INDIAN_IP_PREFIXES.random(Random.Default)
        val c = (1..253).random()
        val d = (1..253).random()
        return "$prefix.$c.$d"
    }

    private fun randomUuid(): String {
        fun hex(len: Int) = (0 until len).joinToString("") { "%x".format((0..15).random()) }
        return "${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}"
    }

    private fun md5(input: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateXClientToken(hardcodedTimestamp: Long? = null): String {
        // timestamp = System.currentTimeMillis() (ms), hash = md5(reversed_timestamp_string)
        val timestamp = (hardcodedTimestamp ?: System.currentTimeMillis()).toString()
        val reversed = timestamp.reversed()
        val bytes = reversed.toByteArray(Charsets.UTF_8)
        val hash = md5(bytes)
        return "$timestamp,$hash"
    }

    private fun buildCanonicalString(
        method: String,
        accept: String?,
        contentType: String?,
        url: String,
        body: String?,
        timestamp: Long
    ): String {
        // Use Android Uri for query param extraction (same as reference Java)
        val androidUri = try { android.net.Uri.parse(url) } catch (_: Exception) { null }
        val path = androidUri?.path ?: ""
        val queryNames = androidUri?.queryParameterNames ?: emptySet()
        val query = if (queryNames.isNotEmpty()) {
            queryNames.sorted().joinToString("&") { key ->
                (androidUri?.getQueryParameters(key) ?: emptyList()).joinToString("&") { v -> "$key=$v" }
            }
        } else ""

        val canonicalUrl = if (query.isNotEmpty()) "$path?$query" else path

        val bodyBytes = body?.toByteArray(Charsets.UTF_8)
        val bodyHash = if (bodyBytes != null) {
            val trimmed = if (bodyBytes.size > 102400) bodyBytes.copyOfRange(0, 102400) else bodyBytes
            md5(trimmed)
        } else ""
        val bodyLength = bodyBytes?.size?.toString() ?: ""

        // CRITICAL: accept MUST be "application/json" (not null/empty) in canonical[1]
        // This was discovered by testing — the server validates this field.
        return buildString {
            append(method.uppercase(Locale.ROOT)).append('\n')
            append(accept ?: "").append('\n')   // must be "application/json"
            append(contentType ?: "").append('\n')
            append(bodyLength).append('\n')
            append(timestamp).append('\n')
            append(bodyHash).append('\n')
            append(canonicalUrl)
        }
    }

    private fun decodeB64(input: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(input)
        } catch (_: Exception) {
            android.util.Base64.decode(input, android.util.Base64.DEFAULT)
        }
    }

    private fun encodeB64(input: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(input)
        } catch (_: Exception) {
            android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP)
        }
    }

    fun generateXTrSignature(
        method: String,
        accept: String?,
        contentType: String?,
        url: String,
        body: String?,
        useAltKey: Boolean = false,
        hardcodedTimestamp: Long? = null
    ): String {
        val timestamp = hardcodedTimestamp ?: System.currentTimeMillis()
        val canonical = buildCanonicalString(method, accept, contentType, url, body, timestamp)

        val secretB64 =
            if (useAltKey) MovieBoxConfig.SECRET_KEY_ALT_B64 else MovieBoxConfig.SECRET_KEY_DEFAULT_B64
        val secretBytes = decodeB64(String(decodeB64(secretB64)))

        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))

        val signature = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        val signatureB64 = encodeB64(signature)

        return "$timestamp|2|$signatureB64"
    }

    var xUserToken: String? = null

    /**
     * IMPORTANT: accept and contentType MUST always be "application/json" to pass signature validation.
     * The server validates the Accept and Content-Type headers in the canonical string at positions [1] and [2].
     * Passing null or omitting contentType for GET causes HTTP 407 Signature invalid.
     *
     * Added X-Forwarded-For with a spoofed Indian IP (per MovieBox-Tui crypto.rs).
     */
    private const val ACCEPT_JSON = "application/json"

    fun getHeaders(
        method: String,
        contentType: String? = "application/json",
        url: String,
        body: String? = null,
        useAltKey: Boolean = false
    ): Map<String, String> {
        val actualContentType = contentType ?: ACCEPT_JSON
        val xClientToken = generateXClientToken()
        // Always pass ACCEPT_JSON as accept and contentType — required for valid signature
        val xTrSignature = generateXTrSignature(method, ACCEPT_JSON, actualContentType, url, body, useAltKey)

        val map = mutableMapOf(
            "x-client-info"   to clientInfo,
            "x-client-status" to "0",
            "x-client-token"  to xClientToken,
            "x-tr-signature"  to xTrSignature,
            "x-forwarded-for" to spoofedIp,
            "user-agent"      to userAgent,
            "accept"          to ACCEPT_JSON,
            "content-type"    to ACCEPT_JSON,
            "connection"      to "keep-alive"
        )
        xUserToken?.let { map["authorization"] = "Bearer $it" }
        return map
    }

    /**
     * Ensure a valid anonymous token is available for authenticated requests.
     * Tries visitor-login first (universal), then falls back to ranking-list x-user header.
     */
    suspend fun ensureToken(baseUrl: String = MovieBoxConfig.DEFAULT_DOMAIN): String? {
        val existing = xUserToken
        if (!existing.isNullOrBlank()) return existing

        return withContext(Dispatchers.IO) {
            // Method 1: visitor-login (POST /wefeed-mobile-bff/user-api/visitor-login)
            try {
                val loginUrl = "$baseUrl/wefeed-mobile-bff/user-api/visitor-login"
                val body = "{}"
                val headers = getHeaders("POST", ACCEPT_JSON, loginUrl, body)
                val req = RequestBuilder()
                    .url(loginUrl)
                    .post(body.toByteArray(Charsets.UTF_8))
                    .headers(headers)
                    .build()
                when (val resp = HttpClient.execute(req)) {
                    is NetworkResult.Success -> {
                        val json = resp.data.bodyAsString()
                        val root = JsonParser.parse(json)
                        val data = root?.let { JsonParser.objectOf(it, "data") }
                        val token = data?.let { JsonParser.string(it, "token") }
                        if (!token.isNullOrBlank()) {
                            xUserToken = token
                            return@withContext token
                        }
                    }
                    else -> {}
                }
            } catch (_: Exception) {}

            // Method 2: ranking-list fallback on apig.inmoviebox.com
            try {
                val rankingUrl = "https://apig.inmoviebox.com/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"
                val headers = getHeaders("GET", ACCEPT_JSON, rankingUrl)
                val req = RequestBuilder().url(rankingUrl).get().headers(headers).build()
                when (val resp = HttpClient.execute(req)) {
                    is NetworkResult.Success -> {
                        resp.data.header("x-user")?.let { xUser ->
                            val root = JsonParser.parse(xUser)
                            val token = root?.let { JsonParser.string(it, "token") }
                            if (!token.isNullOrBlank()) {
                                xUserToken = token
                                return@withContext token
                            }
                        }
                    }
                    else -> {}
                }
            } catch (_: Exception) {}

            null
        }
    }
}
