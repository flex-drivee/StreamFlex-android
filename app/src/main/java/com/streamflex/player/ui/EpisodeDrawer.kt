package com.streamflex.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflex.player.episodes.PlayerEpisode

@Composable
fun EpisodeDrawer(
    isVisible: Boolean,
    episodes: List<PlayerEpisode>,
    currentEpisode: PlayerEpisode?,
    onEpisodeClick: (PlayerEpisode) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + slideInHorizontally(
            initialOffsetX = { -it }, 
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it }, 
            animationSpec = tween(300)
        ) + fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dismiss background area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                        indication = null,
                        onClick = onDismiss
                    )
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            
            // Drawer Panel on the Left
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Color(0xFF161616))
            ) {
                Text(
                    text = "Episodes",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                )
                
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(episodes) { episode ->
                        val isCurrent = episode.id == currentEpisode?.id
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (!isCurrent) onEpisodeClick(episode) 
                                }
                                .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bigger Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 130.dp, height = 74.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!episode.stillPath.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = episode.stillPath,
                                        contentDescription = episode.title,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "Ep ${episode.episodeNumber}",
                                        color = Color.Gray,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)), 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Playing", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${episode.episodeNumber}. ${episode.title}",
                                    color = if (isCurrent) Color.White else Color.LightGray,
                                    fontSize = 16.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
