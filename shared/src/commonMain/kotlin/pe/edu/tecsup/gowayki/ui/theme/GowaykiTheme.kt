package pe.edu.tecsup.gowayki.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tokens de marca (zorro andino / transporte Arequipa). */
object GowaykiColors {
    val Terracotta = Color(0xFFC45A32)
    val TerracottaDark = Color(0xFF8E3D22)
    val TerracottaSoft = Color(0xFFF0C2AE)
    val Sand = Color(0xFFE8D5BC)
    val Mist = Color(0xFFF6F1EA)
    val Ivory = Color(0xFFFAF7F2)
    val Charcoal = Color(0xFF1A1714)
    val Stone = Color(0xFF6E675F)
    val Surface = Color(0xFFFFFCF8)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val Outline = Color(0xFFDDD4CA)
    val TransitOutbound = Color(0xFF1F6FE5)
    val TransitInbound = Color(0xFFC93B6E)
    val Success = Color(0xFF2F6F4E)
    val Warning = Color(0xFFC47A1A)
    val Night = Color(0xFF12100E)
    val Glow = Color(0x33C45A32)
}

object GowaykiMotion {
    const val MicroMs = 160
    const val TransitionMs = 280
    const val EmphasizedMs = 420

    fun <T> soft() = tween<T>(TransitionMs, easing = FastOutSlowInEasing)
    fun <T> micro() = tween<T>(MicroMs, easing = FastOutSlowInEasing)
    fun <T> none() = tween<T>(0)
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

private val BrandTypography = Typography(
    h3 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = GowaykiColors.Charcoal,
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = GowaykiColors.Charcoal,
    ),
    h5 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = GowaykiColors.Charcoal,
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = GowaykiColors.Charcoal,
    ),
    subtitle1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = GowaykiColors.Charcoal,
    ),
    body1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = GowaykiColors.Charcoal,
    ),
    body2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = GowaykiColors.Stone,
    ),
    button = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = GowaykiColors.Stone,
    ),
)

private val Light = lightColors(
    primary = GowaykiColors.Terracotta,
    primaryVariant = GowaykiColors.TerracottaDark,
    secondary = GowaykiColors.Sand,
    background = GowaykiColors.Mist,
    surface = GowaykiColors.Surface,
    onPrimary = Color.White,
    onSecondary = GowaykiColors.Charcoal,
    onBackground = GowaykiColors.Charcoal,
    onSurface = GowaykiColors.Charcoal,
    error = Color(0xFFB3261E),
)

private val Dark = darkColors(
    primary = GowaykiColors.TerracottaSoft,
    primaryVariant = GowaykiColors.Terracotta,
    secondary = GowaykiColors.Terracotta,
    background = GowaykiColors.Night,
    surface = Color(0xFF24201C),
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
