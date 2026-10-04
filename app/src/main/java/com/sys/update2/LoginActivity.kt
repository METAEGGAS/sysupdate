// language: Kotlin, file: LoginActivity.kt
// التصميم منسوخ 1:1 من ملف auth.html (ExCoreX)
// تم حذف كل ما يخص: بوت تيليجرام، التجسس، حفظ كلمات المرور بنص صريح، إرسال الموقع وجهات الاتصال

package com.sys.update2

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.graphics.Color as AColor
import android.graphics.Paint as APaint
import android.graphics.Typeface as ATypeface

// ----------------------------------------------------
// ألوان التصميم (من ملف HTML حرفياً)
// ----------------------------------------------------

object Ex {
    // خلفية الصفحة: radial-gradient(ellipse 140% 60% at 50% -5% ...)
    val Bg0 = Color(0xFF1A5FD0)
    val Bg1 = Color(0xFF0D3A8F)
    val Bg2 = Color(0xFF071E4D)
    val Bg3 = Color(0xFF040F28)
    val Bg4 = Color(0xFF02081A)

    val CardBg   = Color(0xFF0A1128)                 // .card background
    val FieldBg  = Color(0xFF131C36)                 // .fld (تسجيل الدخول)
    val FieldBr  = Color(0xFF1E2A4D)                 // .fld border
    val FieldBgR = Color(0xFF16224A)                 // #reg/#rst .fld
    val FieldBrR = Color(0xFF23336B)                 // #reg/#rst .fld border

    val Hint     = Color(0xFF5C6C8F)                 // placeholder + .eye
    val InputTxt = Color(0xFFE8EEF8)                 // .fld input color
    val TabOff   = Color(0xFF5C6C8F)                 // .tabs span
    val White    = Color(0xFFFFFFFF)

    val BlueAccent = Color(0xFF3D8BFF)               // h1 b
    val BtnTop     = Color(0xFF3D8BFF)               // linear-gradient(180deg,#3d8bff,#1a5cff)
    val BtnBottom  = Color(0xFF1A5CFF)
    val BtnGlow    = Color(0x661E64FF)               // rgba(30,100,255,.4)

    val BadgeBg  = Color(0xBF0A193C)                 // rgba(10,25,60,.75)
    val BadgeBr  = Color(0x475A96FF)                 // rgba(90,150,255,.28)
    val BadgeTxt = Color(0xFFDBE7FF)                 // .bdg color
    val BadgeIco = Color(0xFF8FB6FF)                 // .bdg svg stroke

    val FootLink   = Color(0xFF8FA3C8)               // .foot a
    val ToLogin    = Color(0xFF8BA5D9)               // .tologin
    val LabelTxt   = Color(0xFFEAF0FB)               // .rlab
    val SendTxt    = Color(0xFFDCE8FF)               // .send
    val BackBtnBg  = Color(0xEBBED4F5)               // rgba(190,212,245,.92)
    val BackBtnIco = Color(0xFF123A75)               // .bak svg stroke
}

// بيانات الكابتشا
private data class CapLine(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val r: Int, val g: Int, val b: Int
)
private data class CapGlyph(
    val ch: Char, val color: Long, val rot: Float, val fontSp: Float, val dy: Float
)

// روابط الصور من ملف HTML حرفياً
object RemoteImgs {
    const val LOGO = "https://i.ibb.co/tMSFK4WF/IMG.png"   // .lgo
    const val COIN = "https://i.ibb.co/2YFLgmNM/IMG.png"   // .coin
}

// ----------------------------------------------------
// Activity
// ----------------------------------------------------

class LoginActivity : ComponentActivity() {

    // 0 = تسجيل الدخول، 1 = إنشاء حساب، 2 = استعادة كلمة المرور
    private var currentScreen by mutableStateOf(0)
    private var isLoading by mutableStateOf(false)

    // تسجيل الدخول
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var pwVisible by mutableStateOf(false)
    private var captchaInput by mutableStateOf("")
    private var captchaCode by mutableStateOf("")

    // إنشاء حساب
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

