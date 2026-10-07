import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

fun main(args: Array<String>) {
    val baseUrl = "https://api.hlowb.com"
    val keySuffix = "T!BgJB".toByteArray(StandardCharsets.US_ASCII)

    fun get(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    val secUrl = "$baseUrl/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US"
    val secResp = get(secUrl)
    // Extract base64 manually since we don't have json lib
    val b64Start = secResp.indexOf("\"data\":\"") + 8
    val b64End = secResp.indexOf("\"", b64Start)
    val apiKeyB64 = secResp.substring(b64Start, b64End)

    val apiKeyBytes = Base64.getDecoder().decode(apiKeyB64)
    val keyMaterial = apiKeyBytes + keySuffix
    val finalKey = ByteArray(16)
    System.arraycopy(keyMaterial, 0, finalKey, 0, minOf(keyMaterial.size, 16))

    fun fetchAndDecrypt(url: String): String {
        var rawResponse = get(url).trim()
        if (rawResponse.startsWith("\"") && rawResponse.endsWith("\"")) {
            rawResponse = rawResponse.substring(1, rawResponse.length - 1)
        }
        val encryptedData = Base64.getDecoder().decode(rawResponse)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(finalKey, "AES"), IvParameterSpec(finalKey))
        return String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8)
    }

    val movieId = "294109" // 294109 is probably a valid one or we can fetch a valid one
    
    // search first to get valid movie ID
    val searchUrl = "$baseUrl/film-api/v1.1.0/movie/searchByKeyword?channel=IndiaA&clientType=1&clientType=1&keyword=Batman"
    val searchJson = fetchAndDecrypt(searchUrl)
    val idStart = searchJson.indexOf("\"id\":") + 5
    val idEnd = searchJson.indexOf(",", idStart)
    val realId = searchJson.substring(idStart, idEnd).trim()
    println("Got ID: $realId")
    
    val videoUrl = "$baseUrl/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US&movieId=$realId"
    println(fetchAndDecrypt(videoUrl))
}
