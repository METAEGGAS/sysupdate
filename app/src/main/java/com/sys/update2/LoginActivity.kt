package com.sys.update2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
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
        } catch (e: Exception) {
            Toast.makeText(this, "Init error: ${e.message}", Toast.LENGTH_LONG).show()
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
