package pe.edu.tecsup.gowayki.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Identidad visual GoWayki Go (manual + hoja de marca).
 * Primarios: #F05709 / #292929 · acentos: #F98E00 / #E84513 / #BC5433
 * Tipografías (manual): Righteous · Ubuntu · Poppins.
 */
object GowaykiColors {
    /** Naranja corporativo principal. */
    val BrandOrange = Color(0xFFF05709)
    val BrandOrangeHot = Color(0xFFE84513)
    val BrandAmber = Color(0xFFF98E00)
    val BrandClay = Color(0xFFBC5433)
    val BrandInk = Color(0xFF292929)

    // Aliases usados en UI existente
    val Terracotta = BrandOrange
    val TerracottaDark = BrandOrangeHot
    val TerracottaSoft = Color(0xFFFFC4A3)
    val Sand = Color(0xFFFFE4D4)
    val Mist = Color(0xFFFFF7F2)
    val Ivory = Color(0xFFFFFCFA)
    val Charcoal = BrandInk
    val Stone = Color(0xFF6B6560)
    val Surface = Color(0xFFFFFCFA)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val Outline = Color(0xFFE8D5CA)
    val TransitOutbound = Color(0xFF1C61A5)
    val TransitInbound = Color(0xFFE84513)
    val Success = Color(0xFF2F6F4E)
    val Warning = BrandAmber
    val Amber = BrandAmber
    val Night = Color(0xFF161616)
    val Glow = Color(0x55F05709)
}

object GowaykiMotion {
    const val MicroMs = 160
    const val TransitionMs = 280
    const val EmphasizedMs = 420
    const val StaggerMs = 55

    fun <T> soft() = tween<T>(TransitionMs, easing = FastOutSlowInEasing)
    fun <T> micro() = tween<T>(MicroMs, easing = FastOutSlowInEasing)
    fun <T> emphasized() = tween<T>(EmphasizedMs, easing = FastOutSlowInEasing)
    fun <T> none() = tween<T>(0)
    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

object GowaykiSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object GowaykiRadius {
    val sm = 12.dp
    val md = 18.dp
    val lg = 28.dp
    val pill = 999.dp
}

data class MotionPrefs(val reduceMotion: Boolean = false)

val LocalMotionPrefs = staticCompositionLocalOf { MotionPrefs() }

/**
 * Escala tipográfica del manual:
 * - Righteous → display / wordmark (h3–h4)
 * - Ubuntu → títulos de UI y cuerpo (h5–body)
 * - Poppins → énfasis secundario (subtitle / button / caption)
 */
private val BrandTypography = Typography(
    h3 = TextStyle(
        fontFamily = GowaykiFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = GowaykiColors.Charcoal,
    ),
    h4 = TextStyle(
        fontFamily = GowaykiFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = GowaykiColors.Charcoal,
    ),
    h5 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = GowaykiColors.Charcoal,
    ),
    h6 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = GowaykiColors.Charcoal,
    ),
    subtitle1 = TextStyle(
        fontFamily = GowaykiFonts.Secondary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = GowaykiColors.Charcoal,
    ),
    body1 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = GowaykiColors.Charcoal,
    ),
    body2 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = GowaykiColors.Stone,
    ),
    button = TextStyle(
        fontFamily = GowaykiFonts.Secondary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp,
    ),
    caption = TextStyle(
        fontFamily = GowaykiFonts.Secondary,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = GowaykiColors.Stone,
    ),
)

private val Light = lightColors(
    primary = GowaykiColors.BrandOrange,
    primaryVariant = GowaykiColors.BrandOrangeHot,
    secondary = GowaykiColors.BrandAmber,
    background = GowaykiColors.Mist,
    surface = GowaykiColors.Surface,
    onPrimary = Color.White,
    onSecondary = GowaykiColors.Charcoal,
    onBackground = GowaykiColors.Charcoal,
    onSurface = GowaykiColors.Charcoal,
    error = Color(0xFFB3261E),
)

private val Dark = darkColors(
    primary = GowaykiColors.BrandAmber,
    primaryVariant = GowaykiColors.BrandOrange,
    secondary = GowaykiColors.BrandOrange,
    background = GowaykiColors.Night,
    surface = Color(0xFF242424),
    onPrimary = GowaykiColors.Charcoal,
    onSecondary = Color.White,
    onBackground = GowaykiColors.Ivory,
    onSurface = GowaykiColors.Ivory,
)

@Composable
fun GowaykiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMotionPrefs provides MotionPrefs(reduceMotion)) {
        MaterialTheme(
            colors = if (darkTheme) Dark else Light,
            typography = BrandTypography,
            content = content,
        )
    }
}
