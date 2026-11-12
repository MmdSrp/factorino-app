package ir.shaerpour.factorino

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

private fun face(c: Context, font: Int, bold: Boolean): Typeface = when (font) {
    1 -> Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    2 -> Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    else -> try { Typeface.createFromAsset(c.assets, if (bold) "fonts/vazir_bold.ttf" else "fonts/vazir_regular.ttf") } catch (e: Exception) { Typeface.DEFAULT }
}

fun makePdf(c: Context, v: Invoice, s: Settings): File {
    val w = if (v.landscape) 842 else 595
    val h = if (v.landscape) 595 else 842
    val m = 34f
    val rEdge = w - m
    val lEdge = m
    val acc = accents[v.accent].toInt()
    val dark = 0xFF1B1D20.toInt()
    val gray = 0xFF6A7078.toInt()
    val line = 0xFFDADDE1.toInt()
    val soft = 0xFFF4F5F6.toInt()
    val white = 0xFFFFFFFF.toInt()
    val sz = v.size
    val lh = sz + 7f
    val reg = face(c, v.font, false)
    val bld = face(c, v.font, true)
    val cur = currencyNames[s.currency]
    val doc = PdfDocument()
    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
    var cv = page.canvas

    fun p(size: Float, bold: Boolean = false, color: Int = dark, al: Paint.Align = Paint.Align.RIGHT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = if (bold) bld else reg; this.color = color; textAlign = al }
    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    fun stroke(color: Int, wd: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = wd }
    fun wrap(t: String, pt: Paint, maxW: Float): List<String> {
        val out = ArrayList<String>()
        for (para in t.split("\n")) {
            var cur0 = ""
            for (word in para.split(" ")) {
                val tryS = if (cur0.isEmpty()) word else "$cur0 $word"
                if (pt.measureText(tryS) <= maxW || cur0.isEmpty()) cur0 = tryS else { out.add(cur0); cur0 = word }
            }
            out.add(cur0)
        }
        return out
    }
    fun footer() {
        cv.drawText(fa("ساخته‌شده با فاکتورینو  •  صفحه $pageNo"), w / 2f, h - 16f, p(8.5f, false, gray, Paint.Align.CENTER))
    }
    fun newPage() {
        footer(); doc.finishPage(page); pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create()); cv = page.canvas
        if (v.style == 3) cv.drawRect(0f, 0f, 12f, h.toFloat(), fill(acc))
    }

    // ---------- هدر ----------
    val titleCol = if (v.style == 1) white else acc
    val textCol = if (v.style == 1) white else dark
    val subCol = if (v.style == 1) 0xFFE3E5E8.toInt() else gray
    when (v.style) {
        0 -> cv.drawRect(0f, 0f, w.toFloat(), 10f, fill(acc))
        1 -> cv.drawRect(0f, 0f, w.toFloat(), 128f, fill(acc))
        3 -> cv.drawRect(0f, 0f, 12f, h.toFloat(), fill(acc))
    }
    var y = 52f
    cv.drawText(s.biz.ifBlank { "کسب‌وکار من" }, rEdge, y, p(sz + 6, true, textCol)); y += lh
    val bizLines = listOf(s.phone, s.addr, if (s.eco.isNotBlank()) "کد اقتصادی: ${s.eco}" else "", if (s.postal.isNotBlank()) "کد پستی: ${s.postal}" else "").filter { it.isNotBlank() }
    bizLines.forEach { cv.drawText(fa(it), rEdge, y, p(sz - 1.5f, false, subCol)); y += lh - 2 }
    var ly = 52f
    cv.drawText(typeNames[v.type], lEdge + (if (v.style == 3) 8f else 0f), ly, p(sz + 10, true, titleCol, Paint.Align.LEFT)); ly += lh + 4
    cv.drawText(fa("شماره: ${v.no}"), lEdge + (if (v.style == 3) 8f else 0f), ly, p(sz, false, textCol, Paint.Align.LEFT)); ly += lh
    cv.drawText(fa("تاریخ: ${v.date}"), lEdge + (if (v.style == 3) 8f else 0f), ly, p(sz, false, textCol, Paint.Align.LEFT)); ly += lh
    if (v.type == 0 && v.due.isNotBlank()) cv.drawText(fa("سررسید: ${v.due}"), lEdge + (if (v.style == 3) 8f else 0f), ly, p(sz, false, textCol, Paint.Align.LEFT))
    y = maxOf(y, 128f) + 14f
    if (v.style == 2 || v.style == 0) cv.drawLine(lEdge, y - 6, rEdge, y - 6, stroke(line))

    // ---------- مشتری ----------
    val cl = listOf(v.cPhone, v.cAddr, if (v.cEco.isNotBlank()) "کد اقتصادی/ملی: ${v.cEco}" else "").filter { it.isNotBlank() }
    val boxH = 30f + (cl.size) * (lh - 2) + 8f
    cv.drawRoundRect(RectF(lEdge, y, rEdge, y + boxH), 10f, 10f, fill(soft))
    cv.drawText("مشتری", rEdge - 12, y + 18, p(sz - 2, false, gray))
    cv.drawText(v.cName.ifBlank { "—" }, rEdge - 12, y + 18 + lh, p(sz + 1, true))
    var cy = y + 18 + lh * 2 - 2
    cl.forEach { cv.drawText(fa(it), rEdge - 12, cy, p(sz - 1.5f, false, gray)); cy += lh - 2 }
    y += boxH + 16

    // ---------- جدول ----------
    val tw = rEdge - lEdge - (if (v.style == 3) 10f else 0f)
    val fr = listOf(0.06f, 0.33f, 0.09f, 0.09f, 0.17f, 0.12f, 0.14f)
    val heads = listOf("ردیف", "شرح کالا / خدمت", "تعداد", "واحد", "قیمت واحد", "تخفیف", "مبلغ")
    val xr = ArrayList<Float>(); val xl = ArrayList<Float>()
    var acc0 = rEdge
    fr.forEach { f -> xr.add(acc0); acc0 -= tw * f; xl.add(acc0) }
    fun tableHead() {
        cv.drawRoundRect(RectF(lEdge, y, rEdge, y + 26f), 8f, 8f, fill(if (v.style == 2) soft else acc))
        val hc = if (v.style == 2) dark else white
        heads.forEachIndexed { i, t ->
            if (i == 1) cv.drawText(t, xr[i] - 6, y + 17, p(sz - 1, true, hc))
            else cv.drawText(t, (xr[i] + xl[i]) / 2, y + 17, p(sz - 1, true, hc, Paint.Align.CENTER))
        }
        y += 26f
    }
    tableHead()
    v.items.filter { it.name.isNotBlank() || it.price != 0.0 }.forEachIndexed { idx, it0 ->
        val pn = p(sz)
        val lines = wrap(it0.name, pn, tw * fr[1] - 12)
        val rowH = lines.size * lh + 6
        if (y + rowH > h - 120) { newPage(); y = 44f; tableHead() }
        if (idx % 2 == 1 && v.style != 2) cv.drawRect(lEdge, y, rEdge, y + rowH, fill(soft))
        lines.forEachIndexed { li, ln -> cv.drawText(ln, xr[1] - 6, y + 15 + li * lh, pn) }
        val mid = y + 15
        fun cen(i: Int, t: String) = cv.drawText(fa(t), (xr[i] + xl[i]) / 2, mid, p(sz, false, dark, Paint.Align.CENTER))
        cen(0, "${idx + 1}"); cen(2, fmtNum(it0.qty).ifBlank { "0" }); cen(3, it0.unit)
        cen(4, money(it0.price)); cen(5, if (it0.discount != 0.0) money(it0.discount) else "—"); cen(6, money(it0.amount))
        cv.drawLine(lEdge, y + rowH, rEdge, y + rowH, stroke(line, 0.6f))
        y += rowH
    }
    y += 14f

    // ---------- جمع‌ها ----------
    val rows = ArrayList<Pair<String, String>>()
    rows.add("جمع اقلام" to money(v.sub))
    if (v.discount > 0) rows.add("تخفیف کل" to "−" + money(v.discount))
    if (v.taxPct > 0) rows.add("مالیات (${fmtNum(v.taxPct)}٪)" to money(v.tax))
    if (v.shipping > 0) rows.add("هزینه حمل" to money(v.shipping))
    val extra = if (v.paid > 0) 2 else 0
    val needed = rows.size * lh + 40 + extra * lh + 150
    if (y + needed > h - 30) { newPage(); y = 44f }
    val bw = 250f
    val bl = lEdge + (if (v.style == 3) 10f else 0f)
    var ty = y
    rows.forEach { (a, b) ->
        cv.drawText(fa(a), bl + bw, ty + 12, p(sz, false, gray)); cv.drawText(b, bl, ty + 12, p(sz, false, dark, Paint.Align.LEFT)); ty += lh
    }
    cv.drawRoundRect(RectF(bl, ty + 2, bl + bw, ty + 34f), 9f, 9f, fill(acc))
    cv.drawText("مبلغ قابل پرداخت ($cur)", bl + bw - 10, ty + 23, p(sz, true, white))
    cv.drawText(money(v.total), bl + 10, ty + 23, p(sz + 2, true, white, Paint.Align.LEFT))
    ty += 44
    if (v.paid > 0) {
        cv.drawText("پرداخت‌شده", bl + bw, ty + 4, p(sz, false, gray)); cv.drawText(money(v.paid), bl, ty + 4, p(sz, false, dark, Paint.Align.LEFT)); ty += lh
        cv.drawText("مانده", bl + bw, ty + 4, p(sz, true, dark)); cv.drawText(money(v.remaining), bl, ty + 4, p(sz, true, dark, Paint.Align.LEFT)); ty += lh
    }
    // توضیحات سمت راست
    var ny = y + 6
    cv.drawText("نحوه پرداخت: ${methodNames[v.method]}", rEdge, ny + 6, p(sz - 1, false, gray)); ny += lh + 4
    if (v.notes.isNotBlank()) {
        cv.drawText("توضیحات", rEdge, ny, p(sz - 1, true, dark)); ny += lh
        wrap(fa(v.notes), p(sz - 1.5f), rEdge - (bl + bw) - 20).forEach { cv.drawText(it, rEdge, ny, p(sz - 1.5f, false, gray)); ny += lh - 2 }
    }
    y = maxOf(ty, ny) + 24

    // ---------- امضا ----------
    val sy = maxOf(y + 20, h - 100f)
    if (sy < h - 20) {
        cv.drawLine(rEdge - 150, sy, rEdge, sy, stroke(gray, 0.8f)); cv.drawText("امضای فروشنده", rEdge - 75, sy + 16, p(sz - 2, false, gray, Paint.Align.CENTER))
        cv.drawLine(lEdge + 10, sy, lEdge + 160, sy, stroke(gray, 0.8f)); cv.drawText("امضای خریدار", lEdge + 85, sy + 16, p(sz - 2, false, gray, Paint.Align.CENTER))
    }
    // ---------- مهر وضعیت ----------
    if (v.stamp && v.type != 1) {
        val (txt, col) = when (v.status) { 2 -> "پرداخت شد" to 0xFF2E9E6B.toInt(); 1 -> "تسویه جزئی" to 0xFFD08A1E.toInt(); else -> "پرداخت نشده" to 0xFFD2453D.toInt() }
        val cx = w / 2f; val cyy = h / 2f + 40
        cv.save(); cv.rotate(-18f, cx, cyy)
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 34f; typeface = bld; color = col; textAlign = Paint.Align.CENTER; alpha = 70 }
        val tw2 = sp.measureText(txt)
        cv.drawRoundRect(RectF(cx - tw2 / 2 - 22, cyy - 38, cx + tw2 / 2 + 22, cyy + 16), 12f, 12f, stroke(col, 3f).apply { alpha = 70 })
        cv.drawText(txt, cx, cyy, sp)
        cv.restore()
    }
    footer()
    doc.finishPage(page)
    val f = File(c.cacheDir, "invoice_${v.no.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "x" }}.pdf")
    f.outputStream().use { doc.writeTo(it) }
    doc.close()
    return f
}

