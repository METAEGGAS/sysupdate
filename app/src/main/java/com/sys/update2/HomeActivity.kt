package com.sys.update2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Investment(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val type: String = "استثمار",
    val date: Long = System.currentTimeMillis()
)

class HomeActivity : ComponentActivity() {

    private var showAddDialog by mutableStateOf(false)
    private var newName by mutableStateOf("")
    private var newAmount by mutableStateOf("")

    private val investments = mutableStateListOf<Investment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.parseColor("#040018")
        window.navigationBarColor = android.graphics.Color.parseColor("#040018")

        // بيانات تجريبية
        investments.addAll(listOf(
            Investment("1", "أسهم Tesla", 5000.0, "USD", "أسهم"),
            Investment("2", "بيتكوين", 2500.0, "USD", "عملات رقمية"),
            Investment("3", "عقار الرياض", 150000.0, "SAR", "عقار"),
            Investment("4", "صندوق استثمار", 10000.0, "USD", "صناديق")
        ))

        setContent {
            HomeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeBackground()
                        Column(modifier = Modifier.fillMaxSize()) {
                            TopBar(
                                userName = FirebaseAuthHelper.currentUser()?.displayName
                                    ?: FirebaseAuthHelper.currentUser()?.email?.split("@")?.get(0)
                                    ?: "مستخدم",
                                onLogout = { logout() }
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                item {
                                    WelcomeCard(
                                        userName = FirebaseAuthHelper.currentUser()?.displayName
                                            ?: "مستخدم",
                                        count = investments.size,
                                        total = investments.sumOf { it.amount }
                                    )
                                }

                                item {
                                    Text(
                                        "استثماراتي",
                                        color = Neon.Txt,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(investments) { inv ->
                                    InvestmentCard(inv)
                                }
                            }
                        }

                        // زر إضافة استثمار
                        FloatingAddButton(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp),
                            onClick = { showAddDialog = true }
                        )

                        if (showAddDialog) {
                            AddInvestmentDialog(
                                name = newName,
                                onNameChange = { newName = it },
                                amount = newAmount,
                                onAmountChange = { newAmount = it },
                                onConfirm = {
                                    if (newName.isNotBlank() && newAmount.isNotBlank()) {
                                        val amount = newAmount.toDoubleOrNull() ?: 0.0
                                        investments.add(
                                            Investment(
                                                id = System.currentTimeMillis().toString(),
                                                name = newName,
                                                amount = amount,
                                                currency = "USD",
                                                type = "استثمار"
                                            )
                                        )
                                        newName = ""
                                        newAmount = ""
                                        showAddDialog = false
                                    }
                                },
                                onCancel = {
                                    newName = ""
                                    newAmount = ""
                                    showAddDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun logout() {
        FirebaseAuthHelper.logout()
        try {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    //  TopBar
    // ═══════════════════════════════════════════
    @Composable
    fun TopBar(userName: String, onLogout: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // صورة المستخدم
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Neon.Cyan, Neon.Magenta)))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("مرحباً", color = Neon.Hint, fontSize = 13.sp)
                Text(
                    userName,
                    color = Neon.Txt,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // زر تسجيل خروج
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FF3D5A))
                    .clickable { onLogout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "خروج",
                    tint = Color(0xFFFF5C7A), modifier = Modifier.size(22.dp))
            }
        }
    }

    // ═══════════════════════════════════════════
    //  WelcomeCard
    // ═══════════════════════════════════════════
    @Composable
    fun WelcomeCard(userName: String, count: Int, total: Double) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(24.dp),
                    ambientColor = Neon.Purple, spotColor = Neon.Magenta)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A0B3D),
                            Color(0xFF2A1258),
                            Color(0xFF3D1A6B)
                        )
                    )
                )
                .border(1.dp, Brush.horizontalGradient(
                    listOf(Neon.Cyan, Neon.Purple, Neon.Magenta)
                ), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Text("إجمالي استثماراتك",
                    color = Neon.Hint, fontSize = 13.sp)

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%,.0f".format(total),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(Neon.Cyan, Neon.Magenta)
                            ),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("USD", color = Neon.Hint, fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp))
                }

                Spacer(Modifier.height(16.dp))

                Row {
                    StatBox("عدد الاستثمارات", count.toString(), Icons.Default.Add)
                }
            }
        }
    }

    @Composable
    fun StatBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x40FFFFFF))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Neon.Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("$label: ", color = Neon.Hint, fontSize = 12.sp)
            Text(value, color = Neon.Txt, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    // ═══════════════════════════════════════════
    //  InvestmentCard
    // ═══════════════════════════════════════════
    @Composable
    fun InvestmentCard(inv: Investment) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Neon.FieldBg)
                .border(1.dp, Neon.FieldBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // أيقونة دائرية
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(
                        listOf(Neon.Purple, Neon.Blue)
                    )),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    inv.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(inv.name, color = Neon.Txt, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(inv.type, color = Neon.Hint, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%,.0f".format(inv.amount),
                    color = Neon.Cyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(inv.currency, color = Neon.Hint, fontSize = 11.sp)
            }
        }
    }

    // ═══════════════════════════════════════════
    //  FloatingAddButton
    // ═══════════════════════════════════════════
    @Composable
    fun FloatingAddButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
        Box(
            modifier = modifier
                .size(60.dp)
                .shadow(20.dp, CircleShape,
                    ambientColor = Neon.Magenta, spotColor = Neon.Purple)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Neon.Magenta, Neon.Purple, Neon.Cyan)
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة",
                tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }

    // ═══════════════════════════════════════════
    //  AddInvestmentDialog
    // ═══════════════════════════════════════════
    @Composable
    fun AddInvestmentDialog(
        name: String,
        onNameChange: (String) -> Unit,
        amount: String,
        onAmountChange: (String) -> Unit,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131B45))
                    .border(1.dp, Neon.FieldBorder, RoundedCornerShape(24.dp))
                    .clickable { /* منع الإغلاق */ }
                    .padding(24.dp)
            ) {
                Text("إضافة استثمار جديد",
                    color = Neon.Txt, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(20.dp))

                Text("الاسم", color = Neon.Hint, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Neon.FieldBg)
                        .border(1.dp, Neon.FieldBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) Text("مثال: أسهم Tesla",
                                color = Neon.Hint, fontSize = 14.sp)
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text("المبلغ (USD)", color = Neon.Hint, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Neon.FieldBg)
                        .border(1.dp, Neon.FieldBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Neon.Txt, fontSize = 15.sp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (amount.isEmpty()) Text("0.00",
                                color = Neon.Hint, fontSize = 14.sp)
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x40FF3D5A))
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("إلغاء", color = Color(0xFFFF5C7A),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(
                                listOf(Neon.Magenta, Neon.Purple, Neon.Cyan)
                            ))
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("إضافة", color = Color.White,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  HomeBackground
    // ═══════════════════════════════════════════
    @Composable
    fun HomeBackground() {
        val stars = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(
                listOf(Neon.DeepTop, Neon.DeepMid, Neon.DeepBottom)
            ))

            drawCircle(
                Brush.radialGradient(listOf(Neon.Purple.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 0.6f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.8f, size.height * 0.1f)
            )
            drawCircle(
                Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.7f),
                    radius = size.width * 0.6f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.1f, size.height * 0.7f)
            )

            stars.forEach {
                drawCircle(Neon.Cyan.copy(alpha = 0.4f), radius = 1.2.dp.toPx(),
                    center = Offset(it.x * size.width, it.y * size.height))
            }
        }
    }
}

@Composable
fun HomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Blue, background = Neon.DeepBottom,
            surface = Neon.FieldBg, onPrimary = Color.White,
            onBackground = Color.White, onSurface = Color.White
        )
    ) { content() }
}
