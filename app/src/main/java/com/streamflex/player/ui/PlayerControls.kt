package com.streamflex.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflex.player.core.PlayerState
import kotlinx.coroutines.delay

@Composable
fun PlayerControls(
    state: PlayerState,
    title: String,
    subtitle: String?,
    showEpisodesButton: Boolean = false,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSettingsClick: (tab: Int) -> Unit,
    onEpisodesClick: () -> Unit,
    onBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isVisible, state.isPlaying) {
        if (isVisible && state.isPlaying) {
            delay(3000L)
            isVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isVisible = !isVisible }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top bar (Close button + Multi-line Title)
                PlayerTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Bottom bar
                PlayerBottomBar(
                    state = state,
                    showEpisodesButton = showEpisodesButton,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onSeekTo = onSeekTo,
                    onSeekForward = onSeekForward,
                    onSeekBackward = onSeekBackward,
                    onSettingsClick = onSettingsClick,
                    onEpisodesClick = onEpisodesClick,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (state.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        }
    }
}

@Composable
private fun PlayerTopBar(title: String, subtitle: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomBar(
    state: PlayerState,
    showEpisodesButton: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSettingsClick: (Int) -> Unit,
    onEpisodesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Custom Timeline Progress Bar
        PlayerProgressBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            bufferedPositionMs = state.bufferedPositionMs,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: -10, Play, +10, Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSeekBackward) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Replay, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                        Text("10", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Play button is centered between Rewind and Forward and is slightly larger
                IconButton(onClick = onPlayPauseToggle) {
                    val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    Icon(icon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(42.dp))
                }
                
                IconButton(onClick = onSeekForward) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                        Text("10", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}", 
                    color = Color.White, 
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Right Side: Episodes, CC, Settings, Fullscreen
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showEpisodesButton) {
                    IconButton(onClick = onEpisodesClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Episodes", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                IconButton(onClick = { onSettingsClick(2) }) { // 2 = Subtitles Tab
                    Icon(Icons.Default.ClosedCaption, contentDescription = "CC", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { onSettingsClick(0) }) { // 0 = Video Quality Tab
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { /* TODO Fullscreen */ }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}