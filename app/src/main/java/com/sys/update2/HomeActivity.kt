package com.sys.update2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage

private object SahraColors {
    val Background = Color(0xFF030510)
    val Card = Color(0xFF0D101B)
    val CardBorder = Color(0xFF171B29)
    val Text = Color(0xFFF8F7FC)
    val Muted = Color(0xFF9393A7)
    val Purple = Color(0xFF7932DC)
    val Pink = Color(0xFFFF63A4)
    val Navigation = Color(0xFF001014)
    val Up = Color(0xFF2ECC71)
    val Down = Color(0xFFE74C3C)
}

private object VoiceColors {
    val BackgroundTop = Color(0xFF0A1F1A)
    val BackgroundMid = Color(0xFF051210)
    val BackgroundBottom = Color(0xFF020806)
    val CardGray = Color(0xFF2A3532)
    val TextWhite = Color(0xFFF2F5F3)
    val TextMuted = Color(0xFF8BA39D)
}

private data class SahraNotice(val title: String, val message: String)
private data class SahraTab(val title: String, val remoteName: String)
private data class SahraRoom(val id: String, val name: String, val color: Color)
private data class CryptoCoin(
    val symbol: String, val price: String, val change: String,
    val up: Boolean, val color: Color, val iconUrl: String? = null
)

private val sahraRooms = listOf(
    SahraRoom("r1", "سهرة وأصحاب ✨", Color(0xFF6A4CFF)),
    SahraRoom("r2", "غرفة الموسيقى 🎵", Color(0xFF00A8A8)),
    SahraRoom("r3", "دردشة عربية 💜", Color(0xFFB14CFF)),
    SahraRoom("r4", "قهوة آخر الليل ☕", Color(0xFFA87A3D))
)

private val sahraTabs = listOf(
    SahraTab("الرئيسية", "tab_home"),
    SahraTab("الغرف", "tab_rooms"),
    SahraTab("يستكشف", "tab_explore"),
    SahraTab("الرسائل", "tab_messages"),
    SahraTab("أنا", "tab_profile")
)

private const val GH = "https://cdn.jsdelivr.net/gh/METAEGGAS/special-octo-goggleajajjajws@main/images/"
private fun ghIcon(name: String): String = "$GH$name.jpeg"

