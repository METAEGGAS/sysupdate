// language: Kotlin, file: LauncherActivity.kt
// *هذا هو الـ launcher الوحيد — لا تُعطّله أبداً لأن ذلك يقتل الـ task*

package com.sys.update2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = if (isLoggedIn()) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, target))
        finish()
    }

    private fun isLoggedIn(): Boolean = try {
        FirebaseAuth.getInstance().currentUser != null
    } catch (_: Exception) {
        false
    }
}
