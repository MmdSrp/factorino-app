package ir.shaerpour.factorino

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun Editor(init: Invoice, d: AppData, isNew: Boolean, onSave: (Invoice) -> Unit, onPdf: (Invoice) -> Unit, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val cur = currencyNames[d.settings.currency]
    var type by remember(init.id) { mutableIntStateOf(init.type) }
    var no by remember(init.id) { mutableStateOf(init.no) }
    var date by remember(init.id) { mutableStateOf(init.date) }
    var due by remember(init.id) { mutableStateOf(init.due) }
    var cName by remember(init.id) { mutableStateOf(init.cName) }
    var cPhone by remember(init.id) { mutableStateOf(init.cPhone) }
    var cAddr by remember(init.id) { mutableStateOf(init.cAddr) }
    var cEco by remember(init.id) { mutableStateOf(init.cEco) }
    var items by remember(init.id) { mutableStateOf(init.items) }
    var tax by remember(init.id) { mutableDoubleStateOf(init.taxPct) }
    var disc by remember(init.id) { mutableDoubleStateOf(init.discount) }
    var ship by remember(init.id) { mutableDoubleStateOf(init.shipping) }
    var paid by remember(init.id) { mutableDoubleStateOf(init.paid) }
    var method by remember(init.id) { mutableIntStateOf(init.method) }
    var notes by remember(init.id) { mutableStateOf(init.notes) }
    var style by remember(init.id) { mutableIntStateOf(init.style) }
    var accent by remember(init.id) { mutableIntStateOf(init.accent) }
    var font by remember(init.id) { mutableIntStateOf(init.font) }
    var size by remember(init.id) { mutableFloatStateOf(init.size) }
    var land by remember(init.id) { mutableStateOf(init.landscape) }
    var stamp by remember(init.id) { mutableStateOf(init.stamp) }
    var rev by remember { mutableIntStateOf(0) }
    var clientMenu by remember { mutableStateOf(false) }
    var pickIdx by remember { mutableIntStateOf(-1) }

    fun build() = Invoice(init.id, type, no, date, due, cName, cPhone, cAddr, cEco, items, tax, disc, ship, paid, method, notes,
        style, accent, font, size, land, stamp)
    fun setItem(i: Int, f: (Item) -> Item) { items = items.mapIndexed { j, x -> if (j == i) f(x) else x } }
    val v = build()
    val accColor by androidx.compose.animation.animateColorAsState(Color(accents[accent]), label = "acc")

    Column(Modifier.fillMaxSize()) {
        TopHeader(if (isNew) "فاکتور جدید" else "ویرایش ${typeNames[type]}", "شماره ${fa(no)}") {
            Text("✕", Modifier.clickable { onBack() }.padding(10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Section("نوع و مشخصات") {
                Chips(typeNames, type) { type = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(no, { no = it }, "شماره", Modifier.weight(1f)); Field(date, { date = it }, "تاریخ", Modifier.weight(1f)) }
                if (type == 0) Field(due, { due = it }, "تاریخ سررسید", Modifier.fillMaxWidth())
                Text("نحوه پرداخت", fontSize = 13.sp, color = cs.onSurfaceVariant)
                Chips(methodNames, method) { method = it }
            } }
            item { Section("مشتری") {
                Box {
                    GButton(if (d.clients.isEmpty()) "ابتدا از بخش «مشتریان» مشتری ذخیره کنید" else "انتخاب از مشتریان ▾") { if (d.clients.isNotEmpty()) clientMenu = true }
                    DropdownMenu(clientMenu, { clientMenu = false }) {
                        d.clients.forEach { c -> DropdownMenuItem({ Text(c.name) }, {
                            cName = c.name; cPhone = c.phone; cAddr = c.address; cEco = c.eco; clientMenu = false }) }
                    }
                }
                Field(cName, { cName = it }, "نام مشتری", Modifier.fillMaxWidth())
                Field(cPhone, { cPhone = it }, "تلفن", Modifier.fillMaxWidth(), num = true)
                Field(cAddr, { cAddr = it }, "آدرس", Modifier.fillMaxWidth(), multi = true)
                Field(cEco, { cEco = it }, "کد اقتصادی / ملی", Modifier.fillMaxWidth(), num = true)
            } }
            item { Section("اقلام") {
                items.forEachIndexed { i, it0 ->
                    key(rev, i) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.background).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${fa((i + 1).toString())}", color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Field(it0.name, { x -> setItem(i) { r -> r.copy(name = x) } }, "شرح کالا / خدمت", Modifier.weight(1f))
                                if (d.products.isNotEmpty()) Box {
                                    Text("▾", Modifier.clickable { pickIdx = i }.padding(8.dp), fontSize = 20.sp)
                                    DropdownMenu(pickIdx == i, { pickIdx = -1 }) {
                                        d.products.forEach { p -> DropdownMenuItem({ Text("${p.name}  —  ${money(p.price)}") }, {
                                            setItem(i) { r -> r.copy(name = p.name, price = p.price, unit = p.unit) }; pickIdx = -1; rev++ }) }
                                    }
                                }
                                if (items.size > 1) Text("✕", Modifier.clickable { items = items.filterIndexed { j, _ -> j != i }; rev++ }.padding(8.dp), color = Color(0xFFE5534B), fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumField(it0.qty, { x -> setItem(i) { r -> r.copy(qty = x) } }, "تعداد", Modifier.weight(1f))
                                Field(it0.unit, { x -> setItem(i) { r -> r.copy(unit = x) } }, "واحد", Modifier.weight(1f))
                                NumField(it0.price, { x -> setItem(i) { r -> r.copy(price = x) } }, "قیمت واحد", Modifier.weight(1.5f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumField(it0.discount, { x -> setItem(i) { r -> r.copy(discount = x) } }, "تخفیف ردیف", Modifier.weight(1f))
                                Text("= ${money(it0.amount)} $cur", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                GButton("＋ افزودن ردیف") { items = items + Item(); rev++ }
            } }
            item { Section("محاسبات") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(tax, { tax = it }, "مالیات ٪", Modifier.weight(1f)); NumField(disc, { disc = it }, "تخفیف کل", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(ship, { ship = it }, "هزینه حمل", Modifier.weight(1f)); NumField(paid, { paid = it }, "مبلغ پرداخت‌شده", Modifier.weight(1f)) }
                Row { Text("جمع اقلام", Modifier.weight(1f), color = cs.onSurfaceVariant); Text("${money(v.sub)} $cur") }
                if (v.tax > 0) Row { Text("مالیات", Modifier.weight(1f), color = cs.onSurfaceVariant); Text("${money(v.tax)} $cur") }
                Row { Text("مبلغ قابل پرداخت", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("${money(v.total)} $cur", fontWeight = FontWeight.Bold, color = accColor) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("مانده", Modifier.weight(1f), color = cs.onSurfaceVariant); Text("${money(v.remaining)} $cur"); Spacer(Modifier.width(8.dp)); StatusChip(v.status) }
            } }
            item { Section("توضیحات و شرایط") { Field(notes, { notes = it }, "توضیحات / شرایط پرداخت", Modifier.fillMaxWidth(), multi = true) } }
            item { Section("ظاهر فاکتور (PDF)") {
                Text("استایل", fontSize = 13.sp, color = cs.onSurfaceVariant); Chips(styleNames, style) { style = it }
                Text("رنگ", fontSize = 13.sp, color = cs.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    accents.forEachIndexed { i, c ->
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c)).border(if (accent == i) 3.dp else 1.dp, if (accent == i) cs.onSurface else cs.outline, CircleShape).clickable { accent = i })
                    }
                }
                Text("فونت", fontSize = 13.sp, color = cs.onSurfaceVariant); Chips(fontNames, font) { font = it }
                Text("اندازه متن: ${fa(size.toInt().toString())}", fontSize = 13.sp)
                Slider(size, { size = it }, valueRange = 8f..20f, steps = 11, colors = SliderDefaults.colors(thumbColor = cs.primary, activeTrackColor = cs.primary, inactiveTrackColor = cs.outline))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("جهت صفحه: ${if (land) "افقی" else "عمودی"}", Modifier.weight(1f))
                    Switch(land, { land = it }, colors = SwitchDefaults.colors(checkedTrackColor = cs.primary, checkedThumbColor = cs.onPrimary)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("مهر وضعیت (پرداخت شد / نشده)", Modifier.weight(1f))
                    Switch(stamp, { stamp = it }, colors = SwitchDefaults.colors(checkedTrackColor = cs.primary, checkedThumbColor = cs.onPrimary)) }
            } }
            item { Section("پیش‌نمایش") {
                val ink = Color(0xFF1B1D20)
                val ff = fontFamilies[font]
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.dp, accColor, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${typeNames[type]}  ${fa(no)}", color = accColor, fontWeight = FontWeight.Bold, fontFamily = ff, fontSize = (size + 4).sp)
                    Text("${d.settings.biz.ifBlank { "کسب‌وکار من" }}  ←  ${cName.ifBlank { "مشتری" }}", color = Color(0xFF6A7078), fontFamily = ff, fontSize = size.sp)
                    HorizontalDivider(color = accColor.copy(alpha = .35f))
                    v.items.filter { it.name.isNotBlank() }.forEach { Text("${it.name}   ×${fa(fmtNum(it.qty))}   =   ${money(it.amount)}", color = ink, fontFamily = ff, fontSize = size.sp) }
                    HorizontalDivider(color = accColor.copy(alpha = .35f))
                    Text("جمع کل: ${money(v.total)} $cur", color = accColor, fontWeight = FontWeight.Bold, fontFamily = ff, fontSize = (size + 2).sp)
                }
            } }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GButton("ذخیره", Modifier.weight(1f)) { onSave(build()) }
            PButton("ذخیره و خروجی PDF", Modifier.weight(2f)) { onPdf(build()) }
        }
    }
}
