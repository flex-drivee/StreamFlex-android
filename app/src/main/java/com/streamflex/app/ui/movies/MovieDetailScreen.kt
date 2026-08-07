package com.streamflex.app.ui.movies

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.streamflex.app.domain.models.Episode
import com.streamflex.app.domain.models.SearchResult
import com.streamflex.app.ui.home.SFBadge
import com.streamflex.app.ui.home.SFVideoCard
import com.streamflex.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// MovieDetailScreen — Full premium detail page
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    viewModel: MovieDetailViewModel,
    onBackClick: () -> Unit,
    onMoviePlayClick: (List<String>) -> Unit,
    onEpisodePlayClick: (Episode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val isShow       = state.show != null
    val title        = state.movie?.title    ?: state.show?.title    ?: ""
    val backdrop     = state.movie?.backdrop ?: state.show?.backdrop
    val poster       = backdrop // use backdrop as hero bg
    val overview     = state.movie?.overview ?: state.show?.overview ?: ""
    val year         = state.movie?.year     ?: state.show?.year
    val rating       = state.movie?.rating   ?: state.show?.rating
    val movieRuntime = state.movie?.runtime

    var isSeasonMenuExpanded  by remember { mutableStateOf(false) }
    var areAllEpisodesVisible by remember { mutableStateOf(false) }
    var isInMyList            by remember { mutableStateOf(false) }
    var isStreamLoading       by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedSeason) { areAllEpisodesVisible = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SFBgPrimary)
    ) {
        if (state.isLoading) {
            SFDetailShimmer()
        } else {
            val scrollState = rememberLazyListState()

            LazyColumn(
                state   = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

                // ── 1. HERO BACKDROP ─────────────────────────────────────────
                item {
                    SFDetailHero(
                        backdrop = backdrop,
                        title    = title,
                        year     = year,
                        rating   = rating,
                        runtime  = movieRuntime,
                        isShow   = isShow
                    )
                }

                // ── 2. ACTION BUTTONS ─────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PLAY button
                        Button(
                            onClick = {
                                isStreamLoading = true
                                viewModel.fetchMovieStreams { links ->
                                    isStreamLoading = false
                                    if (links.isNotEmpty()) onMoviePlayClick(links)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(6.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                            enabled  = !isStreamLoading
                        ) {
                            if (isStreamLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color    = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, null,
                                    tint = Color.Black, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isStreamLoading) "Finding streams…" else "Play",
                                color      = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                        }

                        // SECONDARY ROW — Download + My List + Share
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Download
                            OutlinedButton(
                                onClick  = {},
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape    = RoundedCornerShape(6.dp),
                                border   = BorderStroke(1.dp, SFOutline),
                                colors   = ButtonDefaults.outlinedButtonColors(containerColor = SFBgElevated)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, null,
                                    tint = SFTextPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Download", color = SFTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium)
                            }

                            // My List toggle
                            IconButton(
                                onClick  = { isInMyList = !isInMyList },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isInMyList) SFAccent else SFBgElevated)
                                    .border(BorderStroke(1.dp, SFOutline), RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                                    null, tint = Color.White, modifier = Modifier.size(22.dp)
                                )
                            }

                            // Share
                            IconButton(
                                onClick  = {},
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SFBgElevated)
                                    .border(BorderStroke(1.dp, SFOutline), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.Share, null,
                                    tint = SFTextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // ── 3. OVERVIEW ───────────────────────────────────────────────
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text     = overview,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = SFTextSecondary,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (overview.length > 120) {
                            Text(
                                text     = if (expanded) "Less ▲" else "More ▼",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = SFAccent,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { expanded = !expanded }
                            )
                        }
                    }
                }

                // ── 4. DIVIDER ────────────────────────────────────────────────
                item {
                    HorizontalDivider(color = SFDivider, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // ── 5. SEASONS + EPISODES (TV only) ───────────────────────────
                if (isShow && state.show != null) {
                    item {
                        SFSeasonSelector(
                            show               = state.show!!,
                            selectedSeason     = state.selectedSeason,
                            isMenuExpanded     = isSeasonMenuExpanded,
                            onToggleMenu       = { isSeasonMenuExpanded = !isSeasonMenuExpanded },
                            onSeasonSelected   = { s ->
                                viewModel.loadSeason(s)
                                isSeasonMenuExpanded = false
                            }
                        )
                    }

                    val episodesToShow = if (areAllEpisodesVisible)
                        state.episodes
                    else
                        state.episodes.take(10)

                    items(episodesToShow, key = { it.episodeNumber }) { ep ->
                        SFEpisodeItem(
                            episode = ep,
                            onClick = { onEpisodePlayClick(ep) }
                        )
                    }

                    if (state.episodes.size > 10 && !areAllEpisodesVisible) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { areAllEpisodesVisible = true }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment          = Alignment.CenterVertically,
                                    horizontalArrangement      = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Show all ${state.episodes.size} episodes",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SFAccent
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown,
                                        null, tint = SFAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = SFDivider,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }

                // ── 6. MORE LIKE THIS — Auto-adjust grid ──────────────────────
                if (state.similarContent.isNotEmpty()) {
                    item {
                        SFMoreLikeThis(
                            items       = state.similarContent,
                            onItemClick = { /* navigate */ }
                        )
                    }
                }
            }

            // ── Floating Back Button (always visible) ──────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SFBgPrimary.copy(alpha = 0.75f))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = SFTextPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO — Full-bleed backdrop with gradient + metadata chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFDetailHero(
    backdrop: String?,
    title:    String,
    year:     Int?,
    rating:   Double?,
    runtime:  Int?,
    isShow:   Boolean
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(screenH * 0.55f)
    ) {
        SubcomposeAsyncImage(
            model              = backdrop,
            contentDescription = title,
            contentScale       = ContentScale.Crop,
            loading            = {
                Box(Modifier.fillMaxSize().background(SFBgElevated))
            },
            modifier           = Modifier.fillMaxSize()
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(0.4f),
                            0.5f to Color.Transparent,
                            0.8f to SFBgPrimary.copy(0.6f),
                            1.0f to SFBgPrimary
                        )
                    )
                )
        )

        // Bottom metadata
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text  = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 26.sp
                ),
                color    = SFTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.spacedBy(8.dp)
            ) {
                year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodyMedium,
                        color = SFTextSecondary)
                }
                SFBadge("HD", SFHDTag, Color.Black)
                if (isShow) SFBadge("SERIES", SFDubBg, Color.White)
                runtime?.let {
                    if (it > 0) {
                        Text(formatRuntime(it),
                            style = MaterialTheme.typography.bodyMedium, color = SFTextSecondary)
                    }
                }
                rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Star, null, tint = SFRatingBg,
                            modifier = Modifier.size(14.dp))
                        Text(String.format("%.1f", it),
                            style = MaterialTheme.typography.bodyMedium, color = SFTextSecondary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEASON SELECTOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFSeasonSelector(
    show:             com.streamflex.app.domain.models.Show,
    selectedSeason:   Int,
    isMenuExpanded:   Boolean,
    onToggleMenu:     () -> Unit,
    onSeasonSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SFBgElevated)
                .clickable { onToggleMenu() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment          = Alignment.CenterVertically,
            horizontalArrangement      = Arrangement.SpaceBetween
        ) {
            Text(
                "Season $selectedSeason",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SFTextPrimary
            )
            Icon(
                if (isMenuExpanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                null, tint = SFTextSecondary
            )
        }

        AnimatedVisibility(
            visible      = isMenuExpanded,
            enter        = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit         = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(SFBgSurface)
                    .padding(vertical = 4.dp)
            ) {
                show.seasons.forEach { season ->
                    val isSelected = season.seasonNumber == selectedSeason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeasonSelected(season.seasonNumber) }
                            .background(if (isSelected) SFAccent.copy(0.1f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Season ${season.seasonNumber}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) SFAccent else SFTextPrimary
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, null,
                                tint = SFAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (season.seasonNumber != show.seasons.last().seasonNumber) {
                        HorizontalDivider(color = SFDivider)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EPISODE ITEM — Thumbnail + title + overview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFEpisodeItem(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SFBgElevated),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder — no stillPath in Episode model yet
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint     = SFTextDisabled,
                modifier = Modifier.size(28.dp)
            )
            // Runtime badge
            episode.runtime?.let { rt ->
                if (rt > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("${rt}m", style = MaterialTheme.typography.labelSmall,
                            color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${episode.episodeNumber}. ${episode.title}",
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color    = SFTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                episode.overview ?: "No description available",
                style    = MaterialTheme.typography.bodyMedium,
                color    = SFTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = SFTextDisabled, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORE LIKE THIS — Auto-adjusting responsive grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFMoreLikeThis(
    items:       List<SearchResult>,
    onItemClick: (String) -> Unit
) {
    val screenW   = LocalConfiguration.current.screenWidthDp.dp
    val padding   = 16.dp
    val gap       = 10.dp
    // Auto-calculate columns: aim for ~110dp cards, minimum 2 cols
    val columns   = maxOf(2, ((screenW - padding * 2 + gap) / (110.dp + gap)).toInt())
    val cardWidth = (screenW - padding * 2 - gap * (columns - 1)) / columns

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "More Like This",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = SFTextPrimary
            )
            Text("See All", style = MaterialTheme.typography.labelMedium, color = SFAccent)
        }

        Spacer(Modifier.height(12.dp))

        // Chunked into rows of `columns`
        val rows = items.take(9).chunked(columns)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    rowItems.forEach { item ->
                        SFVideoCard(
                            item    = item,
                            onClick = { onItemClick(item.id) },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                    // Fill empty slots in last row
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIMMER LOADING STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SFDetailShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue  = -800f,
        targetValue   = 800f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label         = "shimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(SFBgCard, SFBgElevated, SFBgCard),
        start  = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
        end    = androidx.compose.ui.geometry.Offset(shimmerX + 500f, 300f)
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(340.dp).background(shimmerBrush))
        Spacer(Modifier.height(12.dp))
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 16.dp).background(shimmerBrush))
            Spacer(Modifier.height(8.dp))
        }
    }
}

// Helpers
fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes == 0) return ""
    val h = minutes / 60; val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}