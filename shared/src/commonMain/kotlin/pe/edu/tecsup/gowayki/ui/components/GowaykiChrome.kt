package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiFonts
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

@Composable
fun AtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    val infinite = rememberInfiniteTransition(label = "atmosphere")
    val glowAnim by infinite.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atmosphereGlow",
    )
    val glow = if (reduce) 0.4f else glowAnim
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GowaykiColors.Mist, GowaykiColors.Ivory, GowaykiColors.Sand.copy(alpha = 0.42f)),
                ),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GowaykiColors.Terracotta.copy(alpha = glow * 0.2f), Color.Transparent),
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

/** Entrada escalonada (cards, chips, pasos). */
@Composable
fun StaggerIn(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    var visible by remember { mutableStateOf(reduce) }
    LaunchedEffect(index) {
        if (!reduce) delay((index * GowaykiMotion.StaggerMs).toLong().coerceAtMost(360))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
            slideInHorizontally(
                animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.soft(),
                initialOffsetX = { if (reduce) 0 else 28 },
            ),
        exit = fadeOut(GowaykiMotion.micro()),
    ) { content() }
}

@Composable
fun BrandMark(
    subtitle: String? = null,
    large: Boolean = false,
    showLogo: Boolean = true,
    light: Boolean = false,
) {
    val ink = if (light) Color.White else GowaykiColors.BrandInk
    val go = if (light) Color(0xFFFFC4A3) else GowaykiColors.BrandOrange
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
                size = if (large) 64.dp else 44.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            )
            Spacer(Modifier.size(12.dp))
        }
        Column {
            // Wordmark: Righteous (display del manual)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = go, fontFamily = GowaykiFonts.Display)) { append("go") }
                    withStyle(SpanStyle(color = ink, fontFamily = GowaykiFonts.Display)) { append("wayki") }
                    append(" ")
                    withStyle(SpanStyle(color = go, fontFamily = GowaykiFonts.Display)) { append("go") }
                },
                style = if (large) MaterialTheme.typography.h3 else MaterialTheme.typography.h4,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.body2,
                    color = if (light) Color.White.copy(alpha = 0.9f) else GowaykiColors.Stone,
                )
            }
        }
    }
}

/** Logo centrado grande (login / splash) con wordmark debajo. */
@Composable
fun BrandHero(
    subtitle: String? = null,
    logoSize: Dp = 112.dp,
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
        Spacer(Modifier.height(16.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = GowaykiColors.BrandOrange, fontFamily = GowaykiFonts.Display)) { append("go") }
                withStyle(SpanStyle(color = GowaykiColors.BrandInk, fontFamily = GowaykiFonts.Display)) { append("wayki") }
                append(" ")
                withStyle(SpanStyle(color = GowaykiColors.BrandOrange, fontFamily = GowaykiFonts.Display)) { append("go") }
            },
            style = MaterialTheme.typography.h3,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.body1,
                color = GowaykiColors.Stone,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PressScale(
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = PressScale(reduce, pressed && enabled)
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    val shimmer = remember { Animatable(0f) }
    LaunchedEffect(enabled, reduce) {
        if (!enabled || reduce) {
            shimmer.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            shimmer.animateTo(1f, tween(1800))
            shimmer.snapTo(0f)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled) if (pressed) 4.dp else 14.dp else 0.dp,
                shape = shape,
                spotColor = GowaykiColors.Glow,
                ambientColor = GowaykiColors.Terracotta.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(
                            GowaykiColors.BrandOrangeHot,
                            GowaykiColors.BrandOrange,
                            GowaykiColors.BrandAmber,
                        ),
                    )
                } else {
                    Brush.horizontalGradient(listOf(GowaykiColors.Sand, GowaykiColors.Sand))
                },
            )
            .then(
                if (enabled) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f * (1f - shimmer.value)),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.12f * shimmer.value),
                                ),
                            ),
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else GowaykiColors.Stone,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = PressScale(reduce, pressed)
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation = if (pressed) 2.dp else 6.dp,
                shape = shape,
                spotColor = Color(0x22000000),
            )
            .clip(shape)
            .background(GowaykiColors.SurfaceElevated)
            .border(1.5.dp, GowaykiColors.Terracotta.copy(alpha = 0.35f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = GowaykiColors.Charcoal,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale = if (onClick == null) 1f else PressScale(reduce, pressed)
    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .shadow(12.dp, RoundedCornerShape(GowaykiRadius.lg), spotColor = Color(0x22000000)),
        color = GowaykiColors.SurfaceElevated.copy(alpha = 0.95f),
        shape = RoundedCornerShape(GowaykiRadius.lg),
        elevation = 0.dp,
    ) {
        Box(Modifier.padding(GowaykiSpace.md)) { content() }
    }
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
    Surface(
        color = GowaykiColors.SurfaceElevated.copy(alpha = 0.97f),
        elevation = 16.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == activeIndex
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "navScale",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = item.onClick)
                        .background(if (active) GowaykiColors.BrandOrange.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) GowaykiColors.BrandOrange else GowaykiColors.Stone,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.caption,
                        color = if (active) GowaykiColors.BrandOrange else GowaykiColors.Stone,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = GowaykiSpace.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Icon(
                        GowaykiIcons.Back,
                        contentDescription = "Volver",
                        tint = GowaykiColors.BrandOrange,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        }
        action?.invoke()
    }
}
