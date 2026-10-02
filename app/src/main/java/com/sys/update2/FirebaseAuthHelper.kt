package com.sys.update2

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseAuthHelper {

    private const val TAG = "FirebaseAuthHelper"

    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ═══════════════════════════════════════════
    //  إنشاء حساب جديد
    // ═══════════════════════════════════════════
    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth().createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // تحديث الاسم
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (_: Exception) {}

                // حفظ البيانات في Firestore
                try {
                    val data = hashMapOf(
                        "email" to email,
                        "displayName" to displayName,
                        "uid" to user.uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db().collection("users").document(user.uid)
                        .set(data, SetOptions.merge()).await()
                } catch (_: Exception) {}

                Result.success(user)
            } else {
                Result.failure(Exception("فشل إنشاء الحساب"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "register err: ${e.message}")
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════
    //  تسجيل دخول
    // ═══════════════════════════════════════════
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth().signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("فشل تسجيل الدخول"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "login err: ${e.message}")
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════
    //  استعادة كلمة السر
    // ═══════════════════════════════════════════
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "reset err: ${e.message}")
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════
    //  تسجيل خروج
    // ═══════════════════════════════════════════
    fun logout() {
        try { auth().signOut() } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  المستخدم الحالي
    // ═══════════════════════════════════════════
    fun currentUser(): FirebaseUser? = auth().currentUser

    fun isLoggedIn(): Boolean = currentUser() != null

    // ═══════════════════════════════════════════
    //  ترجمة أخطاء Firebase للعربية
    // ═══════════════════════════════════════════
    fun translateError(message: String?): String {
        if (message.isNullOrBlank()) return "حدث خطأ"
        return when {
            message.contains("email address is already in use") -> "البريد الإلكتروني مستخدم بالفعل"
            message.contains("email address is badly formatted") -> "صيغة البريد الإلكتروني غير صحيحة"
            message.contains("password is invalid") -> "كلمة المرور غير صحيحة"
            message.contains("no user record") -> "لا يوجد حساب بهذا البريد"
            message.contains("password should be at least") -> "كلمة المرور قصيرة جداً"
            message.contains("network error") -> "خطأ في الاتصال بالإنترنت"
            message.contains("too many requests") -> "محاولات كثيرة — حاول لاحقاً"
            message.contains("user disabled") -> "الحساب معطّل"
            message.contains("operation not allowed") -> "العملية غير مسموحة — تأكد من تفعيل Email/Password"
            else -> message
        }
    }
}