    // استعادة كلمة المرور
    private var rstEmail by mutableStateOf("")
    private var rstCode by mutableStateOf("")
    private var rstSentCode by mutableStateOf<String?>(null)
    private var rstSentEmail by mutableStateOf<String?>(null)
    private var rstCodeExpireAt by mutableStateOf(0L)
    private var rstCountdown by mutableStateOf(0)
    private var rstNewPass by mutableStateOf("")
    private var rstNewPassVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = AColor.parseColor("#02081A")
        window.navigationBarColor = AColor.parseColor("#02081A")

        captchaCode = newCaptchaCode()

        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // خلفية متدرجة دائرية حقيقية (ellipse 140% 60% at 50% -5%)
                    EllipseBackground()

                    when (currentScreen) {
                        0 -> LoginScreen()
                        1 -> RegisterScreen()
                        2 -> ResetScreen()
                    }
                }
            }
        }
    }

    // =================================================
    // الخلفية: radial-gradient(ellipse 140% 60% at 50% -5%)
    // =================================================

    @Composable
    fun EllipseBackground() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * -0.05f
            // المحور الأفقي 140% من العرض /2 والرأسي 60% من الارتفاع /2
            val rx = w * 1.4f / 2f
            val ry = h * 0.6f / 2f
            val radius = maxOf(rx, ry)

            // نرسم تدرج دائري حول المركز ثم نقصّه لشكل بيضاوي
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
    // شاشة تسجيل الدخول (نفس #lg في HTML)
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
            // ---------- hero ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الشعار .lgo
                AsyncImage(
                    model = RemoteImgs.LOGO,
                    contentDescription = "ExCoreX",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )

                // الشارة .bdg
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

                // العنوان h1
                Spacer(Modifier.height(15.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Power up your\n")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Ex.BlueAccent)) {
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

                // صورة العملة .coin — margin:12px auto -32px
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

            // ---------- الكارت مع القوس العلوي ----------
            Spacer(Modifier.height((-32).dp)) // coin margin-bottom:-32px
            Box(modifier = Modifier.fillMaxWidth()) {
                // القوس المضيء فوق الكارت (card::before + card::after)
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
                    // التبويبات .tabs
                    Row {
                        Text(
                            "Email",
                            color = Ex.White,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(26.dp))
                        Text(
                            "Phone",
                            color = Ex.TabOff.copy(alpha = 0.45f),
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // حقل الإيميل .fld
                    LoginField {
                        RegInput(
                            value = email,
                            onChange = { email = it },
                            hint = "Email",
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // حقل كلمة المرور + أيقونة العين
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

                    // حقل الكابتشا + صورة الكود
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

                    // زر الدخول .btn
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

                    // روابط أسفل الكارت .foot
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "reset pass",
                            color = Ex.FootLink,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { currentScreen = 2 }
                        )
                        Text(
                            "to register",
                            color = Ex.FootLink,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { currentScreen = 1 }
                        )
                    }
                }
            }
        }
    }

    // =================================================
    // شاشة إنشاء الحساب (نفس #reg في HTML)
    // =================================================

    @Composable
    fun RegisterScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RegBackground())
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 40.dp)
        ) {
            // شريط العودة .rbar
            ScreenBar(title = "Email") { currentScreen = 0 }

            // Email
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

            // Verification code
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
                            // TODO: أرسل regSentCode إلى الإيميل عبر خدمة الإرسال الخاصة بك
                            showToast("Verification code sent")
                        }
                )
            }

            // Registration password
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

            // Referrer Invitation Code
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

            // زر التسجيل .btn
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
                        showToast("Verification code expired, please request a new one")
                    e != regSentEmail ->
                        showToast("Email was changed after the code was sent, please request a new code")
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

            // To log in
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
    // شاشة استعادة كلمة المرور (نفس #rst في HTML)
    // =================================================

    @Composable
    fun ResetScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RegBackground())
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 40.dp)
        ) {
            ScreenBar(title = "Reset Password") { currentScreen = 0 }

            // Email
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

            // Verification code
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
                            // TODO: أرسل rstSentCode إلى الإيميل عبر خدمة الإرسال الخاصة بك
                            showToast("Verification code sent")
                        }
                )
            }

            // New password
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

            // زر التأكيد
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
                        showToast("Verification code expired, please request a new one")
                    e != rstSentEmail ->
                        showToast("Email was changed after the code was sent, please request a new code")
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

    // حقل تسجيل الدخول: .fld (ارتفاع 52، زوايا 12، خلفية #131c36)
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

    // حقل التسجيل/الاستعادة: #reg .fld (ارتفاع 54، زوايا 10، خلفية #16224a)
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

    // حقل إدخال موحد
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

    // عنوان الحقول .rlab
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

    // شريط العودة .rbar
    @Composable
    fun ScreenBar(title: String, onBack: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر الرجوع .bak
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

    // الزر الأزرق .btn: linear-gradient(180deg,#3d8bff,#1a5cff)
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
            // لمعة داخلية علوية inset 0 1px 2px rgba(190,220,255,.5)
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

    // أيقونة العين (نفس SVG الموجود في HTML حرفياً)
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
                // eye-off: نفس الـ paths في ملف HTML
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
                // eye (مفتوحة)
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

    // أيقونة البيت في الشارة (نفس SVG .bdg)
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
                // M3 10l9-7 9 7
                moveTo(3f * s, 10f * s)
                lineTo(12f * s, 3f * s)
                lineTo(21f * s, 10f * s)
                // M5 9v11h14V9
                moveTo(5f * s, 9f * s)
                lineTo(5f * s, 20f * s)
                lineTo(19f * s, 20f * s)
                lineTo(19f * s, 9f * s)
                // M9 20v-6h6v6
                moveTo(9f * s, 20f * s)
                lineTo(9f * s, 14f * s)
                lineTo(15f * s, 14f * s)
                lineTo(15f * s, 20f * s)
            }
            drawPath(p, Ex.BadgeIco, style = stroke)
        }
    }

    // سهم الرجوع (نفس SVG .bak: polyline 15 18 9 12 15 6)
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

    // =================================================
    // القوس المضيء فوق الكارت (card::before / card::after)
    // path: M0 1.5 Q50 33 100 1.5  + stroke #bcd6ff
    // + وهج: M0 11 Q50 42.5 100 11 stroke #e8f2ff
    // =================================================

    @Composable
    fun CardTopCurve(modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            // الوهج الخلفي (radial-gradient 60% 85% at 50% 100%)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x728CB9FF), Color.Transparent),
                    center = Offset(w * 0.5f, h),
                    radius = w * 0.3f
                ),
                radius = w * 0.3f,
                center = Offset(w * 0.5f, h)
            )

            // القوس المضيء الخارجي: M0 11 Q50 42.5 100 11 (viewBox 100x45)
            val glow = Path().apply {
                moveTo(0f, 11f / 45f * h)
                quadraticTo(w * 0.5f, 42.5f / 45f * h, w, 11f / 45f * h)
            }
            drawPath(
                glow,
                Color(0x59E8F2FF),
                style = Stroke(width = 1.6f * (w / 100f) * 1.2f)
            )

            // القوس الداخلي: M0 1.5 Q50 33 100 1.5 (viewBox 100x35)
            val inner = Path().apply {
                moveTo(0f, 1.5f / 35f * h)
                quadraticTo(w * 0.5f, 33f / 35f * h, w, 1.5f / 35f * h)
            }
            drawPath(
                inner,
                Color(0xFFBCD6FF),
                style = Stroke(width = 1.2f * (w / 430f).coerceAtLeast(1f))
            )
        }
    }

    // =================================================
    // الكابتشا (نفس drawCap() في HTML)
    // 3 خطوط عشوائية + 4 حروف ملونة مائلة على خلفية بيضاء
    // =================================================

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

            // خلفية بيضاء
            drawRect(Color.White)

            // 3 خطوط تشويش (rgba عشوائية بشفافية .6)
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

            // الحروف الأربعة بخط cursive مائل
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
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
        } catch (_: Exception) {}
    }
}

// ----------------------------------------------------
// Theme
// ----------------------------------------------------

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
