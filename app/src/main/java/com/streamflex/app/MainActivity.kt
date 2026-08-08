package com.streamflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamflex.app.di.RepositoryModule
import com.streamflex.app.ui.navigation.AppNavigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize


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

        val contentRepository = RepositoryModule.contentRepository
        val streamRepository = RepositoryModule.streamRepository

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = context.getSharedPreferences("streamflex_settings", android.content.Context.MODE_PRIVATE)
            
            // Re-read preference explicitly on recomposition if needed, or use a state
            var isDarkMode by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(prefs.getBoolean("dark_mode", true))
            }
            
            // Listen for changes
            androidx.compose.runtime.DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "dark_mode") {
                        isDarkMode = sharedPreferences.getBoolean("dark_mode", true)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            com.streamflex.app.ui.theme.StreamFlexTheme(darkTheme = isDarkMode) {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        repository = contentRepository,
                        streamRepository = streamRepository
                    )
                }
            }
        }
    }
}