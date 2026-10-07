package com.cinetheta.providers.castletv

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object CastleTvCrypto {
    private val keySupFixx = "T!BgJB".toByteArray(StandardCharsets.US_ASCII)
    private var cachedAesKey: ByteArray? = null
    private val baseUrl = "https://api.hlowb.com"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        return client.newCall(request).execute().body?.string() ?: ""
    }

    suspend fun getAesKey(): ByteArray {
        cachedAesKey?.let { return it }
        
        val url = "$baseUrl/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US"
        val response = get(url)
        val jsonObj = Json.parseToJsonElement(response).jsonObject
        val apiKeyB64 = jsonObj["data"]?.jsonPrimitive?.content ?: throw Exception("Failed to get security key")
        
        val apiKeyBytes = Base64.decode(apiKeyB64, Base64.DEFAULT)
        val keyMaterial = apiKeyBytes + keySupFixx
        
        val finalKey = ByteArray(16)
        if (keyMaterial.size < 16) {
            System.arraycopy(keyMaterial, 0, finalKey, 0, keyMaterial.size)
        } else {
            System.arraycopy(keyMaterial, 0, finalKey, 0, 16)
        }
        
        cachedAesKey = finalKey
        return finalKey
    }

    suspend fun fetchAndDecrypt(url: String): JsonObject {
        val key = getAesKey()
        val rawResponse = get(url).trim().removeSurrounding("\"")
        if (rawResponse.startsWith("{")) {
            return Json.parseToJsonElement(rawResponse).jsonObject
        }
        val encryptedData = Base64.decode(rawResponse, Base64.DEFAULT)
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        
        val decryptedBytes = cipher.doFinal(encryptedData)
        return Json.parseToJsonElement(String(decryptedBytes, StandardCharsets.UTF_8)).jsonObject
    }

    suspend fun postAndDecrypt(url: String, jsonBody: String): JsonObject {
        val key = getAesKey()
        
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonBody.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .post(body)
            .build()
            
        val rawResponse = client.newCall(request).execute().body?.string()?.trim()?.removeSurrounding("\"") ?: ""
        if (rawResponse.startsWith("{")) {
            return Json.parseToJsonElement(rawResponse).jsonObject
        }
        val encryptedData = Base64.decode(rawResponse, Base64.DEFAULT)
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        
        val decryptedBytes = cipher.doFinal(encryptedData)
        return Json.parseToJsonElement(String(decryptedBytes, StandardCharsets.UTF_8)).jsonObject
    }
}
