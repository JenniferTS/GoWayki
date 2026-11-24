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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
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
 *
 * [GowaykiColors] es la paleta cruda de marca (estática). Las pantallas leen
 * colores semánticos con [GowaykiTheme.colors], que cambian con el modo oscuro.
 */
object GowaykiColors {
    /** Naranja corporativo principal. */
    val BrandOrange = Color(0xFFF05709)
    val BrandOrangeHot = Color(0xFFE84513)
    val BrandAmber = Color(0xFFF98E00)
    val BrandClay = Color(0xFFBC5433)
    val BrandInk = Color(0xFF292929)
    val BrandBlue = Color(0xFF1C61A5)
    val BrandBlueDeep = Color(0xFF154A7C)

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
    val TransitOutbound = BrandBlue
    val TransitInbound = Color(0xFFE84513)
    val Success = Color(0xFF2F6F4E)
    val Warning = BrandAmber
    val Amber = BrandAmber
    val Night = Color(0xFF161616)
    val Glow = Color(0x55F05709)
    val Walk = Color(0xFF5C6B73)
    val Origin = Color(0xFF34A853)
    val Destination = Color(0xFFEA4335)
    val UserDot = Color(0xFF1E88E5)
    val MapStop = Color(0xFFC47A1A)
    val Error = Color(0xFFB3261E)
}

/** Colores semánticos; una instancia clara y otra oscura. */
@Immutable
data class GowaykiPalette(
    val isDark: Boolean,
    val primary: Color,
    val primaryStrong: Color,
    val accent: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val background: Color,
    val backgroundAlt: Color,
    val backgroundWarm: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val outlineAccent: Color,
    val disabled: Color,
    val onDisabled: Color,
    val scrim: Color,
    val heroScrim: Color,
    val onHero: Color,
    val onHeroAccent: Color,
    val shadow: Color,
    val glow: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val error: Color,
    val walk: Color,
    val origin: Color,
    val destination: Color,
    val userDot: Color,
    val transitOutbound: Color,
    val transitOutboundDeep: Color,
    val illustrationCard: Color,
    val illustrationSoft: Color,
    val illustrationLine: Color,
    /** Pista de Wayki Combi: asfalto, bordes, líneas de carril, berma y baches. */
    val gameRoad: Color,
    val gameRoadEdge: Color,
    val gameLaneMark: Color,
    val gameVerge: Color,
    val gameVergeDetail: Color,
    val gamePothole: Color,
    val gamePotholeRim: Color,
    val gameCrack: Color,
    val onboardingSoft: List<Color>,
)

private val LightPalette = GowaykiPalette(
    isDark = false,
    primary = GowaykiColors.BrandOrange,
    primaryStrong = GowaykiColors.BrandOrangeHot,
    accent = GowaykiColors.BrandAmber,
    onPrimary = Color.White,
    primarySoft = GowaykiColors.BrandOrange.copy(alpha = 0.12f),
    background = GowaykiColors.Mist,
    backgroundAlt = GowaykiColors.Ivory,
    backgroundWarm = GowaykiColors.Sand,
    surface = GowaykiColors.Surface,
    surfaceElevated = GowaykiColors.SurfaceElevated,
    surfaceSunken = GowaykiColors.Mist,
    onSurface = GowaykiColors.BrandInk,
    onSurfaceMuted = GowaykiColors.Stone,
    outline = GowaykiColors.Outline,
    outlineAccent = GowaykiColors.BrandOrange.copy(alpha = 0.35f),
    disabled = GowaykiColors.Sand,
    onDisabled = GowaykiColors.Stone,
    scrim = Color(0x99000000),
    heroScrim = Color(0xB3000000),
    onHero = Color.White,
    onHeroAccent = GowaykiColors.TerracottaSoft,
    shadow = Color(0x22000000),
    glow = GowaykiColors.Glow,
    success = GowaykiColors.Success,
    onSuccess = Color.White,
    warning = GowaykiColors.BrandAmber,
    error = GowaykiColors.Error,
    walk = GowaykiColors.Walk,
    origin = GowaykiColors.Origin,
    destination = GowaykiColors.Destination,
    userDot = GowaykiColors.UserDot,
    transitOutbound = GowaykiColors.BrandBlue,
    transitOutboundDeep = GowaykiColors.BrandBlueDeep,
    illustrationCard = Color.White,
    illustrationSoft = Color(0xFFFFF7F2),
    illustrationLine = GowaykiColors.Outline,
    gameRoad = Color(0xFF4A4744),
    gameRoadEdge = Color(0xFFF7F1EA),
    gameLaneMark = Color(0xFFF6D46A),
    gameVerge = Color(0xFFD9B48A),
    gameVergeDetail = Color(0xFFB98F64),
    gamePothole = Color(0xFF1C1A19),
    gamePotholeRim = Color(0xFF77706A),
    gameCrack = Color(0xFF2B2826),
    onboardingSoft = listOf(Color(0xFFFFE0CC), Color(0xFFFFF0D6), Color(0xFFD6E8F8)),
)

