@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.streamflex.app.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.activity.compose.setContent
// 1. REMOVED: import androidx.annotation.OptIn (This was causing the conflict)
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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide System Bars for Immersive Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""

        setContent {
            PlayerScreen(videoUrl = videoUrl, onBack = { finish() })
        }
    }
}

// 2. FIXED ANNOTATION: Explicitly use the AndroidX OptIn for Media3
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        // 1. Define the exact same browser User-Agent we used in the Extractor
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

        // 2. Create a DataSourceFactory that injects this User-Agent into every video request
        val dataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(userAgent)

        // 3. Create a MediaSourceFactory using our custom DataSource
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // 4. Build the ExoPlayer with the custom MediaSourceFactory
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {

                // Your existing fallback logic
                val finalPlayUrl = if (videoUrl == "play" || videoUrl == "play_movie" || videoUrl.isEmpty()) {
                    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
                } else {
                    videoUrl
                }

                setMediaItem(MediaItem.fromUri(finalPlayUrl))
                prepare()
                playWhenReady = true
            }
    }
    Log.d("PLAYER_DEBUG", "URL: $videoUrl")

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. The Video Player
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

        // 2. Custom Top Overlay (Settings Icon)
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

        // 3. Settings Bottom Sheet
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF1A1A1A),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                PlayerSettingsContent(
                    onClose = { showSettingsSheet = false }
                )
            }
        }
    }
}

@Composable
fun PlayerSettingsContent(onClose: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val audioTracks = listOf("English (Original)", "Spanish", "French")
    val qualities = listOf("Auto", "1080p", "720p", "480p")

    var currentAudio by remember { mutableStateOf("English (Original)") }
    var currentQuality by remember { mutableStateOf("Auto") }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingTab(
                text = "Audio & Subtitles",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            Spacer(modifier = Modifier.width(16.dp))
            SettingTab(
                text = "Video Quality",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.DarkGray)

        LazyColumn(modifier = Modifier.height(200.dp)) {
            if (selectedTab == 0) {
                items(audioTracks) { track ->
                    SettingItem(
                        text = track,
                        isSelected = track == currentAudio,
                        onClick = { currentAudio = track }
                    )
                }
            } else {
                items(qualities) { quality ->
                    SettingItem(
                        text = quality,
                        isSelected = quality == currentQuality,
                        onClick = { currentQuality = quality }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (isSelected) {
            Box(modifier = Modifier.height(2.dp).width(40.dp).background(Color.Red))
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
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = if (isSelected) Color.White else Color.LightGray, fontSize = 16.sp)
    }
}