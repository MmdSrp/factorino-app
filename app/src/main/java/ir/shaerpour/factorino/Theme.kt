package ir.shaerpour.factorino

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

var Vazir: FontFamily = FontFamily.Default

/** فونت از پوشه‌ی assets/fonts خوانده می‌شود؛ اگر نبود، فونت پیش‌فرض گوشی استفاده می‌شود و برنامه خطا نمی‌دهد. */
fun initFonts(c: Context) {
    try {
        val l = c.assets.list("fonts")?.toList() ?: emptyList()
        if ("vazir_regular.ttf" in l && "vazir_bold.ttf" in l) {
            Vazir = FontFamily(
                Font("fonts/vazir_regular.ttf", c.assets, FontWeight.Normal),
                Font("fonts/vazir_bold.ttf", c.assets, FontWeight.Bold)
            )
        }
    } catch (e: Exception) { }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1F2F3), onPrimary = Color(0xFF141518),
    secondary = Color(0xFFB7BCC3), onSecondary = Color(0xFF141518),
    background = Color(0xFF0E0F11), onBackground = Color(0xFFECEDEE),
    surface = Color(0xFF17181B), onSurface = Color(0xFFECEDEE),
    surfaceVariant = Color(0xFF222428), onSurfaceVariant = Color(0xFF9CA2AA),
    outline = Color(0xFF33363B)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E2024), onPrimary = Color.White,
    secondary = Color(0xFF50555C), onSecondary = Color.White,
    background = Color(0xFFF2F3F4), onBackground = Color(0xFF1B1D20),
    surface = Color.White, onSurface = Color(0xFF1B1D20),
    surfaceVariant = Color(0xFFEBEDEF), onSurfaceVariant = Color(0xFF6A7078),
    outline = Color(0xFFD6D9DD)
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography().withFont(Vazir), content = content)
}
