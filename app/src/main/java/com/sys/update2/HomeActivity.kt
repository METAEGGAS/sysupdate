package com.sys.update2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// ----------------------------------------------------
// الألوان
// ----------------------------------------------------

private object SahraColors {
    val Background = Color(0xFF030510)
    val Card = Color(0xFF0D101B)
    val CardBorder = Color(0xFF171B29)
    val Text = Color(0xFFF8F7FC)
    val Muted = Color(0xFF9393A7)

    val Purple = Color(0xFF7932DC)
    val Pink = Color(0xFFFF63A4)
    val Gold = Color(0xFFFFD866)

    val Navigation = Color(0xFF001014)
}

private data class SahraUser(
    val id: String,
    val name: String,
    val age: Int,
    val country: String,
    val avatar: Int,
    val photos: List<Int> = emptyList(),
    val status: String = "",
    val vip: Boolean = false,
    val joinRoom: Boolean = false
)

private data class SahraNotice(
    val title: String,
    val message: String
)

private data class SahraTab(
    val title: String,
    val artIndex: Int
)

private val sahraUsers = listOf(
    SahraUser(
        id = "aseel",
        name = "🪽 ❶❶ اسيل",
        age = 18,
        country = "🇸🇾",
        avatar = 5,
        photos = listOf(8, 9, 10, 11),
        vip = true
    ),
    SahraUser(
        id = "sara",
        name = "🌙Sàra🌙",
        age = 23,
        country = "🇸🇾",
        avatar = 6,
        status = "💚⚔313⚔💚",
        joinRoom = true
    ),
    SahraUser(
        id = "roro",
        name = "💄 ›🅂🅁 رورو",
        age = 18,
        country = "🇹🇷",
        avatar = 7,
        photos = listOf(12, 13, 14, 15),
        status = "🙂🤭🌚"
    )
)

private val sahraTabs = listOf(
    SahraTab("الرئيسية", 18),
    SahraTab("الغرف", 19),
    SahraTab("يستكشف", 20),
    SahraTab("الرسائل", 21),
    SahraTab("أنا", 22)
)

// ----------------------------------------------------
// Activity
// ----------------------------------------------------

class HomeActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            SahraTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    SahraApp()
                }
            }
        }
    }
}

// ----------------------------------------------------
// Theme
// ----------------------------------------------------

@Composable
private fun SahraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SahraColors.Purple,
            secondary = SahraColors.Pink,
            background = SahraColors.Background,
            surface = SahraColors.Card,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = SahraColors.Text,
            onSurface = SahraColors.Text
        ),
        content = content
    )
}

// ----------------------------------------------------
// فحص إذن الموقع
// ----------------------------------------------------

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

// ----------------------------------------------------
// الشاشة الرئيسية وحالة التنقل
// ----------------------------------------------------

