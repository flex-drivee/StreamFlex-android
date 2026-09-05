package com.streamflex.app.ui.movies

import com.streamflex.app.data.bookmarks.BookmarkManager
import com.streamflex.app.data.bookmarks.BookmarkItem
import com.streamflex.player.resume.PlaybackProgressManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton


import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
    onMainPlayClick: (String?) -> Unit,
    onEpisodePlayClick: (Episode) -> Unit,
    onNavigateToDetail: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val allDownloads by viewModel.allDownloads.collectAsState()

    val context = LocalContext.current
    val progressManager = remember { PlaybackProgressManager(context) }
    
    val mediaId = state.movie?.id ?: state.show?.id
    val historyItem = remember(mediaId) {
        if (mediaId != null) progressManager.getHistory().find { it.id == mediaId } else null
    }

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
            .background(MaterialTheme.colorScheme.background)
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
                        poster   = poster,
                        title    = title,
                        year     = year,
                        rating   = rating,
                        runtime  = movieRuntime,
                        isShow   = isShow,
                        genres   = state.movie?.genres ?: state.show?.genres ?: emptyList()
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
                                // Pass the resumed episode ID (if it's a show with history) so it launches the correct episode
                                onMainPlayClick(historyItem?.episodeId)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(6.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Default.PlayArrow, null,
                                tint = Color.Black, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (historyItem != null && historyItem.positionMs > 10000L) "Resume" else "Play",
                                color      = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                        }



                        // SECONDARY ROW — Download + My List + Share
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val movieDownloadItem = allDownloads.firstOrNull { it.mediaId == (state.movie?.id ?: "") && !it.isShow }

                            // Download Button (Netflix Style)
                            OutlinedButton(
                                onClick = {
                                    if (movieDownloadItem == null || movieDownloadItem.status == com.streamflex.domain.models.download.DownloadStatus.FAILED) {
                                        viewModel.downloadMovie()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                when {
                                    movieDownloadItem?.status == com.streamflex.domain.models.download.DownloadStatus.COMPLETED -> {
                                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Downloaded", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    movieDownloadItem?.status?.isActive == true || state.isResolvingDownload -> {
                                        CircularProgressIndicator(
                                            progress = { movieDownloadItem?.progress ?: 0f },
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val percent = movieDownloadItem?.progressPercent ?: 0
                                        Text(if (percent > 0) "Downloading ($percent%)" else "Resolving...", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    else -> {
                                        Icon(Icons.Outlined.DownloadForOffline, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            // My List toggle
                            IconButton(
                                onClick  = { 
                                    isInMyList = !isInMyList 
                                    val currentId = state.movie?.id ?: state.show?.id ?: return@IconButton
                                    if (isInMyList) {
                                        val title = state.movie?.title ?: state.show?.title ?: "Unknown"
                                        val poster = state.movie?.poster ?: state.show?.poster
                                        val isShow = state.show != null
                                        BookmarkManager.addBookmark(BookmarkItem(currentId, title, poster, isShow))
                                    } else {
                                        BookmarkManager.removeBookmark(currentId)
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isInMyList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    if (isInMyList) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    null, tint = Color.White, modifier = Modifier.size(22.dp)
                                )
                            }

                            // Share
                            IconButton(
                                onClick  = {},
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.Share, null,
                                    tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
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
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (overview.length > 120) {
                            Text(
                                text     = if (expanded) "Less ▲" else "More ▼",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { expanded = !expanded }
                            )
                        }
                    }
                }

                // ── 4. DIVIDER ────────────────────────────────────────────────
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
                
                // ── SKYSTREAM WIDGETS ──────────────────────────────────────────
                item {
                    val cast = state.movie?.cast ?: state.show?.cast ?: emptyList()
                    val trailers = state.movie?.trailers ?: state.show?.trailers ?: emptyList()
                    val studios = state.movie?.productionCompanies ?: state.show?.productionCompanies ?: emptyList()
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SFCastList(cast = cast)
                        SFTrailersCarousel(trailers = trailers)
                        SFProductionCompanies(companies = studios)
                    }
                
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
                        val epDownloadItem = allDownloads.firstOrNull { 
                            it.mediaId == (state.show?.id ?: "") && 
                            it.seasonNumber == state.selectedSeason && 
                            it.episodeNumber == ep.episodeNumber 
                        }
                        SFEpisodeItem(
                            episode = ep,
                            downloadItem = epDownloadItem,
                            onDownloadClick = {
                                viewModel.downloadEpisode(ep)
                            },
                            onClick = { 
                                onEpisodePlayClick(ep) 
                            }
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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown,
                                        null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }

                // ── 6. MORE LIKE THIS — Auto-adjust grid ──────────────────────
                if (state.similarContent.isNotEmpty()) {
                    item {
                        SFMoreLikeThis(
                            items       = state.similarContent,
                            onItemClick = { clickedId -> 
                                val clickedItem = state.similarContent.find { it.id == clickedId }
                                if (clickedItem != null) {
                                    onNavigateToDetail(clickedItem.type.name, clickedItem.id)
                                }
                            }
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
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                    .clickable { 
                        onBackClick() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
        }

        // --- DOWNLOAD STREAMS DIALOG ---
        val availableStreams = state.downloadStreamsAvailable
        if (!availableStreams.isNullOrEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDownloadDialog() },
                title = { Text("Select Download Link", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(availableStreams) { stream ->
                            val isFast = stream.url.contains("google", true) || stream.url.contains("fsl", true)
                            val isResume = stream.url.contains("google", true) || stream.url.contains("pixeldrain", true) || stream.url.contains("buzz", true)
                            
                            val tags = mutableListOf<String>()
                            if (isFast) tags.add("Fast downloading")
                            if (isResume) tags.add("Resume support")
                            if (!isFast && !isResume) tags.add("Reliable")
                            tags.add(stream.quality.name)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.startSelectedDownload(stream) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stream.name, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tags.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.cancelDownloadDialog() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO — Full-bleed backdrop with gradient + metadata chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable

@OptIn(ExperimentalLayoutApi::class)
private fun SFDetailHero(
    backdrop: String?,
    poster:   String?,
    title:    String,
    year:     Int?,
    rating:   Double?,
    runtime:  Int?,
    isShow:   Boolean,
    genres:   List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
    ) {
        // Blurred Background
        SubcomposeAsyncImage(
            model              = backdrop ?: poster,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
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
                    model              = poster,
                    contentDescription = title,
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
                    text  = title,
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
                    year?.let {
                        Text(it.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                    }
                    SFBadge("HD", SFHDTag, Color.Black)
                    if (isShow) SFBadge("SERIES", SFDubBg, Color.White)
                    runtime?.let {
                        if (it > 0) {
                            Text(formatRuntime(it), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                    }
                    rating?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, tint = SFRatingBg, modifier = Modifier.size(14.dp))
                            Text(String.format("%.1f", it), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                    }
                }
                
                if (genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genres.take(4).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(genre, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onToggleMenu() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment          = Alignment.CenterVertically,
            horizontalArrangement      = Arrangement.SpaceBetween
        ) {
            Text(
                "Season $selectedSeason",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                if (isMenuExpanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 4.dp)
            ) {
                show.seasons.forEach { season ->
                    val isSelected = season.seasonNumber == selectedSeason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeasonSelected(season.seasonNumber) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Season ${season.seasonNumber}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (season.seasonNumber != show.seasons.last().seasonNumber) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
fun SFEpisodeItem(
    episode: Episode,
    downloadItem: com.streamflex.domain.models.download.DownloadItem? = null,
    onDownloadClick: () -> Unit = {},
    onClick: () -> Unit
) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (episode.stillPath != null) {
                AsyncImage(
                    model = episode.stillPath,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = SFTextDisabled,
                    modifier = Modifier.size(28.dp)
                )
            }
            
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
                color    = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // TMDB Rating & Date Row
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
                episode.rating?.let { rating ->
                    if (rating > 0.0) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(String.format("%.1f", rating), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(8.dp))
                    }
                }
                episode.airDate?.let { date ->
                    Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Text(
                episode.overview ?: "No description available",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Download Button (Netflix Style)
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                downloadItem?.status == com.streamflex.domain.models.download.DownloadStatus.COMPLETED -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                downloadItem?.status?.isActive == true -> {
                    CircularProgressIndicator(
                        progress = { downloadItem.progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DownloadForOffline,
                            contentDescription = "Download Episode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORE LIKE THIS — Auto-adjusting responsive grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SFMoreLikeThis(
    items:       List<com.streamflex.app.domain.models.SearchResult>,
    onItemClick: (String) -> Unit
) {
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text("See All", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .width(125.dp)
                        .aspectRatio(2f/3f)
                        .clickable { onItemClick(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = item.poster,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        )
                        item.year?.let { yr ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(yr.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
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
        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
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

// ─────────────────────────────────────────────────────────────────────────────
// CAST LIST
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SFCastList(cast: List<com.streamflex.app.domain.models.CastMember>) {
    if (cast.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = "Cast",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cast.take(15)) { member ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp)
                ) {
                    AsyncImage(
                        model = member.imageUrl ?: "https://ui-avatars.com/api/?name=${member.name}&background=random",
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(90.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    member.character?.let {
                        Text(
                            text = it,
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
}

// ─────────────────────────────────────────────────────────────────────────────
// TRAILERS CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SFTrailersCarousel(trailers: List<com.streamflex.app.domain.models.Trailer>) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    if (trailers.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            text = "Trailers & Extras",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(trailers) { trailer ->
                Column(modifier = Modifier.width(220.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f/9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { uriHandler.openUri("https://www.youtube.com/watch?v=${trailer.key}") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://img.youtube.com/vi/${trailer.key}/hqdefault.jpg",
                            contentDescription = trailer.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = trailer.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRODUCTION COMPANIES
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SFProductionCompanies(companies: List<com.streamflex.app.domain.models.ProductionCompany>) {
    if (companies.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
        Text(
            text = "Studios",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(companies) { company ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        company.logoUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(24.dp).padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = company.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
