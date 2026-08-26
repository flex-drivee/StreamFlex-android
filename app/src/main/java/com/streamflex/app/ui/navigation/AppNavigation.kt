package com.streamflex.app.ui.navigation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamflex.app.domain.repository.ContentRepository
import com.streamflex.domain.repositories.StreamRepository
import com.streamflex.app.ui.home.HomeScreen
import com.streamflex.app.ui.home.HomeViewModel
import com.streamflex.app.ui.home.HomeViewModelFactory
import com.streamflex.app.ui.movies.MovieDetailScreen
import com.streamflex.app.ui.movies.MovieDetailViewModel
import com.streamflex.app.ui.movies.MovieDetailViewModelFactory
import com.streamflex.player.PlayerActivity
import com.streamflex.app.ui.search.SearchScreen
import com.streamflex.app.ui.search.SearchViewModel
import com.streamflex.app.ui.search.SearchViewModelFactory

sealed class BottomNavItem(val route: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, Icons.Outlined.Home, Icons.Filled.Home)
    object Search : BottomNavItem(Screen.Search.route, Icons.Outlined.Search, Icons.Filled.Search)
    object Explore : BottomNavItem("explore", Icons.Outlined.Explore, Icons.Filled.Explore)
    object Library : BottomNavItem("library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary)
    object Settings : BottomNavItem(Screen.Settings.route, Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun AppNavigation(
    repository: ContentRepository,
    streamRepository: StreamRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Search.route,
        "explore",
        "library",
        Screen.Settings.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HOME ---
            composable(route = Screen.Home.route) {
                val progressManager = remember { com.streamflex.player.resume.PlaybackProgressManager(context) }
                val viewModelFactory = HomeViewModelFactory(repository, progressManager)
                val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)

                HomeScreen(
                    viewModel = viewModel,
                    providerRepository = com.streamflex.app.di.ProviderModule.repository,
                    onNavigateToDetail = { type, id -> navController.navigate(Screen.Detail.createRoute(type, id)) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                    onExploreClick = { categoryId, title -> navController.navigate("see_all/$categoryId/$title") }
                )
            }
            
            // --- SETTINGS ---
            composable(route = Screen.Settings.route) {
                com.streamflex.app.ui.settings.SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) }
                )
            }

            // --- DOWNLOADS ---
            composable(route = Screen.Downloads.route) {
                com.streamflex.app.ui.downloads.DownloadsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // --- SEARCH ---
            composable(route = Screen.Search.route) {
                val viewModelFactory = SearchViewModelFactory(repository)
                val viewModel: SearchViewModel = viewModel(factory = viewModelFactory)

                SearchScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { type, id -> navController.navigate(Screen.Detail.createRoute(type, id)) },
                    onGenreClick = { id, title -> navController.navigate("see_all/$id/$title") }
                )
            }

            // --- EXPLORE ---
            composable(route = "explore") {
                val viewModelFactory = com.streamflex.app.ui.explore.ExploreViewModelFactory(repository)
                val viewModel: com.streamflex.app.ui.explore.ExploreViewModel = viewModel(factory = viewModelFactory)

                com.streamflex.app.ui.explore.ExploreScreen(
                    viewModel = viewModel,
                    onItemClick = { type, id -> navController.navigate(Screen.Detail.createRoute(type, id)) }
                )
            }

            // --- LIBRARY ---
            composable(route = "library") {
                com.streamflex.app.ui.library.LibraryScreen(
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { id -> navController.navigate(Screen.Detail.createRoute("MOVIE", id)) }
                )
            }

            // --- SEE ALL ---
            composable(
                route = "see_all/{categoryId}/{title}",
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val title = backStackEntry.arguments?.getString("title") ?: return@composable
                
                val viewModelFactory = com.streamflex.app.ui.seeall.SeeAllViewModelFactory(repository, categoryId, title)
                val viewModel: com.streamflex.app.ui.seeall.SeeAllViewModel = viewModel(factory = viewModelFactory)

                com.streamflex.app.ui.seeall.SeeAllScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { type, id -> navController.navigate(Screen.Detail.createRoute(type, id)) }
                )
            }

            // --- DETAIL ---
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("movieId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val contentType = backStackEntry.arguments?.getString("type") ?: return@composable
                val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
                val viewModelFactory = MovieDetailViewModelFactory(
                    contentRepository = repository,
                    streamRepository = streamRepository,
                    contentId = movieId,
                    contentType = contentType
                )
                val viewModel: MovieDetailViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsState()

                MovieDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onMoviePlayClick = {
                        val intent = Intent(context, com.streamflex.player.PlayerActivity::class.java).apply {
                            putExtra("MEDIA_ID", movieId)
                            putExtra("VIDEO_TITLE", uiState.movie?.title ?: "Unknown Media")
                            putExtra("VIDEO_YEAR", uiState.movie?.year ?: 0)
                            putExtra("IS_SHOW", false)
                            putExtra("POSTER_PATH", uiState.movie?.poster)
                        }
                        context.startActivity(intent)
                    },
                    onEpisodePlayClick = { episode ->
                        val intent = Intent(context, com.streamflex.player.PlayerActivity::class.java).apply {
                            val epIds = ArrayList(uiState.episodes.map { it.id })
                            val epTitles = ArrayList(uiState.episodes.map { it.title })
                            val epSeasons = ArrayList(uiState.episodes.map { uiState.selectedSeason })
                            val epNumbers = ArrayList(uiState.episodes.map { it.episodeNumber })
                            val epStills = ArrayList(uiState.episodes.map { it.stillPath ?: "" })
                            
                            putStringArrayListExtra("EPISODE_IDS", epIds)
                            putStringArrayListExtra("EPISODE_TITLES", epTitles)
                            putIntegerArrayListExtra("EPISODE_SEASONS", epSeasons)
                            putIntegerArrayListExtra("EPISODE_NUMBERS", epNumbers)
                            putStringArrayListExtra("EPISODE_STILLS", epStills)
                            
                            putExtra("MEDIA_ID", movieId)
                            putExtra("VIDEO_TITLE", uiState.show?.title ?: "Unknown Media")
                            putExtra("VIDEO_YEAR", uiState.show?.year ?: 0)
                            putExtra("IS_SHOW", true)
                            putExtra("POSTER_PATH", uiState.show?.poster)
                            
                            putExtra("CURRENT_EPISODE_ID", episode.id)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Floating Bottom Navigation Bar
        if (showBottomBar) {
            val items = listOf(
                BottomNavItem.Home,
                BottomNavItem.Search,
                BottomNavItem.Explore,
                BottomNavItem.Library,
                BottomNavItem.Settings
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}