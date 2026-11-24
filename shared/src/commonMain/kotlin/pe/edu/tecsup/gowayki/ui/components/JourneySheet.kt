package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.tecsup.gowayki.ui.theme.GowaykiElevation
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable

enum class JourneySheetSnap { Peek, Mid, Full }

/**
 * Panel inferior con snapeos claros (peek / mid / full).
 * El gesto vertical acompaña; al soltar asienta con spring controlado.
 */
@Composable
fun JourneySheet(
    snap: JourneySheetSnap,
    onSnapChange: (JourneySheetSnap) -> Unit,
    modifier: Modifier = Modifier,
    peekHeight: Dp = 168.dp,
    midHeight: Dp = 340.dp,
    fullHeight: Dp = 520.dp,
    content: @Composable ColumnScope.(JourneySheetSnap) -> Unit,
) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun heightOf(s: JourneySheetSnap): Float = with(density) {
        when (s) {
            JourneySheetSnap.Peek -> peekHeight.toPx()
            JourneySheetSnap.Mid -> midHeight.toPx()
            JourneySheetSnap.Full -> fullHeight.toPx()
        }
    }
    val heightPx = remember { Animatable(heightOf(snap)) }
    LaunchedEffect(snap, reduce) {
        val target = heightOf(snap)
        if (reduce) heightPx.snapTo(target)
        else heightPx.animateTo(target, GowaykiMotion.panel())
    }
    val currentDp = with(density) { heightPx.value.toDp() }
    val colors = GowaykiTheme.colors
    val sheetShape = RoundedCornerShape(topStart = GowaykiRadius.lg, topEnd = GowaykiRadius.lg)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(currentDp)
            .shadow(GowaykiElevation.raised, sheetShape),
        shape = sheetShape,
        color = colors.surfaceElevated,
        contentColor = colors.onSurface,
        elevation = GowaykiElevation.none,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(reduce) {
                        if (reduce) return@pointerInput
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                scope.launch {
                                    val next = (heightPx.value - dragAmount)
                                        .coerceIn(heightOf(JourneySheetSnap.Peek), heightOf(JourneySheetSnap.Full))
                                    heightPx.snapTo(next)
                                }
                            },
                            onDragEnd = {
                                val h = heightPx.value
                                val peek = heightOf(JourneySheetSnap.Peek)
                                val mid = heightOf(JourneySheetSnap.Mid)
                                val full = heightOf(JourneySheetSnap.Full)
                                val nearest = listOf(
                                    JourneySheetSnap.Peek to peek,
                                    JourneySheetSnap.Mid to mid,
                                    JourneySheetSnap.Full to full,
                                ).minBy { kotlin.math.abs(it.second - h) }.first
                                onSnapChange(nearest)
                                scope.launch {
                                    heightPx.animateTo(heightOf(nearest), GowaykiMotion.panel())
                                }
                            },
                        )
                    }
                    .clickable {
                        onSnapChange(
                            when (snap) {
                                JourneySheetSnap.Peek -> JourneySheetSnap.Mid
                                JourneySheetSnap.Mid -> JourneySheetSnap.Full
                                JourneySheetSnap.Full -> JourneySheetSnap.Peek
                            },
                        )
                    }
                    .padding(top = GowaykiSpace.smd, bottom = GowaykiSpace.sm),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(GowaykiSize.sheetHandleWidth)
                        .height(GowaykiSize.sheetHandleHeight)
                        .clip(RoundedCornerShape(GowaykiRadius.pill))
                        .background(colors.outline),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = fullHeight)
                    .padding(horizontal = GowaykiSpace.md)
                    .padding(bottom = GowaykiSpace.smd),
            ) {
                content(snap)
            }
        }
    }
}

/** Altura aproximada en dp para padding de cámara del mapa. */
fun JourneySheetSnap.mapPaddingDp(): Int = when (this) {
    JourneySheetSnap.Peek -> 176
    JourneySheetSnap.Mid -> 348
    JourneySheetSnap.Full -> 520
}
