package com.streamflex.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.streamflex.app.di.ProviderModule
import com.streamflex.player.episodes.PlayerEpisode
import com.streamflex.player.ui.PlayerController
import com.streamflex.player.ui.PlayerScreen
import com.streamflex.player.media3.Media3PlayerFactory
import com.streamflex.player.resume.PlaybackProgressManager

class PlayerActivity : ComponentActivity() {
    
    private var playerController: PlayerController? = null
    
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(com.streamflex.app.di.RepositoryModule.streamRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide System Bars for Immersive Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Extract intent data
        val mediaId = intent.getStringExtra("MEDIA_ID") ?: "unknown"
        val videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "Unknown Media"
        val videoYear = intent.getIntExtra("VIDEO_YEAR", 0)
        val isShow = intent.getBooleanExtra("IS_SHOW", false)
        val posterPath = intent.getStringExtra("POSTER_PATH")
        
        val epIds = intent.getStringArrayListExtra("EPISODE_IDS") ?: arrayListOf()
        val epTitles = intent.getStringArrayListExtra("EPISODE_TITLES") ?: arrayListOf()
        val epSeasons = intent.getIntegerArrayListExtra("EPISODE_SEASONS") ?: arrayListOf()
        val epNumbers = intent.getIntegerArrayListExtra("EPISODE_NUMBERS") ?: arrayListOf()
        val epStills = intent.getStringArrayListExtra("EPISODE_STILLS") ?: arrayListOf()
        
        val currentEpisodeId = intent.getStringExtra("CURRENT_EPISODE_ID")
        
        val episodes = epIds.mapIndexed { index, id ->
            PlayerEpisode(
                id = id,
                title = epTitles.getOrNull(index) ?: "Episode ${epNumbers.getOrNull(index)}",
                seasonNumber = epSeasons.getOrNull(index) ?: 1,
                episodeNumber = epNumbers.getOrNull(index) ?: (index + 1),
                stillPath = epStills.getOrNull(index)
            )
        }.toMutableList()
        
        var currentEpisode = episodes.find { it.id == currentEpisodeId }
        
        if (currentEpisode == null && currentEpisodeId != null && isShow) {
            currentEpisode = PlayerEpisode(id = currentEpisodeId, title = "Resumed Episode", seasonNumber = 1, episodeNumber = 1)
            episodes.add(currentEpisode)
        }
        
        val session = PlayerSession(
            mediaId = mediaId,
            title = videoTitle,
            year = videoYear,
            isShow = isShow,
            episodes = episodes,
            currentEpisode = currentEpisode
        )
        
        viewModel.initializeSession(session)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            
            // Re-create the controller if it's null
            val controller = remember { 
                val player = Media3PlayerFactory.create(context)
                val progressManager = PlaybackProgressManager(context)
                PlayerController(
                    player = player, 
                    progressManager = progressManager, 
                    mediaId = mediaId, 
                    scope = scope, 
                    viewModel = viewModel,
                    title = videoTitle,
                    type = if (isShow) "TV" else "MOVIE",
                    posterPath = posterPath
                ).also { playerController = it }
            }

            // Sync streams to controller
            LaunchedEffect(uiState.streams) {
                if (uiState.streams.isNotEmpty()) {
                    controller.setStreams(uiState.streams)
                }
            }
            
            if (uiState.isLoading && uiState.streams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Close button at top-left or top-right
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    // Centered loading content
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF3300), // Red Lava Color
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val baseTitle = session.title
                val videoSubtitle = uiState.session?.currentEpisode?.let { ep ->
                    val s = ep.seasonNumber.toString().padStart(2, '0')
                    val e = ep.episodeNumber.toString().padStart(2, '0')
                    "S$s E$e - ${ep.title}"
                }
                
                PlayerScreen(
                    controller = controller,
                    videoTitle = baseTitle,
                    videoSubtitle = videoSubtitle,
                    onBack = { finish() }
                )
            }
        }
    }
    

    fun triggerPiP() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerController?.setPiPMode(isInPictureInPictureMode)
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