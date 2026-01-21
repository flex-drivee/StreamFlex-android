package com.streamflex.app.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamflex.app.domain.repository.ContentRepository
import com.streamflex.app.ui.home.HomeScreen
import com.streamflex.app.ui.home.HomeViewModel
import com.streamflex.app.ui.home.HomeViewModelFactory
import com.streamflex.app.ui.movies.MovieDetailScreen
import com.streamflex.app.ui.movies.MovieDetailViewModel
import com.streamflex.app.ui.movies.MovieDetailViewModelFactory
import com.streamflex.app.ui.mylist.MyListScreen
import com.streamflex.app.ui.player.PlayerActivity
import com.streamflex.app.ui.search.SearchScreen
import com.streamflex.app.ui.search.SearchViewModel
import com.streamflex.app.ui.search.SearchViewModelFactory

@Composable
fun AppNavigation(
    repository: ContentRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // --- HOME ---
        composable(route = Screen.Home.route) {
            val viewModelFactory = HomeViewModelFactory(repository)
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)

            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onSettingsClick = { /* Navigate to Settings */ }
            )
        }

        // --- SEARCH ---
        composable(route = Screen.Search.route) {
            val viewModelFactory = SearchViewModelFactory(repository)
            val viewModel: SearchViewModel = viewModel(factory = viewModelFactory)

            SearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onItemClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
            )
        }

        // --- MY LIST (Placeholder for now) ---
        composable(route = Screen.MyList.route) {
            MyListScreen(
                onBackClick = { navController.popBackStack() },
                onItemClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
            )
        }

        // --- DETAIL ---
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
            val viewModelFactory = MovieDetailViewModelFactory(repository, movieId)
            val viewModel: MovieDetailViewModel = viewModel(factory = viewModelFactory)

            MovieDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { videoUrl ->
                    // Launch Player Activity
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("VIDEO_URL", videoUrl) // Pass the URL to player
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}