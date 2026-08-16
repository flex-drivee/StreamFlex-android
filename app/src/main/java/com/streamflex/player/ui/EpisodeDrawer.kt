package com.streamflex.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamflex.player.episodes.PlayerEpisode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDrawer(
    episodes: List<PlayerEpisode>,
    currentEpisode: PlayerEpisode?,
    onEpisodeClick: (PlayerEpisode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Episodes",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            Divider(color = Color.DarkGray)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                items(episodes) { episode ->
                    val isCurrent = episode.id == currentEpisode?.id
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (!isCurrent) onEpisodeClick(episode) 
                            }
                            .background(if (isCurrent) Color.DarkGray.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${episode.episodeNumber}.",
                            color = if (isCurrent) Color.White else Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width(32.dp)
                        )
                        
                        Text(
                            text = episode.title,
                            color = if (isCurrent) Color.White else Color.LightGray,
                            fontSize = 16.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isCurrent) {
                            Text(
                                text = "Playing",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
