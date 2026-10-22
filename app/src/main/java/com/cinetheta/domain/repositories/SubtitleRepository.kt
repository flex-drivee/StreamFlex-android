package com.cinetheta.domain.repositories

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.CineThetaHttpClient
import com.cinetheta.domain.models.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * SubtitleRepository
 *
 * Universal subtitle resolver that integrates Stremio's free, keyless OpenSubtitles v3 service.
 * Automatically resolves IMDb ID from TMDB ID or title, then fetches and curates subtitles
 * for any movie or TV series episode.
 *
 * Provides instant subtitle availability for direct-link and torrent-scrape providers
 * (UHDMovies, HDHub4u, PirateXplay, AnimeDekho, ToonStream, Castle TV, etc.).
 */
object SubtitleRepository {

    private const val TAG = "SubtitleRepository"
    private const val OPENSUBTITLES_BASE = "https://opensubtitles-v3.strem.io"
    private const val TMDB_BASE = "https://api.themoviedb.org/3"

    // TMDB API keys for resolving IMDb ID
    private val TMDB_KEYS = listOf(
        "938e6a6becd5cb517ec3267ea0e736fa",
        "eaa97563a1d87c75a203f24368b1e420",
        "f996a57ef8896650d793311e867d372e",
        "77fd2f3fa58f2d69ba7f815b580b3de7"
    )
    private var keyIndex = 0
    private fun getTmdbKey(): String = TMDB_KEYS[(keyIndex++) % TMDB_KEYS.size]

    // In-memory cache for fast subsequent track queries
    private val memoryCache = ConcurrentHashMap<String, List<Subtitle>>()
    private val imdbCache = ConcurrentHashMap<String, String>()

    /**
     * Resolves the IMDb ID for a movie or TV show.
     */
    suspend fun resolveImdbId(
        mediaId: String? = null,
        title: String? = null,
        year: Int? = null,
        isShow: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (!mediaId.isNullOrBlank() && mediaId.startsWith("tt", ignoreCase = true)) {
            return@withContext mediaId
        }

        val cacheKey = "${mediaId ?: ""}_${title ?: ""}_${year ?: 0}_$isShow"
        imdbCache[cacheKey]?.let { return@withContext it }

        // 1. Try by numeric TMDB ID
        val numericId = mediaId?.toIntOrNull()
        if (numericId != null && numericId > 0) {
            val resolved = fetchImdbFromTmdbId(numericId, isShow)
            if (!resolved.isNullOrBlank()) {
                imdbCache[cacheKey] = resolved
                return@withContext resolved
            }
        }

        // 2. Try by searching TMDB with title
        if (!title.isNullOrBlank()) {
            val resolved = searchImdbFromTmdbTitle(title, year, isShow)
            if (!resolved.isNullOrBlank()) {
                imdbCache[cacheKey] = resolved
                return@withContext resolved
            }
        }

        null
    }

