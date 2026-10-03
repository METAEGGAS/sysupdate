// language: Kotlin, file: LoginActivity.kt

package com.sys.update2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import android.graphics.Color as AColor

// ----------------------------------------------------
// الألوان
// ----------------------------------------------------

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

data class LangItem(val name: String, val flag: String)

// ----------------------------------------------------
// Activity
// ----------------------------------------------------

class LoginActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(-1)
    private var isLoading by mutableStateOf(false)

    // تسجيل دخول
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var pwVisible by mutableStateOf(false)

    // إنشاء حساب
    private var regEmail by mutableStateOf("")
    private var regP1 by mutableStateOf("")
    private var regP2 by mutableStateOf("")
    private var regP1Visible by mutableStateOf(false)
    private var regP2Visible by mutableStateOf(false)
    private var regVerifyCode by mutableStateOf("")
    private var regCountdown by mutableStateOf(0)

    // استعادة كلمة المرور
    private var resetEmail by mutableStateOf("")

    // صورة المستخدم
    private var profileUri by mutableStateOf<Uri?>(null)
    private var profileBmp by mutableStateOf<Bitmap?>(null)

    // صورة الخلفية
    private var bgUri by mutableStateOf<Uri?>(null)
    private var bgBmp by mutableStateOf<Bitmap?>(null)

    private var selectedLang by mutableStateOf(0)

    private val pickProfile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                profileUri = uri
                profileBmp = null
            }
        }

    private val shotProfile =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
            if (bmp != null) {
                profileBmp = bmp
                profileUri = null
            }
        }

    private val pickBg =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                bgUri = uri
                bgBmp = null
            }
        }

    private val shotBg =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
            if (bmp != null) {
                bgBmp = bmp
                bgUri = null
            }
        }

    private var pendingMediaAction: (() -> Unit)? = null
    private val MEDIA_PERM_REQUEST = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = AColor.parseColor("#040018")
        window.navigationBarColor = AColor.parseColor("#040018")

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
                        if (isLoading) LoadingOverlay()
                    }
                }
            }
        }
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

    private fun requestMediaPermissionThen(action: () -> Unit) {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (needed.isEmpty()) action()
        else {
            pendingMediaAction = action
            ActivityCompat.requestPermissions(
                this, needed.toTypedArray(), MEDIA_PERM_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_PERM_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                pendingMediaAction?.invoke()
            } else {
                showToast("يجب السماح بالوصول للصور")
            }
            pendingMediaAction = null
        }
    }

    // =================================================
    // شاشة اختيار اللغة
    // =================================================

    @Composable
    fun LanguageScreen() {
        val languages = listOf(
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

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
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
                            .background(
                                if (selected) Color(0xFF161F33) else Color.Transparent
                            )
                            .clickable { selectedLang = index }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, color = Neon.Txt, fontSize = 15.sp,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(14.dp))
                        FlagIcon(code = item.flag,
                            modifier = Modifier.width(32.dp).height(23.dp))
                        Spacer(Modifier.width(14.dp))
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Neon.Purple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(2.dp, Color(0xFF7D8798), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(18.dp, RoundedCornerShape(16.dp),
                        ambientColor = Neon.Magenta, spotColor = Neon.Purple)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(
                        listOf(Neon.Magenta, Neon.Purple, Neon.Blue, Neon.Cyan)))
                    .clickable { currentScreen = 0 },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("متابعة", color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("←", color = Color.White, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
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
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginHeader()

            Spacer(Modifier.height(40.dp))

            NeonInput(email, { email = it }, "البريد الإلكتروني",
                Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            NeonPassword(password, { password = it }, pwVisible,
                { pwVisible = !pwVisible }, "كلمة المرور")

            Spacer(Modifier.height(26.dp))

            GradientButton("تسجيل الدخول", enabled = !isLoading) {
                when {
                    email.isBlank() -> showToast("من فضلك أدخل البريد الإلكتروني")
                    password.isBlank() -> showToast("من فضلك أدخل كلمة المرور")
                    else -> {
                        isLoading = true
                        scope.launch {
                            val result = FirebaseAuthHelper.login(email.trim(), password)
                            isLoading = false
                            if (result.isSuccess) {
                                showToast("✅ تم تسجيل الدخول")
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

            Spacer(Modifier.height(22.dp))
            OrDivider()
            Spacer(Modifier.height(22.dp))
            GoogleButton { showToast("قريباً") }

            Spacer(Modifier.height(16.dp))

            Text("نسيت كلمة المرور؟", color = Neon.Cyan,
                fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { currentScreen = 2 }
                    .padding(vertical = 6.dp))

            Spacer(Modifier.height(26.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ليس لديك حساب؟ ", color = Neon.Hint, fontSize = 14.sp)
                Text(
                    "إنشاء حساب",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(Neon.Cyan, Neon.Magenta)
                        ),
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
    fun LoginHeader() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(34.dp))

            AsyncImage(
                model = RemoteAssets.url("avataro"),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "CREFTEX",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)
                    ),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "exchange",
                color = Neon.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // =================================================
    // شاشة إنشاء الحساب
    // =================================================

    @Composable
    fun RegisterScreen() {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RegisterHeader()

            Spacer(Modifier.height(30.dp))

            NeonInput(regEmail, { regEmail = it }, "البريد الإلكتروني",
                Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(12.dp))

            NeonPassword(regP1, { regP1 = it }, regP1Visible,
                { regP1Visible = !regP1Visible }, "كلمة المرور")
            Spacer(Modifier.height(12.dp))

            NeonPassword(regP2, { regP2 = it }, regP2Visible,
                { regP2Visible = !regP2Visible }, "تأكيد كلمة المرور")
            Spacer(Modifier.height(12.dp))

            VerifyCodeField(
                value = regVerifyCode,
                onValueChange = { regVerifyCode = it },
                countdown = regCountdown,
                onSend = {
                    if (regCountdown > 0) return@VerifyCodeField
                    if (regEmail.isBlank()) {
                        showToast("أدخل البريد الإلكتروني أولاً")
                        return@VerifyCodeField
                    }
                    regCountdown = 60
                    object : CountDownTimer(60_000L, 1_000L) {
                        override fun onTick(millisUntilFinished: Long) {
                            regCountdown = (millisUntilFinished / 1000L).toInt()
                        }
                        override fun onFinish() {
                            regCountdown = 0
                        }
                    }.start()
                    showToast("تم إرسال كود التحقق")
                }
            )

            Spacer(Modifier.height(20.dp))

            // قسم صورة الخلفية
            BackgroundImageCard()

            Spacer(Modifier.height(26.dp))

            GradientButton("إنشاء الحساب", enabled = !isLoading) {
                when {
                    regEmail.isBlank() -> showToast("من فضلك أدخل البريد الإلكتروني")
                    regP1.length < 6 -> showToast("كلمة المرور 6 أحرف على الأقل")
                    regP1 != regP2 -> showToast("كلمتا المرور غير متطابقتين")
                    regVerifyCode.isBlank() -> showToast("أدخل كود التحقق")
                    else -> {
                        isLoading = true
                        scope.launch {
                            val displayName = regEmail.substringBefore("@")
                            val result = FirebaseAuthHelper.register(
                                regEmail.trim(), regP1, displayName
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                showToast("✅ تم إنشاء الحساب")
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
            OrDivider()
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لديك حساب بالفعل؟ ", color = Neon.Hint, fontSize = 14.sp)
                Text(
                    "تسجيل الدخول",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(Neon.Cyan, Neon.Magenta)
                        ),
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
    fun RegisterHeader() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(34.dp))

            ProfileImageCircle()

            Spacer(Modifier.height(8.dp))

            Text(
                "CREFTEX",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)
                    ),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "exchange",
                color = Neon.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun ProfileImageCircle() {
        val ctx = LocalContext.current
        val bmp = remember(profileUri, profileBmp) {
            profileBmp ?: profileUri?.let { uriToBitmap(ctx, it) }
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Neon.FieldBg)
                .border(
                    width = 2.dp,
                    color = Neon.FieldBorder,
                    shape = CircleShape
                )
                .clickable {
                    requestMediaPermissionThen { pickProfile.launch("image/*") }
                },
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Neon.Cyan.copy(alpha = 0.75f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("صورة", color = Neon.Hint, fontSize = 11.sp)
                }
            }
        }
    }

    @Composable
    fun BackgroundImageCard() {
        val ctx = LocalContext.current
        val bmp = remember(bgUri, bgBmp) {
            bgBmp ?: bgUri?.let { uriToBitmap(ctx, it) }
        }

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
                            .border(1.dp, Neon.FieldBorder,
                                RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Image, null,
                                tint = Neon.Hint,
                                modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text("صورة الخلفية", color = Neon.Txt,
                        fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("اختر صورة خلفية", color = Neon.Hint, fontSize = 11.5.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerButton("من المعرض", Icons.Default.Image) {
                    requestMediaPermissionThen { pickBg.launch("image/*") }
                }
                PickerButton("التقاط", Icons.Default.PhotoCamera) {
                    shotBg.launch(null)
                }
            }
        }
    }

    @Composable
    fun PickerButton(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x40131B45))
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(50))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = Neon.Cyan, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Neon.Txt, fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                BackButton { currentScreen = 0 }
                Text("استعادة كلمة المرور", color = Neon.Txt,
                    fontSize = 17.5.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(30.dp))

            NeonInput(resetEmail, { resetEmail = it }, "البريد الإلكتروني",
                Icons.Default.Email, KeyboardType.Email)

            Spacer(Modifier.height(28.dp))

            GradientButton("إرسال رابط الاستعادة", enabled = !isLoading) {
                if (resetEmail.isBlank()) {
                    showToast("من فضلك أدخل البريد الإلكتروني")
                } else {
                    isLoading = true
                    scope.launch {
                        val result = FirebaseAuthHelper.resetPassword(resetEmail.trim())
                        isLoading = false
                        if (result.isSuccess) {
                            showToast("✅ تم إرسال رابط الاستعادة")
                            currentScreen = 0
                        } else {
                            showToast(FirebaseAuthHelper.translateError(
                                result.exceptionOrNull()?.message))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    // =================================================
    // عناصر مشتركة
    // =================================================

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
                tint = Neon.Cyan.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(
                    listOf(Neon.Cyan, Neon.Magenta)),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(hint, color = Neon.Hint, fontSize = 14.5.sp)
                    }
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
                tint = Neon.Cyan.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password),
                textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(
                    listOf(Neon.Cyan, Neon.Magenta)),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(hint, color = Neon.Hint, fontSize = 14.5.sp)
                    }
                    inner()
                }
            )
            Icon(
                imageVector = if (visible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = Neon.Hint,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggle() }
            )
        }
    }

    @Composable
    fun VerifyCodeField(
        value: String,
        onValueChange: (String) -> Unit,
        countdown: Int,
        onSend: () -> Unit
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
            Icon(Icons.Default.Phone, contentDescription = null,
                tint = Neon.Cyan.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(
                    listOf(Neon.Cyan, Neon.Magenta)),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("كود التحقق", color = Neon.Hint, fontSize = 14.5.sp)
                    }
                    inner()
                }
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (countdown > 0) Color(0x333B2E85)
                        else Color(0x55A855F7)
                    )
                    .clickable(enabled = countdown == 0) { onSend() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdown > 0) "$countdown" else "إرسال",
                    color = if (countdown > 0) Neon.Hint else Neon.Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    fun GradientButton(
        text: String,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp),
                    ambientColor = Neon.Magenta, spotColor = Neon.Purple)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.horizontalGradient(
                    listOf(Neon.Magenta, Neon.Purple, Neon.Blue, Neon.Cyan)))
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("←", color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun OrDivider() {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(
                    Color.Transparent, Neon.Purple.copy(alpha = 0.6f)))))
            Text("أو", color = Neon.Hint, fontSize = 13.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp))
            Box(Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(
                    Neon.Purple.copy(alpha = 0.6f), Color.Transparent))))
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
                .border(1.dp,
                    Brush.horizontalGradient(
                        listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)),
                    RoundedCornerShape(27.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", style = TextStyle(
                    brush = Brush.linearGradient(listOf(
                        Color(0xFF4285F4), Color(0xFFEA4335),
                        Color(0xFFFBBC05), Color(0xFF34A853))),
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.width(10.dp))
                Text("الدخول باستخدام Google", color = Neon.Txt,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            Text("→", color = Neon.Cyan, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }
    }

    // =================================================
    // الأعلام — مرسومة برمجياً
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
                    drawLine(color = Color.White,
                        start = Offset(w * 0.20f, h * 0.38f),
                        end = Offset(w * 0.80f, h * 0.38f),
                        strokeWidth = stroke)
                    drawLine(color = Color.White,
                        start = Offset(w * 0.22f, h * 0.50f),
                        end = Offset(w * 0.78f, h * 0.50f),
                        strokeWidth = stroke)
                    drawLine(color = Color.White,
                        start = Offset(w * 0.24f, h * 0.62f),
                        end = Offset(w * 0.76f, h * 0.62f),
                        strokeWidth = stroke)
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
                    drawRect(Color(0xFF3C3B6E),
                        topLeft = Offset.Zero,
                        size = Size(w * 0.45f, stripe * 7f))
                    val dotR = stripe * 0.3f
                    for (row in 0 until 4) {
                        for (col in 0 until 5) {
                            drawCircle(
                                Color.White,
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
                    drawRect(Color(0xFFDE2910))
                    drawPath(starPath(w * 0.22f, h * 0.32f, h * 0.22f),
                        Color(0xFFFFDE00))
                }
                "tw" -> {
                    drawRect(Color(0xFFFE0000))
                    drawRect(Color(0xFF000095), topLeft = Offset.Zero,
                        size = Size(w * 0.5f, h * 0.5f))
                    drawCircle(Color.White, radius = h * 0.12f,
                        center = Offset(w * 0.25f, h * 0.25f))
                    drawCircle(Color(0xFF000095), radius = h * 0.08f,
                        center = Offset(w * 0.25f, h * 0.25f))
                }
                "jp" -> {
                    drawRect(Color.White)
                    drawCircle(Color(0xFFBC002D),
                        radius = h * 0.3f,
                        center = Offset(w / 2f, h / 2f))
                }
                "tr" -> {
                    drawRect(Color(0xFFE30A17))
                    drawCircle(Color.White, radius = h * 0.3f,
                        center = Offset(w * 0.38f, h / 2f))
                    drawCircle(Color(0xFFE30A17), radius = h * 0.25f,
                        center = Offset(w * 0.44f, h / 2f))
                    drawPath(
                        starPath(w * 0.6f, h / 2f, h * 0.13f, startDeg = 180),
                        Color.White
                    )
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
                    drawPath(starPath(w / 2f, h / 2f, h * 0.28f),
                        Color(0xFFFFFF00))
                }
                "fr" -> {
                    drawRect(Color(0xFF0055A4), size = Size(w / 3f, h))
                    drawRect(Color.White,
                        topLeft = Offset(w / 3f, 0f),
                        size = Size(w / 3f, h))
                    drawRect(Color(0xFFEF4135),
                        topLeft = Offset(2f * w / 3f, 0f),
                        size = Size(w / 3f, h))
                }
                "pt" -> {
                    drawRect(Color(0xFF046A38), size = Size(w * 0.4f, h))
                    drawRect(Color(0xFFDA291C),
                        topLeft = Offset(w * 0.4f, 0f),
                        size = Size(w * 0.6f, h))
                    drawCircle(Color(0xFFFFE900),
                        radius = h * 0.16f,
                        center = Offset(w * 0.4f, h / 2f))
                }
                "pk" -> {
                    drawRect(Color(0xFF01411C))
                    drawRect(Color.White, size = Size(w * 0.25f, h))
                    drawCircle(Color.White, radius = h * 0.28f,
                        center = Offset(w * 0.62f, h * 0.45f))
                    drawCircle(Color(0xFF01411C), radius = h * 0.24f,
                        center = Offset(w * 0.68f, h * 0.4f))
                }
                "ir" -> {
                    drawRect(Color(0xFF239F40), size = Size(w, h / 3f))
                    drawRect(Color.White, topLeft = Offset(0f, h / 3f),
                        size = Size(w, h / 3f))
                    drawRect(Color(0xFFDA0000),
                        topLeft = Offset(0f, 2f * h / 3f),
                        size = Size(w, h / 3f))
                    drawCircle(Color(0xFFDA0000), radius = h * 0.11f,
                        center = Offset(w / 2f, h / 2f))
                }
                "th" -> {
                    val s = h / 6f
                    drawRect(Color(0xFFA51931), size = Size(w, s))
                    drawRect(Color(0xFFF4F5F8),
                        topLeft = Offset(0f, s),
                        size = Size(w, s))
                    drawRect(Color(0xFF2D2A4A),
                        topLeft = Offset(0f, 2f * s),
                        size = Size(w, 2f * s))
                    drawRect(Color(0xFFF4F5F8),
                        topLeft = Offset(0f, 4f * s),
                        size = Size(w, s))
                    drawRect(Color(0xFFA51931),
                        topLeft = Offset(0f, 5f * s),
                        size = Size(w, s))
                }
                else -> drawRect(Color(0xFF555555))
            }
        }
    }

    private fun starPath(
        cx: Float,
        cy: Float,
        outerR: Float,
        startDeg: Int = -90
    ): Path {
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
    // الخلفية — تحمّل background.png من assets
    // =================================================

    @Composable
    fun NeonBackground() {
        val ctx = LocalContext.current
        val bg = remember { loadAssetBitmap(ctx, "background.png") }
        val stars = remember {
            List(60) { Offset(Random.nextFloat(), Random.nextFloat() * 0.7f) }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. الصورة من assets (إن وُجدت)
            if (bg != null) {
                val scale = maxOf(
                    size.width / bg.width,
                    size.height / bg.height
                )
                val dw = bg.width * scale
                val dh = bg.height * scale
                drawImage(
                    image = bg.asImageBitmap(),
                    dstOffset = IntOffset(
                        ((size.width - dw) / 2).roundToInt(),
                        ((size.height - dh) / 2).roundToInt()
                    ),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
                )
                // تعتيم فوق الصورة
                drawRect(Brush.verticalGradient(
                    listOf(Color(0x99060021), Color(0xCC040018))
                ))
            } else {
                // بديل: تدرج لوني
                drawRect(Brush.verticalGradient(
                    listOf(Neon.DeepTop, Neon.DeepMid, Neon.DeepBottom)
                ))
            }

            // 2. هالة بنفسجية
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Neon.Magenta.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.07f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.07f)
            )

            // 3. هالة سماوية
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Neon.Cyan.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.06f, size.height * 0.30f),
                    radius = size.width * 0.60f
                ),
                radius = size.width * 0.60f,
                center = Offset(size.width * 0.06f, size.height * 0.30f)
            )

            // 4. نجوم
            stars.forEach {
                drawCircle(
                    Neon.Cyan.copy(alpha = 0.45f),
                    radius = 1.4.dp.toPx(),
                    center = Offset(it.x * size.width, it.y * size.height)
                )
            }
        }
    }

    @Composable
    fun LoadingOverlay() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Neon.Cyan)
        }
    }
}

// ----------------------------------------------------
// دوال مساعدة
// ----------------------------------------------------

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

fun loadAssetBitmap(context: Context, path: String): Bitmap? = try {
    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
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
    ) { content() }
}
