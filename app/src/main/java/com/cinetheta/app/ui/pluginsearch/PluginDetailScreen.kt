package com.cinetheta.app.ui.pluginsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cinetheta.domain.models.ProviderEpisode
import com.cinetheta.domain.models.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    searchResult: SearchResult,
    viewModel: PluginDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (ProviderEpisode?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(searchResult) {
        viewModel.loadContent(searchResult)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(searchResult.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                    if (result.seasons.isNotEmpty()) {
                        // TV Show
                        var selectedSeason by remember { mutableStateOf(result.seasons.firstOrNull()) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Season Selector
                            ScrollableTabRow(
                                selectedTabIndex = result.seasons.indexOf(selectedSeason).coerceAtLeast(0),
                                edgePadding = 16.dp
                            ) {
                                result.seasons.forEach { season ->
                                    Tab(
                                        selected = selectedSeason == season,
                                        onClick = { selectedSeason = season },
                                        text = { Text("Season ${season.number}") }
                                    )
                                }
                            }
                            // Episodes
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(selectedSeason?.episodes ?: emptyList()) { episode ->
                                    ListItem(
                                        headlineContent = { Text(episode.name ?: "Episode ${episode.number}") },
                                        modifier = Modifier.clickable { onPlayClick(episode) }
                                    )
                                }
                            }
                        }
                    } else if (result.sources.isNotEmpty()) {
                        // Movie
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(onClick = { onPlayClick(null) }) {
                                Text("Play Movie")
                            }
                        }
                    } else {
                        Text("No content available", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}
