package com.cinetheta.app.ui.pluginsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

    LaunchedEffect(searchResult) {
        viewModel.loadContent(searchResult)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(searchResult.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        // Header
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                if (!result.poster.isNullOrBlank()) {
                                    AsyncImage(
                                        model = result.poster,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                                    startY = 100f
                                                )
                                            )
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (result.year != null) {
                                        Text(text = result.year.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                        
                        // Overview
                        item {
                            if (!result.overview.isNullOrBlank()) {
                                Text(
                                    text = result.overview!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (result.seasons.isNotEmpty()) {
                            // TV Show
                            item {
                                var selectedSeason by remember { mutableStateOf(result.seasons.firstOrNull()) }
                                ScrollableTabRow(
                                    selectedTabIndex = result.seasons.indexOf(selectedSeason).coerceAtLeast(0),
                                    edgePadding = 16.dp,
                                    containerColor = Color.Transparent
                                ) {
                                    result.seasons.forEach { season ->
                                        Tab(
                                            selected = selectedSeason == season,
                                            onClick = { selectedSeason = season },
                                            text = { Text("Season ${season.number}") }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                selectedSeason?.episodes?.forEach { episode ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clickable { onPlayClick(episode.sources, episode) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!episode.thumbnail.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = episode.thumbnail,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(80.dp, 45.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${episode.number}. ${episode.title}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        } else if (result.sources.isNotEmpty()) {
                            // Movie
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Button(
                                        onClick = { onPlayClick(result.sources, null) },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play Movie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("No content available", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
