@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.streamflex.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.streamflex.app.ui.theme.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

class PlayerActivity : ComponentActivity() {
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

        setContent {
            val dynamicStreams by StreamStateHolder.streams.collectAsState()
            PlayerScreen(
                videoUrls = videoUrls, 
                videoReferers = videoReferers, 
                videoCookies = videoCookies, 
                videoUserAgents = videoUserAgents,
                dynamicStreams = dynamicStreams,
                onBack = { finish() }
            )
        }
    }
}

// 2. FIXED ANNOTATION: Explicitly use the AndroidX OptIn for Media3
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrls: ArrayList<String>, 
    videoReferers: ArrayList<String>, 
    videoCookies: ArrayList<String>, 
    videoUserAgents: ArrayList<String>,
    dynamicStreams: List<com.streamflex.domain.models.StreamLink>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Combine initial intent URLs with dynamically discovered streams
    val allStreams = remember(videoUrls, dynamicStreams) {
        val initialLinks = videoUrls.mapIndexed { index, url ->
            com.streamflex.domain.models.StreamLink(
                name = "Initial Source ${index + 1}",
                url = url,
                quality = com.streamflex.domain.models.Quality.UNKNOWN,
                host = com.streamflex.domain.models.HostType.DIRECT,
                referer = videoReferers.getOrNull(index)
            )
        }
        (initialLinks + dynamicStreams).distinctBy { it.url }
    }
    
    val allUrls = allStreams.map { it.url }
    
    var currentUrlIndex by remember { mutableStateOf(0) }

    Log.d("PLAYER_DEBUG", "Received ${allUrls.size} Total URLs")

    val exoPlayer = remember {
        // Setup fallbacks for testing
        val urlsToPlay = allUrls

        if (urlsToPlay.isEmpty()) {

            Toast.makeText(
                context,
                "No playable streams found.",
                Toast.LENGTH_LONG
            ).show()

            (context as Activity).finish()

            return@remember ExoPlayer.Builder(context).build()
        }
        
        val defaultReferer = videoReferers.firstOrNull { it.isNotEmpty() } ?: ""
        val defaultCookie = videoCookies.firstOrNull { it.isNotEmpty() } ?: ""
        val defaultUserAgent = videoUserAgents.firstOrNull { it.isNotEmpty() } ?: ""
        
        val requestProperties = mutableMapOf<String, String>()
        if (defaultReferer.isNotEmpty()) {
            requestProperties["Referer"] = defaultReferer
        }
        if (defaultCookie.isNotEmpty()) {
            requestProperties["Cookie"] = defaultCookie
        }

        // Add User-Agent AND dynamic Referer/Cookies
        val userAgent = if (defaultUserAgent.isNotEmpty()) {
            defaultUserAgent
        } else {
            com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT
        }
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(requestProperties)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {

                val listener = object : Player.Listener {
                    var currentIndex = 0

                    override fun onPlayerError(error: PlaybackException) {
                        val currentUrl = urlsToPlay.getOrNull(currentUrlIndex)
                        android.util.Log.e("PLAYER_DEBUG", "Link failed: $currentUrl - Error: ${error.message}")

                        currentUrlIndex++ // Move to the next link

                        if (currentUrlIndex < urlsToPlay.size) {
                            android.util.Log.d("PLAYER_DEBUG", "Trying next link fallback: ${urlsToPlay[currentUrlIndex]}")
                            setMediaItem(MediaItem.fromUri(urlsToPlay[currentUrlIndex]))
                            prepare()
                            play() // Start playing the new link
                        } else {
                            android.util.Log.e("PLAYER_DEBUG", "All streaming links failed!")
                            Toast.makeText(context, "All streaming links failed or expired.", Toast.LENGTH_LONG).show()
                            stop() // Stop to prevent further retries or error events
                            clearMediaItems()
                        }
                    }
                }

                addListener(listener)

                if (urlsToPlay.isNotEmpty()) {
                    setMediaItem(MediaItem.fromUri(urlsToPlay[currentUrlIndex]))
                    prepare()
                    playWhenReady = true
                }
            }
    }

    // Effect to handle URL switching when user selects a different source manually
    LaunchedEffect(currentUrlIndex) {
        if (allUrls.isNotEmpty() && currentUrlIndex < allUrls.size) {
            val url = allUrls[currentUrlIndex]
            val currentMedia = exoPlayer.currentMediaItem
            if (currentMedia?.localConfiguration?.uri?.toString() != url) {
                val position = exoPlayer.currentPosition
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.seekTo(position)
                exoPlayer.play()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Top Overlay (Settings Icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { showSettingsSheet = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Settings Bottom Sheet
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor   = SFBgSurface,
                dragHandle       = { BottomSheetDefaults.DragHandle(color = SFOutline) }
            ) {
                PlayerSettingsContent(
                    player = exoPlayer,
                    allStreams = allStreams,
                    currentStreamIndex = currentUrlIndex,
                    onStreamSelected = { index ->
                        currentUrlIndex = index
                        showSettingsSheet = false
                    },
                    onClose = { showSettingsSheet = false }
                )
            }
        }
    }
}

@Composable
fun PlayerSettingsContent(
    player: Player, 
    allStreams: List<com.streamflex.domain.models.StreamLink>,
    currentStreamIndex: Int,
    onStreamSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    var currentTracks by remember { mutableStateOf(player.currentTracks) }
    var currentSpeed by remember { mutableStateOf(player.playbackParameters.speed) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                currentSpeed = playbackParameters.speed
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val videoGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    val audioGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val textGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Player Settings",
                color      = SFTextPrimary,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SFTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingTab("Quality", selectedTab == 0) { selectedTab = 0 }
            Spacer(modifier = Modifier.width(16.dp))
            SettingTab("Audio", selectedTab == 1) { selectedTab = 1 }
            Spacer(modifier = Modifier.width(16.dp))
            SettingTab("Subtitles", selectedTab == 2) { selectedTab = 2 }
            Spacer(modifier = Modifier.width(16.dp))
            SettingTab("Speed", selectedTab == 3) { selectedTab = 3 }
            Spacer(modifier = Modifier.width(16.dp))
            SettingTab("Sources", selectedTab == 4) { selectedTab = 4 }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SFDivider)

        LazyColumn(modifier = Modifier.height(250.dp)) {
            when (selectedTab) {
                0 -> { // Quality
                    item {
                        val isAuto = player.trackSelectionParameters.overrides.isEmpty()
                        SettingItem(text = "Auto", isSelected = isAuto) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .build()
                        }
                    }
                    videoGroups.forEach { group ->
                        items(group.length) { trackIndex ->
                            val format = group.getTrackFormat(trackIndex)
                            val isSelected = group.isTrackSelected(trackIndex) && player.trackSelectionParameters.overrides.isNotEmpty()
                            val resolution = "${format.width}x${format.height}"
                            SettingItem(text = resolution, isSelected = isSelected) {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                    .build()
                            }
                        }
                    }
                }
                1 -> { // Audio
                    item {
                        val isAuto = player.trackSelectionParameters.overrides.isEmpty()
                        SettingItem(text = "Auto", isSelected = isAuto) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .build()
                        }
                    }
                    audioGroups.forEach { group ->
                        items(group.length) { trackIndex ->
                            val format = group.getTrackFormat(trackIndex)
                            val isSelected = group.isTrackSelected(trackIndex) && player.trackSelectionParameters.overrides.isNotEmpty()
                            val name = format.language ?: format.label ?: "Track ${trackIndex + 1}"
                            SettingItem(text = name, isSelected = isSelected) {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                    .build()
                            }
                        }
                    }
                }
                2 -> { // Subtitles
                    item {
                        val isOff = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                        SettingItem(text = "Off", isSelected = isOff) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                        }
                    }
                    textGroups.forEach { group ->
                        items(group.length) { trackIndex ->
                            val format = group.getTrackFormat(trackIndex)
                            val isSelected = group.isTrackSelected(trackIndex) && !player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                            val name = format.language ?: format.label ?: "Subtitle ${trackIndex + 1}"
                            SettingItem(text = name, isSelected = isSelected) {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                    .build()
                            }
                        }
                    }
                }
                3 -> { // Speed
                    items(speeds) { speed ->
                        val isSelected = currentSpeed == speed
                        SettingItem(text = "${speed}x", isSelected = isSelected) {
                            player.setPlaybackSpeed(speed)
                        }
                    }
                }
                4 -> { // Sources
                    items(allStreams.size) { index ->
                        val stream = allStreams[index]
                        val name = if (stream.name.isNotEmpty()) stream.name else "${stream.host.name} (${stream.quality})"
                        SettingItem(text = name, isSelected = index == currentStreamIndex) {
                            onStreamSelected(index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(
            text       = text,
            color      = if (isSelected) SFTextPrimary else SFTextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style      = MaterialTheme.typography.titleMedium,
            modifier   = Modifier.padding(bottom = 8.dp)
        )
        if (isSelected) {
            Box(modifier = Modifier.height(2.dp).width(40.dp).background(SFAccent))
        }
    }
}

@Composable
fun SettingItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = SFAccent, modifier = Modifier.size(20.dp))
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = text,
            color = if (isSelected) SFTextPrimary else SFTextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}