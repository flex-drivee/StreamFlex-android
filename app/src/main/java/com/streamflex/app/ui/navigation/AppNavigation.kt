package com.streamflex.app.ui.navigation

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.streamflex.domain.repositories.StreamRepository
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
    repository: ContentRepository,
    streamRepository: StreamRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        // --- SETTINGS ---
        composable(route = Screen.Settings.route) {
            com.streamflex.app.ui.settings.SettingsScreen(
                onBackClick = { navController.popBackStack() }
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
            val viewModelFactory =
                MovieDetailViewModelFactory(

                    contentRepository = repository,

                    streamRepository = streamRepository,

                    contentId = movieId

                )
            val viewModel: MovieDetailViewModel = viewModel(factory = viewModelFactory)

            MovieDetailScreen(

                viewModel = viewModel,

                onBackClick = {

                    navController.popBackStack()

                },

                onMoviePlayClick = { links ->

                    android.util.Log.d(
                        "STREAM_DEBUG",
                        "Movie URLs: ${links.map { it.url }}"
                    )

                    val intent = Intent(
                        context,
                        PlayerActivity::class.java
                    ).apply {
                        val urls = ArrayList(links.map { it.url })
                        val referers = ArrayList(links.map { it.headers["Referer"] ?: it.referer ?: "" })
                        val cookies = ArrayList(links.map { it.headers["Cookie"] ?: it.cookies.entries.joinToString("; ") { c -> "${c.key}=${c.value}" } })
                        val userAgents = ArrayList(links.map { it.headers["User-Agent"] ?: "" })
                        putStringArrayListExtra("VIDEO_URLS", urls)
                        putStringArrayListExtra("VIDEO_REFERERS", referers)
                        putStringArrayListExtra("VIDEO_COOKIES", cookies)
                        putStringArrayListExtra("VIDEO_USER_AGENTS", userAgents)
                    }

                    context.startActivity(intent)

                },

                onEpisodePlayClick = { episode ->

                    viewModel.fetchEpisodeStreams(
                        episode
                    ) { links ->

                        android.util.Log.d(
                            "STREAM_DEBUG",
                            "Episode URLs: ${links.map { it.url }}"
                        )

                        val intent = Intent(
                            context,
                            PlayerActivity::class.java
                        ).apply {
                            val urls = ArrayList(links.map { it.url })
                            val referers = ArrayList(links.map { it.headers["Referer"] ?: it.referer ?: "" })
                            val cookies = ArrayList(links.map { it.headers["Cookie"] ?: it.cookies.entries.joinToString("; ") { c -> "${c.key}=${c.value}" } })
                            val userAgents = ArrayList(links.map { it.headers["User-Agent"] ?: "" })
                            putStringArrayListExtra("VIDEO_URLS", urls)
                            putStringArrayListExtra("VIDEO_REFERERS", referers)
                            putStringArrayListExtra("VIDEO_COOKIES", cookies)
                            putStringArrayListExtra("VIDEO_USER_AGENTS", userAgents)
                        }

                        context.startActivity(intent)

                    }

                }

            )
        }
    }
}