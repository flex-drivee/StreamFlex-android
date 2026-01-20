package com.streamflex.app.ui.navigation

import androidx.compose.runtime.Composable
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

@Composable
fun AppNavigation(
    repository: ContentRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // --- HOME SCREEN ---
        composable(route = Screen.Home.route) {
            val viewModelFactory = HomeViewModelFactory(repository)
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)

            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { movieId ->
                    // Navigate to detail screen passing the ID
                    navController.navigate(Screen.Detail.createRoute(movieId))
                }
            )
        }

        // --- DETAIL SCREEN ---
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extract the movieId from the URL
            val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable

            val viewModelFactory = MovieDetailViewModelFactory(repository, movieId)
            val viewModel: MovieDetailViewModel = viewModel(factory = viewModelFactory)

            MovieDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }, // Go back
                onPlayClick = { id ->
                    // Placeholder: We will add Player navigation later
                    println("Play requested for: $id")
                }
            )
        }
    }
}