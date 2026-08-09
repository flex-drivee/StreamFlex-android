package com.streamflex.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.streamflex.app.ui.downloads.DownloadsScreen
import com.streamflex.app.ui.mylist.MyListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Downloads", "Bookmarks")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Library", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (selectedTab == 0) {
                // We wrap DownloadsScreen to suppress its own TopAppBar by not passing a back click, 
                // but since DownloadsScreen has a hardcoded Scaffold, it will draw a nested TopAppBar.
                // For a seamless look, we should ideally refactor DownloadsScreen, 
                // but for now we just render it. It will have a double header until refactored.
                // We'll refactor it immediately after.
                DownloadsScreen(onBackClick = {})
            } else {
                MyListScreen(onBackClick = {}, onItemClick = onItemClick)
            }
        }
    }
}
