package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamflex.app.ui.detail.DetailScreen
import com.streamflex.app.ui.home.HomeScreen
import com.streamflex.app.ui.theme.StreamFlexTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamFlexTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // 1. Home Screen
        composable("home") {
            HomeScreen(onMovieClick = { movie ->
                // Encode URL to pass it safely (slashes break navigation otherwise)
                val encodedUrl = URLEncoder.encode(movie.url, StandardCharsets.UTF_8.toString())
                navController.navigate("detail/$encodedUrl")
            })
        }

        // 2. Detail Screen
        composable(
            route = "detail/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            DetailScreen(url = url)
        }
    }
}