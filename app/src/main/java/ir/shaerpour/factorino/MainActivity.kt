package ir.shaerpour.factorino

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

// ---------- مدل داده ----------
data class Item(val name: String, val qty: Double, val price: Double)
data class Invoice(
    val id: Long, val no: String, val date: String, val seller: String, val customer: String,
    val items: List<Item>, val tax: Double, val discount: Double,
    val style: Int, val font: Int, val size: Float, val landscape: Boolean
) {
    val sub get() = items.sumOf { it.qty * it.price }
    val total get() = sub - discount + (sub - discount) * tax / 100
}

val styleNames = listOf("کلاسیک", "مدرن", "مینیمال")
val styleColors = listOf(Color(0xFF1F3A68), Color(0xFF7A5CFF), Color(0xFF444444))
val fontNames = listOf("پیش‌فرض", "سریف", "تایپ‌رایتر")
val fontFamilies = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace)
fun money(v: Double) = String.format(Locale.US, "%,.0f", v)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("history", Context.MODE_PRIVATE)
    fun load(c: Context): List<Invoice> = try {
        val a = JSONArray(sp(c).getString("list", "[]"))
        (0 until a.length()).map { val o = a.getJSONObject(it)
            val its = o.getJSONArray("items")
            Invoice(o.getLong("id"), o.getString("no"), o.getString("date"), o.getString("seller"),
                o.getString("customer"),
                (0 until its.length()).map { i -> its.getJSONObject(i).let { x -> Item(x.getString("n"), x.getDouble("q"), x.getDouble("p")) } },
                o.getDouble("tax"), o.getDouble("disc"), o.getInt("style"), o.getInt("font"),
                o.getDouble("size").toFloat(), o.getBoolean("land"))
        }
    } catch (e: Exception) { emptyList() }
    fun save(c: Context, l: List<Invoice>) {
        val a = JSONArray()
        l.forEach { v -> a.put(JSONObject().apply {
            put("id", v.id); put("no", v.no); put("date", v.date); put("seller", v.seller); put("customer", v.customer)
            put("items", JSONArray().also { ja -> v.items.forEach { ja.put(JSONObject().put("n", it.name).put("q", it.qty).put("p", it.price)) } })
            put("tax", v.tax); put("disc", v.discount); put("style", v.style); put("font", v.font)
            put("size", v.size.toDouble()); put("land", v.landscape) }) }
        sp(c).edit().putString("list", a.toString()).apply()
    }
}

