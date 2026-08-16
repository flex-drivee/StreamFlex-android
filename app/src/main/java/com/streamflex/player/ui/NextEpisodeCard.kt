package com.streamflex.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
fun NextEpisodeCard(
    episode: PlayerEpisode,
    countdown: Int,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Next Episode in $countdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "S${episode.seasonNumber.toString().padStart(2, '0')} E${episode.episodeNumber.toString().padStart(2, '0')}",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            
            Text(
                text = episode.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onPlayNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Next", fontWeight = FontWeight.Bold)
            }
        }
    }
}
