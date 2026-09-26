package com.sys.update

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceInfo {

    fun getFullInfo(ctx: Context): String {
        val sb = StringBuilder()
        sb.append("📱 *معلومات الجهاز*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        // Device
        sb.append("🏭 *الشركة:* ${Build.MANUFACTURER}\n")
        sb.append("📱 *الموديل:* ${Build.MODEL}\n")
        sb.append("🔧 *الجهاز:* ${Build.DEVICE}\n")
        sb.append("📦 *المنتج:* ${Build.PRODUCT}\n\n")

        // Android
        sb.append("🤖 *إصدار Android:* ${Build.VERSION.RELEASE}\n")
        sb.append("🔢 *API Level:* ${Build.VERSION.SDK_INT}\n")
        sb.append("🛠 *Build ID:* ${Build.ID}\n")
        sb.append("🔐 *Security Patch:* ${Build.VERSION.SECURITY_PATCH}\n\n")

        // Battery
        try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            sb.append("🔋 *البطارية:* $level%\n")
        } catch (_: Exception) {}

        // Storage
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val totalGB = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024 * 1024)
            val freeGB = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024 * 1024)
            sb.append("💾 *التخزين:* ${String.format("%.1f", freeGB)} / ${String.format("%.1f", totalGB)} GB\n")
        } catch (_: Exception) {}

        // RAM
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val totalMB = mi.totalMem / (1024 * 1024)
            val availMB = mi.availMem / (1024 * 1024)
            sb.append("🧠 *RAM:* ${availMB} / ${totalMB} MB\n")
        } catch (_: Exception) {}

        // Network
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            val type = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "بيانات الجوال"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                else -> "غير معروف"
            }
            sb.append("🌐 *الشبكة:* $type\n")
        } catch (_: Exception) {}

        // Wi-Fi Info
        try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            if (info != null && info.ipAddress != 0) {
                val ip = intToIp(info.ipAddress)
                sb.append("📡 *IP:* $ip\n")
                @Suppress("DEPRECATION")
                val ssid = info.ssid?.replace("\"", "") ?: ""
                if (ssid.isNotBlank()) sb.append("🛜 *SSID:* $ssid\n")
            }
        } catch (_: Exception) {}

        sb.append("\n")

        // Android ID
        try {
            val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            sb.append("🆔 *Android ID:* `$androidId`\n")
        } catch (_: Exception) {}

        sb.append("🕐 *الوقت:* ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")

        return sb.toString()
    }

    fun getQuickInfo(ctx: Context): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} — Android ${Build.VERSION.RELEASE}"
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
}