private val cryptoCoins = listOf(
    CryptoCoin("BTC", "$68,803.74", "↑ 2.37%", true, Color(0xFFF7931A),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/5968/5968260.png"),
    CryptoCoin("ETH", "$2,018.92", "↓ 1.12%", false, Color(0xFF627EEA),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/14446/14446160.png"),
    CryptoCoin("BNB", "$585.40", "↑ 1.68%", true, Color(0xFFF3BA2F),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/12114/12114208.png"),
    CryptoCoin("SOL", "$162.75", "↓ 0.84%", false, Color(0xFF9945FF),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/14446/14446237.png"),
    CryptoCoin("USDC", "$1.00", "↑ 0.01%", true, Color(0xFF2775CA),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/14446/14446285.png"),
    CryptoCoin("TRON", "$0.1620", "↓ 0.45%", false, Color(0xFFEB0029),
        iconUrl = "https://cdn-icons-png.flaticon.com/128/14446/14446268.png"),
    CryptoCoin("TREE", "$201.12", "↑ 2.20%", true, Color(0xFF2ECC71), iconUrl = ghIcon("TREE")),
    CryptoCoin("A2Z", "$255.10", "↓ 0.40%", false, Color(0xFF20A98D), iconUrl = ghIcon("A2Z")),
    CryptoCoin("TOWNS", "$236.11", "↑ 2.90%", true, Color(0xFF3D86F6), iconUrl = ghIcon("TOWNS")),
    CryptoCoin("PROVE", "$73.17", "↑ 2.50%", true, Color(0xFFB14CFF), iconUrl = ghIcon("PROVE")),
    CryptoCoin("BFUSD", "$1.00", "↑ 0.00%", true, Color(0xFF16A085), iconUrl = ghIcon("BFUSD")),
    CryptoCoin("PLUME", "$0.1842", "↑ 3.10%", true, Color(0xFF8E44AD), iconUrl = ghIcon("PLUME")),
    CryptoCoin("DOLO", "$0.5120", "↓ 1.25%", false, Color(0xFFE67E22), iconUrl = ghIcon("DOLO")),
    CryptoCoin("MITO", "$0.8734", "↑ 1.85%", true, Color(0xFF3498DB), iconUrl = ghIcon("MITO")),
    CryptoCoin("WLFI", "$0.2140", "↓ 0.62%", false, Color(0xFF1ABC9C), iconUrl = ghIcon("WLFI")),
    CryptoCoin("SOMI", "$1.42", "↑ 2.15%", true, Color(0xFF9B59B6), iconUrl = ghIcon("SOMI")),
    CryptoCoin("OPEN", "$0.6290", "↑ 1.40%", true, Color(0xFF2980B9), iconUrl = ghIcon("OPEN")),
    CryptoCoin("USDE", "$1.00", "↑ 0.01%", true, Color(0xFF27AE60), iconUrl = ghIcon("USDE")),
    CryptoCoin("LINEA", "$0.0284", "↓ 0.90%", false, Color(0xFF34495E), iconUrl = ghIcon("LINEA")),
    CryptoCoin("HOLO", "$0.0471", "↑ 4.20%", true, Color(0xFFE74C3C), iconUrl = ghIcon("HOLO")),
    CryptoCoin("PUMP", "$0.0062", "↓ 2.10%", false, Color(0xFF16A085), iconUrl = ghIcon("PUMP")),
    CryptoCoin("AVNT", "$0.9180", "↑ 0.75%", true, Color(0xFF8E44AD), iconUrl = ghIcon("AVNT")),
    CryptoCoin("ZKC", "$1.24", "↓ 1.60%", false, Color(0xFF2C3E50), iconUrl = ghIcon("ZKC")),
    CryptoCoin("SKY", "$0.0512", "↑ 3.40%", true, Color(0xFF3498DB), iconUrl = ghIcon("SKY")),
    CryptoCoin("BARD", "$2.87", "↑ 1.15%", true, Color(0xFFB14CFF), iconUrl = ghIcon("BARD")),
    CryptoCoin("OG", "$4.92", "↓ 0.55%", false, Color(0xFFE67E22), iconUrl = ghIcon("OG")),
    CryptoCoin("HEMI", "$0.1180", "↑ 2.05%", true, Color(0xFF1ABC9C), iconUrl = ghIcon("HEMI")),
    CryptoCoin("TUSD", "$1.00", "↑ 0.00%", true, Color(0xFF16A085), iconUrl = ghIcon("TUSD")),
    CryptoCoin("ADA", "$0.7210", "↑ 1.90%", true, Color(0xFF0033AD), iconUrl = ghIcon("ADA")),
    CryptoCoin("QTUM", "$3.14", "↓ 0.30%", false, Color(0xFF2C3E50), iconUrl = ghIcon("QTUM"))
)

class HomeActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            SahraTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    SahraRoot()
                }
            }
        }
    }
}

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

@Composable
private fun SahraRoot() {
    var showVoiceMatch by rememberSaveable { mutableStateOf(false) }

    if (showVoiceMatch) {
        VoiceMatchScreen(onBack = { showVoiceMatch = false })
    } else {
        SahraApp(onVoiceMatchClick = { showVoiceMatch = true })
    }
}

