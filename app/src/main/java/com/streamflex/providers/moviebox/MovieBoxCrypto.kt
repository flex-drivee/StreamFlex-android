package com.streamflex.providers.moviebox

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object MovieBoxCrypto {

    private val random = java.security.SecureRandom()
    val deviceId = generateDeviceId()

    // Pick a persistent brand/model for this session
    val currentBrandModel by lazy { randomBrandModel() }

    private fun generateDeviceId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun randomBrandModel(): Pair<String, String> {
        val brands = MovieBoxConfig.BRAND_MODELS.keys.toList()
        val brand = brands.random(Random.Default)
        val models = MovieBoxConfig.BRAND_MODELS[brand]!!
        val model = models.random(Random.Default)
        return Pair(brand, model)
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
     * IMPORTANT: accept MUST always be "application/json" to pass signature validation.
     * The server validates the Accept header in the canonical string at position [1].
     */
    private const val ACCEPT_JSON = "application/json"

    fun getHeaders(
        method: String,
        contentType: String? = "application/json",
        url: String,
        body: String? = null,
        useAltKey: Boolean = false
    ): Map<String, String> {
        // For GET requests, Content-Type is null (not sent); for POST it's application/json
        val actualContentType = if (method.uppercase(Locale.ROOT) == "GET") null else contentType
        val clientInfo = "{\"package_name\":\"com.community.oneroom\",\"version_name\":\"3.0.13.0325.03\",\"version_code\":50020088,\"os\":\"android\",\"os_version\":\"13\",\"install_ch\":\"ps\",\"device_id\":\"$deviceId\",\"install_store\":\"ps\",\"gaid\":\"1b2212c1-dadf-43c3-a0c8-bd6ce48ae22d\",\"brand\":\"${currentBrandModel.first}\",\"model\":\"${currentBrandModel.second}\",\"system_language\":\"en\",\"net\":\"NETWORK_WIFI\",\"region\":\"US\",\"timezone\":\"Asia/Calcutta\",\"sp_code\":\"\",\"X-Play-Mode\":\"1\",\"X-Idle-Data\":\"1\",\"X-Family-Mode\":\"0\",\"X-Content-Mode\":\"0\"}"
        val xClientToken = generateXClientToken()
        // Always pass ACCEPT_JSON as accept parameter — required for valid signature
        val xTrSignature = generateXTrSignature(method, ACCEPT_JSON, actualContentType, url, body, useAltKey)

        val map = mutableMapOf(
            "X-Client-Info"   to clientInfo,
            "X-Client-Status" to "0",
            "X-Client-Token"  to xClientToken,
            "X-Tr-Signature"  to xTrSignature,
            "User-Agent"      to "com.community.oneroom/50020088 (Linux; U; Android 13; en_US; ${currentBrandModel.first}; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
            "Accept"          to ACCEPT_JSON
        )
        if (actualContentType != null) {
            map["Content-Type"] = actualContentType
        }
        xUserToken?.let { map["Authorization"] = "Bearer $it" }
        return map
    }
}
