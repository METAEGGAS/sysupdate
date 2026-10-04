// language: Kotlin, file: BootReceiver.kt
// *يستقبل BOOT_COMPLETED و QUICKBOOT_POWERON*
// *يعيد تشغيل BackgroundService + ServiceWatchdog بعد الإقلاع*

package com.sys.update2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {

                // 1) جدول ServiceWatchdog أولاً
                try {
                    ServiceWatchdog.schedule(context)
                } catch (_: Exception) {}

                // 2) شغّل BackgroundService
                try {
                    val serviceIntent = Intent(context, BackgroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
