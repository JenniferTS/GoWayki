package pe.edu.tecsup.gowayki.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Tokens de marca (zorro andino / transporte Arequipa). */
object GowaykiColors {
    val Terracotta = Color(0xFFB9532F)
    val TerracottaDark = Color(0xFF8E3D22)
    val Sand = Color(0xFFE9D8C2)
    val Ivory = Color(0xFFFAF7F2)
    val Charcoal = Color(0xFF252321)
    val Stone = Color(0xFF756E67)
    val Surface = Color(0xFFFFFFFF)
    val Outline = Color(0xFFE2DCD6)
    val TransitOutbound = Color(0xFF2E86DE)
    val TransitInbound = Color(0xFFD6336C)
    val Success = Color(0xFF2F6F4E)
    val Warning = Color(0xFFC47A1A)
}

object GowaykiMotion {
    const val MicroMs = 150
    const val TransitionMs = 240
}

object GowaykiSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

data class MotionPrefs(val reduceMotion: Boolean = false)

val LocalMotionPrefs = staticCompositionLocalOf { MotionPrefs() }

private val Light = lightColors(
    primary = GowaykiColors.Terracotta,
    primaryVariant = GowaykiColors.TerracottaDark,
    secondary = GowaykiColors.Sand,
    background = GowaykiColors.Ivory,
    surface = GowaykiColors.Surface,
    onPrimary = Color.White,
    onSecondary = GowaykiColors.Charcoal,
    onBackground = GowaykiColors.Charcoal,
    onSurface = GowaykiColors.Charcoal,
    error = Color(0xFFB3261E),
)

private val Dark = darkColors(
    primary = GowaykiColors.Sand,
    primaryVariant = GowaykiColors.Terracotta,
    secondary = GowaykiColors.Terracotta,
    background = GowaykiColors.Charcoal,
    surface = Color(0xFF2F2C2A),
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
            typography = Typography(),
            content = content,
        )
    }
}
