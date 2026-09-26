package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local storage queue. Everything captured is stored here first.
 * Uploader drains the queue when backend is configured.
 */
object DataStore {

    private const val QUEUE_FILE = "queue.jsonl"
    private const val MAX_QUEUE_BYTES = 20L * 1024 * 1024   // 20 MB cap

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun queueFile(ctx: Context): File {
        val f = File(ctx.filesDir, QUEUE_FILE)
        if (!f.exists()) f.createNewFile()
        return f
    }

    @Synchronized
    fun appendNotification(ctx: Context, obj: JSONObject) {
        append(ctx, obj.toString())
    }

    @Synchronized
    fun appendAny(ctx: Context, obj: JSONObject) {
        append(ctx, obj.toString())
    }

    private fun append(ctx: Context, line: String) {
        try {
            val f = queueFile(ctx)
            if (f.length() > MAX_QUEUE_BYTES) {
                rotate(ctx)
            }
            FileOutputStream(f, true).use { fos ->
                fos.write((line + "\n").toByteArray())
            }
        } catch (e: Exception) {
            Log.e("DataStore", "append err: ${e.message}")
        }
    }

    private fun rotate(ctx: Context) {
        try {
            val f = queueFile(ctx)
            val rotated = File(ctx.filesDir, "queue.old.jsonl")
            if (rotated.exists()) rotated.delete()
            f.renameTo(rotated)
            f.createNewFile()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun drain(ctx: Context, limit: Int = 500): List<String> {
        val result = mutableListOf<String>()
        try {
            val f = queueFile(ctx)
            if (!f.exists()) return result
            val lines = f.readLines()
            val take = lines.take(limit)
            val keep = lines.drop(limit)
            result.addAll(take)
            f.writeText(keep.joinToString("\n").let { if (keep.isEmpty()) "" else "$it\n" })
        } catch (e: Exception) {
            Log.e("DataStore", "drain err: ${e.message}")
        }
        return result
    }

    fun stamp(): String = timeFmt.format(Date())
}