// ---------- ساخت PDF با اندازه و جهت دقیق ----------
fun makePdf(c: Context, v: Invoice): File {
    val w = if (v.landscape) 842 else 595
    val h = if (v.landscape) 595 else 842
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
    val cv = page.canvas
    val col = styleColors[v.style].let { android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
    val tf = when (v.font) { 1 -> Typeface.SERIF; 2 -> Typeface.MONOSPACE; else -> Typeface.DEFAULT }
    fun paint(sz: Float, bold: Boolean = false, color: Int = android.graphics.Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sz; typeface = Typeface.create(tf, if (bold) Typeface.BOLD else Typeface.NORMAL)
        this.color = color; textAlign = Paint.Align.RIGHT }
    val s = v.size
    val r = w - 40f
    if (v.style == 1) cv.drawRect(0f, 0f, w.toFloat(), 90f, Paint().apply { color = col })
    else cv.drawRect(0f, 0f, w.toFloat(), 8f, Paint().apply { color = col })
    cv.drawText("فاکتور فروش", r, 55f, paint(s + 10, true, if (v.style == 1) android.graphics.Color.WHITE else col))
    var y = 120f
    cv.drawText("شماره: ${v.no}    تاریخ: ${v.date}", r, y, paint(s)); y += s + 10
    cv.drawText("فروشنده: ${v.seller}", r, y, paint(s)); y += s + 10
    cv.drawText("خریدار: ${v.customer}", r, y, paint(s)); y += s + 20
    cv.drawRect(40f, y - s, w - 40f, y + 8, Paint().apply { color = col; alpha = if (v.style == 2) 40 else 255 })
    val hc = if (v.style == 2) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    val cw = (w - 80f)
    val cols = listOf(r - 6, 40 + cw * 0.40f, 40 + cw * 0.22f, 40 + cw * 0.08f)
    listOf("شرح کالا", "تعداد", "قیمت", "جمع").forEachIndexed { i, t -> cv.drawText(t, if (i == 0) r - 6 else cols[i], y, paint(s, true, hc)) }
    y += s + 14
    v.items.forEach { it0 ->
        if (y < h - 120) {
            cv.drawText(it0.name, cols[0], y, paint(s))
            cv.drawText(money(it0.qty), cols[1], y, paint(s))
            cv.drawText(money(it0.price), cols[2], y, paint(s))
            cv.drawText(money(it0.qty * it0.price), cols[3], y, paint(s))
            cv.drawLine(40f, y + 6, w - 40f, y + 6, Paint().apply { color = android.graphics.Color.LTGRAY })
            y += s + 14
        }
    }
    y += 10
    cv.drawText("تخفیف: ${money(v.discount)}   مالیات: ${v.tax}٪", r, y, paint(s)); y += s + 12
    cv.drawText("مبلغ قابل پرداخت: ${money(v.total)}", r, y, paint(s + 3, true, col))
    cv.drawText("ساخته‌شده با فاکتورینو", w / 2f, h - 20f, paint(9f, false, android.graphics.Color.GRAY).apply { textAlign = Paint.Align.CENTER })
    doc.finishPage(page)
    val f = File(c.cacheDir, "factor_${v.no}.pdf")
    f.outputStream().use { doc.writeTo(it) }; doc.close()
    return f
}

fun sharePdf(c: Context, v: Invoice) {
    val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", makePdf(c, v))
    c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "اشتراک‌گذاری فاکتور"))
}

fun today(): String = android.icu.text.SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("fa-IR-u-ca-persian")).format(java.util.Date())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF7A5CFF), secondary = Color(0xFF00B8F0), background = Color.White, surface = Color.White)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Root() }
            }
        }
    }
}

@Composable fun Root() {
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1400); splash = false }
    Crossfade(splash, animationSpec = tween(500), label = "s") { if (it) Splash() else Home() }
}

@Composable fun Splash() {
    val sc by rememberInfiniteTransition(label = "p").animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sc")
    Box(Modifier.fillMaxSize().background(Color.White), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(110.dp).scale(sc).background(Brush.linearGradient(listOf(Color(0xFF6A5CFF), Color(0xFF00C2FF))), RoundedCornerShape(28.dp)), Alignment.Center) {
                Text("F", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text("فاکتورینو", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A5CFF))
            Text("توسعه توسط تیم شاعرپور", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

val Violet = Color(0xFF7A5CFF)
val Grad = Brush.linearGradient(listOf(Color(0xFF6A5CFF), Color(0xFF00C2FF)))
val Pill = RoundedCornerShape(14.dp)

@Composable fun Header(sub: String) {
    Box(Modifier.fillMaxWidth().background(Grad, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Color.White, RoundedCornerShape(14.dp)), Alignment.Center) {
                Text("F", color = Violet, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column { Text("فاکتورینو", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = Color.White.copy(alpha = .85f), fontSize = 12.sp) }
        }
    }
}

@Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 5 }) {
        Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(Color(0xFFF8F7FF)), border = BorderStroke(1.dp, Color(0xFFE6E2FF))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Violet, fontSize = 15.sp); content() }
        }
    }
}

@Composable fun GradButton(text: String, modifier: Modifier = Modifier, brush: Brush = Grad, onClick: () -> Unit) {
    Box(modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(brush).clickable(onClick = onClick), Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable fun Field(v: String, on: (String) -> Unit, label: String, m: Modifier = Modifier, num: Boolean = false) =
    OutlinedTextField(v, on, m, label = { Text(label) }, singleLine = true, shape = Pill,
        keyboardOptions = if (num) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFFD9D4FF)))

