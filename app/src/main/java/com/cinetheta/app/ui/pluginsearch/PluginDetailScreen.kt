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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 340.dp)
                            ) {
                                // Blurred Background
                                SubcomposeAsyncImage(
                                    model = result.poster,
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
                                            model              = result.poster,
                                            contentDescription = result.title,
                                            contentScale       = ContentScale.Crop,
                                            modifier           = Modifier.fillMaxSize(),
                                            loading            = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    // Text Details
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text  = result.title,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize   = 22.sp
                                            ),
                                            color    = Color.White,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Row(
                                            verticalAlignment      = Alignment.CenterVertically,
                                            horizontalArrangement  = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (result.year != null && result.year != 0) {
                                                Text(result.year.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                            }
                                        }
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
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clickable { onPlayClick(episode.sources, episode) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                                if (!episode.thumbnail.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = episode.thumbnail,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Play",
                                                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                                                    tint = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${episode.number}. ${episode.title}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "45m", // Placeholder for duration
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (result.sources.isNotEmpty()) {
// Movie
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Button(
                                        onClick = { onPlayClick(result.sources, null) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
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