@Composable
private fun SahraApp(onVoiceMatchClick: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf("الرئيسية") }
    var unreadMessages by rememberSaveable { mutableStateOf(0) }
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    "الرئيسية" -> {
                        SahraHome(
                            onMatch = onVoiceMatchClick,
                            onQuickAction = { title ->
                                val message = when (title) {
                                    "الأنشطة" -> "قسم الأنشطة جاهز في الواجهة."
                                    "السحب" -> "قسم السحب جاهز للربط."
                                    "الشحن" -> "واجهة تجريبية للشحن."
                                    else -> "قسم الأمان جاهز للربط."
                                }
                                openNotice(title, message)
                            }
                        )
                    }
                    "الغرف" -> {
                        SahraRoomsPage(
                            onJoin = { room ->
                                openNotice(room.name, "غرفة تجريبية لعرض التصميم.")
                            }
                        )
                    }
                    "يستكشف" -> {
                        SahraPlaceholderPage("يستكشف", "قيد الإنشاء", "هذا القسم قيد الإنشاء حاليًا.")
                    }
                    "الرسائل" -> {
                        SahraPlaceholderPage("الرسائل", "قيد الإنشاء", "قسم الرسائل قيد الإنشاء حاليًا.")
                    }
                    "أنا" -> {
                        SahraProfilePage(
                            onAction = { title ->
                                openNotice(title, "القسم جاهز للربط ببيانات المستخدم.")
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
            title = { Text(current.title, fontWeight = FontWeight.Bold) },
            text = { Text(current.message, lineHeight = 24.sp) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text("تمام", color = SahraColors.Pink)
                }
            }
        )
    }
}

@Composable
private fun SahraBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0C1022), Color(0xFF040612), Color(0xFF02040D))
            )
        )
        val glowCenter = Offset(size.width * 0.45f, 0f)
        val glowRadius = size.width * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF33457D).copy(alpha = 0.28f), Color.Transparent),
                center = glowCenter,
                radius = glowRadius
            ),
            center = glowCenter,
            radius = glowRadius
        )
    }
}

@Composable
private fun SahraHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("SahraChat", color = Color.Transparent, fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SahraHome(
    onMatch: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp, 5.dp, 9.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item { SahraUnifiedCard(onClick = onMatch) }
        item { SahraQuickActions(onAction = onQuickAction) }
        item { SahraCryptoSection() }
    }
}

@Composable
private fun SahraUnifiedCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(108.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(
                Color(0xFF9027DD), Color(0xFF6420AD), Color(0xFF201046))))
            .border(1.dp, Color(0xFFC87AFF).copy(alpha = 0.45f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
    ) {
        BannerStars()
    }
}

@Composable
private fun BannerStars() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(16) { index ->
            val x = (((index * 37 + 17) % 100) / 100f) * size.width
            val y = (((index * 29 + 11) % 100) / 100f) * size.height
            drawCircle(
                color = Color.White.copy(alpha = if (index % 3 == 0) 0.19f else 0.08f),
                radius = if (index % 4 == 0) 1.4.dp.toPx() else 0.7.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun SahraQuickActions(onAction: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SahraQuickTile(
                title = "الأنشطة",
                remoteIconName = "icon_activities",
                colors = listOf(Color(0xFF754219), Color(0xFF291C16), Color(0xFF101019)),
                notification = true,
                modifier = Modifier.weight(1f),
                onClick = { onAction("الأنشطة") }
            )
            SahraQuickTile(
                title = "السحب",
                remoteIconName = "ic_action_road_top",
                colors = listOf(Color(0xFF1B80BE), Color(0xFF123C70), Color(0xFF09132C)),
                modifier = Modifier.weight(1f),
                onClick = { onAction("السحب") }
            )
            SahraQuickTile(
                title = "الشحن",
                remoteIconName = "icon_recharge",
                colors = listOf(Color(0xFF9538EB), Color(0xFF5522A3), Color(0xFF201340)),
                modifier = Modifier.weight(1f),
                onClick = { onAction("الشحن") }
            )
            SahraQuickTile(
                title = "الأمان",
                remoteIconName = "icon_ludo",
                colors = listOf(Color(0xFF20A98D), Color(0xFF086954), Color(0xFF072C32)),
                modifier = Modifier.weight(1f),
                onClick = { onAction("الأمان") }
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
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.verticalGradient(colors))
                .border(1.dp, colors.first().copy(alpha = 0.75f), RoundedCornerShape(13.dp))
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(title, color = Color.White, fontSize = 13.sp, maxLines = 1,
                modifier = Modifier.padding(bottom = 12.dp))
        }
        RemoteImage(
            name = remoteIconName,
            modifier = Modifier.align(Alignment.TopCenter).size(57.dp)
                .clip(RoundedCornerShape(15.dp)).clickable(onClick = onClick),
            contentScale = ContentScale.Fit
        )
        if (notification) {
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 1.dp).size(10.dp)
                    .background(Color(0xFFFF4C5B), CircleShape)
            )
        }
    }
}

