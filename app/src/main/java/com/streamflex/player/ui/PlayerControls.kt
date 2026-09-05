package com.streamflex.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onFullscreenToggle: () -> Unit,
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
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Season / Episode Pills or Empty if Movie
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showEpisodesButton) {
                            // S1 Pill (Green)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF008000)) // Green
                                    .clickable { onEpisodesClick() }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(subtitle?.split(" ")?.take(2)?.joinToString(" ") ?: "Episodes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // E1 Pill (Dark Grey)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF333333)) // Dark Grey
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(subtitle?.split(" ")?.drop(2)?.joinToString(" ") ?: "", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp) // Cyan text
                            }
                        }
                    }

                    // Network Stats (Dummy/Placeholder)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ping: 38 ms  |  ServerLoc: H-MCT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    // Close Button
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // CENTER CONTROLS (-10, Play, +10)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onSeekBackward, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Replay, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(64.dp))
                            Text("10", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(100.dp)) {
                        val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                        Icon(icon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(80.dp))
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(onClick = onSeekForward, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Refresh, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(64.dp))
                            Text("10", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // BOTTOM BAR
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Progress Bar
                    PlayerProgressBar(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        bufferedPositionMs = state.bufferedPositionMs,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottom Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Play, Volume, Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(40.dp)) {
                                val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                                Icon(icon, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { /* TODO: Volume Toggle */ }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}", 
                                color = Color.White, 
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Center: Title
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // Right: Comments, PIP, CC, Settings, Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* TODO: Comments */ }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { /* TODO: PIP */ }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = "PIP", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { onSettingsClick(2) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.ClosedCaption, contentDescription = "CC", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { onSettingsClick(0) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = onFullscreenToggle, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
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
