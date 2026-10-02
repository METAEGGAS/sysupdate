package com.sys.update2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

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
// أيقونات Vector (شكل SVG) لقسم "أنا"
// ----------------------------------------------------

private object SahraIcons {

    private fun build(
        name: String,
        path: String
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(path),
        fill = SolidColor(Color.White)
    ).build()

    val Crown = build(
        "crown",
        "M3.5 17.5 L5.5 7.5 L9.5 11.5 L12 4.5 L14.5 11.5 L18.5 7.5 L20.5 17.5 Z"
    )

    val Diamond = build(
        "diamond",
        "M12 2.8 L19.5 8.8 L12 21.2 L4.5 8.8 Z"
    )

    val Shirt = build(
        "shirt",
        "M8.5 3 L12 5.6 L15.5 3 L21 6.4 L19 10.2 L16.8 8.8 L16.8 20.5 " +
            "L7.2 20.5 L7.2 8.8 L5 10.2 L3 6.4 Z"
    )

    val PersonAdd = build(
        "person_add",
        "M8.8 11.4 C10.7 11.4 12.3 9.8 12.3 7.9 C12.3 6 10.7 4.4 8.8 4.4 " +
            "C6.9 4.4 5.3 6 5.3 7.9 C5.3 9.8 6.9 11.4 8.8 11.4 Z " +
            "M2.8 19.6 C2.8 16.1 6.4 14.2 8.8 14.2 C11.2 14.2 14.8 16.1 " +
            "14.8 19.6 L14.8 20.4 L2.8 20.4 Z " +
            "M17.6 7.2 L19 7.2 L19 10.4 L22.2 10.4 L22.2 11.8 L19 11.8 " +
            "L19 15 L17.6 15 L17.6 11.8 L14.4 11.8 L14.4 10.4 L17.6 10.4 Z"
    )

    val Chat = build(
        "chat",
        "M3.5 4.5 L20.5 4.5 L20.5 16 L12 16 L7.5 20.5 L7.5 16 L3.5 16 Z"
    )

    val Copy = build(
        "copy",
        "M9 9 L19.5 9 L19.5 20 L9 20 Z " +
            "M4.5 4 L15 4 L15 6 L6.5 6 L6.5 15 L4.5 15 Z"
    )

    val Star = build(
        "star",
        "M12 2.6 L14.9 8.8 L21.4 9.2 L16.3 13.6 L18 20.3 L12 16.7 " +
            "L6 20.3 L7.7 13.6 L2.6 9.2 L9.1 8.8 Z"
    )
}

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
    onQuickAction: (String) -> Unit
) {
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
                artwork = artwork,
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

        item {
            SahraLocationGate()
        }
    }
}

// ----------------------------------------------------
// بوابة تفعيل الموقع بدل قائمة المستخدمين
// ----------------------------------------------------

