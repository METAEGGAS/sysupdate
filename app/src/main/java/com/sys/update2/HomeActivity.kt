package com.sys.update2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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

// ----------------------------------------------------
// النماذج — الصور أصبحت مراجع drawable من R
// ----------------------------------------------------

private data class SahraUser(
    val id: String,
    val name: String,
    val age: Int,
    val country: String,
    @DrawableRes val avatar: Int,
    @DrawableRes val photos: List<Int> = emptyList(),
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
    @DrawableRes val iconRes: Int
)

// ----------------------------------------------------
// البيانات — بدّل R.drawable بالموارد الفعلية بتاعتك
// ----------------------------------------------------

private val sahraUsers = listOf(
    SahraUser(
        id = "aseel",
        name = "🪽 ❶❶ اسيل",
        age = 18,
        country = "🇸🇾",
        avatar = R.drawable.avatar_aseel,
        photos = listOf(
            R.drawable.photo_aseel_1,
            R.drawable.photo_aseel_2,
            R.drawable.photo_aseel_3,
            R.drawable.photo_aseel_4
        ),
        vip = true
    ),
    SahraUser(
        id = "sara",
        name = "🌙Sàra🌙",
        age = 23,
        country = "🇸🇾",
        avatar = R.drawable.avatar_sara,
        status = "💚⚔313⚔💚",
        joinRoom = true
    ),
    SahraUser(
        id = "roro",
        name = "💄 ›🅂🅁 رورو",
        age = 18,
        country = "🇹🇷",
        avatar = R.drawable.avatar_roro,
        photos = listOf(
            R.drawable.photo_roro_1,
            R.drawable.photo_roro_2,
            R.drawable.photo_roro_3,
            R.drawable.photo_roro_4
        ),
        status = "🙂🤭🌚"
    )
)

private val sahraTabs = listOf(
    SahraTab("الرئيسية", R.drawable.ic_tab_home),
    SahraTab("الغرف", R.drawable.ic_tab_rooms),
    SahraTab("يستكشف", R.drawable.ic_tab_explore),
    SahraTab("الرسائل", R.drawable.ic_tab_messages),
    SahraTab("أنا", R.drawable.ic_tab_profile)
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
                        res = user.avatar,
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
    onRooms: () -> Unit,
    onMatch: () -> Unit,
    onQuickAction: (String) -> Unit,
    onUser: (SahraUser) -> Unit,
    onHi: (SahraUser) -> Unit,
    onJoin: (SahraUser) -> Unit
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

        items(
            items = sahraUsers,
            key = { it.id }
        ) { user ->
            SahraUserCard(
                user = user,
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
                    res = R.drawable.ic_banner_rooms,
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
                        res = R.drawable.avatar_match_1,
                        ring = Color(0xFFE856FF),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(35.dp)
                    )

                    SahraAvatar(
                        res = R.drawable.avatar_match_2,
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
                artRes = R.drawable.ic_action_activities,
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
                artRes = R.drawable.ic_action_road_top,
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
                artRes = R.drawable.ic_action_recharge,
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
                artRes = R.drawable.ic_action_ludo,
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
    @DrawableRes artRes: Int,
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

        SahraArt(
            res = artRes,
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
                    res = user.avatar,
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
// الصور والأفاتار — تحميل مباشر من drawable
// ----------------------------------------------------

@Composable
private fun SahraAvatar(
    @DrawableRes res: Int,
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
            res = res,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun SahraArt(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Image(
        painter = painterResource(id = res),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun SahraPhotoStrip(
    @DrawableRes photos: List<Int>,
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
            photos.forEach { res ->
                val imageModifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(7.dp))

                SahraArt(
                    res = res,
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
                        res = tab.iconRes,
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
                    SahraArt(
                        res = R.drawable.ic_banner_rooms,
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
                        res = user.avatar,
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
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SahraColors.Purple,
                                        SahraColors.Pink
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "أنا",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                        .clickable {
                            onAction(title)
                        },
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
