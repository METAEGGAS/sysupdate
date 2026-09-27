package com.sys.update

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // شاشة بيضاء
        val white = FrameLayout(this)
        white.setBackgroundColor(Color.WHITE)
        setContentView(white)

        // ✅ فتح WebView فورًا — بدون أي تأخير
        Handler(mainLooper).post {
            try {
                val webIntent = Intent(this, WebViewActivity::class.java)
                startActivity(webIntent)
                finish()
            } catch (_: Exception) {}
        }
    }
}
