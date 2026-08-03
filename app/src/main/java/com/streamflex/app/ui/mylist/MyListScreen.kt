package com.streamflex.app.ui.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamflex.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    Scaffold(
        containerColor = SFBgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My List",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SFTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SFTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = SFBgPrimary,
                    scrolledContainerColor = SFBgSurface
                )
            )
        }
    ) { padding ->
        // Empty state — Phase 3 will wire real saved list
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SFBgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint     = SFTextDisabled,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your list is empty",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SFTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add movies and shows to watch later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SFTextSecondary
                )
            }
        }
    }
}