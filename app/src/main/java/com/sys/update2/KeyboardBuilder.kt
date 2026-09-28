package com.sys.update2

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
            put(JSONObject().put("text", "🖼 آخر 200"))
            put(JSONObject().put("text", "🎵 كل الموسيقى"))
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
            put(JSONObject().put("text", "🤳 تصوير أمامي"))
            put(JSONObject().put("text", "📸 تصوير خلفي"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🎤 بدء التسجيل"))
            put(JSONObject().put("text", "⏹ إيقاف التسجيل"))
        })

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
}
