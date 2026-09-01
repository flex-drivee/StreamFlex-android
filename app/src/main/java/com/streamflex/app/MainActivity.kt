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

        // Restore selected provider before UI dependencies evaluate
        val mainPrefs = getSharedPreferences("streamflex_settings", android.content.Context.MODE_PRIVATE)
        val spId = mainPrefs.getString("selected_provider", null)
        com.streamflex.app.di.ProviderModule.repository.selectedProviderId = spId

        val contentRepository = RepositoryModule.contentRepository
        
        android.util.Log.d("MainActivity", "Selected Provider ID from Prefs: $spId")
        android.util.Log.d("MainActivity", "Content Repository Class: ${contentRepository::class.java.simpleName}")
        val streamRepository = RepositoryModule.streamRepository

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = context.getSharedPreferences("streamflex_settings", android.content.Context.MODE_PRIVATE)
            
            // Initialize Provider configurations from SharedPreferences
            com.streamflex.providers.moviebox.MovieBoxConfig.savedDomain = prefs.getString("moviebox_api", null)
            
            // Re-read preference explicitly on recomposition if needed, or use a state
            var appTheme by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(prefs.getString("app_theme", "SKY_DARK") ?: "SKY_DARK")
            }
            
            // Listen for changes
            androidx.compose.runtime.DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "app_theme") {
                        appTheme = sharedPreferences.getString("app_theme", "SKY_DARK") ?: "SKY_DARK"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            com.streamflex.app.ui.theme.StreamFlexTheme(appTheme = appTheme) {
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