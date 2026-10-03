package com.sys.update2

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

// ═══════════════════════════════════════════
// الألوان
// ═══════════════════════════════════════════
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

private data class SahraNotice(val title: String, val message: String)

// ═══════════════════════════════════════════
// Activity
// ═══════════════════════════════════════════
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

// ═══════════════════════════════════════════
// Theme
// ═══════════════════════════════════════════
@Composable
private fun SahraTheme(content: @Composable () -> Unit) {
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

// ═══════════════════════════════════════════
// الشاشة الرئيسية
// ═══════════════════════════════════════════
@Composable
private fun SahraApp() {
    var notice by remember { mutableStateOf<SahraNotice?>(null) }

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
                        onRooms = {
                            openNotice(
                                "الغرف الصوتية",
                                "دي معاينة لواجهة الغرف. " +
                                    "الانضمام الفعلي محتاج خدمة صوت."
                            )
                        },
                        onMatch = {
                            openNotice(
                                "التوافق الصوتي",
                                "دي معاينة لواجهة التوافق الصوتي."
                            )
                        }
                    )
                }

                item {
                    SahraQuickActions(
                        onAction = { title ->
                            val message = when (title) {
                                "الأنشطة" ->
                                    "قسم الأنشطة جاهز في الواجهة."
                                "طريق القمة" ->
                                    "قسم المستويات والترتيب."
                                "الشحن" ->
                                    "دي واجهة تجريبية للشحن."
                                else ->
                                    "زر LUDO جاهز للربط."
                            }
                            openNotice(title, message)
                        }
                    )
                }

                item {
                    val context = LocalContext.current
                    var locationGranted by remember {
                        mutableStateOf(hasLocationPermission(context))
                    }

                    val lifecycleOwner = rememberLifecycleOwner()

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                locationGranted = hasLocationPermission(context)
                            }
                        }
                        lifecycleOwner?.lifecycle?.addObserver(observer)
                        onDispose {
                            lifecycleOwner?.lifecycle?.removeObserver(observer)
                        }
                    }

                    if (!locationGranted) {
                        SahraLocationNotice(
                            onEnable = { openAppSettings(context) }
                        )
                    } else {
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
                }

                if (hasLocationPermission(LocalContext.current)) {
                    items(
                        items = listOf(
                            Triple("🪽 اسيل", "🇸🇾  •  18 سنة", "♛ VIP1"),
                            Triple("🌙Sàra🌙", "🇸🇾  •  23 سنة", ""),
                            Triple("💄 رورو", "🇹🇷  •  18 سنة", "")
                        ),
                        key = { it.first }
                    ) { (name, info, badge) ->
                        SahraSimpleUserCard(
                            name = name,
                            info = info,
                            badge = badge
                        )
                    }
                }
            }
        }

        notice?.let { current ->
            AlertDialog(
                onDismissRequest = { notice = null },
                containerColor = Color(0xFF131222),
                titleContentColor = SahraColors.Text,
                textContentColor = SahraColors.Muted,
                title = {
                    Text(current.title, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(current.message, lineHeight = 24.sp)
                },
                confirmButton = {
                    TextButton(onClick = { notice = null }) {
                        Text("تمام", color = SahraColors.Pink)
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════
// الخلفية
// ═══════════════════════════════════════════
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

        val glowCenter = Offset(x = size.width * 0.45f, y = 0f)
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

// ═══════════════════════════════════════════
// العنوان
// ═══════════════════════════════════════════
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

// ═══════════════════════════════════════════
// بطاقتا الغرف والتوافق
// ═══════════════════════════════════════════
@Composable
private fun SahraFeatureCards(onRooms: () -> Unit, onMatch: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // بطاقة الغرف الصوتية
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

                Image(
                    painter = painterResource(id = R.drawable.htval),
                    contentDescription = "الغرف الصوتية",
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

            // بطاقة التوافق الصوتي
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

// ═══════════════════════════════════════════
// الأزرار السريعة — الأنشطة / طريق القمة / الشحن / LUDO
// ═══════════════════════════════════════════
@Composable
private fun SahraQuickActions(onAction: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                onClick = { onAction("الأنشطة") }
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
                onClick = { onAction("طريق القمة") }
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
                onClick = { onAction("الشحن") }
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
                onClick = { onAction("LUDO") }
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

        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
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

// ═══════════════════════════════════════════
// SahraPill
// ═══════════════════════════════════════════
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
            .padding(horizontal = 13.dp, vertical = 5.dp),
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

// ═══════════════════════════════════════════
// بطاقة مستخدم بسيطة
// ═══════════════════════════════════════════
@Composable
private fun SahraSimpleUserCard(
    name: String,
    info: String,
    badge: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
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
                shape = RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(57.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(SahraColors.Purple, SahraColors.Pink)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = SahraColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = info,
                color = SahraColors.Muted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        if (badge.isNotBlank()) {
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
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    color = Color(0xFFFFF1C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// شاشة تفعيل الموقع
// ═══════════════════════════════════════════
@Composable
private fun SahraLocationNotice(onEnable: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF141A33),
                        Color(0xFF0B0E1C)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2A3560),
                shape = shape
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF3C6BFF).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF7FB4FF),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "يجب تفعيل الموقع",
            color = SahraColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "للعثور على أصدقاء قريبين منك، لازم تسمح " +
                "لإذن الموقع من إعدادات التطبيق ← الأذونات ← الموقع. " +
                "اضغط زر \"تفعيل\" وبتفتح لك صفحة إعدادات " +
                "التطبيق مباشرة.",
            color = SahraColors.Muted,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SahraColors.Purple,
                            SahraColors.Pink
                        )
                    )
                )
                .clickable(onClick = onEnable)
                .padding(horizontal = 34.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = "تفعيل",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════
// أدوات الأذونات
// ═══════════════════════════════════════════
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

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(intent)
    }
}

@Composable
private fun rememberLifecycleOwner(): LifecycleOwner? {
    val context = LocalContext.current

    return remember(context) {
        generateSequence(context) { current ->
            (current as? ContextWrapper)?.baseContext
        }.filterIsInstance<LifecycleOwner>().firstOrNull()
    }
}
