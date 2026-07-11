package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamflex.app.di.RepositoryModule
import com.streamflex.app.ui.navigation.AppNavigation


/**
 * Main entry point of the application.
 *
 * Currently the UI still uses the legacy TMDB repository.
 * The new backend (ProviderRepository + StreamEngine)
 * will be integrated screen-by-screen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val contentRepository =
            RepositoryModule.contentRepository

        val streamRepository =
            RepositoryModule.streamRepository

        setContent {

            AppNavigation(

                repository = contentRepository,

                streamRepository = streamRepository

            )

        }
    }
}