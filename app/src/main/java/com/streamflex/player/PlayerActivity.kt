package com.streamflex.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.Quality
import com.streamflex.core.network.detector.ContentType
import com.streamflex.player.ui.PlayerController
import com.streamflex.player.ui.PlayerScreen
import com.streamflex.player.media3.Media3PlayerFactory
import com.streamflex.player.resume.PlaybackProgressManager

class PlayerActivity : ComponentActivity() {
    
    private var playerController: PlayerController? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide System Bars for Immersive Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val videoUrls = intent.getStringArrayListExtra("VIDEO_URLS") ?: arrayListOf()
        val videoReferers = intent.getStringArrayListExtra("VIDEO_REFERERS") ?: arrayListOf()
        val videoCookies = intent.getStringArrayListExtra("VIDEO_COOKIES") ?: arrayListOf()
        val videoUserAgents = intent.getStringArrayListExtra("VIDEO_USER_AGENTS") ?: arrayListOf()
        
        val videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "Unknown Media"
        val tmdbId = intent.getIntExtra("TMDB_ID", -1)
        val season = intent.getIntExtra("SEASON", -1)
        val episode = intent.getIntExtra("EPISODE", -1)
        
        val uniqueMediaId = if (tmdbId != -1) {
            if (season != -1 && episode != -1) "tmdb_${tmdbId}_S${season}E${episode}" else "tmdb_${tmdbId}"
        } else {
            videoTitle
        }

        setContent {
            val dynamicStreams by StreamStateHolder.streams.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            
            val controller = remember { 
                val player = Media3PlayerFactory.create(context)
                val progressManager = PlaybackProgressManager(context)
                PlayerController(player, progressManager, uniqueMediaId, scope).also { playerController = it }
            }

            // Combine initial intent URLs with dynamically discovered streams
            val allStreams = remember(videoUrls, dynamicStreams) {
                val initialLinks = videoUrls.mapIndexed { index, url ->
                    val cookieStr = videoCookies.getOrNull(index) ?: ""
                    val refererStr = videoReferers.getOrNull(index) ?: ""
                    val uaStr = videoUserAgents.getOrNull(index) ?: ""
                    val hdrs = mutableMapOf<String, String>()
                    if (refererStr.isNotBlank()) hdrs["Referer"] = refererStr
                    if (uaStr.isNotBlank()) hdrs["User-Agent"] = uaStr
                    if (cookieStr.isNotBlank()) hdrs["Cookie"] = cookieStr

                    val cookiesMap = mutableMapOf<String, String>()
                    if (cookieStr.isNotBlank()) {
                        cookieStr.split(";").forEach { c ->
                            val p = c.trim().split("=", limit = 2)
                            if (p.size == 2) cookiesMap[p[0].trim()] = p[1].trim()
                        }
                    }

                    StreamLink(
                        name = "Source ${index + 1}",
                        url = url,
                        quality = Quality.UNKNOWN,
                        host = HostType.DIRECT,
                        referer = refererStr,
                        cookies = cookiesMap,
                        headers = hdrs,
                        contentType = when {
                            url.contains(".mpd") -> ContentType.DASH
                            url.contains(".m3u8") -> ContentType.M3U8
                            else -> ContentType.VIDEO
                        },
                        adaptive = url.contains(".mpd") || url.contains(".m3u8")
                    )
                }
                
                val combined = if (dynamicStreams.isNotEmpty()) {
                    (dynamicStreams + initialLinks).distinctBy { it.url }
                } else {
                    initialLinks
                }
                
                // Automatically set streams in the controller
                controller.setStreams(combined)
                
                combined
            }

            val currentEpisode by StreamStateHolder.currentEpisode.collectAsState()
            
            val baseTitle = intent.getStringExtra("VIDEO_TITLE") ?: "Unknown Media"
            val baseSubtitle = intent.getStringExtra("VIDEO_SUBTITLE")
            
            val videoSubtitle = currentEpisode?.let { ep ->
                val s = ep.seasonNumber.toString().padStart(2, '0')
                val e = ep.episodeNumber.toString().padStart(2, '0')
                "S$s E$e - ${ep.title}"
            } ?: baseSubtitle
            
            PlayerScreen(
                controller = controller,
                videoTitle = baseTitle,
                videoSubtitle = videoSubtitle,
                onBack = { finish() }
            )
        }
    }
    
    override fun onPause() {
        super.onPause()
        playerController?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        playerController?.release()
    }
}