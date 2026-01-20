package com.streamflex.app.ui.navigation

sealed class Screen(val route: String) {
    // Route for Home
    object Home : Screen("home")

    // Route for Detail: "detail/{movieId}"
    object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
}