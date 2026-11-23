package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
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
    val infinite = rememberInfiniteTransition()
    val glowAnim by infinite.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduce) 0 else 2400),
            repeatMode = RepeatMode.Reverse,
        ),
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

@Composable
fun BrandMark(
    subtitle: String? = null,
    large: Boolean = false,
) {
    Column {
        Text(
            "GoWayki",
            style = if (large) MaterialTheme.typography.h3 else MaterialTheme.typography.h4,
            color = GowaykiColors.Terracotta,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.body2, color = GowaykiColors.Stone)
        }
    }
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
    val scale = if (reduce) 1f else if (pressed) 0.97f else 1f
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(GowaykiRadius.pill), spotColor = GowaykiColors.Glow),
        shape = RoundedCornerShape(GowaykiRadius.pill),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = GowaykiColors.Terracotta,
            contentColor = Color.White,
            disabledBackgroundColor = GowaykiColors.Sand,
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.button)
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(GowaykiRadius.pill),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GowaykiColors.Charcoal),
    ) { Text(text) }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .shadow(12.dp, RoundedCornerShape(GowaykiRadius.lg), spotColor = Color(0x22000000)),
        color = GowaykiColors.SurfaceElevated.copy(alpha = 0.95f),
        shape = RoundedCornerShape(GowaykiRadius.lg),
        elevation = 0.dp,
    ) {
        Box(Modifier.padding(GowaykiSpace.md)) { content() }
    }
}

@Composable
fun BottomNavBar(
    items: List<Pair<String, () -> Unit>>,
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
            items.forEachIndexed { index, (label, action) ->
                val active = index == activeIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = action)
                        .background(if (active) GowaykiColors.Terracotta.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        Modifier
                            .size(if (active) 9.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) GowaykiColors.Terracotta else GowaykiColors.Outline),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        color = if (active) GowaykiColors.Terracotta else GowaykiColors.Stone,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp,
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
                TextButton(onClick = onBack) { Text("←") }
            }
            Text(title, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        }
        action?.invoke()
    }
}
