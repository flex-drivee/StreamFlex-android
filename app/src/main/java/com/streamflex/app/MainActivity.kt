package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamflex.app.data.metadata.TmdbApi
import com.streamflex.app.data.repositories.ContentRepositoryImpl
import com.streamflex.app.ui.navigation.AppNavigation
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

        setContent {
            // 3. Launch the Navigation
            // (We pass the repository down so ViewModels can use it)
            AppNavigation(repository = repository)
        }
    }
}