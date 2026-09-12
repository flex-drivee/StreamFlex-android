package com.cinetheta.app.ui.search

import com.cinetheta.app.data.search.SearchHistoryManager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import coil.compose.AsyncImage
import com.cinetheta.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// SearchScreen — search fires only on Enter press, no auto-suggestions
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel:    SearchViewModel,
    onBackClick:  () -> Unit,
    onItemClick:  (String, String) -> Unit,
    onGenreClick: (String, String) -> Unit = { _, _ -> }
) {
    val state          by viewModel.uiState.collectAsState()
    val focusManager    = LocalFocusManager.current
    val focusRequester  = remember { FocusRequester() }

    // Auto-focus on entry
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Filter chips
    val filterOptions = listOf("All", "Movies", "TV Shows", "Anime")
    var selectedFilter by remember { mutableIntStateOf(0) }

    // Animated search icon pulse when loading
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.4f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "iconAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP SEARCH BAR ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Back button — circular
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint     = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Search field — glassmorphism pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            .border(
                                width  = 1.dp,
                                brush  = Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ),
                                shape  = RoundedCornerShape(25.dp)
                            )
                    ) {
                        TextField(
                            value         = state.query,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier      = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester),
                            placeholder   = {
                                Text(
                                    "Search movies, shows, anime…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (state.query.isNotBlank()) {
                                    SearchHistoryManager.addSearchQuery(state.query.trim())
                                    viewModel.onSearch()
                                }
                                focusManager.clearFocus()
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onBackground,
                                cursorColor             = MaterialTheme.colorScheme.primary
                            ),
                            textStyle   = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, null,
                                    tint     = if (state.isLoading)
                                        MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        viewModel.onQueryChange("")
                                        focusRequester.requestFocus()
                                    }) {
                                        Icon(
                                            Icons.Default.Close, "Clear",
                                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ── FILTER CHIPS ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.results.isNotEmpty() || state.submittedQuery.isNotEmpty(),
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEachIndexed { index, label ->
                        val selected = selectedFilter == index
                        val chipBg by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            label = "chipBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(chipBg)
                                .clickable { selectedFilter = index }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── CONTENT ───────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Loading shimmer
                    state.isLoading -> {
                        SFSearchShimmerGrid()
                    }

                    // No query yet → browse genres + history
                    state.submittedQuery.isEmpty() && state.query.isEmpty() -> {
                        SFBrowseCategories(
                            onGenreClick   = onGenreClick,
                            onHistoryClick = {
                                viewModel.onQueryChange(it)
                                viewModel.onSearch()
                                focusManager.clearFocus()
                            }
                        )
                    }

                    // Typed but not searched yet → hint to press Search
                    state.submittedQuery.isEmpty() && state.query.isNotEmpty() -> {
                        SFSearchHint(query = state.query)
                    }

                    // No results after search
                    state.results.isEmpty() -> {
                        SFNoResults(query = state.submittedQuery)
                    }

                    // Results grid
                    else -> {
                        val screenW = LocalConfiguration.current.screenWidthDp.dp
                        val cols    = maxOf(2, ((screenW + 10.dp) / (120.dp + 10.dp)).toInt())

                        LazyVerticalGrid(
                            columns               = GridCells.Fixed(cols),
                            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier              = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(cols) }) {
                                Text(
                                    "Results for \"${state.submittedQuery}\"",
                                    style  = MaterialTheme.typography.labelMedium,
                                    color  = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(state.results) { item ->
                                SFSearchCard(
                                    item    = item,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onItemClick(item.type.name, item.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HINT — user typed but hasn't pressed Search yet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFSearchHint(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Search, null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(52.dp)
            )
            Text(
                "Press Search to find",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "\"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NO RESULTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFNoResults(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.SearchOff, null,
                tint     = SFTextDisabled,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "No results for",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "\"$query\"",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Try different keywords or check spelling",
                style = MaterialTheme.typography.bodySmall,
                color = SFTextDisabled
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH RESULT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFSearchCard(
    item:    com.cinetheta.app.domain.models.SearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model              = item.poster,
                contentDescription = item.title,
                modifier           = Modifier.fillMaxSize()
            )
            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
            // Type badge (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (item.type.name == "TV") MaterialTheme.colorScheme.secondary.copy(0.85f)
                        else MaterialTheme.colorScheme.primary.copy(0.85f)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    if (item.type.name == "TV") "TV" else "Movie",
                    style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color    = Color.White,
                    fontSize = 9.sp
                )
            }
            // Year (bottom-left)
            item.year?.let {
                Text(
                    it.toString(),
                    style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color    = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 7.dp, bottom = 6.dp),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BROWSE CATEGORIES — genres + search history
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFBrowseCategories(
    onGenreClick:   (String, String) -> Unit,
    onHistoryClick: (String) -> Unit
) {
    var history by remember { mutableStateOf(SearchHistoryManager.getHistory()) }

    data class Genre(val name: String, val id: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

    val genres = listOf(
        Genre("Action",   "genre_28",    Color(0xFFE53935), Icons.Filled.Bolt),
        Genre("Drama",    "genre_18",    Color(0xFF8E24AA), Icons.Filled.TheaterComedy),
        Genre("Comedy",   "genre_35",    Color(0xFFE67E22), Icons.Filled.SentimentVerySatisfied),
        Genre("Thriller", "genre_53",    Color(0xFF00897B), Icons.Filled.Visibility),
        Genre("Romance",  "genre_10749", Color(0xFFE91E63), Icons.Filled.Favorite),
        Genre("Sci-Fi",   "genre_878",   Color(0xFF1E88E5), Icons.Filled.RocketLaunch),
        Genre("Horror",   "genre_27",    Color(0xFF546E7A), Icons.Filled.BrightnessMedium),
        Genre("Anime",    "genre_16",    Color(0xFFFF5722), Icons.Filled.AutoAwesome),
        Genre("Crime",    "genre_80",    Color(0xFF37474F), Icons.Filled.GppMaybe),
        Genre("Family",   "genre_10751", Color(0xFF43A047), Icons.Filled.EmojiPeople),
        Genre("Fantasy",  "genre_14",    Color(0xFF5E35B1), Icons.Filled.AutoFixHigh),
        Genre("History",  "genre_36",    Color(0xFF6D4C41), Icons.Filled.Museum),
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {

        // ── Recent Searches ──────────────────────────────────────────────────
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.History, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                        Text(
                            "Recent",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        "Clear all",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                SearchHistoryManager.clearHistory()
                                history = emptyList()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(history) { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(query) }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.History, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                    Text(
                        query,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = {
                            SearchHistoryManager.removeSearchQuery(query)
                            history = SearchHistoryManager.getHistory()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, "Remove",
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp))
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }

        // ── Browse by Genre ──────────────────────────────────────────────────
        item {
            Row(
                modifier          = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.GridView, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Text(
                    "Browse by Genre",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                genres.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(genre.color, genre.color.copy(alpha = 0.55f))
                                        )
                                    )
                                    .clickable { onGenreClick(genre.id, genre.name) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        genre.icon, null,
                                        tint     = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        genre.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIMMER LOADING GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFSearchShimmerGrid() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -800f,
        targetValue   = 800f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label         = "shimmerX"
    )
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
        end   = androidx.compose.ui.geometry.Offset(shimmerX + 500f, 300f)
    )
    LazyVerticalGrid(
        columns               = GridCells.Fixed(3),
        contentPadding        = PaddingValues(16.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(9) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(brush)
                )
            }
        }
    }
}
