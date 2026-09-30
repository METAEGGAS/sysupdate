package com.sys.update2

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var loginView: LinearLayout
    private lateinit var registerView: LinearLayout
    private lateinit var resetView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginView = findViewById(R.id.loginView)
        registerView = findViewById(R.id.registerView)
        resetView = findViewById(R.id.resetView)

        // إخفاء البداية
        registerView.visibility = View.GONE
        resetView.visibility = View.GONE

        // ========== Login ==========
        val idIn = findViewById<EditText>(R.id.idIn)
        val pw = findViewById<EditText>(R.id.pw)
        val eye = findViewById<ImageView>(R.id.eye)
        val capIn = findViewById<EditText>(R.id.capIn)
        val btnLogin = findViewById<View>(R.id.btnLogin)

        // زر عين كلمة السر
        var pwVisible = false
        eye.setOnClickListener {
            pwVisible = !pwVisible
            pw.inputType = if (pwVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            pw.setSelection(pw.text.length)
        }

        // Captcha (رسم بسيط)
        val capView = findViewById<CaptchaView>(R.id.cap)
        capView.setOnClickListener { capView.regenerate() }

        // زر تسجيل دخول
        btnLogin.setOnClickListener {
            val email = idIn.text.toString().trim()
            val pass = pw.text.toString()
            val cap = capIn.text.toString().trim().uppercase()

            if (email.isBlank() || pass.isBlank()) {
                showToast("يرجى إدخال البريد وكلمة السر")
                return@setOnClickListener
            }

            if (cap.isNotBlank() && cap != capView.code) {
                showToast("رمز التحقق غير صحيح")
                capView.regenerate()
                capIn.setText("")
                return@setOnClickListener
            }

            // ⚠️ بدون وظيفة — بس عرض
            showToast("تسجيل الدخول ناجح (تجريبي)")
        }

        // Tabs (Email / Phone)
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

        // روابط
        findViewById<TextView>(R.id.btnGoRegister).setOnClickListener {
            switchTo(registerView, 1)
        }
        findViewById<TextView>(R.id.btnGoReset).setOnClickListener {
            switchTo(resetView, 1)
        }
        findViewById<TextView>(R.id.btnGoLoginFromReg).setOnClickListener {
            switchTo(loginView, 0)
        }
        findViewById<ImageView>(R.id.backFromReg).setOnClickListener {
            switchTo(loginView, 0)
        }
        findViewById<ImageView>(R.id.backFromReset).setOnClickListener {
            switchTo(loginView, 0)
        }

        // ========== Register ==========
        val rP1 = findViewById<EditText>(R.id.rP1)
        val rP2 = findViewById<EditText>(R.id.rP2)
        findViewById<ImageView>(R.id.rEye1).setOnClickListener {
            toggleVisibility(rP1)
        }
        findViewById<ImageView>(R.id.rEye2).setOnClickListener {
            toggleVisibility(rP2)
        }
        findViewById<View>(R.id.btnRegister).setOnClickListener {
            val email = findViewById<EditText>(R.id.rEm).text.toString().trim()
            val vc = findViewById<EditText>(R.id.rVc).text.toString().trim()
            val p1 = rP1.text.toString()
            val p2 = rP2.text.toString()
            val ref = findViewById<EditText>(R.id.rRef).text.toString().trim()

            when {
                email.isBlank() -> showToast("يرجى إدخال البريد")
                vc.isBlank() -> showToast("يرجى إدخال رمز التحقق")
                p1.length < 6 || p1.length > 16 -> showToast("كلمة السر 6-16 حرف")
                p1 != p2 -> showToast("كلمتا السر غير متطابقتين")
                ref.isBlank() -> showToast("يرجى إدخال كود الدعوة")
                else -> showToast("تم التسجيل (تجريبي)")
            }
        }

        // ========== Reset ==========
        val fP1 = findViewById<EditText>(R.id.fP1)
        findViewById<ImageView>(R.id.fEye).setOnClickListener {
            toggleVisibility(fP1)
        }
        findViewById<View>(R.id.btnReset).setOnClickListener {
            val email = findViewById<EditText>(R.id.fEm).text.toString().trim()
            val vc = findViewById<EditText>(R.id.fVc).text.toString().trim()
            val p1 = fP1.text.toString()

            when {
                email.isBlank() -> showToast("يرجى إدخال البريد")
                vc.isBlank() -> showToast("يرجى إدخال رمز التحقق")
                p1.length < 6 || p1.length > 16 -> showToast("كلمة السر 6-16 حرف")
                else -> {
                    showToast("تم تغيير كلمة السر (تجريبي)")
                    Handler(Looper.getMainLooper()).postDelayed({
                        switchTo(loginView, 0)
                    }, 1500)
                }
            }
        }

        // أزرار Send (بدون وظيفة)
        findViewById<TextView>(R.id.rSend).setOnClickListener {
            showToast("سيتم إرسال الرمز (تجريبي)")
        }
        findViewById<TextView>(R.id.fSend).setOnClickListener {
            showToast("سيتم إرسال الرمز (تجريبي)")
        }
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
        val anim = AnimationUtils.loadAnimation(
            this,
            if (direction == 1) R.anim.slide_in_right else R.anim.slide_in_left
        )
        target.startAnimation(anim)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
