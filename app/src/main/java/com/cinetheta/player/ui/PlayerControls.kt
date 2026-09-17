package com.cinetheta.player.ui

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
import com.cinetheta.player.core.PlayerState
import kotlinx.coroutines.delay

@Composable
fun PlayerControls(
    state: PlayerState,
    title: String,
    subtitle: String?,
    showEpisodesButton: Boolean = false,
    isMuted: Boolean = false,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onMuteToggle: () -> Unit,
    onPipClick: () -> Unit,
    onSettingsClick: (tab: Int) -> Unit,
    onEpisodesClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isUnlockPromptVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible, state.isPlaying, isLocked) {
        if (isVisible && state.isPlaying && !isLocked) {
            delay(3000L)
            isVisible = false
        }
    }

    LaunchedEffect(isUnlockPromptVisible) {
        if (isUnlockPromptVisible) {
            delay(3500L)
            isUnlockPromptVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isLocked) {
                    isUnlockPromptVisible = !isUnlockPromptVisible
                } else {
                    isVisible = !isVisible
                }
            }
    ) {
        // Floating Unlock Banner when screen is locked
        AnimatedVisibility(
            visible = isLocked && isUnlockPromptVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable {
                        isLocked = false
                        isUnlockPromptVisible = false
                        isVisible = true
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Unlock Controls",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Controls Locked",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Tap to unlock",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible && !isLocked,
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
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Episodes quick button if TV Show
                    if (showEpisodesButton) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF008000))
                                .clickable { onEpisodesClick() }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Episodes", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Episodes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }

                    // UPPER MID: Movie/Show Name and Episode subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (showEpisodesButton && !subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Side: Lock button + Close Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            isLocked = true
                            isVisible = false
                            isUnlockPromptVisible = true
                        }) {
                            Icon(
                                Icons.Outlined.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // CENTER CONTROLS (-10, Play, +10)
                Row(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onSeekBackward, modifier = Modifier.size(100.dp)) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                    Spacer(modifier = Modifier.width(80.dp)) // Increased spacing
                    IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(120.dp)) {
                        val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                        Icon(icon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(96.dp))
                    }
                    Spacer(modifier = Modifier.width(80.dp)) // Increased spacing
                    IconButton(onClick = onSeekForward, modifier = Modifier.size(100.dp)) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(72.dp))
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
                            IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(44.dp)) {
                                val icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                                Icon(icon, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            IconButton(onClick = onMuteToggle, modifier = Modifier.size(44.dp)) {
                                val volIcon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp
                                Icon(volIcon, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}", 
                                color = Color.White, 
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Right: Server, Quality, Audio, Subtitles, PIP, Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Server Icon (tab 3)
                            IconButton(onClick = { onSettingsClick(3) }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.Dns, contentDescription = "Servers", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            // Quality / Multiprint (tab 0)
                            IconButton(onClick = { onSettingsClick(0) }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.HighQuality, contentDescription = "Quality", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            // Audio Tracks (tab 1)
                            IconButton(onClick = { onSettingsClick(1) }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.Audiotrack, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            // Subtitles (tab 2)
                            IconButton(onClick = { onSettingsClick(2) }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.ClosedCaption, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            // Picture in Picture
                            IconButton(onClick = onPipClick, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = "PIP", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            // Fullscreen
                            IconButton(onClick = onFullscreenToggle, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
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