@Composable
private fun SahraApp() {
    var selectedTab by rememberSaveable {
        mutableStateOf("الرئيسية")
    }

    var unreadMessages by rememberSaveable {
        mutableStateOf(1)
    }

    var notice by remember {
        mutableStateOf<SahraNotice?>(null)
    }

    var selectedUserId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val artwork = remember {
        SahraArtwork.decode()
    }

    val selectedUser = sahraUsers.firstOrNull {
        it.id == selectedUserId
    }

    val openNotice: (String, String) -> Unit = { title, message ->
        notice = SahraNotice(title, message)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SahraColors.Background)
    ) {
        SahraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            SahraHeader()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    "الرئيسية" -> {
                        SahraHome(
                            artwork = artwork,
                            onRooms = {
                                selectedTab = "الغرف"
                            },
                            onMatch = {
                                openNotice(
                                    "التوافق الصوتي",
                                    "دي معاينة لواجهة التوافق الصوتي. " +
                                        "بدء مكالمة حقيقية محتاج ربط خدمة صوت " +
                                        "وصلاحية الميكروفون."
                                )
                            },
                            onQuickAction = { title ->
                                val message = when (title) {
                                    "الأنشطة" ->
                                        "قسم الأنشطة جاهز في الواجهة. " +
                                            "تقدر تربطه لاحقًا بفعاليات التطبيق."

                                    "طريق القمة" ->
                                        "قسم المستويات والترتيب. " +
                                            "البيانات الحالية تجريبية."

                                    "الشحن" ->
                                        "دي واجهة تجريبية للشحن، " +
                                            "ومفيش أي دفع أو خصم مالي."

                                    else ->
                                        "زر LUDO جاهز للربط بشاشة اللعبة. " +
                                            "لعبة LUDO الكاملة مش مضمّنة هنا."
                                }

                                openNotice(title, message)
                            },
                            onUser = { user ->
                                selectedUserId = user.id
                            },
                            onHi = { user ->
                                openNotice(
                                    "Hi إلى ${user.name}",
                                    "تم اختيار رسالة ترحيب إلى ${user.name}. " +
                                        "الإرسال الفعلي محتاج ربط خدمة الرسائل."
                                )
                            },
                            onJoin = { user ->
                                openNotice(
                                    "غرفة ${user.name}",
                                    "تم اختيار الانضمام لغرفة ${user.name}. " +
                                        "الدخول الصوتي الفعلي محتاج خدمة غرف."
                                )
                            }
                        )
                    }

                    "الغرف" -> {
                        SahraRoomsPage(
                            onJoin = { room ->
                                openNotice(
                                    room,
                                    "دي غرفة تجريبية لعرض التصميم. " +
                                        "مفيش اتصال صوتي أو دخول لسيرفر."
                                )
                            }
                        )
                    }

                    "يستكشف" -> {
                        SahraExplorePage(
                            artwork = artwork,
                            onUser = { user ->
                                selectedUserId = user.id
                            },
                            onHi = { user ->
                                openNotice(
                                    "التواصل مع ${user.name}",
                                    "زر التواصل شغال كمعاينة محلية. " +
                                        "الإرسال الحقيقي محتاج خدمة دردشة."
                                )
                            }
                        )
                    }

                    "الرسائل" -> {
                        SahraMessagesPage(
                            artwork = artwork,
                            onOpen = { user ->
                                unreadMessages = 0

                                openNotice(
                                    "رسائل ${user.name}",
                                    "أهلًا بيك في SahraChat ✨\n\n" +
                                        "دي رسالة تجريبية محلية، " +
                                        "مش رسالة جاية من سيرفر."
                                )
                            }
                        )
                    }

                    "أنا" -> {
                        SahraProfilePage(
                            artwork = artwork,
                            onAction = { title ->
                                openNotice(
                                    title,
                                    "القسم ده جاهز في الواجهة للربط " +
                                        "ببيانات حساب المستخدم."
                                )
                            }
                        )
                    }
                }
            }

            SahraBottomNavigation(
                artwork = artwork,
                selectedTab = selectedTab,
                unreadMessages = unreadMessages,
                onSelect = { tab ->
                    selectedTab = tab
                }
            )
        }
    }

    notice?.let { current ->
        AlertDialog(
            onDismissRequest = {
                notice = null
            },
            containerColor = Color(0xFF131222),
            titleContentColor = SahraColors.Text,
            textContentColor = SahraColors.Muted,
            title = {
                Text(
                    text = current.title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = current.message,
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notice = null
                    }
                ) {
                    Text(
                        text = "تمام",
                        color = SahraColors.Pink
                    )
                }
            }
        )
    }

    selectedUser?.let { user ->
        AlertDialog(
            onDismissRequest = {
                selectedUserId = null
            },
            containerColor = Color(0xFF131222),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SahraAvatar(
                        artwork = artwork,
                        index = user.avatar,
                        modifier = Modifier.size(64.dp),
                        ring = SahraColors.Purple
                    )

                    Column {
                        Text(
                            text = user.name,
                            color = SahraColors.Text,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "${user.country}  •  ${user.age} سنة",
                            color = SahraColors.Muted,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (user.photos.isNotEmpty()) {
                        SahraPhotoStrip(
                            artwork = artwork,
                            photos = user.photos,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = user.status.ifBlank {
                            "أهلًا بيك في SahraChat ✨"
                        },
                        color = SahraColors.Text
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedUserId = null

                        openNotice(
                            "بدء الدردشة",
                            "تم اختيار ${user.name}. " +
                                "الدردشة الفعلية محتاجة ربط خدمة الرسائل."
                        )
                    }
                ) {
                    Text(
                        text = "دردشة",
                        color = SahraColors.Pink
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedUserId = null
                    }
                ) {
                    Text(
                        text = "إغلاق",
                        color = SahraColors.Muted
                    )
                }
            }
        )
    }
}

// ----------------------------------------------------
// الخلفية
// ----------------------------------------------------

@Composable
private fun SahraBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0C1022),
                    Color(0xFF040612),
                    Color(0xFF02040D)
                )
            )
        )

        val glowCenter = Offset(
            x = size.width * 0.45f,
            y = 0f
        )

        val glowRadius = size.width * 0.85f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF33457D).copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = glowCenter,
                radius = glowRadius
            ),
            center = glowCenter,
            radius = glowRadius
        )
    }
}

