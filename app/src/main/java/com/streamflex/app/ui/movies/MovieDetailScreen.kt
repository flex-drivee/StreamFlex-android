package com.streamflex.app.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamflex.app.domain.models.Episode
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    viewModel: MovieDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val isShow = state.show != null
    val title = state.movie?.title ?: state.show?.title ?: ""
    val backdrop = state.movie?.backdrop ?: state.show?.backdrop
    val overview = state.movie?.overview ?: state.show?.overview ?: ""
    val year = state.movie?.year ?: state.show?.year
    val rating = state.movie?.rating ?: state.show?.rating
    // Movie runtime vs Episode runtime handled differently
    val movieRuntime = state.movie?.runtime

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)
            ) {
                // --- 1. HERO SECTION ---
                item {
                    Box(modifier = Modifier.height(450.dp).fillMaxWidth()) {
                        AsyncImage(
                            model = backdrop,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startY = 300f
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(top = 40.dp, end = 16.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // --- 2. TITLE & METADATA ---
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 40.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${year ?: "N/A"}", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = Color.DarkGray,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HD",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            Text(text = " ${rating ?: "N/A"}", color = Color.LightGray, fontSize = 14.sp)

                            // Show Duration for Movies
                            if (!isShow && movieRuntime != null && movieRuntime > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = formatRuntime(movieRuntime), color = Color.LightGray, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // --- 3. ACTION BUTTONS ---
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { onPlayClick("play") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { /* Download */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = overview,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            ActionIcon(icon = Icons.Default.Add, label = "My List")
                            Spacer(modifier = Modifier.width(32.dp))
                            ActionIcon(icon = Icons.Default.Share, label = "Share")
                        }
                    }
                }

                // --- 4. SEASONS & EPISODES (Only if Show) ---
                if (isShow && state.show != null) {
                    item {
                        Divider(color = Color.DarkGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Season Selector
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.show!!.seasons) { season ->
                                val isSelected = season.seasonNumber == state.selectedSeason
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.loadSeason(season.seasonNumber) },
                                    label = { Text("Season ${season.seasonNumber}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Red,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.DarkGray,
                                        labelColor = Color.White
                                    ),
                                    border = null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.episodes) { episode ->
                        EpisodeItem(episode = episode, onClick = { onPlayClick(episode.id) })
                    }
                }

                // --- 5. MORE LIKE THIS (Grid 3x2) ---
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "More Like This",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    // Simple Grid Implementation using Column + Rows
                    val similarItems = state.similarContent.take(6) // Take only first 6
                    val rows = similarItems.chunked(3) // Split into rows of 3

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (video in rowItems) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        VideoCard(
                                            video = video,
                                            onClick = { /* Navigate to detail */ },
                                            modifier = Modifier.fillMaxWidth() // Allow card to fill space
                                        )
                                    }
                                }
                                // Fill empty spaces if last row has fewer than 3 items
                                if (rowItems.size < 3) {
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Components ---

fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes == 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
fun ActionIcon(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun EpisodeItem(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(70.dp)
                .background(Color.DarkGray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Overlay Play Icon
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)

            // Duration Badge (Bottom Right of thumbnail)
            if (episode.runtime != null && episode.runtime > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${episode.runtime}m",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.title}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = episode.overview ?: "No description",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}