package ir.shaerpour.factorino

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(22.dp)
val statusColors = listOf(Color(0xFFE5534B), Color(0xFFE0A030), Color(0xFF3FB27F))
val fontFamilies = listOf(Vazir, androidx.compose.ui.text.font.FontFamily.Serif, androidx.compose.ui.text.font.FontFamily.Monospace)

@Composable fun grayBrush() = Brush.linearGradient(listOf(Color(0xFF8A9099), Color(0xFF2A2C30)))

@Composable fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    var m = modifier.fillMaxWidth().clip(CardShape).background(cs.surface).border(1.dp, cs.outline, CardShape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 6 }) {
        AppCard {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            content()
        }
    }
}

@Composable fun Field(v: String, on: (String) -> Unit, label: String, m: Modifier = Modifier, num: Boolean = false, multi: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(v, on, m, label = { Text(label) }, singleLine = !multi, minLines = if (multi) 2 else 1,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (num) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cs.primary, unfocusedBorderColor = cs.outline,
            focusedContainerColor = cs.surfaceVariant, unfocusedContainerColor = cs.surfaceVariant,
            focusedLabelColor = cs.onSurface, unfocusedLabelColor = cs.onSurfaceVariant, cursorColor = cs.primary))
}

@Composable fun NumField(value: Double, on: (Double) -> Unit, label: String, m: Modifier = Modifier) {
    var t by remember { mutableStateOf(fa(fmtNum(value))) }
    Field(t, { t = it; on(normNum(it).toDoubleOrNull() ?: 0.0) }, label, m, num = true)
}

@Composable fun PButton(text: String, m: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(m.height(50.dp).clip(RoundedCornerShape(16.dp)).background(cs.primary).clickable(onClick = onClick), Alignment.Center) {
        Text(text, color = cs.onPrimary, fontWeight = FontWeight.Bold) }
}
@Composable fun GButton(text: String, m: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(m.height(46.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, cs.outline, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp), Alignment.Center) {
        Text(text, color = cs.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable fun Chips(names: List<String>, sel: Int, on: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEachIndexed { i, n ->
            FilterChip(sel == i, { on(i) }, { Text(n) }, shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = cs.surfaceVariant, labelColor = cs.onSurface,
                    selectedContainerColor = cs.primary, selectedLabelColor = cs.onPrimary))
        }
    }
}

@Composable fun StatusChip(status: Int, overdue: Boolean = false) {
    val col = if (overdue) Color(0xFFE5534B) else statusColors[status]
    val txt = if (overdue) "سررسید گذشته" else statusNames[status]
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(col.copy(alpha = .16f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(txt, color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable fun TopHeader(title: String, sub: String, trailing: (@Composable () -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(cs.surface, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
        .statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(grayBrush()), Alignment.Center) {
            Image(painterResource(R.drawable.ic_mark), null, Modifier.size(30.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(sub, fontSize = 12.sp, color = cs.onSurfaceVariant) }
        if (trailing != null) trailing()
    }
}

@Composable fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val items = listOf("🏠" to "خانه", "🧾" to "فاکتورها", "👥" to "مشتریان", "📦" to "کالاها", "⚙️" to "تنظیمات")
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp).clip(RoundedCornerShape(24.dp)).background(cs.surface)
        .border(1.dp, cs.outline, RoundedCornerShape(24.dp)).padding(6.dp)) {
        items.forEachIndexed { i, (g, t) ->
            val bg by animateColorAsState(if (tab == i) cs.primary else Color.Transparent, tween(300), label = "nb")
            val fg by animateColorAsState(if (tab == i) cs.onPrimary else cs.onSurfaceVariant, tween(300), label = "nf")
            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).clickable { onTab(i) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(g, fontSize = 16.sp)
                Text(t, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable fun Splash() {
    val cs = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "sp")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pu")
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val a by animateFloatAsState(if (go) 1f else 0f, tween(900), label = "a")
    val prog by animateFloatAsState(if (go) 1f else 0f, tween(2300, easing = FastOutSlowInEasing), label = "pr")
    val ring = cs.primary
    val track = cs.outline
    Box(Modifier.fillMaxSize().background(cs.background), Alignment.Center) {
        Column(Modifier.alpha(a), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(170.dp), Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 5.dp.toPx()
                    drawCircle(track, style = Stroke(width = 2.dp.toPx()))
                    drawArc(ring, rot, 80f, false, style = Stroke(width = sw, cap = StrokeCap.Round))
                    drawArc(ring.copy(alpha = .35f), rot + 180f, 40f, false, style = Stroke(width = sw, cap = StrokeCap.Round))
                }
                Box(Modifier.size(104.dp).scale(pulse).clip(RoundedCornerShape(30.dp)).background(grayBrush()), Alignment.Center) {
                    Image(painterResource(R.drawable.ic_mark), null, Modifier.size(62.dp)) }
            }
            Spacer(Modifier.height(26.dp))
            Text("فاکتورینو", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("ساخت فاکتور، سریع و دقیق", color = cs.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))
            LinearProgressIndicator(progress = { prog }, Modifier.width(170.dp).height(5.dp).clip(CircleShape), color = cs.primary, trackColor = cs.outline)
            Spacer(Modifier.height(10.dp))
            Text(if (prog < .4f) "در حال آماده‌سازی" else if (prog < .85f) "بارگذاری اطلاعات" else "آماده‌ایم", color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
        Text("توسعه توسط تیم شاعرپور", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp).alpha(a),
            color = cs.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