// ----------------------------------------------------
// العنوان
// ----------------------------------------------------

@Composable
private fun SahraHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SahraChat",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )

        Spacer(Modifier.weight(1f))
    }
}

// ----------------------------------------------------
// محتوى الرئيسية
// ----------------------------------------------------

@Composable
private fun SahraHome(
    artwork: List<ImageBitmap>,
    onRooms: () -> Unit,
    onMatch: () -> Unit,
    onQuickAction: (String) -> Unit,
    onUser: (SahraUser) -> Unit,
    onHi: (SahraUser) -> Unit,
    onJoin: (SahraUser) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var locationGranted by remember {
        mutableStateOf(hasLocationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // إعادة فحص الإذن كل ما المستخدم يرجع للتطبيق
    // (بعد ما يفعّله من الإعدادات)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationGranted = hasLocationPermission(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 9.dp,
            end = 9.dp,
            top = 5.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            SahraFeatureCards(
                onRooms = onRooms,
                onMatch = onMatch
            )
        }

        item {
            SahraQuickActions(
                onAction = onQuickAction
            )
        }

        item {
            Text(
                text = "المستخدمين الموصى بهم",
                color = SahraColors.Text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 3.dp,
                        bottom = 1.dp,
                        start = 12.dp,
                        end = 7.dp
                    )
            )
        }

        if (locationGranted) {
            items(
                items = sahraUsers,
                key = { it.id }
            ) { user ->
                SahraUserCard(
                    user = user,
                    artwork = artwork,
                    onUser = {
                        onUser(user)
                    },
                    onAction = {
                        if (user.joinRoom) {
                            onJoin(user)
                        } else {
                            onHi(user)
                        }
                    }
                )
            }
        } else {
            item {
                SahraLocationGate(
                    onActivate = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenSettings = {
                        // مفيش Intent عام يفتح صفحة إذن الموقع مباشرة،
                        // فبنفتح شاشة معلومات التطبيق:
                        // المستخدم يدخل "الأذونات ← الموقع" منها.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.fromParts(
                                    "package",
                                    context.packageName,
                                    null
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// بوابة إذن الموقع
// ----------------------------------------------------

@Composable
private fun SahraLocationGate(
    onActivate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF131426),
                        Color(0xFF090D19)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = SahraColors.CardBorder.copy(alpha = 0.65f),
                shape = shape
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📍",
            fontSize = 30.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "يجب تفعيل الموقع من الإعدادات " +
                "من أجل العثور على أصدقاء",
            color = SahraColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "بعد التفعيل هتلاقي المستخدمين " +
                "الأقرب لك هنا تلقائيًا.",
            color = SahraColors.Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height = 14.dp)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            SahraColors.Purple,
                            SahraColors.Pink
                        )
                    )
                )
                .clickable(onClick = onActivate)
                .padding(
                    horizontal = 34.dp,
                    vertical = 9.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "تفعيل",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "فتح الإعدادات",
            color = SahraColors.Muted,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onOpenSettings)
        )
    }
}

// ----------------------------------------------------
// بطاقتا الغرف والتوافق
// ----------------------------------------------------

@Composable
private fun SahraFeatureCards(
    onRooms: () -> Unit,
    onMatch: () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(108.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF137AE5),
                                Color(0xFF124BB1),
                                Color(0xFF101140)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF76C8FF).copy(alpha = 0.42f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clickable(onClick = onRooms)
            ) {
                BannerStars()

                SahraArt(
                    painter = painterResource(R.drawable.htval),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 3.dp)
                        .size(91.dp)
                        .clip(RoundedCornerShape(19.dp)),
                    contentScale = ContentScale.Fit
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الغرف الصوتية",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(26.dp))

                    SahraPill(
                        text = "انضمام",
                        background = Color(0xFF081443),
                        onClick = onRooms
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(108.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF9027DD),
                                Color(0xFF6420AD),
                                Color(0xFF201046)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFC87AFF).copy(alpha = 0.45f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clickable(onClick = onMatch)
            ) {
                BannerStars()

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp, top = 6.dp)
                        .width(66.dp)
                        .height(39.dp)
                ) {
                    SahraAvatar(
                        artwork = remember { emptyList() },
                        index = 0,
                        ring = Color(0xFFE856FF),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(0.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "التوافق الصوتي",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(4.dp))

                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Rtl
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "2",
                                color = SahraColors.Gold,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "مكالمة اليوم",
                                color = Color(0xFFE9D6FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                SahraPill(
                    text = "اتصال",
                    background = Color(0xFF251044),
                    onClick = onMatch,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}
