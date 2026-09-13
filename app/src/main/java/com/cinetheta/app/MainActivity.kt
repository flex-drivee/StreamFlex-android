package com.cinetheta.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.cinetheta.app.utils.AppUpdater
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cinetheta.app.di.RepositoryModule
import com.cinetheta.app.ui.navigation.AppNavigation
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
        
        // Check for updates automatically in the background
        lifecycleScope.launch {
            AppUpdater.checkUpdate(this@MainActivity)
        }

        // Restore selected provider before UI dependencies evaluate
        val mainPrefs = getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
        val spId = mainPrefs.getString("selected_provider", null)
        com.cinetheta.app.di.ProviderModule.repository.selectedProviderId = spId

        val contentRepository = RepositoryModule.contentRepository
        
        android.widget.Toast.makeText(this, "Loaded Provider: $spId | Repo: ${contentRepository::class.java.simpleName}", android.widget.Toast.LENGTH_LONG).show()
        val streamRepository = RepositoryModule.streamRepository

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
            
            // Initialize Provider configurations from SharedPreferences
            com.cinetheta.providers.moviebox.MovieBoxConfig.savedDomain = prefs.getString("moviebox_api", null)
            
            // Re-read preference explicitly on recomposition if needed, or use a state
            var appTheme by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(prefs.getString("app_theme", "SKY_DARK") ?: "SKY_DARK")
            }
            
            // Listen for changes
            androidx.compose.runtime.DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "app_theme") {
                        appTheme = sharedPreferences.getString("app_theme", "SYSTEM") ?: "SYSTEM"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            com.cinetheta.app.ui.theme.CineThetaTheme(appTheme = appTheme) {
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