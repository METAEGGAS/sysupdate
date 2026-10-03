// language: Kotlin, file: HomeActivity.kt

package com.sys.update2

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private data class SahraNotice(
    val title: String,
    val message: String
)

private data class SahraTab(
    val title: String,
    val remoteName: String
)

private data class SahraRoom(
    val id: String,
    val name: String,
    val color: Color
)

private val sahraRooms = listOf(
    SahraRoom("r1", "سهرة وأصحاب ✨", Color(0xFF6A4CFF)),
    SahraRoom("r2", "غرفة الموسيقى 🎵", Color(0xFF00A8A8)),
    SahraRoom("r3", "دردشة عربية 💜", Color(0xFFB14CFF)),
    SahraRoom("r4", "قهوة آخر الليل ☕", Color(0xFFA87A3D))
)

// أيقونات التنقل السفلي — من GitHub
private val sahraTabs = listOf(
    SahraTab("الرئيسية", "tab_home"),
    SahraTab("الغرف", "tab_rooms"),
    SahraTab("يستكشف", "tab_explore"),
    SahraTab("الرسائل", "tab_messages"),
    SahraTab("أنا", "tab_profile")
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
// SahraApp
// ----------------------------------------------------

@Composable
private fun SahraApp() {
    val context = LocalContext.current

    var selectedTab by rememberSaveable {
        mutableStateOf("الرئيسية")
    }

    var unreadMessages by rememberSaveable {
        mutableStateOf(0)
    }

    var notice by remember {
        mutableStateOf<SahraNotice?>(null)
    }

    val openNotice: (String, String) -> Unit = { title, message ->
        notice = SahraNotice(title, message)
    }

    val openLocationSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                openNotice(
                    "تعذّر فتح الإعدادات",
                    "لم نتمكن من فتح إعدادات الموقع على هذا الجهاز."
                )
            }
        }
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
                            onRooms = { selectedTab = "الغرف" },
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
                            onOpenLocationSettings = openLocationSettings
                        )
                    }

                    "الغرف" -> {
                        SahraRoomsPage(
                            onJoin = { room ->
                                openNotice(
                                    room.name,
                                    "دي غرفة تجريبية لعرض التصميم. " +
                                        "مفيش اتصال صوتي أو دخول لسيرفر."
                                )
                            }
                        )
                    }

                    "يستكشف" -> {
                        SahraPlaceholderPage(
                            title = "يستكشف",
                            subtitle = "قيد الإنشاء",
                            message = "هذا القسم قيد الإنشاء حاليًا."
                        )
                    }

                    "الرسائل" -> {
                        SahraPlaceholderPage(
                            title = "الرسائل",
                            subtitle = "قيد الإنشاء",
                            message = "قسم الرسائل قيد الإنشاء حاليًا."
                        )
                    }

                    "أنا" -> {
                        SahraProfilePage(
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
                selectedTab = selectedTab,
                unreadMessages = unreadMessages,
                onSelect = { tab -> selectedTab = tab }
            )
        }
    }

    notice?.let { current ->
        AlertDialog(
            onDismissRequest = { notice = null },
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
                TextButton(onClick = { notice = null }) {
                    Text(
                        text = "تمام",
                        color = SahraColors.Pink
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
    Canvas(modifier = Modifier.fillMaxSize()) {
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
    onRooms: () -> Unit,
    onMatch: () -> Unit,
    onQuickAction: (String) -> Unit,
    onOpenLocationSettings: () -> Unit
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
                onRooms = onRooms,
                onMatch = onMatch
            )
        }

        item {
            SahraQuickActions(onAction = onQuickAction)
        }

        // قسم المستخدمين الموصى بهم + تفعيل الموقع
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // العنوان كما هو
                Text(
                    text = "المستخدمين الموصى بهم",
                    color = SahraColors.Text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        top = 3.dp,
                        bottom = 10.dp
                    )
                )

                // نص أبيض: يجب تفعيل الموقع
                Text(
                    text = "يجب تفعيل الموقع",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                // نص توضيح
                Text(
                    text = "لأفضل تجربة، فعّل خدمة الموقع من إعدادات الجهاز.",
                    color = SahraColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                // زر فتح الإعدادات
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF6A4CFF),
                                    Color(0xFFB14CFF)
                                )
                            )
                        )
                        .clickable(onClick = onOpenLocationSettings)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "فتح الإعدادات",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
            // بطاقة الغرف الصوتية — من GitHub
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

                RemoteImage(
                    name = "banner_rooms",
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

            // بطاقة التوافق — من GitHub: tatabk1, tatabk2
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
                    RemoteImage(
                        name = "tatabk1",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(35.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    RemoteImage(
                        name = "tatabk2",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(35.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
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
    Canvas(modifier = Modifier.fillMaxSize()) {
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
// الأيقونات الأربع من GitHub
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
                remoteIconName = "icon_activities",
                colors = listOf(
                    Color(0xFF754219),
                    Color(0xFF291C16),
                    Color(0xFF101019)
                ),
                notification = true,
                modifier = Modifier.weight(1f),
                onClick = { onAction("الأنشطة") }
            )

            SahraQuickTile(
                title = "طريق القمة",
                remoteIconName = "icon_road_top",
                colors = listOf(
                    Color(0xFF1B80BE),
                    Color(0xFF123C70),
                    Color(0xFF09132C)
                ),
                modifier = Modifier.weight(1f),
                onClick = { onAction("طريق القمة") }
            )

            SahraQuickTile(
                title = "الشحن",
                remoteIconName = "icon_recharge",
                colors = listOf(
                    Color(0xFF9538EB),
                    Color(0xFF5522A3),
                    Color(0xFF201340)
                ),
                modifier = Modifier.weight(1f),
                onClick = { onAction("الشحن") }
            )

            SahraQuickTile(
                title = "LUDO",
                remoteIconName = "icon_ludo",
                colors = listOf(
                    Color(0xFF20A98D),
                    Color(0xFF086954),
                    Color(0xFF072C32)
                ),
                modifier = Modifier.weight(1f),
                onClick = { onAction("LUDO") }
            )
        }
    }
}

@Composable
private fun SahraQuickTile(
    title: String,
    remoteIconName: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    notification: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier = modifier.height(87.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.verticalGradient(colors))
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

        RemoteImage(
            name = remoteIconName,
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
// أفاتار برمجي (يُستخدم في "أنا")
// ----------------------------------------------------

@Composable
private fun ProgrammaticAvatar(
    modifier: Modifier = Modifier,
    ring: Color = SahraColors.Purple,
    fill: Color = Color(0xFF181529)
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 2.dp,
                color = ring,
                shape = CircleShape
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = ring.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
    }
}

// ----------------------------------------------------
// صفحة الغرف
// ----------------------------------------------------

@Composable
private fun SahraRoomsPage(
    onJoin: (SahraRoom) -> Unit
) {
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

        items(sahraRooms, key = { it.id }) { room ->
            SahraPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        room.color,
                                        room.color.copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.name.take(1),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room.name,
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
                        onClick = { onJoin(room) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// صفحة بديلة
// ----------------------------------------------------

@Composable
private fun SahraPlaceholderPage(
    title: String,
    subtitle: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(SahraColors.Purple.copy(alpha = 0.15f))
                .border(2.dp, SahraColors.Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = SahraColors.Purple,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = title,
            color = SahraColors.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = SahraColors.Muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = message,
            color = SahraColors.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ----------------------------------------------------
// صفحة الحساب
// ----------------------------------------------------

@Composable
private fun SahraProfilePage(
    onAction: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SahraPageTitle(
                title = "أنا",
                subtitle = "معاينة الملف الشخصي"
            )
        }

        item {
            SahraPanel {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProgrammaticAvatar(
                        modifier = Modifier.size(82.dp),
                        ring = SahraColors.Pink
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "مستخدم SahraChat",
                        color = SahraColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "حساب تجريبي • غير مرتبط بتسجيل الدخول",
                        color = SahraColors.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SahraProfileStat(
                    title = "متابعين",
                    value = "0",
                    modifier = Modifier.weight(1f)
                )
                SahraProfileStat(
                    title = "متابَعين",
                    value = "0",
                    modifier = Modifier.weight(1f)
                )
                SahraProfileStat(
                    title = "أصدقاء",
                    value = "0",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        items(
            listOf(
                "تعديل الملف الشخصي",
                "المحفظة والشحن",
                "الإعدادات",
                "المساعدة"
            )
        ) { title ->
            SahraPanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(title) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = SahraColors.Text,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "‹",
                        color = SahraColors.Muted,
                        fontSize = 25.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SahraProfileStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SahraColors.Card)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = SahraColors.Pink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = title,
            color = SahraColors.Muted,
            fontSize = 12.sp
        )
    }
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
// شريط التنقل السفلي
// ----------------------------------------------------

@Composable
private fun SahraBottomNavigation(
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
                    .clickable { onSelect(tab.title) }
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

                    RemoteImage(
                        name = tab.remoteName,
                        contentDescription = tab.title,
                        modifier = Modifier
                            .size(if (selected) 35.dp else 31.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Fit
                    )

                    if (tab.title == "الرسائل" && unreadMessages > 0) {
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
                    color = if (selected) Color.White else Color(0xFF777A91),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

// ----------------------------------------------------
// SahraPill
// ----------------------------------------------------

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
