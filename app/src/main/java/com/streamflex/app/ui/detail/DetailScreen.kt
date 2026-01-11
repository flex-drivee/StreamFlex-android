package com.streamflex.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.streamflex.app.ui.player.PlayerActivity

@Composable
fun DetailScreen(url: String) {
    val viewModel: DetailViewModel = viewModel()
    val details by viewModel.details.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Load data when screen opens
    LaunchedEffect(url) {
        viewModel.loadDetails(url)
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    details?.let { movie ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 1. Poster Image
            item {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = movie.title, fontSize = 24.sp, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = movie.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Links / Episodes:", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                }
            }

            // 2. List of Play Buttons
            items(movie.episodes) { episode ->
                Button(
                    onClick = {
                        Toast.makeText(context, "Extracting link...", Toast.LENGTH_SHORT).show()
                        viewModel.loadStream(episode.url) { links ->
                            if (links.isNotEmpty()) {
                                // ⚡ NEW LOGIC: Open Internal Player
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("VIDEO_URL", links.first().url)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No link found!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(text = episode.name)
                }
            }
        }
    }
}