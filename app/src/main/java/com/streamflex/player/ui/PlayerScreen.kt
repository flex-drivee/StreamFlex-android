@file:OptIn(ExperimentalMaterial3Api::class)

package com.streamflex.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.streamflex.domain.models.StreamLink
import com.streamflex.player.core.PlayerState
import com.streamflex.player.quality.QualityOption
import com.streamflex.player.tracks.AudioTrack
import com.streamflex.player.tracks.SubtitleTrack

@Composable
fun PlayerScreen(
    controller: PlayerController,
    videoTitle: String,
    videoSubtitle: String?,
    onBack: () -> Unit
) {
    val state by controller.state.collectAsState()
    val allStreams by controller.allStreams.collectAsState()
    val currentStreamIndex by controller.currentStreamIndex.collectAsState()
    val activeSkipSegment by controller.activeSkipSegment.collectAsState()
    val context = LocalContext.current
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var initialSettingsTab by remember { mutableStateOf(0) }
    
    val uiState by controller.viewModel.uiState.collectAsState()
    val episodes = uiState.session?.episodes ?: emptyList()
    val currentEpisode = uiState.session?.currentEpisode
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    
    val showNextEpCard by controller.nextEpisodeManager.showNextEpisodeCard.collectAsState()
    val countdownSeconds by controller.nextEpisodeManager.countdownSeconds.collectAsState()

    var showBuffering by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(state.isBuffering) {
        if (state.isBuffering) {
            kotlinx.coroutines.delay(400L) // 400ms debounce
            showBuffering = true
        } else {
            showBuffering = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        controller.player.Surface(modifier = Modifier.fillMaxSize(), isFullScreen = isFullScreen)
        
        if (showBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        }
        
        PlayerControls(
            state = state,
            title = videoTitle,
            subtitle = videoSubtitle,
            showEpisodesButton = episodes.isNotEmpty(),
            onPlayPauseToggle = { controller.togglePlayPause() },
            onSeekTo = { controller.seekTo(it) },
            onSeekForward = { controller.seekForward() },
            onSeekBackward = { controller.seekBackward() },
            onSettingsClick = { tab ->
                initialSettingsTab = tab
                showSettingsDialog = true 
            },
            onEpisodesClick = { showEpisodeDrawer = true },
            onFullscreenToggle = { isFullScreen = !isFullScreen },
            onBack = onBack
        )

        // Skip Intro Button
        activeSkipSegment?.let { segment ->
            Button(
                onClick = { controller.skipSegment(segment) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f), contentColor = Color.Black),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 24.dp) // Sits above the controls
            ) {
                Text(segment.label, fontWeight = FontWeight.Bold)
            }
        }

        // Next Episode Card overlay
        if (showNextEpCard) {
            val nextEp = controller.viewModel.getNextEpisode()
            if (nextEp != null) {
                NextEpisodeCard(
                    episode = nextEp,
                    countdown = countdownSeconds,
                    onPlayNext = {
                        controller.nextEpisodeManager.cancelCountdown()
                        controller.viewModel.playEpisode(nextEp)
                    },
                    onCancel = {
                        controller.nextEpisodeManager.cancelCountdown()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 24.dp)
                )
            }
        }
        
        // Error Overlay
        if (state.error != null) {
            PlayerErrorOverlay(
                error = state.error,
                hasNextServer = controller.hasNextServer(),
                onRetry = { controller.retry() },
                onTryAnotherServer = { controller.tryNextServer() },
                onBack = onBack
            )
        }
    }

    EpisodeDrawer(
        isVisible = showEpisodeDrawer,
        episodes = episodes,
        currentEpisode = currentEpisode,
        onEpisodeClick = { ep ->
            showEpisodeDrawer = false
            controller.viewModel.playEpisode(ep)
        },
        onDismiss = { showEpisodeDrawer = false }
    )

    if (showSettingsDialog) {
        SettingsDialog(
            state = state,
            allStreams = allStreams,
            currentStreamIndex = currentStreamIndex,
            initialTab = initialSettingsTab,
            onDismiss = { showSettingsDialog = false },
            onQualitySelected = { 
                controller.player.setQuality(it)
                showSettingsDialog = false 
            },
            onAudioSelected = { 
                controller.player.setAudioTrack(it)
                showSettingsDialog = false 
            },
            onSubtitleSelected = { 
                controller.player.setSubtitleTrack(it)
                showSettingsDialog = false 
            },
            onServerSelected = { index ->
                controller.selectStream(index)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun SettingsDialog(
    state: PlayerState,
    allStreams: List<StreamLink>,
    currentStreamIndex: Int,
    initialTab: Int,
    onDismiss: () -> Unit,
    onQualitySelected: (QualityOption) -> Unit,
    onAudioSelected: (AudioTrack) -> Unit,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onServerSelected: (Int) -> Unit
) {
    var currentTab by remember { mutableStateOf(initialTab) } // 0=Video, 1=Audio, 2=Subtitle, 3=Server, 4=Decoder
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("streamflex_settings", android.content.Context.MODE_PRIVATE) }
    var currentDecoderMode by remember {
        mutableStateOf(
            prefs.getString(
                com.streamflex.player.core.DecoderMode.PREF_KEY,
                com.streamflex.player.core.DecoderMode.AUTO.key
            ) ?: com.streamflex.player.core.DecoderMode.AUTO.key
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E), // Dark pop-up background
            modifier = Modifier.fillMaxWidth().height(350.dp) // Fixed height to prevent resizing
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabButton(
                        icon = Icons.Default.HighQuality, 
                        isSelected = currentTab == 0, 
                        onClick = { currentTab = 0 }
                    )
                    TabButton(
                        icon = Icons.Default.Audiotrack, 
                        isSelected = currentTab == 1, 
                        onClick = { currentTab = 1 }
                    )
                    TabButton(
                        icon = Icons.Default.ClosedCaption, 
                        isSelected = currentTab == 2, 
                        onClick = { currentTab = 2 }
                    )
                    TabButton(
                        icon = Icons.Default.Dns, // Servers Tab
                        isSelected = currentTab == 3, 
                        onClick = { currentTab = 3 }
                    )
                    TabButton(
                        icon = Icons.Default.Memory, // Decoder Mode Tab
                        isSelected = currentTab == 4, 
                        onClick = { currentTab = 4 }
                    )
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                HorizontalDivider(color = Color.DarkGray)
                
                // Content List
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
                    when (currentTab) {
                        0 -> { // Video Quality
                            if (state.availableQualities.isEmpty()) {
                                item { Text("Auto only", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                            } else {
                                items(state.availableQualities) { quality ->
                                    val isSelected = quality.id == state.currentQuality?.id || (state.currentQuality == null && quality.isAuto)
                                    SettingsRow(
                                        label = quality.name,
                                        isSelected = isSelected,
                                        onClick = { onQualitySelected(quality) }
                                    )
                                }
                            }
                        }
                        1 -> { // Audio
                            if (state.audioTracks.isEmpty()) {
                                item { Text("Default audio", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                            } else {
                                items(state.audioTracks) { track ->
                                    val isSelected = track.id == state.selectedAudio?.id
                                    SettingsRow(
                                        label = track.label ?: "Audio",
                                        isSelected = isSelected,
                                        onClick = { onAudioSelected(track) }
                                    )
                                }
                            }
                        }
                        2 -> { // Subtitles
                            item {
                                SettingsRow(
                                    label = "Off",
                                    isSelected = state.selectedSubtitle == null,
                                    onClick = { onSubtitleSelected(null) }
                                )
                            }
                            items(state.subtitleTracks) { track ->
                                val isSelected = track.id == state.selectedSubtitle?.id
                                SettingsRow(
                                    label = track.label?.takeIf { it.isNotBlank() } ?: track.language?.takeIf { it.isNotBlank() && it != "und" } ?: "Subtitle",
                                    isSelected = isSelected,
                                    onClick = { onSubtitleSelected(track) }
                                )
                            }
                        }
                        3 -> { // Servers
                            if (allStreams.isEmpty()) {
                                item { Text("No alternate servers", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                            } else {
                                itemsIndexed(allStreams) { index, stream ->
                                    val isSelected = index == currentStreamIndex
                                    SettingsRow(
                                        label = stream.name ?: "Server ${index + 1}",
                                        isSelected = isSelected,
                                        onClick = { onServerSelected(index) }
                                    )
                                }
                            }
                        }
                        4 -> { // Decoder Mode
                            items(com.streamflex.player.core.DecoderMode.entries) { mode ->
                                val isSelected = currentDecoderMode == mode.key
                                SettingsRow(
                                    label = mode.title,
                                    isSelected = isSelected,
                                    onClick = {
                                        prefs.edit().putString(com.streamflex.player.core.DecoderMode.PREF_KEY, mode.key).apply()
                                        currentDecoderMode = mode.key
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color.White else Color.Gray
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SettingsRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isSelected) Color.White else Color.LightGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}