fun shareFile(c: Context, f: File, mime: String, title: String) {
    val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", f)
    c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, title))
}
fun sharePdf(c: Context, v: Invoice, s: Settings) = shareFile(c, makePdf(c, v, s), "application/pdf", "اشتراک‌گذاری فاکتور")

fun shareText(c: Context, v: Invoice, s: Settings) {
    val cur = currencyNames[s.currency]
    val t = buildString {
        appendLine("${typeNames[v.type]} ${fa(v.no)} — ${s.biz}")
        appendLine("مشتری: ${v.cName}   تاریخ: ${fa(v.date)}")
        v.items.filter { it.name.isNotBlank() }.forEach { appendLine("• ${it.name}  ${fa(fmtNum(it.qty))} × ${money(it.price)} = ${money(it.amount)}") }
        appendLine("مبلغ قابل پرداخت: ${money(v.total)} $cur")
        if (v.paid > 0) appendLine("مانده: ${money(v.remaining)} $cur")
        append("— فاکتورینو")
    }
    c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, t) }, "ارسال متن"))
}

fun exportCsv(c: Context, d: AppData) {
    val sb = StringBuilder("\uFEFFشماره,نوع,تاریخ,سررسید,مشتری,جمع کل,پرداخت‌شده,مانده,وضعیت\n")
    d.invoices.forEach { v -> sb.append("${v.no},${typeNames[v.type]},${v.date},${v.due},\"${v.cName}\",${v.total.toLong()},${v.paid.toLong()},${v.remaining.toLong()},${statusNames[v.status]}\n") }
    val f = File(c.cacheDir, "factorino-invoices.csv"); f.writeText(sb.toString())
    shareFile(c, f, "text/csv", "خروجی اکسل")
}
fun exportBackup(c: Context, d: AppData) {
    val f = File(c.cacheDir, "factorino-backup.json"); f.writeText(Store.toJson(d))
    shareFile(c, f, "application/json", "پشتیبان‌گیری")
}
