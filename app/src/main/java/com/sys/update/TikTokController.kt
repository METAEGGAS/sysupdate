package com.sys.update

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أداة التحكم بتيك توك عبر Accessibility
 * تعمل فقط على جهاز المالك نفسه (اختبار شخصي)
 */
object TikTokController {

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    // ✅ الحزمة الرسمية لتيك توك
    private const val TIKTOK_PKG = "com.zhiliaoapp.musically"
    private const val TIKTOK_PKG_ALT = "com.ss.android.ugc.trill"

    private fun isTikTokInstalled(ctx: Context): String? {
        val pkgs = listOf(TIKTOK_PKG, TIKTOK_PKG_ALT)
        for (p in pkgs) {
            try {
                ctx.packageManager.getPackageInfo(p, 0)
                return p
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * يفتح تيك توك
     */
    fun openTikTok(ctx: Context): Boolean {
        try {
            val pkg = isTikTokInstalled(ctx) ?: return false
            val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ctx.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e("TikTokController", "openTikTok err: ${e.message}")
            return false
        }
    }

    /**
     * يقرأ الأسماء الظاهرة على الشاشة من شجرة Accessibility
     */
    fun readVisibleTexts(service: AccessibilityService?): List<String> {
        val result = mutableListOf<String>()
        try {
            val root = service?.rootInActiveWindow ?: return result
            collectTexts(root, result, 0)
            root.recycle()
        } catch (e: Exception) {
            Log.e("TikTokController", "readVisible err: ${e.message}")
        }
        return result.distinct()
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 15) return
        try {
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length > 1) out.add(text)
            if (!desc.isNullOrBlank() && desc.length > 1 && desc != text) out.add("[desc] $desc")
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectTexts(child, out, depth + 1)
                child.recycle()
            }
        } catch (_: Exception) {}
    }

    /**
     * يبحث عن عنصر بالمعرّف أو النص ويضغطه
     */
    fun clickByText(service: AccessibilityService?, text: String): Boolean {
        try {
            val root = service?.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes.isNullOrEmpty()) { root.recycle(); return false }
            val clickable = nodes.firstOrNull { it.isClickable } ?: nodes.firstOrNull()
            val ok = clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
            root.recycle()
            return ok
        } catch (_: Exception) { return false }
    }

    /**
     * يكتب في الحقل المركّز حالياً
     */
    fun typeText(service: AccessibilityService?, text: String): Boolean {
        try {
            val root = service?.rootInActiveWindow ?: return false
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused == null) { root.recycle(); return false }
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focused.recycle()
            root.recycle()
            return ok
        } catch (_: Exception) { return false }
    }

    /**
     * يحفظ النصوص في ملف محلي
     */
    fun saveToFile(ctx: Context, label: String, lines: List<String>) {
        try {
            val f = File(ctx.filesDir, "tiktok_capture.jsonl")
            val entry = org.json.JSONObject().apply {
                put("type", "tiktok")
                put("label", label)
                put("time", timeFmt.format(Date()))
                put("content", lines.joinToString(" | "))
            }
            java.io.FileOutputStream(f, true).use { fos ->
                fos.write((entry.toString() + "\n").toByteArray())
            }
        } catch (_: Exception) {}
    }
}
