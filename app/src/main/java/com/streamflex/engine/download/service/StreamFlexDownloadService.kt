package com.streamflex.engine.download.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.streamflex.app.MainActivity
import com.streamflex.app.R
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadStatus

/**
 * Foreground Service that manages active download notifications and keeps the CPU/WiFi awake.
 */
class StreamFlexDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "streamflex_downloads"
        const val CHANNEL_NAME = "Downloads"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_NOTIFICATION = "ACTION_UPDATE_NOTIFICATION"

        fun startService(context: Context) {
            val intent = Intent(context, StreamFlexDownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StreamFlexDownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseLocks()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val initialNotification = buildInitialNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StreamFlex:DownloadWakeLock").apply {
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety limit
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "StreamFlex:DownloadWifiLock").apply {
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active and background download progress notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildInitialNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StreamFlex Downloader")
            .setContentText("Initializing download engine...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Updates the foreground notification with current active download stats.
     */
    fun updateDownloadProgress(activeItem: DownloadItem, totalActiveCount: Int) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (totalActiveCount > 1) {
            "Downloading (${totalActiveCount}): ${activeItem.title}"
        } else {
            "Downloading: ${activeItem.title}"
        }

        val subtitle = activeItem.subtitle ?: activeItem.quality.label

        val speedText = if (activeItem.speedBytesPerSec > 0) {
            val mb = activeItem.speedBytesPerSec.toDouble() / (1024 * 1024)
            String.format("%.1f MB/s", mb)
        } else "Connecting..."

        val etaText = if (activeItem.etaSeconds > 0) {
            val mins = activeItem.etaSeconds / 60
            " • ${mins}m left"
        } else ""

        val contentText = "${activeItem.progressPercent}% • ${activeItem.formattedDownloadedSize} / ${activeItem.formattedSize} • $speedText$etaText"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSubText(subtitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, activeItem.progressPercent, activeItem.status == DownloadStatus.CONNECTING)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        // Actions: Pause / Resume & Cancel
        if (activeItem.status == DownloadStatus.DOWNLOADING) {
            val pauseIntent = Intent(this, DownloadActionReceiver::class.java).apply {
                action = DownloadActionReceiver.ACTION_PAUSE
                putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, activeItem.id)
            }
            val pausePending = PendingIntent.getBroadcast(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else if (activeItem.status == DownloadStatus.PAUSED) {
            val resumeIntent = Intent(this, DownloadActionReceiver::class.java).apply {
                action = DownloadActionReceiver.ACTION_RESUME
                putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, activeItem.id)
            }
            val resumePending = PendingIntent.getBroadcast(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }

        val cancelIntent = Intent(this, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_CANCEL
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, activeItem.id)
        }
        val cancelPending = PendingIntent.getBroadcast(this, 3, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
