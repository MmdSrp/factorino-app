package ir.shaerpour.factorino

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_SHORT).show()

@Composable fun StatTile(label: String, value: String, m: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    AppCard(m) {
        Text(label, color = cs.onSurfaceVariant, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun InvoiceRow(v: Invoice, cur: String, onClick: () -> Unit, m: Modifier = Modifier, trailing: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AppCard(m, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(58.dp).clip(RoundedCornerShape(4.dp)).background(Color(accents[v.accent])))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(v.cName.ifBlank { "بدون نام مشتری" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${typeNames[v.type]} ${fa(v.no)}  •  ${fa(v.date)}", color = cs.onSurfaceVariant, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${money(v.total)} $cur", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    StatusChip(v.status, v.overdue)
                }
            }
            trailing()
        }
    }
}

fun newInvoice(d: AppData, type: Int): Invoice {
    val s = d.settings
    return Invoice(System.currentTimeMillis(), type, s.prefix + s.next, today(), today(), taxPct = s.tax, notes = s.notes,
        style = s.style, accent = s.accent, font = s.font)
}

// ------------------------------------------------ خانه
@Composable fun HomeScreen(d: AppData, onNew: (Int) -> Unit, onOpen: (Invoice) -> Unit, goTab: (Int) -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val cur = currencyNames[d.settings.currency]
    var period by remember { mutableIntStateOf(0) }
    val nowN = normNum(today())
    val list = d.invoices.filter { it.type == 0 }.filter {
        when (period) { 1 -> normNum(it.date).startsWith(nowN.take(7)); 2 -> normNum(it.date).startsWith(nowN.take(4)); else -> true } }
    val sales = list.sumOf { it.total }
    val received = list.sumOf { minOf(it.paid, it.total) }
    val overdueAmt = list.filter { it.overdue }.sumOf { it.remaining }
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (d.settings.biz.isBlank()) item {
            AppCard(onClick = { goTab(4) }) {
                Text("اطلاعات کسب‌وکار را کامل کنید", fontWeight = FontWeight.Bold)
                Text("نام و شماره تماس شما روی همه‌ی فاکتورها چاپ می‌شود. برای تکمیل اینجا بزنید.", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item { Chips(listOf("همه", "این ماه", "امسال"), period) { period = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("کل فروش", "${money(sales)} $cur", Modifier.weight(1f))
                StatTile("دریافت‌شده", "${money(received)} $cur", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("مانده", "${money((sales - received).coerceAtLeast(0.0))} $cur", Modifier.weight(1f))
                StatTile("سررسید گذشته", "${money(overdueAmt)} $cur", Modifier.weight(1f))
            }
        }
        item {
            Text("ساخت سریع", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PButton("＋ فاکتور", Modifier.weight(1f)) { onNew(0) }
                GButton("پیش‌فاکتور", Modifier.weight(1f)) { onNew(1) }
                GButton("رسید", Modifier.weight(1f)) { onNew(2) }
            }
        }
        item {
            val last = list.take(6).reversed()
            val mx = last.maxOfOrNull { it.total } ?: 0.0
            AppCard {
                Text("روند آخرین فاکتورها", fontWeight = FontWeight.Bold)
                if (last.isEmpty()) Text("هنوز فاکتوری ثبت نشده.", color = cs.onSurfaceVariant, fontSize = 12.sp)
                else Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    last.forEach { v ->
                        val frac = if (mx > 0) (v.total / mx).toFloat() else 0f
                        val hh by animateFloatAsState(if (go) frac.coerceAtLeast(0.06f) else 0f, tween(900), label = "bar")
                        Box(Modifier.width(28.dp).fillMaxHeight(hh).clip(RoundedCornerShape(9.dp)).background(Color(accents[v.accent]).copy(alpha = .9f)))
                    }
                }
            }
        }
        item { Text("آخرین فاکتورها", fontWeight = FontWeight.Bold) }
        if (d.invoices.isEmpty()) item { Text("با دکمه‌ی «فاکتور» اولین فاکتور را بسازید.", color = cs.onSurfaceVariant, fontSize = 13.sp) }
        items(d.invoices.take(5), key = { it.id }) { v ->
            InvoiceRow(v, cur, { onOpen(v) }, Modifier.animateItem()) {
                Text("PDF", Modifier.clickable { sharePdf(ctx, v, d.settings) }.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------ فاکتورها
@Composable fun InvoicesScreen(d: AppData, onNew: (Int) -> Unit, onOpen: (Invoice) -> Unit, upd: (AppData) -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val cur = currencyNames[d.settings.currency]
    var q by remember { mutableStateOf("") }
    var f by remember { mutableIntStateOf(0) }
    var del by remember { mutableStateOf<Invoice?>(null) }
    val list = d.invoices.filter { v ->
        (q.isBlank() || v.cName.contains(q) || v.no.contains(q)) &&
            when (f) { 1 -> v.status == 0; 2 -> v.status == 1; 3 -> v.status == 2; 4 -> v.overdue; 5 -> v.type == 1; else -> true }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(q, { q = it }, "جستجو (نام مشتری یا شماره)", Modifier.weight(1f))
                PButton("＋ جدید", Modifier.width(96.dp)) { onNew(0) }
            }
        }
        item { Chips(listOf("همه", "پرداخت‌نشده", "جزئی", "پرداخت‌شده", "سررسید گذشته", "پیش‌فاکتور"), f) { f = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GButton("خروجی اکسل (CSV)") { exportCsv(ctx, d) }
                Text("${fa(list.size.toString())} مورد", color = cs.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
        if (list.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("🧾\nفاکتوری پیدا نشد", color = cs.onSurfaceVariant) } }
        items(list, key = { it.id }) { v ->
            var menu by remember { mutableStateOf(false) }
            InvoiceRow(v, cur, { onOpen(v) }, Modifier.animateItem()) {
                Box {
                    Text("⋮", Modifier.clickable { menu = true }.padding(10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("ویرایش") }, { menu = false; onOpen(v) })
                        DropdownMenuItem({ Text("خروجی PDF") }, { menu = false; sharePdf(ctx, v, d.settings) })
                        DropdownMenuItem({ Text("ارسال به‌صورت متن") }, { menu = false; shareText(ctx, v, d.settings) })
                        DropdownMenuItem({ Text(if (v.status == 2) "علامت‌گذاری پرداخت‌نشده" else "علامت‌گذاری پرداخت‌شده") }, {
                            menu = false
                            upd(d.copy(invoices = d.invoices.map { if (it.id == v.id) it.copy(paid = if (v.status == 2) 0.0 else v.total) else it }))
                        })
                        if (v.type == 1) DropdownMenuItem({ Text("تبدیل به فاکتور فروش") }, {
                            menu = false
                            val s = d.settings
                            upd(d.copy(settings = s.copy(next = s.next + 1), invoices = d.invoices.map { if (it.id == v.id) it.copy(type = 0, no = s.prefix + s.next) else it }))
                            toast(ctx, "به فاکتور فروش تبدیل شد")
                        })
                        DropdownMenuItem({ Text("کپی فاکتور") }, {
                            menu = false
                            val s = d.settings
                            val c = v.copy(id = System.currentTimeMillis(), no = s.prefix + s.next, date = today(), due = today(), paid = 0.0)
                            upd(d.copy(settings = s.copy(next = s.next + 1), invoices = listOf(c) + d.invoices))
                            toast(ctx, "کپی ساخته شد")
                        })
                        DropdownMenuItem({ Text("حذف", color = Color(0xFFE5534B)) }, { menu = false; del = v })
                    }
                }
            }
        }
    }
    val dv = del
    if (dv != null) AlertDialog(onDismissRequest = { del = null }, title = { Text("حذف فاکتور؟") },
        text = { Text("فاکتور ${fa(dv.no)} برای همیشه پاک می‌شود.") },
        confirmButton = { TextButton({ upd(d.copy(invoices = d.invoices.filter { it.id != dv.id })); del = null }) { Text("حذف", color = Color(0xFFE5534B)) } },
        dismissButton = { TextButton({ del = null }) { Text("انصراف") } })
}

// ------------------------------------------------ مشتریان
@Composable fun ClientsScreen(d: AppData, upd: (AppData) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val cur = currencyNames[d.settings.currency]
    var edit by remember { mutableStateOf<Client?>(null) }
    var q by remember { mutableStateOf("") }
    val list = d.clients.filter { q.isBlank() || it.name.contains(q) || it.phone.contains(q) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(q, { q = it }, "جستجوی مشتری", Modifier.weight(1f))
                PButton("＋ مشتری", Modifier.width(104.dp)) { edit = Client(System.currentTimeMillis(), "") }
            }
        }
        if (list.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("👥\nهنوز مشتری ثبت نشده", color = cs.onSurfaceVariant) } }
        items(list, key = { it.id }) { c ->
            val owed = d.invoices.filter { it.cName == c.name && it.type == 0 }.sumOf { it.remaining }
            AppCard(Modifier.animateItem(), onClick = { edit = c }) {
                Text(c.name, fontWeight = FontWeight.Bold)
                if (c.phone.isNotBlank()) Text(fa(c.phone), color = cs.onSurfaceVariant, fontSize = 12.sp)
                if (owed > 0) Text("مانده حساب: ${money(owed)} $cur", color = Color(0xFFE0A030), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    val e = edit
    if (e != null) {
        var name by remember(e.id) { mutableStateOf(e.name) }
        var phone by remember(e.id) { mutableStateOf(e.phone) }
        var addr by remember(e.id) { mutableStateOf(e.address) }
        var eco by remember(e.id) { mutableStateOf(e.eco) }
        val exists = d.clients.any { it.id == e.id }
        AlertDialog(onDismissRequest = { edit = null }, title = { Text(if (exists) "ویرایش مشتری" else "مشتری جدید") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, "نام", Modifier.fillMaxWidth()); Field(phone, { phone = it }, "تلفن", Modifier.fillMaxWidth(), num = true)
                Field(addr, { addr = it }, "آدرس", Modifier.fillMaxWidth(), multi = true); Field(eco, { eco = it }, "کد اقتصادی / ملی", Modifier.fillMaxWidth(), num = true) } },
            confirmButton = { TextButton({
                if (name.isNotBlank()) {
                    val n = e.copy(name = name.trim(), phone = phone, address = addr, eco = eco)
                    upd(d.copy(clients = if (exists) d.clients.map { if (it.id == e.id) n else it } else listOf(n) + d.clients))
                    edit = null
                }
            }) { Text("ذخیره") } },
            dismissButton = { Row { if (exists) TextButton({ upd(d.copy(clients = d.clients.filter { it.id != e.id })); edit = null }) { Text("حذف", color = Color(0xFFE5534B)) }
                TextButton({ edit = null }) { Text("انصراف") } } })
    }
}

// ------------------------------------------------ کالاها
@Composable fun ProductsScreen(d: AppData, upd: (AppData) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val cur = currencyNames[d.settings.currency]
    var edit by remember { mutableStateOf<Product?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PButton("＋ کالا یا خدمت جدید", Modifier.fillMaxWidth()) { edit = Product(System.currentTimeMillis(), "") } }
        if (d.products.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("📦\nلیست کالا خالی است.\nکالاهای پرتکرار را اینجا ذخیره کنید تا در فاکتور با یک لمس اضافه شوند.", color = cs.onSurfaceVariant) } }
        items(d.products, key = { it.id }) { p ->
            AppCard(Modifier.animateItem(), onClick = { edit = p }) {
                Text(p.name, fontWeight = FontWeight.Bold)
                Text("${money(p.price)} $cur  /  ${p.unit}", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
    val e = edit
    if (e != null) {
        var name by remember(e.id) { mutableStateOf(e.name) }
        var price by remember(e.id) { mutableDoubleStateOf(e.price) }
        var unit by remember(e.id) { mutableStateOf(e.unit) }
        val exists = d.products.any { it.id == e.id }
        AlertDialog(onDismissRequest = { edit = null }, title = { Text(if (exists) "ویرایش کالا" else "کالای جدید") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, "نام کالا / خدمت", Modifier.fillMaxWidth())
                NumField(price, { price = it }, "قیمت واحد", Modifier.fillMaxWidth())
                Field(unit, { unit = it }, "واحد (عدد، متر، ساعت ...)", Modifier.fillMaxWidth()) } },
            confirmButton = { TextButton({
                if (name.isNotBlank()) {
                    val n = e.copy(name = name.trim(), price = price, unit = unit.ifBlank { "عدد" })
                    upd(d.copy(products = if (exists) d.products.map { if (it.id == e.id) n else it } else listOf(n) + d.products))
                    edit = null
                }
            }) { Text("ذخیره") } },
            dismissButton = { Row { if (exists) TextButton({ upd(d.copy(products = d.products.filter { it.id != e.id })); edit = null }) { Text("حذف", color = Color(0xFFE5534B)) }
                TextButton({ edit = null }) { Text("انصراف") } } })
    }
}

// ------------------------------------------------ تنظیمات
@Composable fun SettingsScreen(d: AppData, upd: (AppData) -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val s = d.settings
    fun set(n: Settings) = upd(d.copy(settings = n))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val txt = try { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } } catch (e: Exception) { null }
            val nd = if (txt != null) Store.parse(txt) else null
            if (nd != null) { upd(nd); toast(ctx, "اطلاعات بازیابی شد") } else toast(ctx, "فایل معتبر نیست")
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Section("ظاهر برنامه") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (s.dark) "تم تاریک" else "تم روشن", Modifier.weight(1f))
                Switch(s.dark, { set(s.copy(dark = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = cs.primary, checkedThumbColor = cs.onPrimary))
            } } }
        item { Section("مشخصات کسب‌وکار (روی فاکتور چاپ می‌شود)") {
            Field(s.biz, { set(s.copy(biz = it)) }, "نام کسب‌وکار / فروشنده", Modifier.fillMaxWidth())
            Field(s.phone, { set(s.copy(phone = it)) }, "تلفن", Modifier.fillMaxWidth(), num = true)
            Field(s.addr, { set(s.copy(addr = it)) }, "آدرس", Modifier.fillMaxWidth(), multi = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(s.eco, { set(s.copy(eco = it)) }, "کد اقتصادی / شناسه ملی", Modifier.weight(1f), num = true)
                Field(s.postal, { set(s.copy(postal = it)) }, "کد پستی", Modifier.weight(1f), num = true)
            } } }
        item { Section("پیش‌فرض فاکتورها") {
            Text("واحد پول", fontSize = 13.sp, color = cs.onSurfaceVariant); Chips(currencyNames, s.currency) { set(s.copy(currency = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(s.prefix, { set(s.copy(prefix = it)) }, "پیشوند شماره", Modifier.weight(1f))
                Field(s.next.toString(), { set(s.copy(next = normNum(it).toIntOrNull() ?: s.next)) }, "شماره‌ی بعدی", Modifier.weight(1f), num = true)
            }
            NumField(s.tax, { set(s.copy(tax = it)) }, "مالیات پیش‌فرض (٪)", Modifier.fillMaxWidth())
            Field(s.notes, { set(s.copy(notes = it)) }, "متن پیش‌فرض توضیحات / شرایط", Modifier.fillMaxWidth(), multi = true)
            Text("استایل پیش‌فرض", fontSize = 13.sp, color = cs.onSurfaceVariant); Chips(styleNames, s.style) { set(s.copy(style = it)) }
            Text("فونت پیش‌فرض", fontSize = 13.sp, color = cs.onSurfaceVariant); Chips(fontNames, s.font) { set(s.copy(font = it)) }
        } }
        item { Section("پشتیبان‌گیری") {
            Text("از اطلاعات خود فایل پشتیبان بگیرید و هر زمان بازیابی کنید.", color = cs.onSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GButton("گرفتن پشتیبان") { exportBackup(ctx, d) }
                GButton("بازیابی از فایل") { picker.launch(arrayOf("*/*")) }
            } } }
        item { Section("درباره") {
            Text("فاکتورینو — نسخه‌ی ۱", fontWeight = FontWeight.Bold)
            Text("توسعه توسط تیم شاعرپور", color = cs.onSurfaceVariant, fontSize = 12.sp)
        } }
    }
}
