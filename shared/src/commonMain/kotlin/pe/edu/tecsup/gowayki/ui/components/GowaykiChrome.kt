package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import pe.edu.tecsup.gowayki.ui.LocalStrings
import pe.edu.tecsup.gowayki.ui.theme.GowaykiElevation
import pe.edu.tecsup.gowayki.ui.theme.GowaykiFonts
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiStroke
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.GowaykiType
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

@Composable
fun AtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    val infinite = rememberInfiniteTransition(label = "atmosphere")
    val glowAnim by infinite.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = GowaykiMotion.ambient(),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atmosphereGlow",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.backgroundAlt, colors.backgroundWarm.copy(alpha = 0.42f)),
                ),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (reduce) 0.4f else glowAnim }
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(80f, 40f),
                        radius = 820f,
                    ),
                ),
        )
        content()
    }
}

@Composable
fun EnterContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    var visible by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
            slideInVertically(
                animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.soft(),
                initialOffsetY = { if (reduce) 0 else 22 },
            ),
    ) { content() }
}

/**
 * Pantalla estándar: fondo de marca, entrada animada, cabecera fija y cuerpo.
 * El cuerpo decide si desplaza (Column con scroll o LazyColumn).
 */
@Composable
fun GowaykiScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AtmosphereBackground(modifier) {
        EnterContent(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = GowaykiSpace.md, vertical = GowaykiSpace.sm)) {
                ScreenHeader(title, onBack = onBack, action = action)
                content()
            }
        }
    }
}

/**
 * Entrada escalonada. [enterKey] fija el plan/lista: al cambiar selección
 * no se reanima; solo al llegar un nuevo conjunto de resultados.
 */
@Composable
fun StaggerIn(
    index: Int,
    modifier: Modifier = Modifier,
    enterKey: Any? = Unit,
    content: @Composable () -> Unit,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    var visible by remember(enterKey) { mutableStateOf(reduce) }
    LaunchedEffect(enterKey, index) {
        if (visible) return@LaunchedEffect
        if (!reduce) delay((index * GowaykiMotion.StaggerMs).toLong().coerceAtMost(GowaykiMotion.StaggerMaxMs))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
            slideInHorizontally(
                animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.soft(),
                initialOffsetX = { if (reduce) 0 else 20 },
            ),
        exit = fadeOut(GowaykiMotion.micro()),
    ) { content() }
}

@Composable
fun Wordmark(
    large: Boolean,
    light: Boolean = false,
    textAlign: TextAlign? = null,
) {
    val colors = GowaykiTheme.colors
    val ink = if (light) colors.onHero else colors.onSurface
    val go = if (light) colors.onHeroAccent else colors.primary
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = go, fontFamily = GowaykiFonts.Display)) { append("go") }
            withStyle(SpanStyle(color = ink, fontFamily = GowaykiFonts.Display)) { append("wayki") }
            append(" ")
            withStyle(SpanStyle(color = go, fontFamily = GowaykiFonts.Display)) { append("go") }
        },
        style = if (large) MaterialTheme.typography.h3 else MaterialTheme.typography.h4,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
fun BrandMark(
    subtitle: String? = null,
    large: Boolean = false,
    showLogo: Boolean = true,
    light: Boolean = false,
) {
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    val logoScale = remember { Animatable(if (reduce) 1f else 0.82f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            logoScale.animateTo(1f, GowaykiMotion.snappy())
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showLogo) {
            BrandLogo(
                size = if (large) GowaykiSize.logoLg else GowaykiSize.logo,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            )
            Spacer(Modifier.width(GowaykiSpace.smd))
        }
        Column {
            Wordmark(large = large, light = light)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.body2,
                    color = if (light) colors.onHero.copy(alpha = 0.92f) else colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Logo centrado grande (login / splash) con wordmark debajo. */
@Composable
fun BrandHero(
    subtitle: String? = null,
    logoSize: Dp = GowaykiSize.logoHero,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = remember { Animatable(if (reduce) 1f else 0.86f) }
    LaunchedEffect(Unit) {
        if (!reduce) scale.animateTo(1f, GowaykiMotion.snappy())
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BrandLogo(
            size = logoSize,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
        Spacer(Modifier.height(GowaykiSpace.md))
        Wordmark(large = true, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(GowaykiSpace.sm))
            Text(
                subtitle,
                style = MaterialTheme.typography.body1,
                color = GowaykiTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun pressScale(
    reduce: Boolean,
    pressed: Boolean,
): Float {
    val target = when {
        reduce -> 1f
        pressed -> 0.94f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = GowaykiMotion.snappy(),
        label = "btnScale",
    )
    return scale
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GowaykiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = pressScale(reduce, pressed && enabled)
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GowaykiSize.buttonHeight)
            .scale(scale)
            .shadow(
                elevation = if (enabled) if (pressed) GowaykiElevation.low else GowaykiElevation.panel else GowaykiElevation.none,
                shape = shape,
                spotColor = colors.glow,
                ambientColor = colors.primary.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(colors.primaryStrong, colors.primary, colors.accent))
                } else {
                    Brush.horizontalGradient(listOf(colors.disabled, colors.disabled))
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) colors.onPrimary else colors.onDisabled,
            style = MaterialTheme.typography.button,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GowaykiSpace.md, vertical = GowaykiSpace.smd),
        )
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = GowaykiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = pressScale(reduce, pressed && enabled)
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GowaykiSize.softButtonHeight)
            .scale(scale)
            .shadow(
                elevation = if (pressed) GowaykiElevation.none else GowaykiElevation.low,
                shape = shape,
                spotColor = colors.shadow,
            )
            .clip(shape)
            .background(colors.surfaceElevated)
            .background(if (selected) colors.primarySoft else Color.Transparent)
            .border(
                if (selected) GowaykiStroke.strong else GowaykiStroke.soft,
                if (selected) colors.primary else colors.outlineAccent,
                shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> colors.onDisabled
                selected -> colors.primary
                else -> colors.onSurface
            },
            style = MaterialTheme.typography.button,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GowaykiSpace.smd, vertical = GowaykiSpace.sm),
        )
    }
}

