package com.cinetheta.app.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.widget.Toast
import com.cinetheta.app.BuildConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

@Serializable
data class GithubRelease(
    val tag_name: String,
    val body: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    val name: String,
    val browser_download_url: String
)

object AppUpdater {
    
    private const val GITHUB_OWNER = "flex-drivee"
    private const val GITHUB_REPO = "StreamFlex-android"
    // Query /releases instead of /releases/latest so pre-releases and beta tags are properly found
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    /**
     * Checks for updates and shows a dialog if one is available.
     * @param isManualCheck If true (e.g. from Settings), notifies the user even if up to date or if error occurs.
     */
    suspend fun checkUpdate(context: Context, isManualCheck: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("User-Agent", "CineTheta-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Unable to check for updates (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
                return@withContext
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        showUpToDateDialog(context)
                    }
                }
                return@withContext
            }

            val releases = json.decodeFromString<List<GithubRelease>>(responseBody)
            val release = releases.firstOrNull { !it.draft }
            
            if (release == null) {
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        showUpToDateDialog(context)
                    }
                }
                return@withContext
            }
            
            val latestVersion = release.tag_name.removePrefix("v").replace("-", ".")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").replace("-", ".")
            
            if (isNewerVersion(currentVersion, latestVersion)) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
                val downloadUrl = apkAsset?.browser_download_url ?: "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
                
                withContext(Dispatchers.Main) {
                    showUpdateDialog(context, release.tag_name, release.body ?: "New release available!", downloadUrl)
                }
            } else {
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        showUpToDateDialog(context)
                    }
                }
            }
        } catch (e: Exception) {
            com.cinetheta.core.utils.StreamLogger.error("AppUpdater", "Error checking for updates: ${e.message}")
            if (isManualCheck) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not check for updates. Check internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        // Clean up any extra tags (e.g. "1.0.beta" -> [1, 0])
        val currParts = current.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun showUpToDateDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("CineTheta is Up to Date")
            .setMessage("You have the latest version installed (v${BuildConfig.VERSION_NAME}).")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showUpdateDialog(context: Context, version: String, changelog: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("New Update Available ($version)")
            .setMessage(changelog.take(500) + if (changelog.length > 500) "..." else "")
            .setPositiveButton("Update") { _, _ ->
                Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
                downloadAndInstallUpdate(context, downloadUrl)
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun downloadAndInstallUpdate(context: Context, url: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful || response.body == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to download update.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()
                
                val sink = apkFile.sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()
                
                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error downloading update.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            com.cinetheta.core.utils.StreamLogger.error("AppUpdater", "Failed to install APK: ${e.message}")
            Toast.makeText(context, "Failed to launch installer", Toast.LENGTH_SHORT).show()
        }
    }
}
