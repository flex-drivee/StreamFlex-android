package com.cinetheta.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import com.cinetheta.app.di.ProviderModule
import com.cinetheta.player.episodes.PlayerEpisode
import com.cinetheta.player.ui.PlayerController
import com.cinetheta.player.ui.PlayerScreen
import com.cinetheta.player.media3.Media3PlayerFactory
import com.cinetheta.player.resume.PlaybackProgressManager
import com.cinetheta.extractors.netmirror.NetMirrorBypassManager
import com.cinetheta.app.utils.SupportManager
import com.cinetheta.app.utils.NetMirrorBypassAdDialog

class PlayerActivity : ComponentActivity() {
    
    private var playerController: PlayerController? = null
    
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(com.cinetheta.app.di.RepositoryModule.streamRepository)
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
        val epSeasonNum = intent.getIntExtra("CURRENT_EPISODE_SEASON", 1)
        val epEpisodeNum = intent.getIntExtra("CURRENT_EPISODE_NUMBER", 1)
        val epTitle = intent.getStringExtra("CURRENT_EPISODE_TITLE") ?: "Episode $epEpisodeNum"
        val localFilePath = intent.getStringExtra("LOCAL_FILE_PATH")
        val downloadItemId = intent.getStringExtra("DOWNLOAD_ITEM_ID")
        
        if (currentEpisode == null && currentEpisodeId != null && isShow) {
            currentEpisode = PlayerEpisode(
                id = currentEpisodeId, 
                title = epTitle, 
                seasonNumber = epSeasonNum, 
                episodeNumber = epEpisodeNum
            )
            episodes.add(currentEpisode)
        }
        
        val session = PlayerSession(
            mediaId = mediaId,
            title = videoTitle,
            year = videoYear,
            isShow = isShow,
            episodes = episodes,
            currentEpisode = currentEpisode,
            pluginProviderId = intent.getStringExtra("PLUGIN_PROVIDER_ID"),
            localFilePath = localFilePath,
            downloadItemId = downloadItemId
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
                    context = context,
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

            val isNetMirrorBypassing by NetMirrorBypassManager.isBypassing.collectAsState()
            var bypassAdDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(isNetMirrorBypassing) {
                if (!isNetMirrorBypassing) {
                    bypassAdDismissed = false
                }
            }
            
            val isUnavailable = uiState.error != null || (!uiState.isLoading && uiState.streams.isEmpty())

            if (isUnavailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1014)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { finish() },
                        containerColor = Color(0xFF1E1F24),
                        titleContentColor = Color.White,
                        textContentColor = Color.LightGray,
                        shape = RoundedCornerShape(16.dp),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF3300),
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text(
                                text = "Video Not Available",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        text = {
                            Text(
                                text = "No playable stream links were found for this title. Please check back later or try another server/provider.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCCCCCC)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { finish() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3300)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            } else if (uiState.isLoading && uiState.streams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1014))
                ) {
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
                            text = if (isNetMirrorBypassing) "Fetching OTT security tokens (~37s)..." else "Loading...",
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

            if (isNetMirrorBypassing && !bypassAdDismissed) {
                NetMirrorBypassAdDialog(
                    onWatchAd = {
                        bypassAdDismissed = true
                        SupportManager.openAd(context)
                    },
                    onDismiss = {
                        bypassAdDismissed = true
                    }
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (!isInPictureInPictureMode) {
                playerController?.pause()
            }
        } else {
            playerController?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        playerController?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        playerController?.release()
    }
}