    private fun fetchImdbFromTmdbId(tmdbId: Int, isShow: Boolean): String? {
        val apiKey = getTmdbKey()
        val url = if (isShow) {
            "$TMDB_BASE/tv/$tmdbId/external_ids?api_key=$apiKey"
        } else {
            "$TMDB_BASE/movie/$tmdbId?api_key=$apiKey"
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
                .build()
            CineThetaHttpClient.streamingOkHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return null
                    val json = JSONObject(body)
                    json.optString("imdb_id").takeIf { it.isNotBlank() && it != "null" }
                } else null
            }
        } catch (e: Throwable) {
            Logger.w("Failed to fetch IMDb ID for TMDB $tmdbId: ${e.message}", TAG)
            null
        }
    }

    private fun searchImdbFromTmdbTitle(title: String, year: Int?, isShow: Boolean): String? {
        val apiKey = getTmdbKey()
        val cleanTitle = title
            .replace(Regex("\\[.*?\\]"), "")         // Remove [Hindi Dub], [Dual Audio], etc.
            .replace(Regex("\\(.*?\\)"), "")         // Remove (2024), (Hindi), etc.
            .replace(Regex("(?i)\\b(Season|Episode|S\\d+E?\\d*|E\\d+|EP\\d+|Hindi|Tamil|Telugu|Malayalam|Kannada|Bengali|Punjabi|Urdu|Dub|Sub|Dubbed|Subbed|Dual\\s+Audio|Multi\\s+Audio|4K|2160p|1080p|720p|480p|360p|WEB-?DL|BluRay|HDRip|DVDRip|HDTV|x264|x265|HEVC|AAC|DD5\\.1|ESub)\\b.*"), "")
            .replace(Regex("[:\\-–—_.'!?]+"), " ")   // Normalize remaining punctuation
            .trim()
            .replace(Regex("\\s+"), " ")
        val encodedQuery = try { URLEncoder.encode(cleanTitle, "UTF-8") } catch (_: Exception) { cleanTitle }
        val yearParam = if (year != null && year > 1900) {
            if (isShow) "&first_air_date_year=$year" else "&year=$year"
        } else ""
        val url = if (isShow) {
            "$TMDB_BASE/search/tv?api_key=$apiKey&query=$encodedQuery$yearParam"
        } else {
            "$TMDB_BASE/search/movie?api_key=$apiKey&query=$encodedQuery$yearParam"
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
                .build()
            CineThetaHttpClient.streamingOkHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return null
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstId = results.getJSONObject(0).optInt("id")
                        if (firstId > 0) {
                            fetchImdbFromTmdbId(firstId, isShow)
                        } else null
                    } else null
                } else null
            }
        } catch (e: Throwable) {
            Logger.w("Failed to search IMDb ID for title '$title': ${e.message}", TAG)
            null
        }
    }

    /**
     * Fetches curated subtitles for a movie or TV episode.
     */
    suspend fun getSubtitles(
        mediaId: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null
    ): List<Subtitle> = withContext(Dispatchers.IO) {
        val isShow = season != null && episode != null
        val subCacheKey = "${mediaId ?: ""}_${title ?: ""}_${season ?: 0}_${episode ?: 0}"
        memoryCache[subCacheKey]?.let { return@withContext it }

        val imdbId = resolveImdbId(mediaId, title, year, isShow)
        if (imdbId.isNullOrBlank()) {
            Logger.d("Could not resolve IMDb ID for subtitles: mediaId=$mediaId title=$title", TAG)
            return@withContext emptyList()
        }

        val openSubsUrl = if (isShow) {
            "$OPENSUBTITLES_BASE/subtitles/series/$imdbId:$season:$episode.json"
        } else {
            "$OPENSUBTITLES_BASE/subtitles/movie/$imdbId.json"
        }

        try {
            val request = Request.Builder()
                .url(openSubsUrl)
                .header("User-Agent", com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT)
                .build()
            val rawSubs = CineThetaHttpClient.streamingOkHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use emptyList<Subtitle>()
                    val json = JSONObject(body)
                    val array = json.optJSONArray("subtitles") ?: return@use emptyList<Subtitle>()
                    parseOpenSubtitles(array)
                } else {
                    Logger.w("OpenSubtitles returned code ${resp.code} for $openSubsUrl", TAG)
                    emptyList()
                }
            }

            if (rawSubs.isNotEmpty()) {
                memoryCache[subCacheKey] = rawSubs
                Logger.i("Successfully loaded ${rawSubs.size} subtitles from OpenSubtitles for $imdbId", TAG)
            }
            rawSubs
        } catch (e: Throwable) {
            Logger.w("Failed to query OpenSubtitles: ${e.message}", TAG)
            emptyList()
        }
    }

    private fun parseOpenSubtitles(array: JSONArray): List<Subtitle> {
        val rawItems = mutableListOf<OpenSubItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val url = obj.optString("url")
            val lang = obj.optString("lang").lowercase().trim()
            if (url.isBlank() || lang.isBlank()) continue
            val fileName = obj.optString("subtitleFileName")
            val releaseName = obj.optString("movieReleaseName")
            rawItems.add(OpenSubItem(url, lang, fileName, releaseName))
        }

        // Group by language code
        val grouped = rawItems.groupBy { it.lang }
        val curatedList = mutableListOf<Subtitle>()

        // Preferred order for common languages
        val priorityLangs = listOf("eng", "spa", "hin", "ara", "fre", "fra", "ger", "deu", "ita", "por", "pob", "rus", "tur", "ind", "jpn", "kor", "zho", "chi")

        val sortedLangCodes = grouped.keys.sortedWith { a, b ->
            val idxA = priorityLangs.indexOf(a).let { if (it == -1) 999 else it }
            val idxB = priorityLangs.indexOf(b).let { if (it == -1) 999 else it }
            if (idxA != idxB) idxA.compareTo(idxB) else a.compareTo(b)
        }

        for (langCode in sortedLangCodes) {
            val items = grouped[langCode] ?: continue
            val langName = getLanguageDisplayName(langCode)
            // Limit to top 2 subtitles per language to keep track list clean
            items.take(2).forEachIndexed { index, item ->
                val label = if (index == 0) langName else "$langName [$index]"
                val isDefault = langCode == "eng" && index == 0
                curatedList.add(
                    Subtitle(
                        url = item.url,
                        language = langName,
                        label = label,
                        isDefault = isDefault
                    )
                )
            }
        }

        return curatedList
    }

    private data class OpenSubItem(
        val url: String,
        val lang: String,
        val fileName: String?,
        val releaseName: String?
    )

    private fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase()) {
            "eng" -> "English"
            "spa" -> "Spanish"
            "fre", "fra" -> "French"
            "ger", "deu" -> "German"
            "ita" -> "Italian"
            "por" -> "Portuguese"
            "pob" -> "Portuguese (Brazil)"
            "rus" -> "Russian"
            "hin" -> "Hindi"
            "ben" -> "Bengali"
            "ara" -> "Arabic"
            "tur" -> "Turkish"
            "ind" -> "Indonesian"
            "zho", "chi" -> "Chinese"
            "jpn" -> "Japanese"
            "kor" -> "Korean"
            "urd" -> "Urdu"
            "tam" -> "Tamil"
            "tel" -> "Telugu"
            "kan" -> "Kannada"
            "mal" -> "Malayalam"
            "mar" -> "Marathi"
            "pan" -> "Punjabi"
            "vie" -> "Vietnamese"
            "tha" -> "Thai"
            "pol" -> "Polish"
            "nld", "dut" -> "Dutch"
            "swe" -> "Swedish"
            "dan" -> "Danish"
            "fin" -> "Finnish"
            "nor" -> "Norwegian"
            "ell", "gre" -> "Greek"
            "heb" -> "Hebrew"
            "ces", "cze" -> "Czech"
            "hun" -> "Hungarian"
            "ron", "rum" -> "Romanian"
            "ukr" -> "Ukrainian"
            "bul" -> "Bulgarian"
            "hrv" -> "Croatian"
            "srp" -> "Serbian"
            "slk", "slo" -> "Slovak"
            "slv" -> "Slovenian"
            "est" -> "Estonian"
            "lav" -> "Latvian"
            "lit" -> "Lithuanian"
            "fas", "per" -> "Persian"
            else -> try {
                val loc = Locale.forLanguageTag(code)
                loc.displayLanguage.takeIf { it.isNotBlank() } ?: code.uppercase()
            } catch (_: Exception) {
                code.uppercase()
            }
        }
    }
}
