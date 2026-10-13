package com.cinetheta.app.data.sports

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LiveStreamResult(
    val url: String,
    val headers: Map<String, String>
)

object LiveSportsRepository {

    private const val TAG = "LiveSportsRepository"
    private const val BASE_URL = "https://damitv.st"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val defaultHeaders: Map<String, String>
        get() = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Accept" to "application/json, text/plain, */*",
            "Referer" to "$BASE_URL/",
            "Origin" to BASE_URL
        )

    val playHeaders: Map<String, String>
        get() = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
            "Referer" to "$BASE_URL/",
            "Origin" to BASE_URL,
            "Accept" to "*/*"
        )

    /**
     * Fetches all live sports events and 24/7 channels from DamiTV.
     */
    suspend fun getLiveSports(): List<LiveSportItem> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder().url("$BASE_URL/papi/matches/all")
            defaultHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch live sports: HTTP ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(body)
            val list = mutableListOf<LiveSportItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isBlank()) continue

                val title = obj.optString("title", "Live Event")
                val category = obj.optString("category", "other")
                val date = if (obj.has("date")) obj.optLong("date") else null
                val status = obj.optString("status", "")
                val isAlwaysLive = obj.optBoolean("always_live", false)
                val isLive = isAlwaysLive || status.equals("live", ignoreCase = true) || (obj.optInt("viewers", 0) > 0)
                val poster = obj.optString("poster", "").takeIf { it.isNotBlank() }
                val league = obj.optString("league", "").takeIf { it.isNotBlank() }

                var homeName: String? = null
                var awayName: String? = null
                if (obj.has("teams")) {
                    val teams = obj.optJSONObject("teams")
                    homeName = teams?.optJSONObject("home")?.optString("name")
                    awayName = teams?.optJSONObject("away")?.optString("name")
                }

                list.add(
                    LiveSportItem(
                        id = id,
                        title = title,
                        category = category,
                        date = date,
                        isLive = isLive,
                        poster = poster,
                        league = league,
                        homeTeam = homeName,
                        awayTeam = awayName,
                        status = status,
                        viewers = obj.optInt("viewers", 0)
                    )
                )
            }

            // Prioritize live items first, then upcoming
            list.sortedWith(compareByDescending<LiveSportItem> { it.isLive }.thenBy { it.date ?: Long.MAX_VALUE })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live sports: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Resolves the direct HLS .m3u8 stream for a given match/channel ID.
     */
    suspend fun resolveStream(eventId: String): LiveStreamResult? = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder().url("$BASE_URL/papi/extract-url/$eventId")
            defaultHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to extract stream for $eventId: HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val obj = JSONObject(body)
            if (!obj.optBoolean("success", false)) {
                return@withContext null
            }

            val hlsUrl = obj.optString("hlsUrl").takeIf { it.isNotBlank() }
                ?: obj.optString("sdUrl").takeIf { it.isNotBlank() }

            if (hlsUrl != null) {
                LiveStreamResult(
                    url = hlsUrl,
                    headers = playHeaders
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving stream for $eventId: ${e.message}", e)
            null
        }
    }
}
