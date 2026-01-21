package com.streamflex.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object MyList : Screen("mylist")

    object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }

    // We don't need a route for Player if we use a separate Activity,
    // but if we use a Composable Player, we would add it here.
    // For this guide, we will use the PlayerActivity you already have structure for.
}