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
        val parsed = Uri.parse(url)
        val path = parsed.path ?: ""

        val queryNames = parsed.queryParameterNames.sorted()
        val query = if (queryNames.isNotEmpty()) {
            queryNames.joinToString("&") { key ->
                parsed.getQueryParameters(key).joinToString("&") { "$key=$it" }
            }
        } else {
            ""
        }

        val canonicalUrl = if (query.isNotEmpty()) "$path?$query" else path

        val bodyBytes = body?.toByteArray(Charsets.UTF_8)
        val bodyHash = if (bodyBytes != null) {
            val trimmed =
                if (bodyBytes.size > 102400) bodyBytes.copyOfRange(0, 102400) else bodyBytes
            md5(trimmed)
        } else ""

        val bodyLength = bodyBytes?.size?.toString() ?: ""

        return buildString {
            append(method.uppercase(Locale.ROOT)).append('\n')
            append(accept ?: "").append('\n')
            append(contentType ?: "").append('\n')
            append(bodyLength).append('\n')
            append(timestamp).append('\n')
            append(bodyHash).append('\n')
            append(canonicalUrl)
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
        val secretBytes = Base64.decode(Base64.decode(secretB64, Base64.DEFAULT), Base64.DEFAULT)

        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))

        val signature = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        val signatureB64 = Base64.encodeToString(signature, Base64.NO_WRAP)

        return "$timestamp|2|$signatureB64"
    }

    var xUserToken: String? = null

    fun getHeaders(
        method: String,
        accept: String? = null,
        contentType: String? = "application/json",
        url: String,
        body: String? = null,
        useAltKey: Boolean = false
    ): Map<String, String> {
        val actualContentType = if (method.uppercase(Locale.ROOT) == "GET") null else contentType
        val clientInfo = "{\"package_name\":\"com.community.mbox.in\",\"version_name\":\"3.0.03.0529.03\",\"version_code\":50020042,\"os\":\"android\",\"os_version\":\"16\",\"device_id\":\"$deviceId\",\"install_store\":\"ps\",\"gaid\":\"d7578036d13336cc\",\"brand\":\"${currentBrandModel.first}\",\"model\":\"${currentBrandModel.second}\",\"system_language\":\"en\",\"net\":\"NETWORK_WIFI\",\"region\":\"IN\",\"timezone\":\"Asia/Calcutta\",\"sp_code\":\"\"}"
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature(method, accept, actualContentType, url, body, useAltKey)

        val map = mutableMapOf(
            "X-Client-Info" to clientInfo,
            "X-Client-Status" to "0",
            "X-Client-Token" to xClientToken,
            "X-Tr-Signature" to xTrSignature,
            "User-Agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)"
        )
        if (actualContentType != null) {
            map["Content-Type"] = actualContentType
        }
        xUserToken?.let { map["Authorization"] = "Bearer $it" }
        return map
    }
}
