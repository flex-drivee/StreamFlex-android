package com.cinetheta.app.ui.pluginsearch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.outlined.FileDownload
import com.cinetheta.app.ui.home.SFBadge
import com.cinetheta.app.ui.theme.SFDubBg
import com.cinetheta.app.ui.theme.SFHDTag
import com.cinetheta.app.ui.theme.SFRatingBg
import com.cinetheta.app.ui.theme.SFTextDisabled
import com.cinetheta.domain.models.ProviderEpisode
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    searchResult: SearchResult,
    viewModel: PluginDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (List<ProviderSource>, ProviderEpisode?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isShow = searchResult.mediaType.name == "TV" || searchResult.mediaType.name == "ANIME"
    var showComingSoonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(searchResult) {
        viewModel.loadContent(searchResult)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val result = uiState.result
                if (result != null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        
                        // ── 1. HERO BACKDROP (Actual App Detail Style) ──────────
                        item {
                            PluginDetailHero(
                                backdrop = searchResult.backdrop ?: searchResult.poster ?: result.poster,
                                poster = result.poster ?: searchResult.poster,
                                title = result.title,
                                year = result.year ?: searchResult.year,
                                rating = searchResult.rating,
                                isShow = isShow,
                                genres = searchResult.genres
                            )
                        }
                        
                        // ── 2. MOVIE PLAY BUTTON ──────────
                        if (result.sources.isNotEmpty() && result.seasons.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { onPlayClick(result.sources, null) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Play Movie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { showComingSoonDialog = true },
                                            modifier = Modifier.height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Outlined.FileDownload, contentDescription = "Download", modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Download", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        // ── 3. OVERVIEW ──────────
                        item {
                            if (!result.overview.isNullOrBlank()) {
                                Text(
                                    text = result.overview!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // ── 4. SEASONS AND EPISODES ──────────
                        if (result.seasons.isNotEmpty()) {
                            item {
                                var selectedSeason by remember { mutableStateOf(result.seasons.firstOrNull()) }
                                var isMenuExpanded by remember { mutableStateOf(false) }

                                // Season Dropdown Selector
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { isMenuExpanded = !isMenuExpanded }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Season ${selectedSeason?.number ?: 1}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Icon(
                                            if (isMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isMenuExpanded,
                                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                                        exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            result.seasons.forEach { season ->
                                                val isSelected = season == selectedSeason
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { 
                                                            selectedSeason = season
                                                            isMenuExpanded = false 
                                                        }
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent)
                                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Season ${season.number}",
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                                    )
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Episodes List
                                selectedSeason?.episodes?.forEach { episode ->
                                    PluginEpisodeItem(
                                        episode = episode,
                                        onClick = { onPlayClick(episode.sources, episode) },
                                        onDownloadClick = { showComingSoonDialog = true }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        } else if (result.sources.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No content available", color = SFTextDisabled)
                                }
                            }
                        }
                    }
                }
            }

            if (showComingSoonDialog) {
                AlertDialog(
                    onDismissRequest = { showComingSoonDialog = false },
                    title = { Text("Coming Soon", fontWeight = FontWeight.Bold) },
                    text = { Text("Downloads from this provider are currently not supported and will be available in an upcoming update.") },
                    confirmButton = {
                        TextButton(onClick = { showComingSoonDialog = false }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: HERO BACKDROP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PluginDetailHero(
    backdrop: String?,
    poster: String?,
    title: String,
    year: Int?,
    rating: Double?,
    isShow: Boolean,
    genres: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
    ) {
        // Blurred Background
        SubcomposeAsyncImage(
            model = backdrop ?: poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .matchParentSize()
                .blur(radiusX = 15.dp, radiusY = 15.dp)
        )

        // Dim Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.75f))
        )
        
        // Gradient fade to background at the bottom
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 400f
                    )
                )
        )

        // Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 80.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Card
            Card(
                modifier = Modifier
                    .width(130.dp)
                    .aspectRatio(2f / 3f),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                SubcomposeAsyncImage(
                    model = poster,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (year != null && year > 0) {
                        Text(year.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                    }
                    SFBadge("HD", SFHDTag, Color.Black)
                    if (isShow) SFBadge("SERIES", SFDubBg, Color.White)
                    if (rating != null && rating > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, tint = SFRatingBg, modifier = Modifier.size(14.dp))
                            Text(String.format("%.1f", rating), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                    }
                }
                
                if (genres.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(genres.take(4)) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(genre, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: EPISODE ITEM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PluginEpisodeItem(
    episode: ProviderEpisode,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!episode.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = SFTextDisabled,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.number}. ${episode.title}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onDownloadClick != null) {
            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Download Episode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
