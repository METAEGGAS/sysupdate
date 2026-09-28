package com.sys.update2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataStore {

    private const val NOTIF_FILE = "notifications.jsonl"
    private const val MAX_QUEUE_BYTES = 30L * 1024 * 1024
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun notifFile(ctx: Context): File {
        val f = File(ctx.filesDir, NOTIF_FILE)
        if (!f.exists()) f.createNewFile()
        return f
    }

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
                val lines = f.readLines()
                val keep = lines.takeLast(lines.size / 2)
                f.writeText(keep.joinToString("\n").let { if (it.isEmpty()) "" else "$it\n" })
            }
            java.io.FileOutputStream(f, true).use { fos ->
                fos.write((entry.toString() + "\n").toByteArray())
            }
        } catch (e: Exception) {
            Log.e("DataStore", "notif err: ${e.message}")
        }
    }

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

    @Synchronized
    fun clearNotifications(ctx: Context) {
        try {
            val f = notifFile(ctx)
            if (f.exists()) f.delete()
        } catch (_: Exception) {}
    }
}
