package com.streamflex.app.ui.search

import com.streamflex.app.data.search.SearchHistoryManager


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
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import com.streamflex.app.ui.home.SFBadge
import com.streamflex.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// SearchScreen — Premium search with auto-adjusting adaptive grid
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel:   SearchViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    onGenreClick: (String, String) -> Unit = { _, _ -> }
) {
    val state        by viewModel.uiState.collectAsState()
    val focusManager  = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the search field on entry
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Filter chips
    val filterOptions = listOf("All", "Movies", "TV Shows", "Anime")
    var selectedFilter by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP BAR ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back
                    IconButton(
                        onClick  = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    }

                    // Search field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        TextField(
                            value            = state.query,
                            onValueChange    = { viewModel.onQueryChange(it) },
                            modifier         = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester),
                            placeholder      = {
                                Text("Search movies, shows, anime…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SFTextDisabled)
                            },
                            singleLine       = true,
                            keyboardOptions  = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions  = KeyboardActions(onSearch = {
                                if (state.query.isNotBlank()) {
                                    SearchHistoryManager.addSearchQuery(state.query)
                                }
                                focusManager.clearFocus()
                            }),
                            colors           = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onBackground,
                                cursorColor             = MaterialTheme.colorScheme.primary
                            ),
                            textStyle        = MaterialTheme.typography.bodyMedium,
                            leadingIcon      = {
                                Icon(Icons.Default.Search, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon     = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        viewModel.onQueryChange("")
                                        focusRequester.requestFocus()
                                    }) {
                                        Icon(Icons.Default.Close, "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ── FILTER CHIPS ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEachIndexed { index, label ->
                    val selected = selectedFilter == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                CircleShape
                            )
                            .clickable { selectedFilter = index }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── CONTENT ───────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Loading
                    state.isLoading -> {
                        SFSearchShimmerGrid()
                    }

                    // No query → show genre browse categories
                    state.query.isEmpty() -> {
                        SFBrowseCategories(
                            onGenreClick = onGenreClick,
                            onHistoryClick = { 
                                viewModel.onQueryChange(it)
                                focusRequester.requestFocus()
                            }
                        )
                    }

                    // No results
                    state.results.isEmpty() && state.query.length > 2 -> {
                        Column(
                            modifier            = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.Search, null,
                                tint = SFTextDisabled, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No results for \"${state.query}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text("Try different keywords",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SFTextDisabled)
                        }
                    }

                    // Results — adaptive grid
                    state.results.isNotEmpty() -> {
                        val screenW = LocalConfiguration.current.screenWidthDp.dp
                        val cols = maxOf(2, ((screenW + 10.dp) / (120.dp + 10.dp)).toInt())

                        LazyVerticalGrid(
                            columns               = GridCells.Fixed(cols),
                            contentPadding        = PaddingValues(16.dp),
                            verticalArrangement   = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier              = Modifier.fillMaxSize()
                        ) {
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
// SEARCH RESULT CARD — Poster + title + year
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFSearchCard(
    item:    com.streamflex.app.domain.models.SearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model              = item.poster,
                contentDescription = item.title,
                modifier           = Modifier.fillMaxSize()
            )
            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(0.9f)))
                    )
            )
            // HD badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.background.copy(0.75f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("HD", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SFHDTag)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        item.year?.let {
            Text(
                it.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = SFTextDisabled
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BROWSE CATEGORIES — shown when query is empty
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFBrowseCategories(
    onGenreClick: (String, String) -> Unit,
    onHistoryClick: (String) -> Unit
) {
    var history by remember { mutableStateOf(SearchHistoryManager.getHistory()) }

    val genres = listOf(
        Triple("Action", "genre_28", MaterialTheme.colorScheme.primary),
        Triple("Drama", "genre_18", Color(0xFF9B59B6)),
        Triple("Comedy", "genre_35", Color(0xFFE67E22)),
        Triple("Thriller", "genre_53", MaterialTheme.colorScheme.secondary),
        Triple("Romance", "genre_10749", Color(0xFFE91E63)),
        Triple("Sci-Fi", "genre_878", Color(0xFF00BCD4)),
        Triple("Horror", "genre_27", Color(0xFF795548)),
        Triple("Anime", "genre_16", Color(0xFFFF5722)),
        Triple("Crime", "genre_80", Color(0xFF607D8B)),
        Triple("Family", "genre_10751", Color(0xFF4CAF50)),
        Triple("Fantasy", "genre_14", Color(0xFF673AB7)),
        Triple("History", "genre_36", Color(0xFF8D6E63))
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Searches",
                        style    = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
                        color    = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { 
                            SearchHistoryManager.clearHistory()
                            history = emptyList()
                        }.padding(4.dp)
                    )
                }
            }
            items(history.size) { index ->
                val query = history[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(query) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(query, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { 
                            SearchHistoryManager.removeSearchQuery(query)
                            history = SearchHistoryManager.getHistory()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        item {
            Text(
                "Browse by Genre",
            style    = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        val cols = 2
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                genres.chunked(cols).forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowGenres.forEach { (genre, id, color) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(color.copy(0.85f), color.copy(0.5f))
                                        )
                                    )
                                    .clickable { onGenreClick(id, genre) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    genre,
                                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color    = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                        if (rowGenres.size < cols) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(90.dp)) // bottom padding
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
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label         = "shimmerX"
    )
    val brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
        start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
        end   = androidx.compose.ui.geometry.Offset(shimmerX + 500f, 300f)
    )
    LazyVerticalGrid(
        columns               = GridCells.Fixed(3),
        contentPadding        = PaddingValues(16.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(9) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
        }
    }
}