@Composable
private fun SahraLocationGate() {
    val context = LocalContext.current

    SahraPanel {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SahraColors.Purple.copy(alpha = 0.6f),
                                Color(0xFF151024)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFB79CFF),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "للعثور على أصدقاء بالقرب منك، لازم تفعّل " +
                    "إذن الموقع من إعدادات التطبيق.",
                color = SahraColors.Text,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SahraColors.Purple,
                                SahraColors.Pink
                            )
                        )
                    )
                    .clickable {
                        try {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.fromParts(
                                    "package",
                                    context.packageName,
                                    null
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        }
                    }
                    .padding(
                        horizontal = 26.dp,
                        vertical = 9.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تفعيل الموقع",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ----------------------------------------------------
// بطاقتا الغرف والتوافق
// ----------------------------------------------------

@Composable
private fun SahraFeatureCards(
    artwork: List<ImageBitmap>,
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

                SahraDrawable(
                    resId = R.drawable.htval,
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
                        artwork = artwork,
                        index = 16,
                        ring = Color(0xFFE856FF),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(35.dp)
                    )

                    SahraAvatar(
                        artwork = artwork,
                        index = 17,
                        ring = Color(0xFFE856FF),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(35.dp)
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

@Composable
private fun BannerStars() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(16) { index ->
            val x = (((index * 37 + 17) % 100) / 100f) * size.width
            val y = (((index * 29 + 11) % 100) / 100f) * size.height

            drawCircle(
                color = Color.White.copy(
                    alpha = if (index % 3 == 0) 0.19f else 0.08f
                ),
                radius = if (index % 4 == 0) {
                    1.4.dp.toPx()
                } else {
                    0.7.dp.toPx()
                },
                center = Offset(x, y)
            )
        }
    }
}

// ----------------------------------------------------
// الأنشطة — طريق القمة — الشحن — LUDO
// الأيقونات دلوقتي من مجلد drawable
// ----------------------------------------------------

@Composable
private fun SahraQuickActions(
    onAction: (String) -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SahraQuickTile(
                title = "الأنشطة",
                iconRes = R.drawable.fssk,
                colors = listOf(
                    Color(0xFF754219),
                    Color(0xFF291C16),
                    Color(0xFF101019)
                ),
                notification = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction("الأنشطة")
                }
            )

            SahraQuickTile(
                title = "طريق القمة",
                iconRes = R.drawable.kmah,
                colors = listOf(
                    Color(0xFF1B80BE),
                    Color(0xFF123C70),
                    Color(0xFF09132C)
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction("طريق القمة")
                }
            )

            SahraQuickTile(
                title = "الشحن",
                iconRes = R.drawable.iosk,
                colors = listOf(
                    Color(0xFF9538EB),
                    Color(0xFF5522A3),
                    Color(0xFF201340)
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction("الشحن")
                }
            )

            SahraQuickTile(
                title = "LUDO",
                iconRes = R.drawable.lode,
                colors = listOf(
                    Color(0xFF20A98D),
                    Color(0xFF086954),
                    Color(0xFF072C32)
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction("LUDO")
                }
            )
        }
    }
}

@Composable
private fun SahraQuickTile(
    title: String,
    iconRes: Int,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    notification: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.height(87.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.verticalGradient(colors)
                )
                .border(
                    width = 1.dp,
                    color = colors.first().copy(alpha = 0.75f),
                    shape = RoundedCornerShape(13.dp)
                )
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        SahraDrawable(
            resId = iconRes,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(57.dp)
                .clip(RoundedCornerShape(15.dp))
                .clickable(onClick = onClick),
            contentScale = ContentScale.Fit
        )

        if (notification) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 1.dp)
                    .size(10.dp)
                    .background(
                        color = Color(0xFFFF4C5B),
                        shape = CircleShape
                    )
            )
        }
    }
}

// ----------------------------------------------------
// بطاقة المستخدم
// ----------------------------------------------------

@Composable
private fun SahraUserCard(
    user: SahraUser,
    artwork: List<ImageBitmap>,
    onUser: () -> Unit,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF10111B),
                        Color(0xFF090D19)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = SahraColors.CardBorder.copy(alpha = 0.65f),
                shape = shape
            )
            .padding(
                horizontal = 8.dp,
                vertical = 12.dp
            )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.joinRoom) {
                    SahraPill(
                        text = "انضمام",
                        background = Color(0xFF30215D),
                        onClick = onAction,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                } else {
                    SahraHiButton(
                        onClick = onAction
                    )
                }

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 6.dp)
                )

                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 195.dp)
                            .clickable(onClick = onUser),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = user.name,
                            color = SahraColors.Text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(5.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SahraCountryBadge(
                                country = user.country
                            )

                            SahraAgeBadge(
                                age = user.age
                            )

                            if (user.vip) {
                                SahraVipBadge()
                            }
                        }

                        if (user.status.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = user.status,
                                color = SahraColors.Muted,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                SahraAvatar(
                    artwork = artwork,
                    index = user.avatar,
                    modifier = Modifier
                        .size(57.dp)
                        .clickable(onClick = onUser),
                    ring = if (user.vip) {
                        Color(0xFFD6D1BD)
                    } else {
                        Color(0xFF503888)
                    }
                )
            }

            if (user.photos.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))

                SahraPhotoStrip(
                    artwork = artwork,
                    photos = user.photos,
                    modifier = Modifier
                        .fillMaxWidth(0.84f),
                    onClick = onUser
                )
            }
        }
    }
}