@Composable
private fun SahraCryptoSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp)
    ) {
        Text(
            "أحدث الأسعار العالمية",
            color = SahraColors.Text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 9.dp, top = 3.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SahraColors.Card)
                .border(1.dp, SahraColors.CardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "العملة",
                    color = Color(0xFF8BA2C2),
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(start = 45.dp).weight(1f)
                )
                Text(
                    "تغير 24 ساعة",
                    color = Color(0xFF8BA2C2),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "أحدث سعر",
                    color = Color(0xFF8BA2C2),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(88.dp)
                )
            }

            cryptoCoins.forEachIndexed { index, coin ->
                CryptoRow(coin)
                if (index != cryptoCoins.lastIndex) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFF5082BE).copy(alpha = 0.12f))
                    )
                }
            }
        }
    }
}

@Composable
private fun CryptoRow(coin: CryptoCoin) {
    val changeColor = if (coin.up) SahraColors.Up else SahraColors.Down

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // أيقونة العملة — على اليسار
            if (coin.iconUrl != null) {
                AsyncImage(
                    model = coin.iconUrl,
                    contentDescription = coin.symbol,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(coin.color.copy(alpha = 0.16f))
                        .border(1.dp, coin.color.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(coin.symbol.take(1), color = coin.color,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                coin.symbol,
                color = SahraColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            CryptoSparkline(
                up = coin.up,
                seed = coin.symbol.hashCode(),
                modifier = Modifier.weight(1f).height(34.dp)
            )

            Column(
                modifier = Modifier.width(88.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    coin.price,
                    color = SahraColors.Text,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    coin.change,
                    color = changeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CryptoSparkline(up: Boolean, seed: Int, modifier: Modifier = Modifier) {
    val lineColor = if (up) SahraColors.Up else SahraColors.Down

    Canvas(modifier = modifier) {
        val points = 12
        val path = Path()
        for (i in 0 until points) {
            val x = size.width * i / (points - 1)
            val wave = ((seed * (i + 3) * 37 + i * 53) % 100) / 100f
            val trend = if (up) {
                (1f - i.toFloat() / points) * 0.30f
            } else {
                (i.toFloat() / points) * 0.30f
            }
            val y = size.height * (0.18f + wave * 0.45f + trend)
                .coerceIn(0.05f, 0.95f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun VoiceMatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "تم تفعيل الميكروفون ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لتشغيل المكالمة نحتاج إذن الميكروفون", Toast.LENGTH_LONG).show()
        }
    }

    val requestMic: () -> Unit = {
        val permission = Manifest.permission.RECORD_AUDIO
        val granted = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            recordAudioLauncher.launch(permission)
        }
    }

    LaunchedEffect(Unit) {
        requestMic()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(
                VoiceColors.BackgroundTop,
                VoiceColors.BackgroundMid,
                VoiceColors.BackgroundBottom
            ))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            VoiceTopBar(onBack = onBack)

            Spacer(Modifier.height(14.dp))

            VoicePreCallsCard()

            Spacer(Modifier.height(50.dp))

            Box(
                modifier = Modifier.fillMaxWidth().clickable(onClick = requestMic),
                contentAlignment = Alignment.Center
            ) {
                PlanetaryIcon(modifier = Modifier.size(210.dp))
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "جار البحث",
                color = VoiceColors.TextWhite,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "أرسل هدية لفتح مكالمات غير محدودة",
                color = VoiceColors.TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun VoiceTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع",
                tint = VoiceColors.TextWhite, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
        Text("التوافق الصوتي", color = VoiceColors.TextWhite,
            fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun VoicePreCallsCard() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp)
            .clip(RoundedCornerShape(14.dp)).background(VoiceColors.CardGray)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("المكالمات المتبقية: 2", color = VoiceColors.TextWhite,
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
    }
}

@Composable
private fun PlanetaryIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val planetRadius = size.minDimension * 0.28f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5564B5F6), Color.Transparent),
                center = Offset(cx, cy), radius = planetRadius * 2.2f
            ),
            radius = planetRadius * 2.2f, center = Offset(cx, cy)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8BC5FF), Color(0xFF5A8FEB), Color(0xFF2E4FB0)),
                center = Offset(cx - planetRadius * 0.35f, cy - planetRadius * 0.35f),
                radius = planetRadius * 1.6f
            ),
            radius = planetRadius, center = Offset(cx, cy)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x99A661FF), Color.Transparent),
                center = Offset(cx + planetRadius * 0.25f, cy + planetRadius * 0.15f),
                radius = planetRadius * 0.7f
            ),
            radius = planetRadius * 0.7f,
            center = Offset(cx + planetRadius * 0.25f, cy + planetRadius * 0.15f)
        )
        val backRing = Path().apply {
            addOval(Rect(
                cx - planetRadius * 1.75f, cy - planetRadius * 0.52f,
                cx + planetRadius * 1.75f, cy + planetRadius * 0.52f
            ))
        }
        drawPath(backRing, Color(0x55A0B0FF), style = Stroke(width = planetRadius * 0.05f))

        val ringPath = Path().apply {
            addOval(Rect(
                cx - planetRadius * 1.6f, cy - planetRadius * 0.42f,
                cx + planetRadius * 1.6f, cy + planetRadius * 0.42f
            ))
        }
        drawPath(ringPath, Color(0xFFE0E7FF), style = Stroke(width = planetRadius * 0.10f))

        val arcPath = Path().apply {
            moveTo(cx - planetRadius * 0.55f, cy + planetRadius * 0.15f)
            quadraticBezierTo(
                cx, cy - planetRadius * 0.6f,
                cx + planetRadius * 0.55f, cy - planetRadius * 0.1f
            )
        }
        drawPath(arcPath, Color.White, style = Stroke(width = planetRadius * 0.13f))

        drawStar(
            center = Offset(cx - planetRadius * 1.35f, cy - planetRadius * 1.1f),
            outerRadius = planetRadius * 0.18f, color = Color(0xFFCDA9FF)
        )
        drawStar(
            center = Offset(cx + planetRadius * 1.15f, cy + planetRadius * 0.85f),
            outerRadius = planetRadius * 0.14f, color = Color(0xFF8FB5FF)
        )
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(cx + planetRadius * 1.05f, cy - planetRadius * 1.2f),
            end = Offset(cx + planetRadius * 1.35f, cy - planetRadius * 0.85f),
            strokeWidth = planetRadius * 0.07f
        )
        drawCircle(
            color = Color(0xFF3D5A8C),
            radius = planetRadius * 0.14f,
            center = Offset(cx - planetRadius * 1.25f, cy + planetRadius * 0.75f)
        )
    }
}

