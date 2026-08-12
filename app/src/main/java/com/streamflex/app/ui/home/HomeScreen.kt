package com.streamflex.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — Netflix/Prime-inspired cinematic home
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    providerRepository: ProviderRepository,
    onNavigateToDetail: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    // Hero auto-cycle through first 5 featured items
    val featuredItems = state.popularMovies.take(5)
    var heroIndex by remember { mutableIntStateOf(0) }
    val featuredContent = featuredItems.getOrNull(heroIndex)

    // Top bar fades from transparent → semi-opaque as user scrolls
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 80
        }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(300),
        label = "topBarAlpha"
    )

    // Tab state for Home / Movies / Series / Anime
    val tabs = listOf("Home", "Movies", "Series", "Anime")
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Main Content ─────────────────────────────────────────────────────
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp) // nav bar space
        ) {
            // ── HERO ─────────────────────────────────────────────────────────
            item {
                AnimatedContent(
                    targetState = featuredContent,
                    transitionSpec = {
                        fadeIn(tween(600)) togetherWith fadeOut(tween(400))
                    },
                    label = "heroTransition"
                ) { hero ->
                    if (hero != null) {
                        SFHeroSection(
                            movie        = hero,
                            heroIndex    = heroIndex,
                            heroCount    = featuredItems.size,
                            onPlayClick  = { onNavigateToDetail(hero.id) },
                            onInfoClick  = { onNavigateToDetail(hero.id) },
                            onDotClick   = { heroIndex = it }
                        )
                    } else {
                        // Shimmer placeholder while loading
                        SFHeroShimmer()
                    }
                }
            }

            // ── CONTENT ROW — Popular Movies ─────────────────────────────────
            item {
                SFSectionRow(
                    title    = "🔥 Trending Now",
                    items    = state.popularMovies,
                    onItemClick = onNavigateToDetail
                )
            }

            // ── CONTENT ROW — Popular Shows ───────────────────────────────────
            item {
                SFSectionRow(
                    title    = "📺 Popular Web Series",
                    items    = state.popularShows,
                    onItemClick = onNavigateToDetail
                )
            }

            // ── CONTENT ROW — Continue Watching (mock, real in Phase 3) ──────
            if (state.popularMovies.size > 5) {
                item {
                    SFContinueWatchingRow(
                        items    = state.popularMovies.drop(5).take(6),
                        onItemClick = onNavigateToDetail
                    )
                }
            }

            // ── CONTENT ROW — Action ──────────────────────────────────────────
            item {
                SFSectionRow(
                    title    = "💥 Action & Adventure",
                    items    = state.popularMovies.takeLast(10),
                    onItemClick = onNavigateToDetail
                )
            }

            // ── CONTENT ROW — Top Rated ───────────────────────────────────────
            item {
                SFSectionRow(
                    title    = "⭐ Top Rated",
                    items    = state.popularMovies.shuffled().take(10),
                    onItemClick = onNavigateToDetail
                )
            }

            // ── Loading / Error state ─────────────────────────────────────────
            if (state.isLoading) {
                item { SFLoadingRow() }
            }
            state.errorMessage?.let { err ->
                item { SFErrorBanner(message = err, onRetry = viewModel::loadHomeData) }
            }
        }

        // ── Floating Top Navigation Bar ───────────────────────────────────────
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            SFTopBar(
                alpha         = topBarAlpha,
                selectedTab   = selectedTab,
                tabs          = tabs,
                providerRepository = providerRepository,
                onTabSelected = { selectedTab = it },
                onSearchClick = onSearchClick,
                onProfileClick = onSettingsClick,
                onDownloadsClick = onDownloadsClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR — Transparent → frosted on scroll, tab navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFTopBar(
    alpha: Float,
    selectedTab: Int,
    tabs: List<String>,
    providerRepository: ProviderRepository,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDownloadsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                // gradient from black → transparent as we near fully scrolled
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f * alpha),
                            Color.Transparent
                        )
                    )
                )
                drawContent()
            }
            .statusBarsPadding()
    ) {
        Column {
            // Row 1: Logo + Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // SF Logo wordmark
                Text(
                    text = "StreamFlex",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp,
                        brush      = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF7B8FFF))
                        )
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search (as per reference image top bar)
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Search, "Search",
                            tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                    }
                    
                    // Provider Selector Chip
                    var showProviderDropdown by remember { mutableStateOf(false) }
                    var selectedProviderName by remember { 
                        mutableStateOf(providerRepository.provider(providerRepository.selectedProviderId ?: "")?.name ?: "All Providers") 
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .clickable { showProviderDropdown = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Extension,
                                contentDescription = "Provider",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedProviderName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        DropdownMenu(
                            expanded = showProviderDropdown,
                            onDismissRequest = { showProviderDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Providers", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    providerRepository.selectedProviderId = null
                                    selectedProviderName = "All Providers"
                                    showProviderDropdown = false
                                }
                            )
                            providerRepository.enabledProviders().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        providerRepository.selectedProviderId = provider.id
                                        selectedProviderName = provider.name
                                        showProviderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Tab navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.Transparent,
                contentColor     = MaterialTheme.colorScheme.onBackground,
                edgePadding      = 16.dp,
                divider          = {},
                indicator        = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        val pos = tabPositions[selectedTab]
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = pos.left + 8.dp)
                                .width(pos.width - 16.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { onTabSelected(index) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text  = title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO SECTION — Full-bleed backdrop, cinematic gradient, action buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFHeroSection(
    movie: SearchResult,
    heroIndex: Int,
    heroCount: Int,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDotClick: (Int) -> Unit
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(screenH * 0.70f) // 70% of screen height
    ) {
        // ── Backdrop image ────────────────────────────────────────────────
        SubcomposeAsyncImage(
            model              = movie.poster,
            contentDescription = movie.title,
            contentScale       = ContentScale.Crop,
            loading            = { SFHeroShimmer() },
            modifier           = Modifier.fillMaxSize()
        )

        // ── Multi-stop gradient overlay: dark top (for nav) + dark bottom ─
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.55f), // nav bar area
                            0.3f to Color.Transparent,               // clear in middle
                            0.7f to MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            1.0f to MaterialTheme.colorScheme.background                     // full bg at bottom
                        )
                    )
                )
        )

        // ── Side gradient for depth ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                            0.5f to Color.Transparent
                        )
                    )
                )
        )

        // ── Bottom Content: genres, title, buttons ─────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Genre tags
            Text(
                text  = "Action  •  Thriller  •  Sci-Fi",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Movie title
            Text(
                text     = movie.title,
                style    = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color    = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Year + type badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                movie.year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SFDot()
                }
                SFBadge("HD", SFHDTag, Color.Black)
                SFBadge("DUB", SFDubBg, Color.White)
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play button (primary — white like Netflix)
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape  = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(Icons.Default.PlayArrow, null,
                        tint = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play", color = Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                // My List button
                OutlinedButton(
                    onClick = { /* TODO: Add to list */ },
                    modifier = Modifier.height(46.dp),
                    shape    = RoundedCornerShape(6.dp),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("List", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                }

                // Info button
                IconButton(
                    onClick  = onInfoClick,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Outlined.Info, "Info",
                        tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                }
            }

            // Hero page dots
            if (heroCount > 1) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(heroCount) { i ->
                        val width by animateDpAsState(
                            targetValue = if (i == heroIndex) 20.dp else 6.dp,
                            animationSpec = tween(300),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (i == heroIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                .clickable { onDotClick(i) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION ROW — Horizontal scrolling card row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFSectionRow(
    title: String,
    items: List<SearchResult>,
    onItemClick: (String) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color    = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text     = "See All",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Horizontal card list
        LazyRow(
            contentPadding         = PaddingValues(horizontal = 16.dp),
            horizontalArrangement  = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                SFVideoCard(
                    item    = item,
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTINUE WATCHING ROW — Wider cards with progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFContinueWatchingRow(
    items: List<SearchResult>,
    onItemClick: (String) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text     = "▶ Continue Watching",
            style    = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                SFContinueCard(item = item, onClick = { onItemClick(item.id) })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFVideoCard(
    item: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .width(110.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        // Poster
        AsyncImage(
            model              = item.poster,
            contentDescription = item.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Bottom gradient + title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    )
                )
        )

        // Year badge (top-left)
        item.year?.let { yr ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text  = yr.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Title at bottom
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )
    }
}

@Composable
private fun SFContinueCard(
    item: SearchResult,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = item.poster,
            contentDescription = item.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
        )

        // Play icon overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint     = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Progress bar at bottom (mock 40%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .background(SFProgressBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Title
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UTILITY COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFBadge(text: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textColor)
    }
}

@Composable
private fun SFDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

@Composable
private fun SFHeroShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue   = -1000f,
        targetValue    = 1000f,
        animationSpec  = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label          = "shimmerX"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(
                Brush.linearGradient(
                    colors  = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
                    start   = Offset(shimmerX, 0f),
                    end     = Offset(shimmerX + 500f, 200f)
                )
            )
    )
}

@Composable
private fun SFLoadingRow() {
    Row(
        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(165.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun SFErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.Info, null, tint = SFError, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) {
            Text("Retry", color = MaterialTheme.colorScheme.primary)
        }
    }
}
