import re

with open('/sdcard/AndroidIDEProjects/StreamFlex-android/app/src/main/java/com/cinetheta/app/ui/pluginsearch/PluginDetailScreen.kt', 'r') as f:
    content = f.read()

hero_section_replacement = """
                        // Header
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 340.dp)
                            ) {
                                // Blurred Background
                                coil.compose.SubcomposeAsyncImage(
                                    model = result.poster,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .matchParentSize()
                                        .androidx.compose.ui.draw.blur(radiusX = 15.dp, radiusY = 15.dp)
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
                                        coil.compose.SubcomposeAsyncImage(
                                            model              = result.poster,
                                            contentDescription = result.title,
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
                                            text  = result.title,
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
                                            if (result.year != null && result.year != 0) {
                                                Text(result.year.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
"""

old_hero_section = """                        // Header
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                if (!result.poster.isNullOrBlank()) {
                                    AsyncImage(
                                        model = result.poster,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                                    startY = 100f
                                                )
                                            )
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (result.year != null) {
                                        Text(text = result.year.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                    }
                                }
                            }
                        }"""

content = content.replace(old_hero_section, hero_section_replacement.strip())

movie_play_replacement = """                            // Movie
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Button(
                                        onClick = { onPlayClick(result.sources, null) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play Movie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }"""

old_movie_play = """                            // Movie
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Button(
                                        onClick = { onPlayClick(result.sources, null) },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play Movie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }"""

content = content.replace(old_movie_play, movie_play_replacement.strip())

tv_episode_replacement = """                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clickable { onPlayClick(episode.sources, episode) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                                if (!episode.thumbnail.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = episode.thumbnail,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Play",
                                                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                                                    tint = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${episode.number}. ${episode.title}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "45m", // Placeholder for duration
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }"""

old_tv_episode = """                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clickable { onPlayClick(episode.sources, episode) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!episode.thumbnail.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = episode.thumbnail,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(80.dp, 45.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${episode.number}. ${episode.title}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }"""

content = content.replace(old_tv_episode, tv_episode_replacement.strip())

with open('/sdcard/AndroidIDEProjects/StreamFlex-android/app/src/main/java/com/cinetheta/app/ui/pluginsearch/PluginDetailScreen.kt', 'w') as f:
    f.write(content)
