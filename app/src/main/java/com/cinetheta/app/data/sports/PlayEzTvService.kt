package com.cinetheta.app.data.sports

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service to fetch and resolve live sports streams and Tapmad events from PlayEZTV.
 */
object PlayEzTvService {

    private const val TAG = "PlayEzTvService"
    private const val BASE_URL = "https://pabbdyzone.shop"

    private val client by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
        com.cinetheta.core.network.DohProviders.applyDoh(builder).build()
    }

    private val NATIVE_KEY = byteArrayOf(99, 122, 49, 52, 82, 83, 116, 107, 78, 48, 49, 80, 86, 69, 53, 119)
    private val NATIVE_IV = byteArrayOf(87, 84, 108, 69, 118, 99, 107, 100, 50, 85, 82, 52, 49, 115, 100, 107)

    private val FALLBACK_KEY = byteArrayOf(109, 53, 75, 108, 53, 110, 107, 52, 120, 75, 49, 107, 78, 55, 112, 78)
    private val FALLBACK_IV = byteArrayOf(107, 53, 75, 52, 110, 77, 56, 109, 75, 108, 78, 76, 55, 108, 49, 53)

    // Cache eventId -> linksPath (e.g. "pro/...txt")
    private val eventLinksCache = ConcurrentHashMap<String, String>()

    private fun swapAdjacentPairs(bytes: ByteArray): ByteArray {
        val out = bytes.clone()
        for (i in 0 until out.size - 1 step 2) {
            val tmp = out[i]
            out[i] = out[i + 1]
            out[i + 1] = tmp
        }
        return out
    }

    private fun reverseBytes(bytes: ByteArray): ByteArray {
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            out[i] = bytes[bytes.size - 1 - i]
        }
        return out
    }

    private fun decryptPayload(raw: String): String? {
        val stripped = raw.replace("\\s+".toRegex(), "")
        if (stripped.startsWith("{") || stripped.startsWith("[")) {
            return stripped
        }

        // Strategy 1: Native (Byte-swap + AES CBC)
        try {
            val b1 = Base64.decode(stripped, Base64.DEFAULT)
            val b1Rev = reverseBytes(b1)
            val b2 = swapAdjacentPairs(b1Rev)
            var b2Str = String(b2, StandardCharsets.US_ASCII).replace("\\s+".toRegex(), "")
            while (b2Str.length % 4 != 0) {
                b2Str += "="
            }
            val b3 = Base64.decode(b2Str, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(NATIVE_KEY, "AES"), IvParameterSpec(NATIVE_IV))
            val pt = cipher.doFinal(b3)
            val text = String(pt, StandardCharsets.UTF_8).trim()
            if (text.startsWith("{") || text.startsWith("[")) {
                return text
            }
        } catch (_: Exception) {
            // Try fallback strategy
        }

        // Strategy 2: Direct Fallback AES CBC
        try {
            var b64 = stripped
            while (b64.length % 4 != 0) {
                b64 += "="
            }
            val ct = Base64.decode(b64, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(FALLBACK_KEY, "AES"), IvParameterSpec(FALLBACK_IV))
            val pt = cipher.doFinal(ct)
            val text = String(pt, StandardCharsets.UTF_8).trim()
            if (text.startsWith("{") || text.startsWith("[")) {
                return text
            }
        } catch (_: Exception) {
            // Failed
        }

        return null
    }

    private fun parseUtcTimestamp(dateStr: String?, timeStr: String?): Long? {
        if (dateStr.isNullOrBlank() || timeStr.isNullOrBlank()) return null
        return try {
            val cleanDate = dateStr.trim()
            val cleanTime = timeStr.trim()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsedDate = if (cleanTime.count { it == ':' } == 1) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse("$cleanDate $cleanTime")
            } else {
                sdf.parse("$cleanDate $cleanTime")
            }
            parsedDate?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches live sports events from PlayEZTV feed and maps them to LiveSportItem.
     */
    suspend fun fetchLiveEvents(): List<LiveSportItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LiveSportItem>()
        try {
            val req = Request.Builder()
                .url("$BASE_URL/events.txt")
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 10; SM-A505F)")
                .build()

            val response = client.newCall(req).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch events: HTTP ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val decrypted = decryptPayload(body) ?: return@withContext emptyList()

            val array = JSONArray(decrypted)
            val now = System.currentTimeMillis()

            for (i in 0 until array.length()) {
                val itemObj = array.getJSONObject(i)
                val eventStr = itemObj.optString("event")
                if (eventStr.isBlank()) continue

                val event = JSONObject(eventStr)
                if (event.optBoolean("visible", true) == false) continue

                val linksPath = event.optString("links").takeIf { it.isNotBlank() } ?: continue
                val categoryRaw = event.optString("category", "Sports").trim()
                val eventName = event.optString("eventName", "Live Sports").trim()
                val teamA = event.optString("teamAName", "").trim()
                val teamB = event.optString("teamBName", "").trim()
                val teamAFlag = event.optString("teamAFlag", "").takeIf { it.isNotBlank() }
                val teamBFlag = event.optString("teamBFlag", "").takeIf { it.isNotBlank() }
                val eventLogo = event.optString("eventLogo", "").takeIf { it.isNotBlank() }
                val dateStr = event.optString("date", "")
                val timeStr = event.optString("time", "")
                val endDateStr = event.optString("end_date", "")
                val endTimeStr = event.optString("end_time", "")

                val startTime = parseUtcTimestamp(dateStr, timeStr)
                val endTime = parseUtcTimestamp(endDateStr, endTimeStr)

                // Determine live status
                val isEnded = endTime != null && now >= endTime
                if (isEnded) continue // Skip ended matches

                val isLive = (startTime != null && now >= startTime) || (startTime == null)
                val status = if (isLive) "live" else "upcoming"

                val title = if (teamA.isNotBlank() && teamB.isNotBlank()) {
                    "$teamA vs $teamB"
                } else if (teamA.isNotBlank()) {
                    teamA
                } else {
                    eventName
                }

                val poster = eventLogo ?: teamAFlag ?: teamBFlag
                val eventId = "playez_${i}_${linksPath.replace("[^a-zA-Z0-9]".toRegex(), "")}"
                eventLinksCache[eventId] = linksPath

                // Check if this event has a Tapmad feed
                val linkNames = event.optJSONArray("link_names")
                var hasTapmad = false
                if (linkNames != null) {
                    for (k in 0 until linkNames.length()) {
                        if (linkNames.optString(k).contains("tapmad", ignoreCase = true)) {
                            hasTapmad = true
                            break
                        }
                    }
                }

                val displayCategory = categoryRaw.lowercase()
                val leagueDisplay = if (hasTapmad) "$eventName • Tapmad" else eventName

                result.add(
                    LiveSportItem(
                        id = eventId,
                        title = title,
                        category = displayCategory,
                        date = startTime,
                        isLive = isLive,
                        isAlwaysLive = false,
                        poster = poster,
                        league = leagueDisplay,
                        homeTeam = teamA.takeIf { it.isNotBlank() },
                        awayTeam = teamB.takeIf { it.isNotBlank() },
                        status = status,
                        viewers = if (isLive) 1000 + (i * 123) % 4500 else null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching PlayEZ live events: ${e.message}", e)
        }
        result
    }

    /**
     * Resolves the best stream URL and HTTP headers for a PlayEZ event.
     */
    suspend fun resolveStream(eventId: String): LiveStreamResult? = withContext(Dispatchers.IO) {
        val linksPath = eventLinksCache[eventId] ?: return@withContext null
        try {
            val req = Request.Builder()
                .url("$BASE_URL/$linksPath")
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 10; SM-A505F)")
                .build()

            val response = client.newCall(req).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch stream links for $eventId: HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val decrypted = decryptPayload(body) ?: return@withContext null

            val array = JSONArray(decrypted)
            if (array.length() == 0) return@withContext null

            var tapmadStream: LiveStreamResult? = null
            var bestHlsStream: LiveStreamResult? = null
            var fallbackStream: LiveStreamResult? = null

            for (i in 0 until array.length()) {
                val streamObj = array.getJSONObject(i)
                val rawLink = streamObj.optString("link", "").trim()
                if (rawLink.isBlank()) continue

                val streamName = streamObj.optString("name", "Stream $i")
                val parsed = parseStreamLinkWithHeaders(rawLink)

                if (rawLink.contains("tapmad", ignoreCase = true) || streamName.contains("tapmad", ignoreCase = true)) {
                    val tapmadHeaders = parsed.headers.toMutableMap()
                    if (!tapmadHeaders.keys.any { it.equals("Referer", ignoreCase = true) }) {
                        tapmadHeaders["Referer"] = "https://tapmad.com/"
                    }
                    if (!tapmadHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
                        tapmadHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                    }
                    tapmadStream = LiveStreamResult(url = parsed.url, headers = tapmadHeaders)
                    break // Highest priority!
                }

                if (parsed.url.contains(".m3u8", ignoreCase = true)) {
                    if (bestHlsStream == null) {
                        bestHlsStream = parsed
                    }
                } else if (fallbackStream == null) {
                    fallbackStream = parsed
                }
            }

            tapmadStream ?: bestHlsStream ?: fallbackStream
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving stream for $eventId: ${e.message}", e)
            null
        }
    }

    private fun parseStreamLinkWithHeaders(rawLink: String): LiveStreamResult {
        if (!rawLink.contains("|")) {
            return LiveStreamResult(
                url = rawLink.trim(),
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                )
            )
        }

        val parts = rawLink.split("|", limit = 2)
        val url = parts[0].trim()
        val headers = mutableMapOf<String, String>()
        headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

        if (parts.size > 1) {
            val queryParams = parts[1].trim().split("&")
            for (param in queryParams) {
                val kv = param.split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    var value = kv[1].trim()
                    try {
                        value = URLDecoder.decode(value, "UTF-8")
                    } catch (_: Exception) {}

                    val normalizedKey = when (key.lowercase()) {
                        "user-agent" -> "User-Agent"
                        "referer", "referrer" -> "Referer"
                        "origin" -> "Origin"
                        "cookie" -> "Cookie"
                        else -> key
                    }
                    headers[normalizedKey] = value
                }
            }
        }

        return LiveStreamResult(url = url, headers = headers)
    }
}
