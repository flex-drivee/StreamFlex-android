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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflex.player.core.PlayerState
import kotlinx.coroutines.delay

@Composable
fun PlayerControls(
    state: PlayerState,
    title: String,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSettingsClick: (tab: Int) -> Unit,
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
                // Top bar (Back button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                // Bottom bar
                PlayerBottomBar(
                    state = state,
                    title = title,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onSeekTo = onSeekTo,
                    onSeekForward = onSeekForward,
                    onSeekBackward = onSeekBackward,
                    onSettingsClick = onSettingsClick,
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
private fun PlayerBottomBar(
    state: PlayerState,
    title: String,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSettingsClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Timeline Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val positionFraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            val bufferedFraction = if (state.durationMs > 0) state.bufferedPositionMs.toFloat() / state.durationMs else 0f
            
            LinearProgressIndicator(
                progress = { bufferedFraction },
                color = Color.LightGray.copy(alpha = 0.5f),
                trackColor = Color.DarkGray.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            
            Slider(
                value = positionFraction,
                onValueChange = { onSeekTo((it * state.durationMs).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Play, -10, +10, Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!state.isBuffering) {
                    IconButton(onClick = onPlayPauseToggle) {
                        val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                        Icon(icon, contentDescription = "Play/Pause", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                IconButton(onClick = onSeekBackward) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Replay, contentDescription = "Rewind", tint = Color.White)
                        Text("10", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                IconButton(onClick = onSeekForward) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = "Forward", tint = Color.White)
                        Text("10", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}", 
                    color = Color.White, 
                    fontSize = 12.sp
                )
            }
            
            // Center: Title
            Text(
                text = title, 
                color = Color.White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
            
            // Right Side: CC, Settings, Fullscreen
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSettingsClick(2) }) { // 2 = Subtitles Tab
                    Icon(Icons.Default.ClosedCaption, contentDescription = "CC", tint = Color.White)
                }
                IconButton(onClick = { onSettingsClick(0) }) { // 0 = Video Quality Tab
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
                IconButton(onClick = { /* TODO Fullscreen */ }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
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