@Composable fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).background(Color(0xFFF1EEFF), RoundedCornerShape(22.dp)).padding(6.dp)) {
        listOf("＋  فاکتور جدید", "☰  تاریخچه").forEachIndexed { i, t ->
            val bg by animateColorAsState(if (tab == i) Violet else Color.Transparent, tween(300), label = "n")
            val fg by animateColorAsState(if (tab == i) Color.White else Violet, tween(300), label = "f")
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(18.dp)).background(bg).clickable { onTab(i) }, Alignment.Center) {
                Text(t, color = fg, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable fun Home() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(Store.load(ctx)) }
    var editing by remember { mutableStateOf<Invoice?>(null) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Header(if (tab == 0) "ساخت فاکتور آنلاین، سریع و زیبا" else "${history.size} فاکتور ذخیره‌شده")
        Box(Modifier.weight(1f)) {
            AnimatedContent(tab, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "t") { t ->
                if (t == 0) Editor(editing) { inv ->
                    history = listOf(inv) + history.filter { it.id != inv.id }; Store.save(ctx, history); editing = null
                } else History(history, onOpen = { editing = it; tab = 0 }, onDelete = { d ->
                    history = history.filter { it.id != d.id }; Store.save(ctx, history) })
            }
        }
        NavBar(tab) { tab = it }
    }
}

@Composable fun History(list: List<Invoice>, onOpen: (Invoice) -> Unit, onDelete: (Invoice) -> Unit) {
    val ctx = LocalContext.current
    if (list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧾", fontSize = 56.sp); Text("هنوز فاکتوری ثبت نشده", color = Color.Gray) } }
    else LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.id }) { v ->
            Card(Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color(0xFFF8F7FF)),
                border = BorderStroke(1.dp, Color(0xFFE6E2FF))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(6.dp).height(64.dp).background(styleColors[v.style], RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("فاکتور ${v.no} — ${v.customer}", fontWeight = FontWeight.Bold)
                        Text("${v.date}  •  ${styleNames[v.style]}", color = Color.Gray, fontSize = 12.sp)
                        Text("${money(v.total)} تومان", color = Violet, fontWeight = FontWeight.Bold)
                        Row { TextButton({ onOpen(v) }) { Text("ویرایش") }; TextButton({ sharePdf(ctx, v) }) { Text("PDF") }
                            TextButton({ onDelete(v) }) { Text("حذف", color = Color(0xFFE53935)) } }
                    }
                }
            }
        }
    }
}

@Composable fun Chips(names: List<String>, sel: Int, on: (Int) -> Unit) =
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { names.forEachIndexed { i, n ->
        FilterChip(sel == i, { on(i) }, { Text(n) }, shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Violet, selectedLabelColor = Color.White)) } }

