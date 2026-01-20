package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.app.data.repositories.ContentRepositoryImpl
import com.streamflex.app.ui.home.HomeScreen
import com.streamflex.app.ui.home.HomeViewModelFactory
import com.streamflex.app.ui.theme.StreamFlexTheme // Make sure this exists, or remove if not using custom theme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val tmdbApi = retrofit.create(TmdbApi::class.java)

        // 2. Setup Repository
        val repository = ContentRepositoryImpl(tmdbApi)

        // 3. Setup ViewModel Factory
        val viewModelFactory = HomeViewModelFactory(repository)

        setContent {
            // StreamFlexTheme {  // Uncomment if you have a theme setup
            // 4. Create ViewModel
            val viewModel: com.streamflex.app.ui.home.HomeViewModel = viewModel(factory = viewModelFactory)

            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    // We will implement Navigation later
                    println("Clicked on movie: $id")
                }
            )
            // }
        }
    }
}