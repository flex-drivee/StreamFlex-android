package com.cinetheta.app.ui.pluginsearch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import coil.compose.AsyncImage
import com.cinetheta.app.ui.theme.*
import com.cinetheta.domain.models.SearchResult

// ─────────────────────────────────────────────────────────────────────────────
// PluginSearchScreen — Premium search fires only on Enter press
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSearchScreen(
    viewModel: PluginSearchViewModel,
    onResultClick: (SearchResult) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val state          by viewModel.uiState.collectAsState()
    val focusManager    = LocalFocusManager.current
    val focusRequester  = remember { FocusRequester() }
    var expanded        by remember { mutableStateOf(false) }

    // Auto-focus on entry
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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

                    // Provider Dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = state.selectedProvider?.name ?: "Provider",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 80.dp)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            state.providers.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.name, color = MaterialTheme.colorScheme.onBackground) },
                                    onClick = {
                                        viewModel.selectProvider(provider)
                                        expanded = false
                                    }
                                )
                            }
                        }
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
                                    "Search plugins…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (state.query.isNotBlank()) {
                                    viewModel.search()
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

            // ── CONTENT ───────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Error state
                    state.error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Error: ${state.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Loading shimmer
                    state.isLoading -> {
                        SFSearchShimmerGrid()
                    }

                    // Typed but not searched yet → hint to press Search
                    state.submittedQuery.isEmpty() && state.query.isNotEmpty() -> {
                        SFSearchHint(query = state.query)
                    }
                    
                    // No results after search
                    state.submittedQuery.isNotEmpty() && state.results.isEmpty() -> {
                        SFNoResults(query = state.submittedQuery)
                    }

                    // Results grid
                    state.results.isNotEmpty() -> {
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
                                        onResultClick(item)
                                    }
                                )
                            }
                            
                            // Load more logic
                            if (state.results.isNotEmpty() && !state.isLastPage) {
                                item(span = { GridItemSpan(cols) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Load More",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.loadMore() }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            item(span = { GridItemSpan(cols) }) {
                                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
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
    item:    SearchResult,
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
                        if (item.mediaType.name == "TV") MaterialTheme.colorScheme.secondary.copy(0.85f)
                        else MaterialTheme.colorScheme.primary.copy(0.85f)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    if (item.mediaType.name == "TV") "TV" else "Movie",
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
