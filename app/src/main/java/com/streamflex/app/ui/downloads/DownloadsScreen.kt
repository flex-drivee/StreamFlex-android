package com.streamflex.app.ui.downloads

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.streamflex.data.local.download.DownloadStorageManager
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadStatus
import com.streamflex.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModelFactory())
) {
    val downloads by viewModel.allDownloads.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val isSmartDownloadsEnabled by viewModel.smartDownloadsEnabled.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Smart Downloads Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Smart Downloads", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "Auto-downloads next episodes", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 13.sp
                            )
                        }
                    }
                    Switch(
                        checked = isSmartDownloadsEnabled,
                        onCheckedChange = { viewModel.toggleSmartDownloads(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Storage Indicator
            item {
                StorageIndicator(storageStats)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
            }

            // Downloads List
            if (downloads.isEmpty()) {
                item {
                    EmptyDownloadsState(onFindClick = onBackClick)
                }
            } else {
                items(downloads, key = { it.id }) { item ->
                    DownloadListItem(
                        item = item,
                        onPlayClick = {
                            if (item.status == DownloadStatus.COMPLETED) {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("MEDIA_ID", item.mediaId)
                                    putExtra("VIDEO_TITLE", item.title)
                                    putExtra("VIDEO_YEAR", item.year ?: 0)
                                    putExtra("IS_SHOW", item.isShow)
                                    putExtra("POSTER_PATH", item.posterUrl)
                                    if (item.isShow && item.episodeNumber != null) {
                                        putExtra("CURRENT_EPISODE_ID", "${item.mediaId}_${item.seasonNumber}_${item.episodeNumber}")
                                    }
                                }
                                context.startActivity(intent)
                            } else if (item.status == DownloadStatus.PAUSED) {
                                viewModel.resumeDownload(item.id)
                            }
                        },
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onRetry = { viewModel.retryDownload(item.id) },
                        onDelete = { viewModel.deleteDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StorageIndicator(stats: DownloadStorageManager.StorageStats) {
    val total = stats.totalBytes.toFloat().coerceAtLeast(1f)
    val appUsedRatio = (stats.streamFlexUsedBytes.toFloat() / total).coerceIn(0f, 1f)
    val otherUsedRatio = ((stats.usedBytes - stats.streamFlexUsedBytes).toFloat() / total).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Internal Storage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text("${stats.formattedFree} Free", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Other apps
            if (otherUsedRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(otherUsedRatio)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            // StreamFlex used
            if (appUsedRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(appUsedRatio)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.width(6.dp))
            Text("StreamFlex (${stats.formattedStreamFlexUsed})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun DownloadListItem(
    item: DownloadItem,
    onPlayClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!item.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (item.status.isActive) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { item.progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (item.status.isPaused) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Paused", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(item.formattedSize, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title, Subtitle, Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            val subtext = when (item.status) {
                DownloadStatus.COMPLETED -> "${item.formattedSize} • ${item.quality.label}"
                DownloadStatus.DOWNLOADING -> {
                    val speed = if (item.speedBytesPerSec > 0) {
                        val mb = item.speedBytesPerSec.toDouble() / (1024 * 1024)
                        String.format("%.1f MB/s", mb)
                    } else "Downloading..."
                    val eta = if (item.etaSeconds > 0) " (${item.etaSeconds / 60}m left)" else ""
                    "${item.progressPercent}% • $speed$eta"
                }
                DownloadStatus.CONNECTING -> "Connecting to mirror..."
                DownloadStatus.QUEUED -> "Queued..."
                DownloadStatus.PAUSED -> "Paused (${item.progressPercent}%)"
                DownloadStatus.FAILED -> "Failed • Tap to retry"
                DownloadStatus.CANCELLED -> "Cancelled"
            }

            Text(
                text = subtext,
                color = if (item.status.isActive) MaterialTheme.colorScheme.primary else if (item.status == DownloadStatus.FAILED) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (item.status.isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Options dropdown
        if (item.status == DownloadStatus.COMPLETED) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Download",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        } else {
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (item.status == DownloadStatus.DOWNLOADING) {
                    DropdownMenuItem(
                        text = { Text("Pause Download") },
                        leadingIcon = { Icon(Icons.Default.Pause, null) },
                        onClick = {
                            showMenu = false
                            onPause()
                        }
                    )
                } else if (item.status == DownloadStatus.PAUSED) {
                    DropdownMenuItem(
                        text = { Text("Resume Download") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = {
                            showMenu = false
                            onResume()
                        }
                    )
                } else if (item.status == DownloadStatus.FAILED) {
                    DropdownMenuItem(
                        text = { Text("Retry Download") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = {
                            showMenu = false
                            onRetry()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Delete Download", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
        }
    }
}

@Composable
fun EmptyDownloadsState(onFindClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DownloadForOffline,
                contentDescription = "No Downloads",
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Downloads Yet",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Movies and TV shows you download\nwill appear here for offline viewing.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFindClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Text("Find Something to Download", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
