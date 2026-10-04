// language: Kotlin, file: ServiceWatchdog.kt
// *ملف جديد — يراقب BackgroundService ويعيد إطلاقه لو مات*
// *يستخدم AlarmManager كـ backup مستقل عن العملية نفسها*
// *setExactAndAllowWhileIdle يعمل حتى في Doze mode*

package com.sys.update2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat

object ServiceWatchdog {

    private const val ALARM_REQUEST_CODE = 77001
    private const val INTERVAL_MS = 15 * 60 * 1000L // 15 دقيقة

    /**
     * يجدول تشغيل الـ watchdog بعد 15 دقيقة.
     * كل مرة الـ BroadcastReceiver يستقبل، يجدول مرة تانية.
     */
    fun schedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).apply {
                action = "com.sys.update2.WATCHDOG"
            }

            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val pi = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                flags
            )

            val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                } catch (_: SecurityException) {
                    // لا يوجد exact alarm permission — نستخدم inexact
                    am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                }
            } else {
                am.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pi
                )
            }
        } catch (_: Exception) {}
    }

    fun cancel(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).apply {
                action = "com.sys.update2.WATCHDOG"
            }

            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val pi = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                flags
            )
            am.cancel(pi)
        } catch (_: Exception) {}
    }
}

/**
 * BroadcastReceiver يعمل كل 15 دقيقة:
 * 1. يتأكد إن BackgroundService شغال
 * 2. لو مش شغال → يعيد إطلاقه
 * 3. يجدول نفسه مرة تانية
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            // 1) أعِد إطلاق الخدمة لو مش شغالة
            BackgroundService.startSafely(context)

            // 2) شغّل SyncWorker كاحتياط
            try { SyncWorker.start(context) } catch (_: Exception) {}

            // 3) جدول مرة تانية
            ServiceWatchdog.schedule(context)
        } catch (_: Exception) {
            // حتى لو فشل، حاول تاني
            try { ServiceWatchdog.schedule(context) } catch (_: Exception) {}
        }
    }
}