private val DarkPalette = GowaykiPalette(
    isDark = true,
    primary = GowaykiColors.BrandOrange,
    primaryStrong = GowaykiColors.BrandOrangeHot,
    accent = GowaykiColors.BrandAmber,
    onPrimary = Color.White,
    primarySoft = GowaykiColors.BrandOrange.copy(alpha = 0.22f),
    background = GowaykiColors.Night,
    backgroundAlt = Color(0xFF1C1A19),
    backgroundWarm = Color(0xFF2A1F19),
    surface = Color(0xFF1F1D1C),
    surfaceElevated = Color(0xFF2A2726),
    surfaceSunken = Color(0xFF1A1817),
    onSurface = Color(0xFFF5EFEA),
    onSurfaceMuted = Color(0xFFBDB3AC),
    outline = Color(0xFF453C37),
    outlineAccent = GowaykiColors.BrandOrange.copy(alpha = 0.55f),
    disabled = Color(0xFF3A3431),
    onDisabled = Color(0xFF9C928B),
    scrim = Color(0xB3000000),
    heroScrim = Color(0xB3000000),
    onHero = Color.White,
    onHeroAccent = GowaykiColors.TerracottaSoft,
    shadow = Color(0x66000000),
    glow = GowaykiColors.Glow,
    success = Color(0xFF3E8E64),
    onSuccess = Color.White,
    warning = GowaykiColors.BrandAmber,
    error = Color(0xFFF2B8B5),
    walk = Color(0xFF9AA8AF),
    origin = GowaykiColors.Origin,
    destination = GowaykiColors.Destination,
    userDot = GowaykiColors.UserDot,
    transitOutbound = Color(0xFF4A8FD6),
    transitOutboundDeep = GowaykiColors.BrandBlue,
    illustrationCard = Color(0xFF2A2726),
    illustrationSoft = Color(0xFF332D2A),
    illustrationLine = Color(0xFF4A403A),
    gameRoad = Color(0xFF2F2D2C),
    gameRoadEdge = Color(0xFFBDB3AC),
    gameLaneMark = Color(0xFFD9B54A),
    gameVerge = Color(0xFF4A3B2C),
    gameVergeDetail = Color(0xFF6A5440),
    gamePothole = Color(0xFF0D0C0C),
    gamePotholeRim = Color(0xFF5E5852),
    gameCrack = Color(0xFF161514),
    onboardingSoft = listOf(Color(0xFF3A2416), Color(0xFF3A2C14), Color(0xFF15283A)),
)

val LocalGowaykiPalette = staticCompositionLocalOf { LightPalette }

object GowaykiMotion {
    const val MicroMs = 160
    const val TransitionMs = 280
    const val EmphasizedMs = 420
    const val StaggerMs = 48
    const val StaggerMaxMs = 280L
    const val ConfirmVisibleMs = 2200L
    const val AmbientMs = 2400
    const val AmbientSlowMs = 3200
    const val BounceMs = 1600
    const val WobbleMs = 2200
    /** Cambio de carril en Wayki Combi. */
    const val GameLaneChangeMs = 140
    /** Un hueco mayor entre cuadros significa app en segundo plano: el juego se pausa. */
    const val GamePauseGapMs = 250L

    fun <T> soft() = tween<T>(TransitionMs, easing = FastOutSlowInEasing)
    fun <T> micro() = tween<T>(MicroMs, easing = FastOutSlowInEasing)
    fun <T> emphasized() = tween<T>(EmphasizedMs, easing = FastOutSlowInEasing)
    fun <T> ambient(durationMs: Int = AmbientMs) = tween<T>(durationMs, easing = FastOutSlowInEasing)
    /** One-shot “sin animación”; no usar dentro de infiniteRepeatable. */
    fun <T> none() = tween<T>(1)
    /** Press / logo: bounce contenido, no excesivo. */
    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    /** Hero de onboarding: rebote más lento. */
    fun <T> hero() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
    /** Paneles y hojas: asienta rápido, sin rebote largo. */
    fun <T> panel() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMedium,
    )
    /** Selección de chip / tarjeta. */
    fun <T> selection() = spring<T>(
        dampingRatio = 0.92f,
        stiffness = Spring.StiffnessMediumLow,
    )
}

object GowaykiSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val smd = 12.dp
    val md = 16.dp
    val mdl = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object GowaykiRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 18.dp
    val lg = 28.dp
    val xl = 36.dp
    val pill = 999.dp
}

object GowaykiElevation {
    val none = 0.dp
    val low = 4.dp
    val card = 8.dp
    val panel = 12.dp
    val raised = 16.dp
    val hero = 22.dp
    /** Capas que deben quedar sobre todo (tutorial): el orden Z de Android sigue la elevación. */
    val overlay = 32.dp
}

object GowaykiStroke {
    val hairline = 1.dp
    val soft = 1.5.dp
    val strong = 2.dp
}

object GowaykiSize {
    val iconSm = 16.dp
    val icon = 22.dp
    val iconLg = 28.dp
    val dot = 10.dp
    val stepDot = 12.dp
    val touchTarget = 48.dp
    val buttonHeight = 56.dp
    val softButtonHeight = 52.dp
    val logoSm = 28.dp
    val logo = 44.dp
    val logoLg = 64.dp
    val logoHero = 120.dp
    val sheetHandleWidth = 42.dp
    val sheetHandleHeight = 4.dp
    val pip = 10.dp
    val pipActive = 34.dp
    val mapHero = 340.dp
    val mapCard = 220.dp
    val mapCardTall = 240.dp
    val mapCompact = 200.dp
    val searchHits = 220.dp
    val onboardingHero = 300.dp
    val microThumb = 64.dp
    val gameTrack = 236.dp
    val gamePicker = 64.dp
}

data class MotionPrefs(val reduceMotion: Boolean = false)

val LocalMotionPrefs = staticCompositionLocalOf { MotionPrefs() }

object GowaykiTheme {
    val colors: GowaykiPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalGowaykiPalette.current
}

/**
 * Escala tipográfica del manual:
 * - Righteous → display / wordmark (h3–h4)
 * - Ubuntu → títulos de UI y cuerpo (h5–body)
 * - Poppins → énfasis secundario (subtitle / button / caption / overline)
 */
private fun brandTypography(palette: GowaykiPalette) = Typography(
    h3 = TextStyle(
        fontFamily = GowaykiFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = palette.onSurface,
    ),
    h4 = TextStyle(
        fontFamily = GowaykiFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = palette.onSurface,
    ),
    h5 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = palette.onSurface,
    ),
    h6 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = palette.onSurface,
    ),
    subtitle1 = TextStyle(
        fontFamily = GowaykiFonts.Secondary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = palette.onSurface,
    ),
    subtitle2 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = palette.onSurface,
    ),
    body1 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = palette.onSurface,
    ),
    body2 = TextStyle(
        fontFamily = GowaykiFonts.Primary,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = palette.onSurfaceMuted,
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
        color = palette.onSurfaceMuted,
    ),
    overline = TextStyle(
        fontFamily = GowaykiFonts.Secondary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
        color = palette.onSurfaceMuted,
    ),
)

/** Estilos de marca que no encajan en la escala Material. */
object GowaykiType {
    val heroTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.h3.copy(
            fontFamily = GowaykiFonts.Primary,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            lineHeight = 36.sp,
        )
    val lead: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.body1.copy(fontSize = 17.sp, lineHeight = 24.sp)
    val metric: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Black, fontSize = 18.sp)
    val navLabel: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.overline
}

private fun materialColors(palette: GowaykiPalette) = if (palette.isDark) {
    darkColors(
        primary = palette.primary,
        primaryVariant = palette.primaryStrong,
        secondary = palette.accent,
        background = palette.background,
        surface = palette.surface,
        onPrimary = palette.onPrimary,
        onSecondary = palette.onSurface,
        onBackground = palette.onSurface,
        onSurface = palette.onSurface,
        error = palette.error,
    )
} else {
    lightColors(
        primary = palette.primary,
        primaryVariant = palette.primaryStrong,
        secondary = palette.accent,
        background = palette.background,
        surface = palette.surface,
        onPrimary = palette.onPrimary,
        onSecondary = palette.onSurface,
        onBackground = palette.onSurface,
        onSurface = palette.onSurface,
        error = palette.error,
    )
}

@Composable
fun GowaykiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalMotionPrefs provides MotionPrefs(reduceMotion),
        LocalGowaykiPalette provides palette,
    ) {
        MaterialTheme(
            colors = remember(palette) { materialColors(palette) },
            typography = remember(palette) { brandTypography(palette) },
            content = content,
        )
    }
}