private fun DrawScope.drawStar(center: Offset, outerRadius: Float, color: Color) {
    val innerRadius = outerRadius * 0.4f
    val path = Path()
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45.0 - 90.0))
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * Math.cos(angle)).toFloat()
        val y = center.y + (r * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

@Composable
private fun SahraRoomsPage(onJoin: (SahraRoom) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SahraPageTitle("الغرف الصوتية", "غرف تجريبية لمعاينة التصميم") }
        items(sahraRooms, key = { it.id }) { room ->
            SahraPanel {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(62.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(
                                room.color, room.color.copy(alpha = 0.5f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(room.name.take(1), color = Color.White,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(room.name, color = SahraColors.Text,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("معاينة محلية • بدون اتصال صوتي",
                            color = SahraColors.Muted, fontSize = 11.sp)
                    }
                    SahraPill("انضمام", Color(0xFF40216B), onClick = { onJoin(room) })
                }
            }
        }
    }
}

@Composable
private fun SahraPlaceholderPage(title: String, subtitle: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape)
                .background(SahraColors.Purple.copy(alpha = 0.15f))
                .border(2.dp, SahraColors.Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, null, tint = SahraColors.Purple,
                modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(title, color = SahraColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SahraColors.Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, color = SahraColors.Muted, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
private fun SahraProfilePage(onAction: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SahraPageTitle("أنا", "معاينة الملف الشخصي") }
        item {
            SahraPanel {
                Column(modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgrammaticAvatar(Modifier.size(82.dp), SahraColors.Pink)
                    Spacer(Modifier.height(12.dp))
                    Text("مستخدم SahraChat", color = SahraColors.Text,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("حساب تجريبي • غير مرتبط بتسجيل الدخول",
                        color = SahraColors.Muted, fontSize = 12.sp,
                        textAlign = TextAlign.Center)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SahraProfileStat("متابعين", "0", Modifier.weight(1f))
                SahraProfileStat("متابَعين", "0", Modifier.weight(1f))
                SahraProfileStat("أصدقاء", "0", Modifier.weight(1f))
            }
        }
        items(listOf("تعديل الملف الشخصي", "المحفظة والشحن", "الإعدادات", "المساعدة")) { title ->
            SahraPanel {
                Row(modifier = Modifier.fillMaxWidth().clickable { onAction(title) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = SahraColors.Text, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                    Text("‹", color = SahraColors.Muted, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
private fun SahraProfileStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(SahraColors.Card).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = SahraColors.Pink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(title, color = SahraColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SahraPageTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) {
        Text(title, color = SahraColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = SahraColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SahraPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(SahraColors.Card)
            .border(1.dp, SahraColors.CardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun ProgrammaticAvatar(
    modifier: Modifier = Modifier,
    ring: Color = SahraColors.Purple,
    fill: Color = Color(0xFF181529)
) {
    Box(
        modifier = modifier.clip(CircleShape).background(fill)
            .border(2.dp, ring, CircleShape).padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Person, null, tint = ring.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize().padding(4.dp))
    }
}

@Composable
private fun SahraBottomNavigation(
    selectedTab: String,
    unreadMessages: Int,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SahraColors.Navigation)
            .padding(top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sahraTabs.forEach { tab ->
            val selected = tab.title == selectedTab
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab.title) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(39.dp), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Box(modifier = Modifier.size(38.dp).background(
                            brush = Brush.radialGradient(listOf(
                                SahraColors.Purple.copy(alpha = 0.35f), Color.Transparent)),
                            shape = CircleShape))
                    }
                    RemoteImage(
                        name = tab.remoteName,
                        contentDescription = tab.title,
                        modifier = Modifier.size(if (selected) 35.dp else 31.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (tab.title == "الرسائل" && unreadMessages > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                                .background(Color(0xFFFF4A58), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(unreadMessages.toString(), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(tab.title,
                    color = if (selected) Color.White else Color(0xFF777A91),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1)
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
        modifier = modifier.clip(RoundedCornerShape(30.dp)).background(background)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}
