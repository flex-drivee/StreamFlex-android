package com.cinetheta.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object PluginSearch : Screen("plugin_search")
    object MyList : Screen("mylist")
    object Settings : Screen("settings")
    object Downloads : Screen("downloads")

    object Detail : Screen("detail/{type}/{movieId}") {
        fun createRoute(type: String, movieId: String) = "detail/$type/$movieId"
    }

    object PluginDetail : Screen("plugin_detail")
}
