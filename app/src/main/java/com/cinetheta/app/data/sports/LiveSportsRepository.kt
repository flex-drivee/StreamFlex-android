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
    val headers: Map<String, String>,
    val backupUrls: List<String> = emptyList()
)

object LiveSportsRepository {

    private const val TAG = "LiveSportsRepository"
    private const val BASE_URL = "https://damitv.st"

    private val client by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
        com.cinetheta.core.network.DohProviders.applyDoh(builder).build()
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

    private val pkHeaders: Map<String, String>
        get() = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
        )

    private val tapmadHeaders: Map<String, String>
        get() = mapOf(
            "Referer" to "https://tapmad.com/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        )

    private data class CustomChannel(
        val id: String,
        val title: String,
        val category: String,
        val streamUrl: String,
        val backupUrls: List<String> = emptyList(),
        val poster: String?,
        val league: String?,
        val customHeaders: Map<String, String>? = null
    )

    private val customChannels = listOf(
        // ── Cricket & Sports Channels (Tested & Verified 200 OK) ──
        CustomChannel(
            id = "ptv_sports_hd",
            title = "PTV Sports HD",
            category = "cricket",
            streamUrl = "https://lbgo.bozztv.com/ssh101/ssh101/pksportshd/playlist.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/0/07/PTV_Sports_logo.svg/320px-PTV_Sports_logo.svg.png",
            league = "Pakistan Sports #1",
            customHeaders = pkHeaders
        ),
        CustomChannel(
            id = "ten_sports_pk",
            title = "Ten Sports PK (Tapmad Feed)",
            category = "cricket",
            streamUrl = "https://tapmadlive.akamaized.net/tapmadold/tensports.smil/playlist.m3u8",
            backupUrls = listOf("http://116.90.120.149:8000/play/a11t/index.m3u8"),
            poster = "https://upload.wikimedia.org/wikipedia/en/9/91/Ten_Cricket_logo.png",
            league = "Live Cricket & Sports",
            customHeaders = tapmadHeaders
        ),
        CustomChannel(
            id = "geo_super",
            title = "Geo Super Live",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a021/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/b/b3/Geo_Super_logo.png/320px-Geo_Super_logo.png",
            league = "Geo Sports PK"
        ),
        CustomChannel(
            id = "star_sports_1_hd",
            title = "Star Sports 1 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a11x/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/e/e6/Star_Sports_1_logo.svg/320px-Star_Sports_1_logo.svg.png",
            league = "Live Cricket & Sports"
        ),
        CustomChannel(
            id = "star_sports_2_hd",
            title = "Star Sports 2 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a120/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/4/4e/Star_Sports_2_logo.svg/320px-Star_Sports_2_logo.svg.png",
            league = "Live Cricket & Sports"
        ),
        CustomChannel(
            id = "star_sports_1_hindi",
            title = "Star Sports 1 Hindi HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a122/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/e/e6/Star_Sports_1_logo.svg/320px-Star_Sports_1_logo.svg.png",
            league = "Hindi Cricket & Sports"
        ),
        CustomChannel(
            id = "star_sports_select_1",
            title = "Star Sports Select 1 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a12s/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/e/e6/Star_Sports_1_logo.svg/320px-Star_Sports_1_logo.svg.png",
            league = "Live Premier Sports"
        ),
        CustomChannel(
            id = "sony_ten_1_hd",
            title = "Sony Sports Ten 1 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a11t/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Sony_Sports_Ten_1.png",
            league = "Live Cricket & Sports"
        ),
        CustomChannel(
            id = "sony_ten_2_hd",
            title = "Sony Sports Ten 2 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a137/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Sony_Sports_Ten_2.png",
            league = "Live Cricket & Sports"
        ),
        CustomChannel(
            id = "eurosport_hd",
            title = "Eurosport HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a118/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/Eurosport_logo_2015.svg/320px-Eurosport_logo_2015.svg.png",
            league = "European Live Sports"
        ),
        CustomChannel(
            id = "unite8_sports_1",
            title = "Unite8 Sports 1 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a125/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Sony_Sports_Ten_1.png",
            league = "Live Sports"
        ),
        CustomChannel(
            id = "unite8_sports_2",
            title = "Unite8 Sports 2 HD",
            category = "cricket",
            streamUrl = "http://116.90.120.149:8000/play/a129/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Sony_Sports_Ten_2.png",
            league = "Live Sports"
        ),

        // ── Entertainment Channels (Pakistan & India) ─────────────
        CustomChannel(
            id = "hum_tv_hd",
            title = "Hum TV HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a0r2/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/b/bf/Hum_TV_Logo_2021.png",
            league = "Pakistan Drama"
        ),
        CustomChannel(
            id = "sony_tv_hd",
            title = "SET HD / Sony TV",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a11u/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/d/df/Sony_Entertainment_Television_logo.png",
            league = "Sony Entertainment"
        ),
        CustomChannel(
            id = "sony_sab_hd",
            title = "Sony SAB HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a128/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/7/7b/Sony_SAB_logo.png",
            league = "Comedy & Drama"
        ),
        CustomChannel(
            id = "zee_tv_hd",
            title = "Zee TV HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a11v/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/9/91/Zee_TV_logo.png",
            league = "Zee Entertainment"
        ),
        CustomChannel(
            id = "geo_ent",
            title = "Geo Entertainment HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a020/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Geo_Entertainment_Logo.png/320px-Geo_Entertainment_Logo.png",
            league = "Geo Entertainment"
        ),
        CustomChannel(
            id = "star_plus_hd",
            title = "Star Plus HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a11w/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/a/a2/Star_Plus_logo.png/320px-Star_Plus_logo.png",
            league = "Star Plus Drama"
        ),
        CustomChannel(
            id = "star_bharat_hd",
            title = "Star Bharat HD",
            category = "entertainment",
            streamUrl = "http://116.90.120.149:8000/play/a13b/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/thumb/e/e4/Star_Bharat_logo.png/320px-Star_Bharat_logo.png",
            league = "Star Bharat"
        ),
        CustomChannel(
            id = "9x_jalwa",
            title = "9X Jalwa",
            category = "entertainment",
            streamUrl = "https://wiselp.wiseplayout.com/9X_Jalwa/master.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/b/b3/9X_Jalwa_logo.png",
            league = "Hindi Hits 24/7"
        ),
        CustomChannel(
            id = "b4u_music",
            title = "B4U Music HD",
            category = "entertainment",
            streamUrl = "https://amg01408-amg01408c5-amgplt0747.playout.now3.amagi.tv/b4um001/playlist.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/c/cb/B4U_Music_logo.png",
            league = "Bollywood Music"
        ),

        // ── Movies Channels ────────────────────────────────────────
        CustomChannel(
            id = "sony_max_hd",
            title = "Sony MAX HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a123/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/e/ea/Sony_Max_logo.png",
            league = "Blockbuster Movies"
        ),
        CustomChannel(
            id = "sony_pix_hd",
            title = "Sony Pix HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a138/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/4/4f/Sony_Pix_logo.png",
            league = "Hollywood Blockbusters"
        ),
        CustomChannel(
            id = "star_gold_hd",
            title = "Star Gold HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a12a/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/5/53/Star_Gold_logo.png",
            league = "Bollywood Movies"
        ),
        CustomChannel(
            id = "star_gold_2_hd",
            title = "Star Gold 2 HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a12b/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/9/97/Star_Gold_2_logo.png",
            league = "Hindi Cinema"
        ),
        CustomChannel(
            id = "star_movies_hd",
            title = "Star Movies HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a13a/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Star_Movies_logo.svg/320px-Star_Movies_logo.svg.png",
            league = "Hollywood Blockbusters"
        ),
        CustomChannel(
            id = "star_movies_select",
            title = "Star Movies Select HD",
            category = "movies",
            streamUrl = "http://116.90.120.149:8000/play/a139/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Star_Movies_logo.svg/320px-Star_Movies_logo.svg.png",
            league = "Cinema Classics"
        ),
        CustomChannel(
            id = "b4u_movies",
            title = "B4U Movies",
            category = "movies",
            streamUrl = "https://streams.tangotv.in/B4UMOVIES/ORIGIN/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/6/66/B4U_Movies_Logo.png",
            league = "Bollywood Cinema"
        ),
        CustomChannel(
            id = "b4u_kadak",
            title = "B4U Kadak",
            category = "movies",
            streamUrl = "https://streams.tangotv.in/B4UKADAK/ORIGIN/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/2/22/B4U_Kadak.png",
            league = "Desi Action Cinema"
        ),

        // ── Pakistan & Indian News Channels ────────────────────────
        CustomChannel(
            id = "dunya_news",
            title = "Dunya News HD",
            category = "news",
            streamUrl = "http://116.90.120.149:8000/play/a0sc/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/e/e0/Dunya_News_logo.png",
            league = "Pakistan News 24/7"
        ),
        CustomChannel(
            id = "geo_news",
            title = "Geo News HD",
            category = "news",
            streamUrl = "http://116.90.120.149:8000/play/a023/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/Geo_News_logo.svg/320px-Geo_News_logo.svg.png",
            league = "Pakistan News"
        ),
        CustomChannel(
            id = "hum_news",
            title = "Hum News HD",
            category = "news",
            streamUrl = "http://116.90.120.149:8000/play/a024/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/en/b/bf/Hum_TV_Logo_2021.png",
            league = "Pakistan News"
        ),
        CustomChannel(
            id = "samaa_news",
            title = "SAMAA News HD",
            category = "news",
            streamUrl = "http://116.90.120.149:8000/play/a0ro/index.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Samaa_TV_logo.svg/320px-Samaa_TV_logo.svg.png",
            league = "Pakistan News 24/7"
        ),
        CustomChannel(
            id = "aaj_tak_hd",
            title = "Aaj Tak HD",
            category = "news",
            streamUrl = "https://feeds.intoday.in/aajtak/api/aajtakhd/master.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/2/28/Aaj_tak_logo.png",
            league = "India News"
        ),
        CustomChannel(
            id = "abp_news",
            title = "ABP News HD",
            category = "news",
            streamUrl = "https://d1rc86nwwc9fag.cloudfront.net/vglive-sk-472500/abpnews/master.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/e/ec/ABP_News_logo.png",
            league = "India News"
        ),
        CustomChannel(
            id = "india_today",
            title = "India Today",
            category = "news",
            streamUrl = "https://d1rc86nwwc9fag.cloudfront.net/vglive-sk-293160/master.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/b/b5/India_Today_logo.png",
            league = "India News"
        ),
        CustomChannel(
            id = "zee_news",
            title = "Zee News",
            category = "news",
            streamUrl = "https://dknttpxmr0dwf.cloudfront.net/index_57.m3u8",
            poster = "https://upload.wikimedia.org/wikipedia/commons/1/15/Zee_News_Logo.png",
            league = "India News"
        )
    )

    /**
     * Fetches all live sports events, news, entertainment, and movie channels.
     */
    suspend fun getLiveSports(): List<LiveSportItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LiveSportItem>()

        try {
            val reqBuilder = Request.Builder().url("$BASE_URL/papi/matches/all")
            defaultHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val array = JSONArray(body)
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
                                isAlwaysLive = isAlwaysLive,
                                poster = poster,
                                league = league,
                                homeTeam = homeName,
                                awayTeam = awayName,
                                status = status,
                                viewers = obj.optInt("viewers", 0)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live sports: ${e.message}", e)
        }

        // Fetch dynamic Live Sports & Tapmad match events from PlayEZTV feed
        val playEzEvents = try {
            PlayEzTvService.fetchLiveEvents()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching PlayEZTV events: ${e.message}", e)
            emptyList()
        }

        // Add verified 24/7 channels (Indian/Pakistani news, entertainment, movies)
        val customItems = customChannels.map { ch ->
            LiveSportItem(
                id = "custom_${ch.id}",
                title = ch.title,
                category = ch.category,
                date = null,
                isLive = true,
                isAlwaysLive = true,
                poster = ch.poster,
                league = ch.league,
                homeTeam = null,
                awayTeam = null,
                status = "live",
                viewers = null
            )
        }

        // Prioritize live items first, then upcoming
        (list + playEzEvents + customItems).distinctBy { it.id }.sortedWith(
            compareByDescending<LiveSportItem> { it.isLive }
                .thenBy { it.date ?: Long.MAX_VALUE }
        )
    }

    /**
     * Resolves the direct HLS .m3u8 stream for a given match/channel ID.
     */
    suspend fun resolveStream(eventId: String): LiveStreamResult? = withContext(Dispatchers.IO) {
        // 1. Check custom 24/7 channels first
        val custom = customChannels.find { "custom_${it.id}" == eventId || it.id == eventId }
        if (custom != null) {
            return@withContext LiveStreamResult(
                url = custom.streamUrl,
                headers = custom.customHeaders ?: playHeaders,
                backupUrls = custom.backupUrls
            )
        }

        // 2. Check dynamic PlayEZTV / Tapmad live sports events
        if (eventId.startsWith("playez_")) {
            val playEzStream = PlayEzTvService.resolveStream(eventId)
            if (playEzStream != null) {
                return@withContext playEzStream
            }
        }

        // 3. Fallback to DamiTV extraction
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
            val sdUrl = obj.optString("sdUrl").takeIf { it.isNotBlank() }

            val primaryUrl = hlsUrl ?: sdUrl
            if (primaryUrl != null) {
                val backups = mutableListOf<String>()
                if (sdUrl != null && sdUrl != primaryUrl) {
                    backups.add(sdUrl)
                }
                val sdStreams = obj.optJSONArray("sdStreams")
                if (sdStreams != null) {
                    for (i in 0 until sdStreams.length()) {
                        val sUrl = sdStreams.optJSONObject(i)?.optString("url")
                        if (!sUrl.isNullOrBlank() && sUrl.contains(".m3u8") && !backups.contains(sUrl)) {
                            backups.add(sUrl)
                        }
                    }
                }
                LiveStreamResult(
                    url = primaryUrl,
                    headers = playHeaders,
                    backupUrls = backups
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