/** Acción destructiva e irreversible (eliminar cuenta): borde y texto en el color de error. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GowaykiTheme.colors
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GowaykiSize.softButtonHeight)
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(GowaykiStroke.strong, if (enabled) colors.error else colors.disabled, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) colors.error else colors.onDisabled,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GowaykiSpace.smd, vertical = GowaykiSpace.sm),
        )
    }
}

/** Fila de opciones exclusivas (idioma, alarma…) que marca la elegida. */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
        options.forEach { (value, label) ->
            SoftButton(
                text = label,
                onClick = { onSelect(value) },
                selected = value == selected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GowaykiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = if (onClick == null) 1f else pressScale(reduce, pressed)
    val shape = RoundedCornerShape(GowaykiRadius.lg)
    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(GowaykiElevation.panel, shape, spotColor = colors.shadow)
            .then(
                if (selected) Modifier.border(GowaykiStroke.strong, colors.primary.copy(alpha = 0.6f), shape) else Modifier,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        color = colors.surfaceElevated,
        contentColor = colors.onSurface,
        shape = shape,
        elevation = GowaykiElevation.none,
    ) {
        Column(Modifier.padding(GowaykiSpace.md)) { content() }
    }
}

/** Título de sección con separación consistente. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.h6,
        modifier = modifier.padding(top = GowaykiSpace.mdl, bottom = GowaykiSpace.sm),
    )
}

/** Mensaje en línea (error o información) con el color semántico correcto. */
@Composable
fun InlineMessage(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    val colors = GowaykiTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.body2,
        color = if (isError) colors.error else colors.onSurfaceMuted,
        modifier = modifier.padding(vertical = GowaykiSpace.xs),
    )
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    activeIndex: Int,
) {
    val colors = GowaykiTheme.colors
    Surface(
        color = colors.surfaceElevated.copy(alpha = 0.98f),
        contentColor = colors.onSurface,
        elevation = GowaykiElevation.raised,
        shape = RoundedCornerShape(topStart = GowaykiRadius.lg, topEnd = GowaykiRadius.lg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = GowaykiSpace.xs, vertical = GowaykiSpace.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NavCell(item = item, active = index == activeIndex)
            }
        }
    }
}

@Composable
private fun RowScope.NavCell(item: NavItem, active: Boolean) {
    val colors = GowaykiTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = GowaykiMotion.snappy(),
        label = "navScale",
    )
    val tint = if (active) colors.primary else colors.onSurfaceMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(GowaykiRadius.md))
            .background(if (active) colors.primarySoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = item.onClick)
            .semantics { selected = active }
            .padding(horizontal = GowaykiSpace.xs, vertical = GowaykiSpace.sm),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(GowaykiSize.icon),
        )
        Spacer(Modifier.height(GowaykiSpace.xs))
        Text(
            item.label,
            style = GowaykiType.navLabel,
            color = tint,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Confirmación breve vinculada a una acción (p. ej. reporte enviado). */
@Composable
fun ActionConfirmBanner(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
            slideInVertically(
                animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.panel(),
                initialOffsetY = { if (reduce) 0 else 24 },
            ),
        exit = fadeOut(GowaykiMotion.micro()),
    ) {
        Surface(
            color = colors.success,
            contentColor = colors.onSuccess,
            shape = RoundedCornerShape(GowaykiRadius.md),
            elevation = GowaykiElevation.overlay,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.subtitle1,
                color = colors.onSuccess,
                modifier = Modifier.padding(horizontal = GowaykiSpace.md, vertical = GowaykiSpace.smd),
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = GowaykiTheme.colors
    val backLabel = LocalStrings.current.navigateBack
    Row(
        Modifier.fillMaxWidth().padding(bottom = GowaykiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(GowaykiSize.touchTarget)
                    .clip(RoundedCornerShape(GowaykiRadius.pill))
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    GowaykiIcons.Back,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(GowaykiSize.icon),
                )
            }
            Spacer(Modifier.width(GowaykiSpace.xs))
        }
        Text(
            title,
            style = MaterialTheme.typography.h5,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}
