package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import pe.edu.tecsup.gowayki.domain.game.CombiRunEngine
import pe.edu.tecsup.gowayki.domain.game.Pothole
import pe.edu.tecsup.gowayki.domain.game.RunPhase
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiScreen
import pe.edu.tecsup.gowayki.ui.components.InlineMessage
import pe.edu.tecsup.gowayki.ui.components.Micro
import pe.edu.tecsup.gowayki.ui.components.MicroIllustration
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.components.rememberMicroBitmap
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiPalette
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiStroke
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

/**
 * Wayki Combi: pista horizontal de 3 carriles; la combi elegida va a la izquierda y los baches
 * llegan desde la derecha. Un bache termina la partida y solo "Empezar" inicia otra.
 * La lógica vive en [CombiRunEngine]; aquí solo se dibuja y se avanza con [withFrameNanos].
 * Si pasan más de [GowaykiMotion.GamePauseGapMs] entre cuadros (app en segundo plano) se pausa.
 */
@Composable
fun GameScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    val engine = remember(reduce) {
        val base = CombiRunEngine.Config(laneChangeSeconds = GowaykiMotion.GameLaneChangeMs / 1000f)
        CombiRunEngine(if (reduce) base.reducedMotion() else base)
    }
    var frame by remember(engine) { mutableStateOf(0L) }
    var phase by remember(engine) { mutableStateOf(engine.phase) }
    var score by remember(engine) { mutableStateOf(0) }
    var best by remember(engine) { mutableStateOf(0) }

    fun sync() {
        phase = engine.phase
        score = engine.score
        best = engine.best
        frame++
    }

    LaunchedEffect(engine) {
        while (true) {
            snapshotFlow { phase }.first { it == RunPhase.Playing }
            var last = -1L
            while (engine.phase == RunPhase.Playing) {
                val now = withFrameNanos { it }
                if (last >= 0L) {
                    val gapNanos = now - last
                    if (gapNanos > GowaykiMotion.GamePauseGapMs * NANOS_PER_MS) {
                        engine.pause()
                    } else if (engine.step(gapNanos / NANOS_PER_SECOND)) {
                        session.submitGameScore(engine.score, engine.durationMs)
                    }
                }
                last = now
                sync()
            }
            sync()
        }
    }

    val density = LocalDensity.current
    val spriteWidthPx = with(density) { GowaykiSize.gamePicker.roundToPx() }
    val sprite = rememberMicroBitmap(session.gameCombi, spriteWidthPx)
    val swipePx = with(density) { GowaykiSpace.lg.toPx() }
    val trackShape = RoundedCornerShape(GowaykiRadius.md)

    GowaykiScreen(
        title = s.gameTitle,
        onBack = {
            engine.pause()
            session.go(Screen.Profile)
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${s.gameScore}: $score",
                    style = MaterialTheme.typography.h5,
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(s.format(s.gameBest, best), style = MaterialTheme.typography.subtitle2, color = colors.onSurfaceMuted)
            }
            Spacer(Modifier.height(GowaykiSpace.sm))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(GowaykiSize.gameTrack)
                    .clip(trackShape)
                    .semantics { contentDescription = s.gameTrackDescription }
                    .pointerInput(engine) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var swiped = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val dy = change.position.y - down.position.y
                                if (!swiped && abs(dy) > swipePx) {
                                    swiped = true
                                    if (dy < 0f) engine.moveUp() else engine.moveDown()
                                }
                                change.consume()
                                if (!change.pressed) break
                            }
                            if (!swiped) {
                                if (down.position.y < size.height / 2f) engine.moveUp() else engine.moveDown()
                            }
                        }
                    },
            ) {
                // Leer el contador de cuadros hace que solo el dibujo se repita en cada cuadro.
                if (frame < 0L) return@Canvas
                drawTrack(engine, colors, sprite, parallax = !reduce)
            }
            Spacer(Modifier.height(GowaykiSpace.smd))
            when (phase) {
                RunPhase.Playing -> Text(s.gameHelp, style = MaterialTheme.typography.body2, color = colors.onSurfaceMuted)
                RunPhase.Paused -> GlassPanel(Modifier.fillMaxWidth()) {
                    Text(s.gamePausedTitle, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    PrimaryButton(s.gameResume, onClick = {
                        engine.resume()
                        sync()
                    })
                }
                RunPhase.Idle, RunPhase.Ended -> GlassPanel(Modifier.fillMaxWidth()) {
                    val over = phase == RunPhase.Ended
                    Text(if (over) s.gameOverTitle else s.gameReadyTitle, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(GowaykiSpace.xs))
                    if (over) {
                        Text(
                            "${s.gameScore}: $score",
                            style = MaterialTheme.typography.h4,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(s.format(s.gameBest, best), style = MaterialTheme.typography.subtitle1)
                    } else {
                        Text(s.gameHelp, style = MaterialTheme.typography.body2)
                    }
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    Text(s.gameChooseCombi, style = MaterialTheme.typography.subtitle2)
                    Spacer(Modifier.height(GowaykiSpace.sm))
                    CombiPicker(selected = session.gameCombi, onSelect = { session.selectGameCombi(it) })
                    Spacer(Modifier.height(GowaykiSpace.md))
                    PrimaryButton(s.gameStart, onClick = {
                        engine.start()
                        sync()
                    })
                }
            }
            if (phase != RunPhase.Playing) {
                session.rankingMessage?.let { InlineMessage(it, isError = false) }
                Spacer(Modifier.height(GowaykiSpace.sm))
                SoftButton(text = s.weeklyRanking, onClick = { session.go(Screen.Ranking) })
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

@Composable
private fun CombiPicker(selected: Micro, onSelect: (Micro) -> Unit) {
    val colors = GowaykiTheme.colors
    val shape = RoundedCornerShape(GowaykiRadius.sm)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.ordinal)
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
        items(Micro.entries, key = { it.name }) { micro ->
            val isSelected = micro == selected
            MicroIllustration(
                micro = micro,
                width = GowaykiSize.gamePicker,
                modifier = Modifier
                    .size(GowaykiSize.gamePicker)
                    .clip(shape)
                    .background(if (isSelected) colors.primarySoft else colors.surfaceElevated)
                    .border(
                        if (isSelected) GowaykiStroke.strong else GowaykiStroke.hairline,
                        if (isSelected) colors.primary else colors.outline,
                        shape,
                    )
                    .clickable(role = Role.Button) { onSelect(micro) }
                    .padding(GowaykiSpace.xs),
            )
        }
    }
}

private fun DrawScope.drawTrack(
    engine: CombiRunEngine,
    colors: GowaykiPalette,
    sprite: ImageBitmap?,
    parallax: Boolean,
) {
    val config = engine.config
    val w = size.width
    val h = size.height
    val unit = w / config.worldWidth
    val verge = h * VERGE_FRACTION
    val roadTop = verge
    val roadBottom = h - verge
    val laneH = (roadBottom - roadTop) / config.lanes
    val stroke = GowaykiStroke.strong.toPx()

    drawRect(colors.gameVerge)
    if (parallax) {
        val period = PEBBLE_PERIOD * unit
        val shift = (engine.distance * PARALLAX_FACTOR * unit) % period
        var x = -shift
        var i = 0
        while (x < w + period) {
            val top = i % 2 == 0
            val y = if (top) verge * 0.45f else h - verge * 0.55f
            drawCircle(colors.gameVergeDetail, radius = verge * 0.14f, center = Offset(x, y))
            x += period / 2f
            i++
        }
    }
    drawRect(colors.gameRoad, topLeft = Offset(0f, roadTop), size = androidx.compose.ui.geometry.Size(w, roadBottom - roadTop))
    drawLine(colors.gameRoadEdge, Offset(0f, roadTop + stroke), Offset(w, roadTop + stroke), strokeWidth = stroke)
    drawLine(colors.gameRoadEdge, Offset(0f, roadBottom - stroke), Offset(w, roadBottom - stroke), strokeWidth = stroke)

    val dashPeriod = DASH_PERIOD * unit
    val dashLength = DASH_LENGTH * unit
    val dashShift = (engine.distance * unit) % dashPeriod
    for (laneIndex in 1 until config.lanes) {
        val y = roadTop + laneH * laneIndex
        var x = -dashShift
        while (x < w) {
            drawLine(colors.gameLaneMark, Offset(x, y), Offset(x + dashLength, y), strokeWidth = stroke, cap = StrokeCap.Round)
            x += dashPeriod
        }
    }

    for (pothole in engine.potholes) {
        drawPothole(pothole, pothole.x * unit, roadTop + laneH * (pothole.lane + 0.5f), config.potholeHalfLength * unit, laneH, colors)
    }

    val cx = config.playerX * unit
    val cy = roadTop + laneH * (engine.lanePosition + 0.5f)
    val targetW = config.playerHalfLength * 2f * SPRITE_OVERSIZE * unit
    if (sprite != null && sprite.width > 0) {
        var dw = targetW
        var dh = dw * sprite.height / sprite.width
        val maxH = laneH * SPRITE_MAX_LANE
        if (dh > maxH) {
            dh = maxH
            dw = dh * sprite.width / sprite.height
        }
        val left = (cx - dw / 2f).roundToInt()
        val top = (cy - dh / 2f).roundToInt()
        val draw = {
            drawImage(sprite, dstOffset = IntOffset(left, top), dstSize = IntSize(dw.roundToInt(), dh.roundToInt()))
        }
        if (MIRROR_SPRITE) scale(-1f, 1f, pivot = Offset(cx, cy)) { draw() } else draw()
    } else {
        drawRoundRect(
            colors.gamePotholeRim,
            topLeft = Offset(cx - targetW / 2f, cy - laneH * 0.3f),
            size = androidx.compose.ui.geometry.Size(targetW, laneH * 0.6f),
        )
    }
}

/** Bache: elipse irregular oscura con borde claro y un par de grietas; la forma sale de [Pothole.seed]. */
private fun DrawScope.drawPothole(
    pothole: Pothole,
    cx: Float,
    cy: Float,
    halfLength: Float,
    laneH: Float,
    colors: GowaykiPalette,
) {
    val random = Random(pothole.seed)
    val rx = halfLength
    val ry = laneH * POTHOLE_HEIGHT
    val jitter = FloatArray(POTHOLE_POINTS) { 0.78f + random.nextFloat() * 0.22f }
    fun outline(grow: Float): Path = Path().apply {
        for (i in 0 until POTHOLE_POINTS) {
            val a = 2.0 * PI * i / POTHOLE_POINTS
            val px = cx + (cos(a) * rx * jitter[i] * grow).toFloat()
            val py = cy + (sin(a) * ry * jitter[i] * grow).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(outline(1.22f), colors.gamePotholeRim)
    drawPath(outline(1f), colors.gamePothole)
    val crack = GowaykiStroke.soft.toPx()
    repeat(CRACKS) {
        val a = random.nextDouble() * 2.0 * PI
        val start = Offset(cx + (cos(a) * rx * 1.1).toFloat(), cy + (sin(a) * ry * 1.1).toFloat())
        val end = Offset(cx + (cos(a) * rx * 1.6).toFloat(), cy + (sin(a) * ry * 1.7).toFloat())
        drawLine(colors.gameCrack, start, end, strokeWidth = crack, cap = StrokeCap.Round)
    }
    drawPath(outline(1f), colors.gameCrack, style = Stroke(width = crack))
}

private const val NANOS_PER_MS = 1_000_000L
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val VERGE_FRACTION = 0.1f
private const val DASH_PERIOD = 110f
private const val DASH_LENGTH = 60f
private const val PEBBLE_PERIOD = 180f
private const val PARALLAX_FACTOR = 0.5f
private const val SPRITE_OVERSIZE = 1.25f
private const val SPRITE_MAX_LANE = 0.92f
private const val POTHOLE_HEIGHT = 0.26f
private const val POTHOLE_POINTS = 9
private const val CRACKS = 2
/** Las ilustraciones miran a la derecha (sentido de avance): no hace falta reflejarlas. */
private const val MIRROR_SPRITE = false
