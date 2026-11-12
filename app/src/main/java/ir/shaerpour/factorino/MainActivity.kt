package ir.shaerpour.factorino

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFonts(this)
        setContent { Root() }
    }
}

@Composable fun Root() {
    val ctx = LocalContext.current
    val view = LocalView.current
    var data by remember { mutableStateOf(Store.load(ctx)) }
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(2700); splash = false }
    val dark = data.settings.dark
    SideEffect {
        val w = (ctx as Activity).window
        val ic = WindowCompat.getInsetsController(w, view)
        ic.isAppearanceLightStatusBars = !dark
        ic.isAppearanceLightNavigationBars = !dark
    }
    AppTheme(dark) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Crossfade(splash, animationSpec = tween(600), label = "root") { s ->
                    if (s) Splash() else Main(data) { nd -> data = nd; Store.save(ctx, nd) }
                }
            }
        }
    }
}

@Composable fun Main(data: AppData, upd: (AppData) -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Invoice?>(null) }
    var isNew by remember { mutableStateOf(false) }
    fun openNew(type: Int) { editing = newInvoice(data, type); isNew = true }
    fun openOld(v: Invoice) { editing = v; isNew = false }
    fun save(inv: Invoice, isNewInv: Boolean) {
        val exists = data.invoices.any { it.id == inv.id }
        val list = if (exists) data.invoices.map { if (it.id == inv.id) inv else it } else listOf(inv) + data.invoices
        val st = if (!exists && isNewInv) data.settings.copy(next = data.settings.next + 1) else data.settings
        upd(data.copy(invoices = list, settings = st))
    }
    val e = editing
    if (e != null) {
        BackHandler { editing = null }
        Editor(e, data, isNew, onSave = { save(it, isNew); editing = null },
            onPdf = { save(it, isNew); isNew = false; editing = it; sharePdf(ctx, it, data.settings) }, onBack = { editing = null })
    } else {
        Column(Modifier.fillMaxSize()) {
            val titles = listOf("خانه" to "خلاصه‌ی کسب‌وکار شما", "فاکتورها" to "همه‌ی فاکتورها و پیش‌فاکتورها",
                "مشتریان" to "دفترچه‌ی مشتریان", "کالاها" to "کالاها و خدمات پرتکرار", "تنظیمات" to "مشخصات و پیش‌فرض‌ها")
            TopHeader(if (tab == 0 && data.settings.biz.isNotBlank()) data.settings.biz else titles[tab].first, titles[tab].second)
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(data, { openNew(it) }, { openOld(it) }, { tab = it })
                    1 -> InvoicesScreen(data, { openNew(it) }, { openOld(it) }, upd)
                    2 -> ClientsScreen(data, upd)
                    3 -> ProductsScreen(data, upd)
                    else -> SettingsScreen(data, upd)
                }
            }
            NavBar(tab) { tab = it }
        }
    }
}
