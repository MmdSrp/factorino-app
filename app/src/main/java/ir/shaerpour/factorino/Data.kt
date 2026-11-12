package ir.shaerpour.factorino

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale

val typeNames = listOf("فاکتور فروش", "پیش‌فاکتور", "رسید پرداخت")
val methodNames = listOf("نقدی", "کارت‌به‌کارت", "POS", "چک")
val styleNames = listOf("کلاسیک", "مدرن", "مینیمال", "نواری")
val accentNames = listOf("ذغالی", "آبی", "سبز", "قرمز", "کهربایی", "بنفش")
val accents = listOf(0xFF2B2D31L, 0xFF2563EBL, 0xFF059669L, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL)
val fontNames = listOf("وزیر", "سریف", "تایپ‌رایتر")
val currencyNames = listOf("تومان", "ریال")
val statusNames = listOf("پرداخت‌نشده", "پرداخت جزئی", "پرداخت‌شده")

fun fa(s: String): String = buildString { for (c in s) append(if (c in '0'..'9') '۰' + (c - '0') else c) }
fun normNum(s: String): String = buildString {
    for (c in s) when (c) {
        in '۰'..'۹' -> append('0' + (c - '۰'))
        in '٠'..'٩' -> append('0' + (c - '٠'))
        '٬', ',' -> {}
        '٫' -> append('.')
        else -> append(c)
    }
}
fun money(v: Double): String = fa(String.format(Locale.US, "%,.0f", v))
fun fmtNum(d: Double): String = if (d == 0.0) "" else BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
fun today(): String = android.icu.text.SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("fa-IR-u-ca-persian")).format(java.util.Date())

data class Item(val name: String = "", val qty: Double = 1.0, val price: Double = 0.0, val unit: String = "عدد", val discount: Double = 0.0) {
    val amount get() = qty * price - discount
}
data class Client(val id: Long, val name: String, val phone: String = "", val address: String = "", val eco: String = "", val postal: String = "")
data class Product(val id: Long, val name: String, val price: Double = 0.0, val unit: String = "عدد")

data class Invoice(
    val id: Long, val type: Int, val no: String, val date: String, val due: String,
    val cName: String = "", val cPhone: String = "", val cAddr: String = "", val cEco: String = "",
    val items: List<Item> = listOf(Item()), val taxPct: Double = 0.0, val discount: Double = 0.0,
    val shipping: Double = 0.0, val paid: Double = 0.0, val method: Int = 0, val notes: String = "",
    val style: Int = 0, val accent: Int = 0, val font: Int = 0, val size: Float = 12f,
    val landscape: Boolean = false, val stamp: Boolean = true
) {
    val sub get() = items.sumOf { it.amount }
    val afterDisc get() = (sub - discount).coerceAtLeast(0.0)
    val tax get() = afterDisc * taxPct / 100
    val total get() = afterDisc + tax + shipping
    val remaining get() = (total - paid).coerceAtLeast(0.0)
    val status get() = if (paid <= 0.0) 0 else if (total > 0 && paid >= total) 2 else 1
    val overdue get() = type == 0 && status != 2 && normNum(due) < normNum(today()) && due.isNotBlank()
}

data class Settings(
    val biz: String = "", val phone: String = "", val addr: String = "", val eco: String = "", val postal: String = "",
    val currency: Int = 0, val tax: Double = 0.0, val prefix: String = "F-", val next: Int = 1001,
    val notes: String = "", val dark: Boolean = true, val style: Int = 0, val accent: Int = 0, val font: Int = 0
)
data class AppData(
    val settings: Settings = Settings(), val clients: List<Client> = emptyList(),
    val products: List<Product> = emptyList(), val invoices: List<Invoice> = emptyList()
)

private fun <T> JSONArray.map2(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("factorino", Context.MODE_PRIVATE)
    fun load(c: Context): AppData = parse(prefs(c).getString("data", "") ?: "") ?: AppData()
    fun save(c: Context, d: AppData) { prefs(c).edit().putString("data", toJson(d)).apply() }

    fun toJson(d: AppData): String {
        val s = d.settings
        return JSONObject().apply {
            put("settings", JSONObject().put("biz", s.biz).put("phone", s.phone).put("addr", s.addr).put("eco", s.eco)
                .put("postal", s.postal).put("cur", s.currency).put("tax", s.tax).put("prefix", s.prefix).put("next", s.next)
                .put("notes", s.notes).put("dark", s.dark).put("style", s.style).put("accent", s.accent).put("font", s.font))
            put("clients", JSONArray().also { a -> d.clients.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)
                .put("phone", it.phone).put("addr", it.address).put("eco", it.eco).put("postal", it.postal)) } })
            put("products", JSONArray().also { a -> d.products.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)
                .put("price", it.price).put("unit", it.unit)) } })
            put("invoices", JSONArray().also { a -> d.invoices.forEach { v -> a.put(JSONObject().apply {
                put("id", v.id); put("type", v.type); put("no", v.no); put("date", v.date); put("due", v.due)
                put("cName", v.cName); put("cPhone", v.cPhone); put("cAddr", v.cAddr); put("cEco", v.cEco)
                put("items", JSONArray().also { ja -> v.items.forEach { ja.put(JSONObject().put("n", it.name).put("q", it.qty)
                    .put("p", it.price).put("u", it.unit).put("d", it.discount)) } })
                put("tax", v.taxPct); put("disc", v.discount); put("ship", v.shipping); put("paid", v.paid)
                put("method", v.method); put("notes", v.notes); put("style", v.style); put("accent", v.accent)
                put("font", v.font); put("size", v.size.toDouble()); put("land", v.landscape); put("stamp", v.stamp)
            }) } })
        }.toString()
    }

    fun parse(txt: String): AppData? = try {
        val o = JSONObject(txt)
        val s = o.getJSONObject("settings")
        AppData(
            Settings(s.optString("biz"), s.optString("phone"), s.optString("addr"), s.optString("eco"), s.optString("postal"),
                s.optInt("cur", 0), s.optDouble("tax", 0.0), s.optString("prefix", "F-"), s.optInt("next", 1001),
                s.optString("notes"), s.optBoolean("dark", true), s.optInt("style", 0), s.optInt("accent", 0), s.optInt("font", 0)),
            o.optJSONArray("clients")?.map2 { Client(it.getLong("id"), it.optString("name"), it.optString("phone"), it.optString("addr"), it.optString("eco"), it.optString("postal")) } ?: emptyList(),
            o.optJSONArray("products")?.map2 { Product(it.getLong("id"), it.optString("name"), it.optDouble("price", 0.0), it.optString("unit", "عدد")) } ?: emptyList(),
            o.optJSONArray("invoices")?.map2 { v ->
                Invoice(v.getLong("id"), v.optInt("type"), v.optString("no"), v.optString("date"), v.optString("due"),
                    v.optString("cName"), v.optString("cPhone"), v.optString("cAddr"), v.optString("cEco"),
                    v.optJSONArray("items")?.map2 { Item(it.optString("n"), it.optDouble("q", 1.0), it.optDouble("p", 0.0), it.optString("u", "عدد"), it.optDouble("d", 0.0)) } ?: listOf(Item()),
                    v.optDouble("tax", 0.0), v.optDouble("disc", 0.0), v.optDouble("ship", 0.0), v.optDouble("paid", 0.0),
                    v.optInt("method"), v.optString("notes"), v.optInt("style"), v.optInt("accent"), v.optInt("font"),
                    v.optDouble("size", 12.0).toFloat(), v.optBoolean("land"), v.optBoolean("stamp", true))
            } ?: emptyList()
        )
    } catch (e: Exception) { null }
}