@Composable
private fun SahraHiButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(start = 3.dp)
            .width(48.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF2B173D),
                        Color(0xFF171221)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hi",
            color = Color(0xFFFF69CF),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
private fun SahraAgeBadge(
    age: Int
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SahraColors.Pink)
            .padding(
                horizontal = 6.dp,
                vertical = 1.dp
            )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Text(
                text = "♀ $age",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SahraCountryBadge(
    country: String
) {
    Text(
        text = country,
        fontSize = 17.sp,
        maxLines = 1
    )
}

@Composable
private fun SahraVipBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF67522B),
                        Color(0xFF25211B)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFFBBA76B),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(
                horizontal = 6.dp,
                vertical = 1.dp
            )
    ) {
        Text(
            text = "♛ VIP1",
            color = Color(0xFFFFF1C7),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------
// الصور والأفاتار
// ----------------------------------------------------

@Composable
private fun SahraAvatar(
    artwork: List<ImageBitmap>,
    index: Int,
    modifier: Modifier = Modifier,
    ring: Color = SahraColors.Purple
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF181529))
            .border(
                width = 2.dp,
                color = ring,
                shape = CircleShape
            )
            .padding(3.dp)
    ) {
        SahraArt(
            artwork = artwork,
            index = index,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun SahraArt(
    artwork: List<ImageBitmap>,
    index: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Image(
        bitmap = artwork[index],
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun SahraDrawable(
    resId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun SahraPhotoStrip(
    artwork: List<ImageBitmap>,
    photos: List<Int>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            photos.forEach { index ->
                val imageModifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(7.dp))

                SahraArt(
                    artwork = artwork,
                    index = index,
                    modifier = if (onClick != null) {
                        imageModifier.clickable(onClick = onClick)
                    } else {
                        imageModifier
                    }
                )
            }
        }
    }
}

@Composable
private fun SahraPill(
    text: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(
                horizontal = 13.dp,
                vertical = 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

// ----------------------------------------------------
// شريط التنقل السفلي
// ----------------------------------------------------

@Composable
private fun SahraBottomNavigation(
    artwork: List<ImageBitmap>,
    selectedTab: String,
    unreadMessages: Int,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SahraColors.Navigation)
            .padding(
                top = 5.dp,
                bottom = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sahraTabs.forEach { tab ->
            val selected = tab.title == selectedTab

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelect(tab.title)
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(39.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(
                                            SahraColors.Purple.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    SahraArt(
                        artwork = artwork,
                        index = tab.artIndex,
                        modifier = Modifier
                            .size(
                                if (selected) 35.dp else 31.dp
                            )
                            .clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Fit
                    )

                    if (
                        tab.title == "الرسائل" &&
                        unreadMessages > 0
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(
                                    color = Color(0xFFFF4A58),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadMessages.toString(),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = tab.title,
                    color = if (selected) {
                        Color.White
                    } else {
                        Color(0xFF777A91)
                    },
                    fontSize = 11.sp,
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    maxLines = 1
                )
            }
        }
    }
}

// ----------------------------------------------------
// صفحة الغرف — بيانات تجريبية
// ----------------------------------------------------

@Composable
private fun SahraRoomsPage(
    onJoin: (String) -> Unit
) {
    val rooms = listOf(
        "سهرة وأصحاب ✨",
        "غرفة الموسيقى 🎵",
        "دردشة عربية 💜",
        "قهوة آخر الليل ☕"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SahraPageTitle(
                title = "الغرف الصوتية",
                subtitle = "غرف تجريبية لمعاينة التصميم"
            )
        }

        items(rooms) { room ->
            SahraPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SahraDrawable(
                        resId = R.drawable.htval,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = room,
                            color = SahraColors.Text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "معاينة محلية • بدون اتصال صوتي",
                            color = SahraColors.Muted,
                            fontSize = 11.sp
                        )
                    }

                    SahraPill(
                        text = "انضمام",
                        background = Color(0xFF40216B),
                        onClick = {
                            onJoin(room)
                        }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// صفحة الاستكشاف
// ----------------------------------------------------

@Composable
private fun SahraExplorePage(
    artwork: List<ImageBitmap>,
    onUser: (SahraUser) -> Unit,
    onHi: (SahraUser) -> Unit
) {
    var filter by rememberSaveable {
        mutableStateOf("الكل")
    }

    val filters = listOf(
        "الكل",
        "صور",
        "غرف"
    )

    val visibleUsers = when (filter) {
        "صور" -> sahraUsers.filter {
            it.photos.isNotEmpty()
        }

        "غرف" -> sahraUsers.filter {
            it.joinRoom
        }

        else -> sahraUsers
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SahraPageTitle(
                title = "يستكشف",
                subtitle = "اكتشف المستخدمين والغرف"
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { label ->
                    SahraPill(
                        text = label,
                        background = if (label == filter) {
                            SahraColors.Purple
                        } else {
                            Color(0xFF1D1930)
                        },
                        onClick = {
                            filter = label
                        }
                    )
                }
            }
        }

        items(
            items = visibleUsers,
            key = { it.id }
        ) { user ->
            SahraUserCard(
                user = user,
                artwork = artwork,
                onUser = {
                    onUser(user)
                },
                onAction = {
                    onHi(user)
                }
            )
        }
    }
}

// ----------------------------------------------------
// صفحة الرسائل
// ----------------------------------------------------

@Composable
private fun SahraMessagesPage(
    artwork: List<ImageBitmap>,
    onOpen: (SahraUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SahraPageTitle(
                title = "الرسائل",
                subtitle = "محادثات تجريبية"
            )
        }

        items(
            items = sahraUsers,
            key = { it.id }
        ) { user ->
            SahraPanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpen(user)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SahraAvatar(
                        artwork = artwork,
                        index = user.avatar,
                        modifier = Modifier.size(54.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = user.name,
                            color = SahraColors.Text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "أهلًا بيك في SahraChat ✨",
                            color = SahraColors.Muted,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "عرض",
                        color = SahraColors.Pink,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// صفحة الحساب — تصميم مطابق للصورة المرجعية
// ----------------------------------------------------

@Composable
private fun SahraProfilePage(
    artwork: List<ImageBitmap>,
    onAction: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 4.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // سهم الرجوع مع النقطة الحمرا (شكل زي الصورة)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "‹",
                        color = Color.White,
                        fontSize = 30.sp
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp)
                            .size(9.dp)
                            .background(
                                color = Color(0xFFFF4C5B),
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // الهيدر: الأفاتار + الاسم + المستوى + ID
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SahraAvatar(
                    artwork = artwork,
                    index = 5,
                    modifier = Modifier.size(84.dp),
                    ring = Color(0xFFF5A45C)
                )

                Column {
                    Text(
                        text = "ابو شحاطة",
                        color = SahraColors.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(7.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF4FC3FF),
                                            Color(0xFF2E7CE6)
                                        )
                                    )
                                )
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = SahraIcons.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )

                                Text(
                                    text = "20",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "🇹🇷",
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(Modifier.height(7.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                clipboard.setText(
                                    AnnotatedString("7905645")
                                )
                            }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = SahraIcons.Copy,
                            contentDescription = "نسخ المعرف",
                            tint = SahraColors.Muted,
                            modifier = Modifier.size(13.dp)
                        )

                        Text(
                            text = "ID: 7905645",
                            color = SahraColors.Muted,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // الإحصائيات: الأصدقاء / يتابع / المتابعون
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                SahraProfileStatItem(
                    value = "0",
                    label = "الأصدقاء",
                    modifier = Modifier.weight(1f)
                )

                SahraProfileStatItem(
                    value = "7",
                    label = "يتابع",
                    modifier = Modifier.weight(1f)
                )

                SahraProfileStatItem(
                    value = "0",
                    label = "المتابعون",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // المحفظة
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF3B2A6E),
                                Color(0xFF241B47)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF5A4A96).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onAction("المحفظة")
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 15.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المحفظة",
                    color = SahraColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = SahraIcons.Diamond,
                    contentDescription = null,
                    tint = Color(0xFF4FD8FF),
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "0",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "‹",
                    color = SahraColors.Muted,
                    fontSize = 24.sp
                )
            }
        }

        // مقتنياتي / المتجر / المستوى / VIP
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SahraProfileQuickItem(
                    icon = SahraIcons.Diamond,
                    label = "VIP",
                    tint = Color(0xFF9FD8FF),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction("VIP")
                    }
                )

                SahraProfileQuickItem(
                    icon = SahraIcons.Crown,
                    label = "المستوى",
                    tint = Color(0xFF7FB6FF),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction("المستوى")
                    }
                )

                SahraProfileQuickItem(
                    icon = Icons.Default.ShoppingCart,
                    label = "المتجر",
                    tint = Color(0xFF7FC4FF),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction("المتجر")
                    }
                )

                SahraProfileQuickItem(
                    icon = SahraIcons.Shirt,
                    label = "مقتنياتي",
                    tint = Color(0xFF8FD0FF),
                    badge = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction("مقتنياتي")
                    }
                )
            }
        }

        // القائمة: طريق القمة / دعوة الأصدقاء / الأسئلة الشائعة / الإعدادات
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SahraColors.Card)
                    .border(
                        width = 1.dp,
                        color = SahraColors.CardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                SahraProfileMenuItem(
                    icon = SahraIcons.Star,
                    title = "طريق القمة",
                    badge = "احصل على مكافأة مجانية",
                    onClick = {
                        onAction("طريق القمة")
                    }
                )

                SahraMenuDivider()

                SahraProfileMenuItem(
                    icon = SahraIcons.PersonAdd,
                    title = "دعوة الأصدقاء",
                    onClick = {
                        onAction("دعوة الأصدقاء")
                    }
                )

                SahraMenuDivider()

                SahraProfileMenuItem(
                    icon = SahraIcons.Chat,
                    title = "الأسئلة الشائعة",
                    onClick = {
                        onAction("الأسئلة الشائعة")
                    }
                )

                SahraMenuDivider()

                SahraProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "الإعدادات",
                    onClick = {
                        onAction("الإعدادات")
                    }
                )
            }
        }
    }
}

