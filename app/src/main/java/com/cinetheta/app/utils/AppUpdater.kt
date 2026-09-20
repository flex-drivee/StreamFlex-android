package com.cinetheta.app.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentDialog
import androidx.core.content.FileProvider
import com.cinetheta.app.BuildConfig
import com.cinetheta.app.ui.theme.CineThetaTheme
import com.cinetheta.core.utils.StreamLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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

    private const val GITHUB_OWNER = "cinetheta"
    private const val GITHUB_REPO = "cinetheta.github.io"
    private const val GITHUB_REPO_ALT = "cinetheta"

    // Query /releases instead of /releases/latest so pre-releases and beta tags are properly found
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val RELEASES_URL_ALT = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO_ALT/releases"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    /**
     * Checks for updates and shows a modern dialog if one is available.
     * @param isManualCheck If true (e.g. from Settings), notifies the user even if up to date or if error occurs.
     */
    suspend fun checkUpdate(context: Context, isManualCheck: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            var request = Request.Builder()
                .url(RELEASES_URL)
                .header("User-Agent", "CineTheta-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            var response = client.newCall(request).execute()

            // If cinetheta.github.io returns 404 or fails, fallback to checking cinetheta repo
            if (!response.isSuccessful || response.code == 404) {
                response.close()
                request = Request.Builder()
                    .url(RELEASES_URL_ALT)
                    .header("User-Agent", "CineTheta-App")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                response = client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No updates found or repository not yet published (${response.code})", Toast.LENGTH_SHORT).show()
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

            val latestVersion = release.tag_name
            val currentVersion = BuildConfig.VERSION_NAME

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
            StreamLogger.error("AppUpdater", "Error checking for updates: ${e.message}")
            if (isManualCheck) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not check for updates. Check internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Semantic version comparison supporting version numbers and pre-release modifiers (alpha, beta, rc).
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        if (current.equals(latest, ignoreCase = true)) return false

        val currClean = current.removePrefix("v").trim()
        val latestClean = latest.removePrefix("v").trim()
        if (currClean.equals(latestClean, ignoreCase = true)) return false

        // Extract numeric sequence (e.g. "1.0.1-beta" -> [1, 0, 1])
        val currParts = currClean.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        val latestParts = latestClean.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        // When base numeric versions match (e.g. 1.0.2-alpha vs 1.0.2-beta or 1.0.2)
        fun preReleaseRank(v: String): Int {
            val lower = v.lowercase()
            return when {
                lower.contains("alpha") -> 1
                lower.contains("beta") -> 2
                lower.contains("rc") -> 3
                !lower.contains("-") && !lower.contains("pre") -> 4
                else -> 0
            }
        }

        val currRank = preReleaseRank(currClean)
        val latestRank = preReleaseRank(latestClean)
        return latestRank > currRank
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun showUpToDateDialog(context: Context) {
        val activity = findActivity(context)
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) return

        val dialog = ComponentDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CineThetaTheme {
                    UpToDateDialogContent(
                        currentVersion = BuildConfig.VERSION_NAME,
                        onDismiss = { dialog.dismiss() }
                    )
                }
            }
        }

        dialog.setContentView(composeView)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.7f)
        }

        dialog.show()
    }

    private fun showUpdateDialog(context: Context, version: String, changelog: String, downloadUrl: String) {
        val activity = findActivity(context)
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) return

        val dialog = ComponentDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        var downloadJob: Job? = null

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CineThetaTheme {
                    UpdateDialogContent(
                        currentVersion = BuildConfig.VERSION_NAME,
                        newVersion = version,
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        onDismiss = {
                            downloadJob?.cancel()
                            dialog.dismiss()
                        },
                        onStartDownload = { onProgress, onComplete, onError ->
                            downloadJob?.cancel()
                            downloadJob = downloadAndInstallUpdate(
                                context = context,
                                url = downloadUrl,
                                onProgress = onProgress,
                                onComplete = {
                                    dialog.dismiss()
                                    onComplete()
                                },
                                onError = onError
                            )
                        },
                        onOpenBrowser = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                dialog.dismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        dialog.setContentView(composeView)

        dialog.setOnDismissListener {
            downloadJob?.cancel()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.7f)
        }

        dialog.show()
    }

    private fun downloadAndInstallUpdate(
        context: Context,
        url: String,
        onProgress: (Float, String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful || response.body == null) {
                    withContext(Dispatchers.Main) {
                        onError("Download failed (HTTP ${response.code})")
                    }
                    return@launch
                }

                val body = response.body!!
                val contentLength = body.contentLength()
                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.source().use { source ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var totalBytesRead = 0L
                        var read: Int
                        var lastProgressUpdate = 0L
                        val inputStream = source.inputStream()

                        while (inputStream.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            totalBytesRead += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 120 || (contentLength > 0 && totalBytesRead == contentLength)) {
                                lastProgressUpdate = now
                                val progress = if (contentLength > 0) {
                                    totalBytesRead.toFloat() / contentLength.toFloat()
                                } else {
                                    -1f
                                }
                                val mbRead = totalBytesRead / (1024f * 1024f)
                                val mbTotal = if (contentLength > 0) contentLength / (1024f * 1024f) else 0f
                                val progressText = if (contentLength > 0) {
                                    String.format(java.util.Locale.US, "%.1f MB / %.1f MB (%.0f%%)", mbRead, mbTotal, progress * 100)
                                } else {
                                    String.format(java.util.Locale.US, "%.1f MB downloaded", mbRead)
                                }
                                withContext(Dispatchers.Main) {
                                    onProgress(progress, progressText)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete()
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                StreamLogger.error("AppUpdater", "Error downloading update: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to download update")
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
            StreamLogger.error("AppUpdater", "Failed to install APK: ${e.message}")
            Toast.makeText(context, "Failed to launch installer", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun UpdateDialogContent(
    currentVersion: String,
    newVersion: String,
    changelog: String,
    downloadUrl: String,
    onDismiss: () -> Unit,
    onStartDownload: (onProgress: (Float, String) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) -> Unit,
    onOpenBrowser: () -> Unit
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }
    var downloadStatusText by remember { mutableStateOf("Preparing download...") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val isApk = downloadUrl.endsWith(".apk", ignoreCase = true)

    fun beginDownload() {
        if (!isApk) {
            onOpenBrowser()
            return
        }
        isDownloading = true
        downloadError = null
        downloadProgress = -1f
        downloadStatusText = "Connecting..."
        onStartDownload(
            { progress, text ->
                downloadProgress = progress
                downloadStatusText = text
            },
            {
                isDownloading = false
            },
            { error ->
                isDownloading = false
                downloadError = error
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF161622),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00D2FF).copy(alpha = 0.55f),
                        Color(0xFF0072FF).copy(alpha = 0.30f),
                        Color(0x1FFFFFFF)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Icon Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00D2FF).copy(alpha = 0.25f),
                                    Color(0xFF0072FF).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = "New Update Available!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Version Transition Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    // Current version chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF222230),
                        border = BorderStroke(1.dp, Color(0xFF333346))
                    ) {
                        Text(
                            text = "v${currentVersion.removePrefix("v")}",
                            color = Color(0xFF9E9EAA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF00D2FF),
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // New version chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0088FF).copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = "v${newVersion.removePrefix("v")}",
                            color = Color(0xFF00F0FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0E0E16))
                        .border(1.dp, Color(0xFF262638), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WHAT'S NEW",
                            color = Color(0xFF00D2FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 130.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = changelog.trim().ifBlank { "Performance improvements, bug fixes, and enhanced playback stability." },
                            color = Color(0xFFD4D4E0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Download Progress / Error Section
                AnimatedVisibility(visible = isDownloading || downloadError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0E0E16))
                            .border(1.dp, if (downloadError != null) Color(0xFFFF5252).copy(alpha = 0.5f) else Color(0xFF262638), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (downloadError != null) {
                            Text(
                                text = downloadError ?: "Download failed",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (downloadProgress >= 0f) "Downloading update..." else "Starting download...",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = downloadStatusText,
                                    color = Color(0xFF00D2FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (downloadProgress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00D2FF),
                                    trackColor = Color(0xFF262638)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00D2FF),
                                    trackColor = Color(0xFF262638)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons Row
                if (isDownloading) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel Download",
                            color = Color(0xFF9E9EAA),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                } else if (downloadError != null) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF333346)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0B0C0))
                        ) {
                            Text(
                                text = "Close",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { beginDownload() },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088FF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF333346)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0B0C0))
                        ) {
                            Text(
                                text = "Later",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { beginDownload() },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0088FF)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isApk) "Update Now" else "Open Release",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpToDateDialogContent(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF161622),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.50f),
                        Color(0xFF00B0FF).copy(alpha = 0.25f),
                        Color(0x1FFFFFFF)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Emerald Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0.25f),
                                    Color(0xFF00B0FF).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Up to Date",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "You're All Up to Date!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.50f))
                ) {
                    Text(
                        text = "v${currentVersion.removePrefix("v")} (Latest)",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You have the latest version of CineTheta installed. No new updates are available at this time.",
                    fontSize = 13.sp,
                    color = Color(0xFFB0B0C0),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0088FF)
                    )
                ) {
                    Text(
                        text = "Great!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
