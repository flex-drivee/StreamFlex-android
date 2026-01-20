package com.streamflex.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (String) -> Unit // Callback for navigation
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "StreamFlex",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // --- Popular Movies Section ---
                    item {
                        SectionHeader(title = "Popular Movies")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.popularMovies) { movie ->
                                VideoCard(video = movie, onClick = { onNavigateToDetail(movie.id) })
                            }
                        }
                    }

                    // --- Popular Shows Section ---
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Trending TV Shows")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.popularShows) { show ->
                                VideoCard(video = show, onClick = { onNavigateToDetail(show.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
    )
}