@Composable fun Editor(init: Invoice?, onSave: (Invoice) -> Unit) {
    val ctx = LocalContext.current
    var no by remember(init) { mutableStateOf(init?.no ?: "1001") }
    var date by remember(init) { mutableStateOf(init?.date ?: today()) }
    var seller by remember(init) { mutableStateOf(init?.seller ?: "") }
    var customer by remember(init) { mutableStateOf(init?.customer ?: "") }
    var items by remember(init) { mutableStateOf(init?.items ?: listOf(Item("", 1.0, 0.0))) }
    var tax by remember(init) { mutableStateOf(init?.tax?.toString() ?: "0") }
    var disc by remember(init) { mutableStateOf(init?.discount?.toString() ?: "0") }
    var style by remember(init) { mutableIntStateOf(init?.style ?: 0) }
    var font by remember(init) { mutableIntStateOf(init?.font ?: 0) }
    var size by remember(init) { mutableFloatStateOf(init?.size ?: 12f) }
    var land by remember(init) { mutableStateOf(init?.landscape ?: false) }
    fun build() = Invoice(init?.id ?: System.currentTimeMillis(), no, date, seller, customer, items,
        tax.toDoubleOrNull() ?: 0.0, disc.toDoubleOrNull() ?: 0.0, style, font, size, land)
    val accent by animateColorAsState(styleColors[style], tween(500), label = "a")
    fun n(d: Double) = if (d == 0.0) "" else d.toString().removeSuffix(".0")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Section("مشخصات فاکتور") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(no, { no = it }, "شماره", Modifier.weight(1f)); Field(date, { date = it }, "تاریخ", Modifier.weight(1f)) }
            Field(seller, { seller = it }, "فروشنده", Modifier.fillMaxWidth())
            Field(customer, { customer = it }, "خریدار", Modifier.fillMaxWidth()) } }
        item { Section("اقلام") {
            items.forEachIndexed { i, it0 ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(it0.name, { x -> items = items.toMutableList().also { l -> l[i] = it0.copy(name = x) } }, "کالا", Modifier.weight(2f))
                    Field(n(it0.qty), { x -> items = items.toMutableList().also { l -> l[i] = it0.copy(qty = x.toDoubleOrNull() ?: 0.0) } }, "تعداد", Modifier.weight(1.1f), true)
                    Field(n(it0.price), { x -> items = items.toMutableList().also { l -> l[i] = it0.copy(price = x.toDoubleOrNull() ?: 0.0) } }, "قیمت", Modifier.weight(1.5f), true)
                    if (items.size > 1) Text("✕", color = Color(0xFFE53935), modifier = Modifier.clickable { items = items.filterIndexed { j, _ -> j != i } }.padding(4.dp))
                } }
            OutlinedButton({ items = items + Item("", 1.0, 0.0) }, shape = Pill) { Text("＋ افزودن ردیف") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(tax, { tax = it }, "مالیات ٪", Modifier.weight(1f), true); Field(disc, { disc = it }, "تخفیف", Modifier.weight(1f), true) } } }
        item { Section("ظاهر و استایل") {
            Text("استایل"); Chips(styleNames, style) { style = it }
            Text("فونت"); Chips(fontNames, font) { font = it }
            Text("اندازه متن: ${size.toInt()}"); Slider(size, { size = it }, valueRange = 8f..20f, steps = 11,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("جهت صفحه: ${if (land) "افقی" else "عمودی"}", Modifier.weight(1f))
                Switch(land, { land = it }, colors = SwitchDefaults.colors(checkedTrackColor = accent)) } } }
        item { val v = build()
            Section("پیش‌نمایش زنده") {
                Box(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(2.dp, accent, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("فاکتور فروش  #${v.no}", color = accent, fontWeight = FontWeight.Bold, fontFamily = fontFamilies[font], fontSize = (size + 4).sp)
                        Text("${v.seller} ← ${v.customer}", fontFamily = fontFamilies[font], fontSize = size.sp, color = Color.Gray)
                        HorizontalDivider(color = accent.copy(alpha = .3f))
                        v.items.forEach { Text("${it.name}  ×${it.qty.toString().removeSuffix(".0")}  =  ${money(it.qty * it.price)}", fontFamily = fontFamilies[font], fontSize = size.sp) }
                        HorizontalDivider(color = accent.copy(alpha = .3f))
                        Text("جمع کل: ${money(v.total)} تومان", fontWeight = FontWeight.Bold, color = accent, fontFamily = fontFamilies[font], fontSize = (size + 2).sp)
                    } } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradButton("ذخیره", Modifier.weight(1f), Brush.linearGradient(listOf(Violet, Violet))) { onSave(build()) }
            GradButton("خروجی PDF", Modifier.weight(1f)) { val v = build(); onSave(v); sharePdf(ctx, v) } } }
    }
}
