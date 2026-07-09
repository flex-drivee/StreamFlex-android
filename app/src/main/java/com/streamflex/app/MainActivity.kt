package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.app.data.repositories.ContentRepositoryImpl
import com.streamflex.app.ui.navigation.AppNavigation
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        val retrofit = Retrofit.Builder()

            .baseUrl("https://api.themoviedb.org/3/")

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

        val tmdbApi = retrofit.create(
            TmdbApi::class.java
        )

        val repository =
            ContentRepositoryImpl(tmdbApi)

        setContent {

            AppNavigation(
                repository = repository
            )

        }
    }
}