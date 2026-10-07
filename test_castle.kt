import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

fun main() {
    val client = OkHttpClient()
    
    // 1. Get Security Key
    val keyReq = Request.Builder()
        .url("https://api.hlowb.com/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US")
        .build()
        
    val keyResp = client.newCall(keyReq).execute()
    val keyJsonStr = keyResp.body?.string() ?: ""
    println("Security Key Response: $keyJsonStr")
    
    val apiKeyB64 = JSONObject(keyJsonStr).getJSONObject("data").getString("securityKey")
    println("Extracted Key B64: $apiKeyB64")
    
    // 2. Derive AES Key
    val apiKeyBytes = Base64.getDecoder().decode(apiKeyB64)
    val keySupFixx = "T!BgJB".toByteArray(StandardCharsets.US_ASCII)
    var keyMaterial = apiKeyBytes + keySupFixx
    if (keyMaterial.size < 16) {
        keyMaterial = keyMaterial + ByteArray(16 - keyMaterial.size)
    } else if (keyMaterial.size > 16) {
        keyMaterial = keyMaterial.copyOfRange(0, 16)
    }
    
    // 3. Test Home Page
    val homeReq = Request.Builder()
        .url("https://api.hlowb.com/v0.1/static/home/page/1?channel=IndiaA&clientType=1&lang=en-US")
        .build()
        
    val homeResp = client.newCall(homeReq).execute()
    val homeJsonStr = homeResp.body?.string() ?: ""
    val homeDataB64 = JSONObject(homeJsonStr).getString("data")
    
    // 4. Decrypt
    val encryptedData = Base64.getDecoder().decode(homeDataB64)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKey = SecretKeySpec(keyMaterial, "AES")
    val ivSpec = IvParameterSpec(keyMaterial)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    val decrypted = cipher.doFinal(encryptedData)
    
    val decryptedStr = String(decrypted, StandardCharsets.UTF_8)
    println("Decrypted Home Page: ${decryptedStr.take(500)}")
}
