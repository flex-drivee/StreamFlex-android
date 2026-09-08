package com.cinetheta.engine.download.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cinetheta.app.di.EngineModule

/**
 * Handles action buttons tapped from the download foreground notification.
 */
class DownloadActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PAUSE = "com.cinetheta.download.ACTION_PAUSE"
        const val ACTION_RESUME = "com.cinetheta.download.ACTION_RESUME"
        const val ACTION_CANCEL = "com.cinetheta.download.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return

        val queueManager = EngineModule.downloadQueueManager

        when (action) {
            ACTION_PAUSE -> {
                queueManager.pauseDownload(downloadId)
            }
            ACTION_RESUME -> {
                queueManager.resumeDownload(downloadId)
            }
            ACTION_CANCEL -> {
                queueManager.cancelDownload(downloadId)
            }
        }
    }
}
