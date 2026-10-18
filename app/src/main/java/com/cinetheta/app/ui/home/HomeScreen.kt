package com.cinetheta.app.ui.home

import com.cinetheta.app.data.bookmarks.BookmarkManager
import com.cinetheta.app.data.bookmarks.BookmarkItem


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import coil.request.ImageRequest
import androidx.compose.runtime.remember
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
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.domain.repositories.ProviderRepository
import com.cinetheta.app.ui.theme.*
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — Netflix/Prime-inspired cinematic home
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    providerRepository: ProviderRepository,
    onNavigateToDetail: (String, String) -> Unit,
    onContinueWatchingClick: (com.cinetheta.player.resume.HistoryItem) -> Unit,
    onNavigateToContinueWatching: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    // Tab state for Home / Movies / Shows / Anime
    val tabs = listOf("Home", "Movies", "Shows", "Anime")
    var selectedTab by rememberSaveable { mutableIntStateOf(viewModel.selectedTabIndex) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showHomeSupportDialog by remember { mutableStateOf(false) }
    var showFullDonationDialog by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var isResolvingLiveSport by remember { mutableStateOf(false) }
    var resolvingSportTitle by remember { mutableStateOf("") }

    LaunchedEffect(providerRepository.selectedProviderId) {
        val pid = providerRepository.selectedProviderId
        if (pid == "animedekho" || pid == "toonstream" || pid == "piratexplay") {
            selectedTab = 3
            viewModel.selectedTabIndex = 3
        } else if (pid == "moviebox" || pid == "netmirror" || pid == "castletv" || pid == "hdhub4u" || pid == "fourkhdhub" || pid == "youtube") {
            // Optional: reset to Home if switching back from an anime provider to a general provider
            if (selectedTab == 3) {
                selectedTab = 0
                viewModel.selectedTabIndex = 0
            }
        }
    }

    val filteredSections = remember(state.sections, selectedTab) {
        when (selectedTab) {
            1 -> state.sections.filter { it.id.contains("movie", ignoreCase = true) || it.id == "trending_cinema" }
            2 -> state.sections.filter { it.id.contains("series", ignoreCase = true) || it.id.contains("show", ignoreCase = true) || it.id.contains("drama", ignoreCase = true) }
            3 -> state.sections.filter { 
                it.id.contains("anime", ignoreCase = true) || 
                it.id.contains("cartoon", ignoreCase = true) || 
                it.id.contains("nickelodeon", ignoreCase = true) 
            }
            else -> state.sections.filter { 
                !it.id.contains("anime", ignoreCase = true) && 
                !it.id.contains("cartoon", ignoreCase = true) && 
                !it.id.contains("nickelodeon", ignoreCase = true) 
            } // 0 -> Home
        }
    }

    val featuredItems = remember(state, selectedTab) {
        when (selectedTab) {
            1 -> state.sections.find { it.id == "trending_cinema" }?.items?.take(5) ?: state.popularMovies.take(5)
            2 -> state.sections.find { it.id == "top_series" }?.items?.take(5) ?: state.popularMovies.take(5)
            3 -> state.sections.find { it.id == "anime_shows" }?.items?.take(5) ?: state.popularMovies.take(5)
            else -> state.popularMovies.take(5)
        }
    }

    val pagerState = rememberSafePagerState(initialPage = 0) { featuredItems.size }

    // Reset hero carousel to page 0 whenever the selected category tab changes
    LaunchedEffect(selectedTab) {
        if (featuredItems.isNotEmpty()) {
            try {
                pagerState.scrollToPage(0)
            } catch (_: Exception) {}
        }
    }

    // Auto-advance hero every 5 seconds when user is not actively swiping
    LaunchedEffect(pagerState, featuredItems.size) {
        if (featuredItems.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(5000L)
                if (!pagerState.isScrollInProgress) {
                    val count = featuredItems.size
                    if (count > 1) {
                        val nextPage = (pagerState.currentPage + 1) % count
                        try {
                            pagerState.animateScrollToPage(nextPage)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

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

    LaunchedEffect(Unit) {
        viewModel.reloadHistory()
        if (com.cinetheta.app.utils.SupportManager.shouldShowHomeSupportPrompt(context)) {
            kotlinx.coroutines.delay(2000L)
            showHomeSupportDialog = true
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Main Content ─────────────────────────────────────────────────────
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) // nav bar space
        ) {
            // ── HERO (Swipeable Carousel) ─────────────────────────────────────
            item {
                if (featuredItems.isNotEmpty()) {
                    val currentHero = featuredItems.getOrNull(pagerState.currentPage) ?: featuredItems.first()
                    
                    // Wrapping in a Box just for the Ambient Background
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Safe Ambient Background
                        val blurRequest = remember(currentHero.poster) {
                            ImageRequest.Builder(context)
                                .data(currentHero.poster)
                                .size(16) // Load a tiny 64x64 thumbnail to save memory
                                .build()
                        }
                        
                        SubcomposeAsyncImage(
                            model = blurRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(50.dp) // Safe blur because the image is tiny
                                .graphicsLayer { alpha = 0.65f }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.6f), // Dark at top for status bar
                                            0.15f to Color.Transparent,
                                            0.75f to Color.Transparent,
                                            1.0f to MaterialTheme.colorScheme.background // Fade to app background at bottom
                                        )
                                    )
                                )
                        )

                        // Content (Pager + Buttons) stacked vertically
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top spacing so the poster sits in the middle and clears the status bar
                            Spacer(modifier = Modifier.height(72.dp))
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                pageSpacing = 16.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val hero = featuredItems.getOrNull(page)

                                Box(modifier = Modifier.graphicsLayer {
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val cardScale = 1f - (kotlin.math.abs(pageOffset) * 0.15f).coerceIn(0f, 1f)
                                    val cardAlpha = 1f - (kotlin.math.abs(pageOffset) * 0.5f).coerceIn(0f, 1f)
                                    val cardRotation = (pageOffset * 15f).coerceIn(-15f, 15f)

                                    scaleX = cardScale
                                    scaleY = cardScale
                                    this.alpha = cardAlpha
                                    rotationY = cardRotation
                                    cameraDistance = 8 * density
                                }.clip(RoundedCornerShape(24.dp))) {
                                    if (hero != null) {
                                        SFHeroBackdrop(movie = hero)
                                    } else {
                                        SFHeroShimmer()
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Static overlay for Buttons and Dots
                            SFHeroStaticOverlay(
                                movie = currentHero,
                                heroIndex = pagerState.currentPage,
                                heroCount = featuredItems.size,
                                onPlayClick = { onNavigateToDetail(currentHero.type.name, currentHero.id) },
                                onInfoClick = { onNavigateToDetail(currentHero.type.name, currentHero.id) },
                                onDotClick = { targetIndex ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetIndex)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Shimmer placeholder while loading
                    SFHeroShimmer()
                }
            }

            // ── CONTENT ROW — Live Sports & Channels (Directly below Hero) ──────
            if (selectedTab == 0 && state.liveSports.isNotEmpty()) {
                item {
                    SFLiveSportsRow(
                        items = state.liveSports,
                        onItemClick = { sportItem ->
                            if (isResolvingLiveSport) return@SFLiveSportsRow
                            isResolvingLiveSport = true
                            resolvingSportTitle = sportItem.title
                            coroutineScope.launch {
                                try {
                                    val stream = com.cinetheta.app.data.sports.LiveSportsRepository.resolveStream(sportItem.id)
                                    if (stream != null) {
                                        val intent = android.content.Intent(context, com.cinetheta.player.PlayerActivity::class.java).apply {
                                            putExtra("MEDIA_ID", "live_sport_${sportItem.id}")
                                            putExtra("VIDEO_TITLE", sportItem.title)
                                            putExtra("IS_SHOW", false)
                                            putExtra("DIRECT_STREAM_URL", stream.url)
                                            if (stream.backupUrls.isNotEmpty()) {
                                                putStringArrayListExtra("BACKUP_STREAM_URLS", ArrayList(stream.backupUrls))
                                            }
                                            putExtra("STREAM_HEADERS", HashMap(stream.headers))
                                            putExtra("POSTER_PATH", sportItem.poster)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        android.widget.Toast.makeText(context, "Stream currently offline or buffering. Try another live channel.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to load live stream: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                } finally {
                                    isResolvingLiveSport = false
                                }
                            }
                        }
                    )
                }
            }

            // ── CONTENT ROW — Continue Watching ──────
            if (state.continueWatching.isNotEmpty()) {
                item {
                    SFContinueWatchingRow(
                        items    = state.continueWatching,
                        onItemClick = onContinueWatchingClick,
                        onSeeAllClick = onNavigateToContinueWatching
                    )
                }
            }

            // ── DYNAMIC CONTENT ROWS ──────────────────────────────────────────
            items(filteredSections) { section ->
                SFSectionRow(
                    title    = section.title,
                    items    = section.items,
                    onItemClick = onNavigateToDetail,
                    onSeeAllClick = { onExploreClick(section.id, section.title) }
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
                onTabSelected = { 
                    selectedTab = it
                    viewModel.selectedTabIndex = it
                },
                onSearchClick = onSearchClick,
                onProfileClick = onSettingsClick,
                onDownloadsClick = onDownloadsClick,
                onProviderChanged = { viewModel.loadHomeData() }
            )
        }

        if (showHomeSupportDialog) {
            com.cinetheta.app.utils.HomeSupportDialog(
                onWatchAd = {
                    showHomeSupportDialog = false
                    com.cinetheta.app.utils.SupportManager.markSupportPromptShown(context)
                    com.cinetheta.app.utils.SupportManager.openAd(context)
                },
                onOpenDonations = {
                    showHomeSupportDialog = false
                    com.cinetheta.app.utils.SupportManager.markSupportPromptShown(context)
                    showFullDonationDialog = true
                },
                onDismiss = {
                    showHomeSupportDialog = false
                    com.cinetheta.app.utils.SupportManager.markSupportPromptShown(context)
                }
            )
        }

        if (showFullDonationDialog) {
            com.cinetheta.app.utils.FullSupportCineThetaDialog(
                onDismiss = { showFullDonationDialog = false },
                context = context,
                onWatchAdSuccess = {
                    showFullDonationDialog = false
                }
            )
        }

        if (isResolvingLiveSport) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { isResolvingLiveSport = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = "Connecting to Live Stream...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = resolvingSportTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

//        // Listens for return from ad: shows big ThankYouSupportDialog if >= 20s, or Toast if < 20s
//        com.cinetheta.app.utils.AdReturnLifecycleTracker(
//            context = context,
//            onShowThankYouDialog = { showThankYouDialog = true }
//        )
//
//        if (showThankYouDialog) {
//            com.cinetheta.app.utils.ThankYouSupportDialog(
//                onDismiss = { showThankYouDialog = false },
//                onOpenAdAgain = {
//                    showThankYouDialog = false
//                    com.cinetheta.app.utils.SupportManager.openAd(context)
//                }
//            )
//        }

        // ── Floating Provider Selector (FAB) ──────────────────────────────────
        var showProviderSelector by remember { mutableStateOf(false) }
        var showMovieBoxSettings by remember { mutableStateOf(false) }

        if (showMovieBoxSettings) {
            MovieBoxSettingsDialog(
                onDismiss = { showMovieBoxSettings = false },
                context = context
            )
        }

        if (showProviderSelector) {
            ProviderSelectorSheet(
                providerRepository = providerRepository,
                onDismiss = { showProviderSelector = false },
                onProviderSelected = { pid ->
                    providerRepository.selectedProviderId = pid
                    context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
                        .edit().putString("selected_provider", pid).apply()
                    showProviderSelector = false
                    viewModel.loadHomeData()
                },
                onSettingsClick = {
                    showProviderSelector = false
                    showMovieBoxSettings = true
                }
            )
        }

        androidx.compose.material3.SmallFloatingActionButton(
            onClick = { showProviderSelector = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Outlined.Extension, contentDescription = "Select Provider")
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
    providerRepository: com.cinetheta.domain.repositories.ProviderRepository,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onProviderChanged: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // SF Logo wordmark
                Text(
                    text = "CineTheta",
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
                    // Search icon
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Search, "Search",
                            tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Row 2: Tab navigation with transparent boxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val bgColor = if (isSelected) Color(0xFFE62117).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.2f)
                    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable { onTabSelected(index) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
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
fun SFHeroBackdrop(movie: SearchResult) {
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenW - 64.dp
    val exactHeight = cardWidth * 1.4f // Standard TMDB poster aspect ratio

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(exactHeight)
    ) {
        SubcomposeAsyncImage(
            model              = movie.poster,
            contentDescription = movie.title,
            contentScale       = ContentScale.Crop,
            loading            = { SFHeroShimmer() },
            modifier           = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 90.dp), // reduced bottom padding since buttons are lower
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = "Action  •  Thriller  •  Sci-Fi",
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                movie.year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SFDot()
                }
                SFBadge("HD", SFHDTag, Color.Black)
                SFBadge("DUB", SFDubBg, Color.White)
            }
        }
    }
}

@Composable
fun SFHeroStaticOverlay(
    movie: SearchResult,
    heroIndex: Int,
    heroCount: Int,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDotClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape  = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play", color = Color.Black, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                var isInMyList by remember(movie.id) { mutableStateOf(BookmarkManager.isBookmarked(movie.id)) }
                OutlinedButton(
                    onClick = {
                        isInMyList = !isInMyList
                        if (isInMyList) {
                            BookmarkManager.addBookmark(BookmarkItem(movie.id, movie.title, movie.poster ?: "", movie.type == com.cinetheta.app.domain.models.ContentType.SHOW))
                        } else {
                            BookmarkManager.removeBookmark(movie.id)
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    shape    = RoundedCornerShape(6.dp),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    val icon = if (isInMyList) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
                    val tint = if (isInMyList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isInMyList) "Added" else "List", color = tint, style = MaterialTheme.typography.bodyMedium)
                }

                IconButton(
                    onClick  = onInfoClick,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Outlined.Info, "Info", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                }
            }

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
    onItemClick: (String, String) -> Unit,
    onSeeAllClick: () -> Unit = {}
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
                modifier = Modifier.clickable { onSeeAllClick() }.padding(4.dp)
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
                    onClick = { onItemClick(item.type.name, item.id) }
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
    items: List<com.cinetheta.player.resume.HistoryItem>,
    onItemClick: (com.cinetheta.player.resume.HistoryItem) -> Unit,
    onSeeAllClick: () -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onSeeAllClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "▶ Continue Watching",
                style    = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color    = MaterialTheme.colorScheme.onBackground
            )
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.ArrowForward,
                contentDescription = "See All",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                SFContinueCard(item = item, onClick = { onItemClick(item) })
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
fun SFContinueCard(
    item: com.cinetheta.player.resume.HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = item.posterPath,
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
                modifier = Modifier.size(18.dp)
            )
        }

        // Real Progress bar at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .background(SFProgressBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
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
fun SFLoadingRow() {
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
fun SFErrorBanner(message: String, onRetry: () -> Unit) {
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MovieBoxSettingsDialog(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val prefs = context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
    val defaultDomain = com.cinetheta.providers.moviebox.MovieBoxConfig.DEFAULT_DOMAIN
    val hosts = com.cinetheta.providers.moviebox.MovieBoxConfig.HOST_POOL

    var selectedApi by remember { mutableStateOf(prefs.getString("moviebox_api", defaultDomain) ?: defaultDomain) }
    var showReloadPrompt by remember { mutableStateOf(false) }

    if (showReloadPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text("Success") },
            text = { androidx.compose.material3.Text("Settings applied successfully!") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showReloadPrompt = false
                    onDismiss()
                }) {
                    androidx.compose.material3.Text("Apply")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text("MovieBox Settings") },
            text = {
                Column {
                    androidx.compose.material3.Text("Select API Host:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    hosts.forEach { host ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApi = host }
                                .padding(vertical = 4.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = (selectedApi == host),
                                onClick = { selectedApi = host }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Text(host)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.IconButton(onClick = {
                    prefs.edit().putString("moviebox_api", selectedApi).apply()
                    com.cinetheta.providers.moviebox.MovieBoxConfig.savedDomain = selectedApi
                    showReloadPrompt = true
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

/**
 * Crash-proof PagerState remember function.
 * 
 * Compose Foundation's default rememberPagerState has a known bug where currentPageOffsetFraction
 * during animation can be serialized outside the valid [-0.5, 0.5] range (e.g. 1.0000001).
 * When navigating back from another screen, PagerStateImpl's restore constructor throws
 * IllegalArgumentException: initialPageOffsetFraction is not within the range -0.5 to 0.5.
 * 
 * SafePagerState stores only the currentPage index and clamps offset to 0f on restore,
 * guaranteeing immunity from crash on navigation back.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberSafePagerState(
    initialPage: Int = 0,
    pageCount: () -> Int
): PagerState {
    val pageCountState = rememberUpdatedState(pageCount)
    return rememberSaveable(
        saver = Saver(
            save = { listOf(it.currentPage) },
            restore = { saved ->
                val list = saved as? List<*>
                val page = (list?.getOrNull(0) as? Int) ?: initialPage
                SafePagerState(page) { pageCountState.value() }
            }
        )
    ) {
        SafePagerState(initialPage) { pageCountState.value() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class SafePagerState(
    currentPage: Int = 0,
    private val pageCountProvider: () -> Int
) : PagerState(currentPage, 0f) {
    override val pageCount: Int get() = pageCountProvider()
}


@Composable
fun ProviderSelectorSheet(
    providerRepository: com.cinetheta.domain.repositories.ProviderRepository,
    onDismiss: () -> Unit,
    onProviderSelected: (String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        text = "Select Provider",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val order = listOf(
                    "moviebox", "netmirror", "castletv", "uhdmovies", "animedekho",
                    "toonstream", "piratexplay", "hdhub4u", "fourkhdhub", "youtube"
                )
                
                val sortedProviders = providerRepository.enabledProviders().sortedBy { provider ->
                    val idx = order.indexOf(provider.id)
                    if (idx != -1) idx else 999
                }

                item {
                    ProviderBox(
                        name = "None",
                        hint = "Disable specific provider",
                        isSelected = providerRepository.selectedProviderId == null,
                        onClick = { onProviderSelected(null) }
                    )
                }

                items(sortedProviders) { provider ->
                    val isSelected = providerRepository.selectedProviderId == provider.id
                    val hint = when (provider.id) {
                        "moviebox", "netmirror" -> "Movie, show and anime in multi audio"
                        "castletv" -> "Indian native languages\nUse VLC for Multi audio"
                        "uhdmovies" -> "MultiAudio Movies & Shows in 4K"
                        "animedekho", "toonstream", "piratexplay" -> "Hindi dub anime"
                        "hdhub4u", "fourkhdhub" -> "Dual Audio Movies & Shows"
                        else -> null
                    }
                    
                    ProviderBox(
                        name = provider.name,
                        hint = hint,
                        isSelected = isSelected,
                        onClick = { onProviderSelected(provider.id) },
                        onSettingsClick = if (provider.id == "moviebox") onSettingsClick else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderBox(
    name: String,
    hint: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    val borderColor = if (isSelected) Color(0xFFE62117) else Color.Transparent
    val bgColor = if (isSelected) Color(0xFFE62117).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (hint != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        
        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIVE SPORTS & LIVE TV ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFLiveSportsRow(
    items: List<com.cinetheta.app.data.sports.LiveSportItem>,
    onItemClick: (com.cinetheta.app.data.sports.LiveSportItem) -> Unit
) {
    if (items.isEmpty()) return

    val preferredOrder = listOf("all", "cricket", "football", "news", "movies", "entertainment", "fight", "basketball", "24/7-streams")
    val categories = remember(items) {
        val cats = items.map { it.category.lowercase() }.distinct()
        listOf("all") + cats.sortedBy { cat ->
            val idx = preferredOrder.indexOf(cat)
            if (idx >= 0) idx else 99
        }
    }
    var selectedCategory by remember { mutableStateOf("all") }

    val filteredItems = remember(items, selectedCategory) {
        if (selectedCategory == "all") items
        else items.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        // Section header with glowing LIVE badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                )
                Text(
                    text = "Live TV & Sports",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE50914).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.4f))
            ) {
                Text(
                    text = "${items.count { it.isLive }} LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFE50914),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Category filter chips
        if (categories.size > 2) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val label = when (cat) {
                        "all" -> "All Channels"
                        "cricket" -> "🏏 Cricket"
                        "football" -> "⚽ Football"
                        "news" -> "📰 News"
                        "movies" -> "🎬 Movies"
                        "entertainment" -> "🎭 Entertainment"
                        "basketball" -> "🏀 Basketball"
                        "american-football" -> "🏈 NFL"
                        "fight" -> "🥊 Combat"
                        "hockey" -> "🏒 Hockey"
                        "motor-sports" -> "🏎️ Motorsport"
                        "tennis" -> "🎾 Tennis"
                        "golf" -> "⛳ Golf"
                        "24/7-streams" -> "📺 24/7 TV"
                        else -> cat.replaceFirstChar { it.uppercase() }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Horizontal match cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredItems) { item ->
                LiveSportCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

private data class MatchStatusInfo(
    val badgeLabel: String,
    val badgeColor: Color,
    val hasDot: Boolean,
    val isBreak: Boolean,
    val statusText: String
)

private fun getMatchStatusInfo(item: com.cinetheta.app.data.sports.LiveSportItem): MatchStatusInfo {
    val now = System.currentTimeMillis()
    val is247 = item.isAlwaysLive || item.category.equals("24/7-streams", ignoreCase = true) || item.category.equals("24/7", ignoreCase = true)

    if (is247) {
        return MatchStatusInfo(
            badgeLabel = "24/7 LIVE",
            badgeColor = Color(0xFFE50914),
            hasDot = true,
            isBreak = false,
            statusText = "24/7 Continuous Broadcast"
        )
    }

    if (item.isLive) {
        val cat = item.category.lowercase()
        val isFootball = cat in listOf("football", "soccer")

        if (isFootball && item.date != null && item.date > 0) {
            val elapsedMin = ((now - item.date) / 60000L).toInt()
            return when {
                elapsedMin in 0..45 -> MatchStatusInfo(
                    badgeLabel = "1st Half ${elapsedMin}'",
                    badgeColor = Color(0xFFE50914),
                    hasDot = true,
                    isBreak = false,
                    statusText = "1st Half in progress"
                )
                elapsedMin in 46..60 -> {
                    val remaining = (60 - elapsedMin).coerceAtLeast(1)
                    MatchStatusInfo(
                        badgeLabel = "⏸ Break (${remaining}m left)",
                        badgeColor = Color(0xFFF59E0B),
                        hasDot = false,
                        isBreak = true,
                        statusText = "Half-Time Break ($remaining mins remaining)"
                    )
                }
                elapsedMin in 61..105 -> {
                    val secondHalfMin = (elapsedMin - 15).coerceAtLeast(46)
                    MatchStatusInfo(
                        badgeLabel = "2nd Half ${secondHalfMin}'",
                        badgeColor = Color(0xFFE50914),
                        hasDot = true,
                        isBreak = false,
                        statusText = "2nd Half in progress"
                    )
                }
                elapsedMin > 105 -> MatchStatusInfo(
                    badgeLabel = "LIVE (ET/FT)",
                    badgeColor = Color(0xFFDC2626),
                    hasDot = true,
                    isBreak = false,
                    statusText = "Extra time / Match concluding"
                )
                else -> MatchStatusInfo(
                    badgeLabel = "LIVE",
                    badgeColor = Color(0xFFE50914),
                    hasDot = true,
                    isBreak = false,
                    statusText = "Live match in progress"
                )
            }
        } else if (item.date != null && item.date > 0) {
            val elapsedMin = ((now - item.date) / 60000L).toInt()
            val timeBadge = if (elapsedMin in 1..480) {
                val h = elapsedMin / 60
                val m = elapsedMin % 60
                if (h > 0) "${h}h ${m}m" else "${m}m"
            } else null

            return MatchStatusInfo(
                badgeLabel = if (timeBadge != null) "LIVE ($timeBadge)" else "LIVE",
                badgeColor = Color(0xFFE50914),
                hasDot = true,
                isBreak = false,
                statusText = "Live stream in progress"
            )
        } else {
            return MatchStatusInfo(
                badgeLabel = "LIVE",
                badgeColor = Color(0xFFE50914),
                hasDot = true,
                isBreak = false,
                statusText = "Live stream"
            )
        }
    } else {
        // Upcoming
        if (item.date != null && item.date > 0) {
            val diffMs = item.date - now
            if (diffMs > 0) {
                val totalMin = (diffMs / 60000L).toInt()
                val hours = totalMin / 60
                val mins = totalMin % 60
                val days = hours / 24
                val remHours = hours % 24

                val badge = when {
                    days > 0 -> "In ${days}d ${remHours}h"
                    hours > 0 -> "In ${hours}h ${mins}m"
                    mins > 0 -> "In ${mins}m"
                    else -> "Starts Soon"
                }

                val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val startTimeStr = try {
                    timeFormatter.format(java.util.Date(item.date))
                } catch (e: Exception) {
                    ""
                }

                return MatchStatusInfo(
                    badgeLabel = badge,
                    badgeColor = Color(0xFF2563EB).copy(alpha = 0.9f),
                    hasDot = false,
                    isBreak = false,
                    statusText = if (startTimeStr.isNotBlank()) "Starts at $startTimeStr" else "Starting soon"
                )
            }
        }
        return MatchStatusInfo(
            badgeLabel = "UPCOMING",
            badgeColor = Color.Black.copy(alpha = 0.6f),
            hasDot = false,
            isBreak = false,
            statusText = "Scheduled match"
        )
    }
}

@Composable
fun LiveSportCard(
    item: com.cinetheta.app.data.sports.LiveSportItem,
    onClick: () -> Unit
) {
    val statusInfo = remember(item) { getMatchStatusInfo(item) }

    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.5f),
            Color.Black.copy(alpha = 0.95f)
        )
    )

    Card(
        modifier = Modifier
            .width(230.dp)
            .height(135.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.poster.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = when (item.category.lowercase()) {
                                    "football" -> listOf(Color(0xFF0F3820), Color(0xFF051C0F))
                                    "cricket" -> listOf(Color(0xFF1E3A5F), Color(0xFF0B192C))
                                    "news" -> listOf(Color(0xFF3E1F1F), Color(0xFF1C0A0A))
                                    "movies" -> listOf(Color(0xFF4A2A0C), Color(0xFF221104))
                                    "entertainment" -> listOf(Color(0xFF381E48), Color(0xFF1A0A26))
                                    "basketball" -> listOf(Color(0xFF5A2E12), Color(0xFF261205))
                                    "fight" -> listOf(Color(0xFF4A1212), Color(0xFF210505))
                                    "american-football" -> listOf(Color(0xFF382312), Color(0xFF190E05))
                                    else -> listOf(Color(0xFF1F2937), Color(0xFF111827))
                                }
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardGradient)
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusInfo.badgeColor,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (statusInfo.hasDot) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        } else if (!statusInfo.isBreak && !item.isLive) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = statusInfo.badgeLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                val badgeText = item.league ?: when (item.category.lowercase()) {
                    "football" -> "Football"
                    "cricket" -> "Cricket"
                    "news" -> "Live News"
                    "movies" -> "Movies"
                    "entertainment" -> "Entertainment"
                    "basketball" -> "Basketball"
                    "american-football" -> "NFL"
                    "fight" -> "Combat"
                    "hockey" -> "Hockey"
                    "golf" -> "Golf"
                    "tennis" -> "Tennis"
                    "24/7-streams" -> "24/7 TV"
                    else -> item.category.replaceFirstChar { it.uppercase() }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = badgeText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom Info (Title & Status Action)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (item.isLive) Icons.Outlined.PlayCircle else Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = if (statusInfo.isBreak) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = statusInfo.statusText,
                        color = if (statusInfo.isBreak) Color(0xFFFCD34D) else Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

