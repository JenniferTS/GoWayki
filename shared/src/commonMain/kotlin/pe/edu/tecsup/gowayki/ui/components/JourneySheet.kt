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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(currentDp)
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        elevation = 0.dp,
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
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(GowaykiColors.Outline),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = fullHeight)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
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
