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

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.MediaSource

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

/**
 * Builds a MediaSource specifically tailored for a given StreamLink,
 * injecting its custom Referer, Cookies, User-Agent, and headers into OkHttpDataSource.
 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun buildMediaSourceForStream(
    context: android.content.Context,
    stream: com.streamflex.domain.models.StreamLink
): MediaSource {
    val headers = mutableMapOf<String, String>()

    val userAgent = stream.headers["User-Agent"]
        ?: com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT
    headers["User-Agent"] = userAgent

    if (!stream.referer.isNullOrBlank()) {
        headers["Referer"] = stream.referer
    } else if (!stream.headers["Referer"].isNullOrBlank()) {
        headers["Referer"] = stream.headers["Referer"]!!
    }

    if (stream.cookies.isNotEmpty()) {
        headers["Cookie"] = stream.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    } else if (!stream.headers["Cookie"].isNullOrBlank()) {
        headers["Cookie"] = stream.headers["Cookie"]!!
    }

    stream.headers.forEach { (k, v) ->
        if (!headers.containsKey(k)) headers[k] = v
    }

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20_000)
        .setReadTimeoutMs(20_000)
        .setDefaultRequestProperties(headers)

    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val extractorsFactory = DefaultExtractorsFactory()

    return DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
        .createMediaSource(MediaItem.fromUri(stream.url))
}

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
                name = "Source ${index + 1}",
                url = url,
                quality = com.streamflex.domain.models.Quality.UNKNOWN,
                host = com.streamflex.domain.models.HostType.DIRECT,
                referer = videoReferers.getOrNull(index)
            )
        }
        (initialLinks + dynamicStreams).distinctBy { it.url }
    }

    var currentStreamIndex by remember { mutableStateOf(0) }
    val allStreamsState = rememberUpdatedState(allStreams)

    Log.d("PLAYER_DEBUG", "Total available streams: ${allStreams.size}")

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,
                120_000,
                1_500,
                3_000
            )
            .setBackBuffer(30_000, true)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
                val listener = object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        val currentList = allStreamsState.value
                        val failedStream = currentList.getOrNull(currentStreamIndex)
                        android.util.Log.e("PLAYER_DEBUG", "Link failed: ${failedStream?.url} - Error: ${error.message}")

                        val nextIndex = currentStreamIndex + 1
                        if (nextIndex < currentList.size) {
                            android.util.Log.d("PLAYER_DEBUG", "Auto-falling back to stream $nextIndex: ${currentList[nextIndex].url}")
                            currentStreamIndex = nextIndex
                            val mediaSource = buildMediaSourceForStream(context, currentList[nextIndex])
                            setMediaSource(mediaSource)
                            prepare()
                            play()
                        } else {
                            android.util.Log.e("PLAYER_DEBUG", "All streaming links exhausted")
                            if (currentList.isNotEmpty()) {
                                Toast.makeText(context, "Playback error. Try selecting another source from Settings.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                addListener(listener)

                val initialStreams = allStreamsState.value
                if (initialStreams.isNotEmpty()) {
                    val mediaSource = buildMediaSourceForStream(context, initialStreams[0])
                    setMediaSource(mediaSource)
                    prepare()
                    playWhenReady = true
                }
            }
    }

    // Handle stream switching when user selects a different source in Settings or fallback triggers
    LaunchedEffect(currentStreamIndex) {
        if (allStreams.isNotEmpty() && currentStreamIndex in allStreams.indices) {
            val targetStream = allStreams[currentStreamIndex]
            val currentMedia = exoPlayer.currentMediaItem
            if (currentMedia?.localConfiguration?.uri?.toString() != targetStream.url) {
                val position = exoPlayer.currentPosition
                val mediaSource = buildMediaSourceForStream(context, targetStream)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                if (position > 0) {
                    exoPlayer.seekTo(position)
                }
                exoPlayer.play()
            }
        }
    }

    // Auto-start playback when background extraction delivers the first stream into an idle player
    LaunchedEffect(allStreams) {
        if (allStreams.isNotEmpty() &&
            exoPlayer.currentMediaItem == null &&
            exoPlayer.playbackState == Player.STATE_IDLE) {
            val firstStream = allStreams[0]
            android.util.Log.d("PLAYER_DEBUG", "Background stream arrived — auto-starting: ${firstStream.url}")
            val mediaSource = buildMediaSourceForStream(context, firstStream)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
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
                    currentStreamIndex = currentStreamIndex,
                    onStreamSelected = { index ->
                        currentStreamIndex = index
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