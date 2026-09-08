import kotlinx.coroutines.launch
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

package com.cinetheta.app.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.cinetheta.app.BuildConfig
import com.cinetheta.core.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

@Serializable
data class GithubRelease(
    val tag_name: String,
    val body: String,
    val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    val name: String,
    val browser_download_url: String
)

object AppUpdater {
    
    // TODO: Replace with your actual GitHub username and repository name
    private const val GITHUB_OWNER = "YourGithubUsername"
    private const val GITHUB_REPO = "CineTheta"
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Checks for updates and shows a dialog if one is available.
     * Call this in your MainActivity's onCreate (wrapped in a coroutine).
     */
    suspend fun checkUpdate(context: Context) = withContext(Dispatchers.IO) {
        try {
            if (GITHUB_OWNER == "YourGithubUsername") {
                com.cinetheta.core.utils.StreamLogger.error("AppUpdater", "Please configure GITHUB_OWNER and GITHUB_REPO in AppUpdater.kt")
                return@withContext
            }

            val request = Request.Builder().url(RELEASES_URL).build()
            val response = HttpClient.client.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext
            
            val responseBody = response.body?.string() ?: return@withContext
            val release = json.decodeFromString<GithubRelease>(responseBody)
            
            // Assuming tag_name is something like "v1.0.2" and BuildConfig.VERSION_NAME is "1.0.1"
            val latestVersion = release.tag_name.removePrefix("v").replace("-", ".")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").replace("-", ".")
            
            if (isNewerVersion(currentVersion, latestVersion)) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: return@withContext
                
                withContext(Dispatchers.Main) {
                    showUpdateDialog(context, release.tag_name, release.body, apkAsset.browser_download_url)
                }
            }
        } catch (e: Exception) {
            com.cinetheta.core.utils.StreamLogger.error("AppUpdater", "Error checking for updates: ${e.message}")
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
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

    private fun downloadAndInstallUpdate(context: Context, url: String) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = HttpClient.client.newCall(request).execute()
                
                if (!response.isSuccessful || response.body == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to download update.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Save to cache directory so FileProvider can access it
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