@Composable
private fun SahraProfileStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = SahraColors.Text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = label,
            color = SahraColors.Muted,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SahraProfileQuickItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141A2E))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF27314F),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (badge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(9.dp)
                        .background(
                            color = Color(0xFFFF4C5B),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            color = SahraColors.Text,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SahraProfileMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 14.dp,
                vertical = 15.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8FD0FF),
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            color = SahraColors.Text,
            fontSize = 14.sp,
            maxLines = 1
        )

        Spacer(Modifier.weight(1f))

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF5A3C),
                                Color(0xFFE8384F)
                            )
                        )
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 3.dp
                    )
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = "‹",
            color = SahraColors.Muted,
            fontSize = 24.sp
        )
    }
}

@Composable
private fun SahraMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(
                SahraColors.CardBorder.copy(alpha = 0.6f)
            )
    )
}

@Composable
private fun SahraPageTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(
            top = 6.dp,
            bottom = 8.dp
        )
    ) {
        Text(
            text = title,
            color = SahraColors.Text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = subtitle,
            color = SahraColors.Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SahraPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SahraColors.Card)
            .border(
                width = 1.dp,
                color = SahraColors.CardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        content = content
    )
}

// ----------------------------------------------------
// الصور والأيقونات المقتطعة من الصورة المرجعية
//
// الصورة المضمّنة عبارة عن شبكة:
// 5 أعمدة × 5 صفوف.
// حجم كل عنصر: 48 × 48.
//
// لا تحتاج drawable أو الإنترنت أو مكتبة صور.
// ----------------------------------------------------

private object SahraArtwork {

    fun decode(): List<ImageBitmap> {
        val bytes = Base64.decode(
            EMBEDDED_ART,
            Base64.DEFAULT
        )

        val atlas = requireNotNull(
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )
        ) {
            "تعذر قراءة الصور المضمّنة. انسخ النص بالكامل بدون تغييره."
        }

        return List(23) { index ->
            Bitmap.createBitmap(
                atlas,
                (index % 5) * 48,
                (index / 5) * 48,
                48,
                48
            ).asImageBitmap()
        }
    }

    private const val EMBEDDED_ART = """
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
YR7AAAAAAAAA
"""
}
