package com.streamflex.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (String) -> Unit,
    onSearchClick: () -> Unit = {},   // Add callback
    onSettingsClick: () -> Unit = {}  // Add callback
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    // Pick the first movie as the "Featured" hero content
    val featuredContent = state.popularMovies.firstOrNull()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            // --- Custom Transparent Top Bar ---
            // Calculates opacity based on scroll position
            val isScrolled = scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 100

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isScrolled) Color.Black.copy(alpha = 0.9f)
                        else Color.Transparent
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. App Logo / Name
                    Text(
                        text = "StreamFlex",
                        color = Color.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    // 2. Right Actions (Search & Settings)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red
                )
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav if added later
                ) {
                    // --- HERO SECTION ---
                    item {
                        if (featuredContent != null) {
                            HeroSection(
                                movie = featuredContent,
                                onPlayClick = { onNavigateToDetail(featuredContent.id) },
                                onInfoClick = { onNavigateToDetail(featuredContent.id) }
                            )
                        }
                    }

                    // --- SECTIONS ---
                    item {
                        SectionRow(
                            title = "Popular Movies",
                            items = state.popularMovies,
                            onItemClick = onNavigateToDetail
                        )
                    }

                    item {
                        SectionRow(
                            title = "Trending TV Shows",
                            items = state.popularShows,
                            onItemClick = onNavigateToDetail
                        )
                    }

                    // You can duplicate sections for categories
                    item {
                        SectionRow(
                            title = "Action & Adventure",
                            items = state.popularMovies.shuffled(), // Mock data for now
                            onItemClick = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSection(
    movie: SearchResult,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp) // Tall hero image
    ) {
        // Background Image
        AsyncImage(
            model = movie.poster, // Ideally use backdrop here if available in SearchResult
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay (Darkens bottom for text readability)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black
                        ),
                        startY = 300f
                    )
                )
        )

        // Content (Buttons, Title, Genres)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Categories/Tags (Mock)
            Text(
                text = "Sci-Fi • Adventure • Action",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // My List Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Text("My List", color = Color.White, fontSize = 10.sp)
                }

                // Play Button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Info Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onInfoClick() }
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    Text("Info", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SectionRow(
    title: String,
    items: List<SearchResult>,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                VideoCard(video = item, onClick = { onItemClick(item.id) })
            }
        }
    }
}