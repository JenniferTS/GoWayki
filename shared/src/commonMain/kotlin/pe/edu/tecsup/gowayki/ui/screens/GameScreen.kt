package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiScreen
import pe.edu.tecsup.gowayki.ui.components.InlineMessage
import pe.edu.tecsup.gowayki.ui.components.Micro
import pe.edu.tecsup.gowayki.ui.components.MicroIllustration
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SectionTitle
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.components.rememberMicroBitmap
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiStroke
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import kotlin.random.Random

/**
 * Wayki Combi ligero en Compose. Recursos y temporizador solo viven mientras
 * esta pantalla está activa; al salir se cancelan. La combi del jugador usa
 * las mismas ilustraciones que el juego de Flutter.
 */
@Composable
fun GameScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    var paused by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var busX by remember { mutableStateOf(0.5f) }
    var obstacles by remember { mutableStateOf(listOf(0.2f to 0f, 0.7f to -0.4f)) }
    var running by remember { mutableStateOf(true) }
    var elapsedMs by remember { mutableStateOf(0) }
    var combi by remember { mutableStateOf(Micro.AlbotranCb) }

    DisposableEffect(Unit) {
        onDispose {
            running = false
            paused = true
        }
    }

    LaunchedEffect(paused, running) {
        while (running && !paused) {
            obstacles = obstacles.map { (x, y) ->
                val ny = y + 0.02f
                if (ny > 1.1f) Random.nextFloat() * 0.8f + 0.1f to -0.1f else x to ny
            }
            obstacles.forEach { (x, y) ->
                if (kotlin.math.abs(x - busX) < 0.08f && y in 0.78f..0.92f) {
                    score = (score - 5).coerceAtLeast(0)
                }
            }
            if (obstacles.any { it.second in 0.0f..0.05f }) score += 1
            elapsedMs += GowaykiMotion.GameTickMs
            delay(GowaykiMotion.GameTickMs.toLong())
        }
    }

    val spritePx = with(LocalDensity.current) { GowaykiSize.comboPicker.roundToPx() }
    val sprite = rememberMicroBitmap(combi, spritePx)
    val boardShape = RoundedCornerShape(GowaykiRadius.md)

    GowaykiScreen(
        title = s.gameTitle,
        onBack = {
            running = false
            session.go(Screen.Home)
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("${s.gameScore}: $score", style = MaterialTheme.typography.h5, color = colors.primary)
            Text(s.gameHelp, style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(GowaykiSpace.smd))
            GlassPanel(Modifier.fillMaxWidth()) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(GowaykiSize.gameBoard)
                        .clip(boardShape)
                        .background(Brush.verticalGradient(listOf(colors.gameSkyTop, colors.gameSkyBottom)))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                busX = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                                score += 1
                            }
                        },
                ) {
                    obstacles.forEach { (x, y) ->
                        drawCircle(
                            colors.gameObstacle,
                            radius = 18f,
                            center = Offset(x * size.width, y * size.height),
                        )
                    }
                    val image = sprite
                    if (image != null) {
                        val w = spritePx
                        val h = (image.height * w / image.width.coerceAtLeast(1))
                        drawImage(
                            image = image,
                            dstOffset = IntOffset((busX * size.width - w / 2f).toInt(), (size.height * 0.80f).toInt() - h / 2),
                            dstSize = IntSize(w, h),
                        )
                    } else {
                        drawRoundRect(
                            color = colors.primary,
                            topLeft = Offset(busX * size.width - 28f, size.height * 0.82f),
                            size = Size(56f, 28f),
                            cornerRadius = CornerRadius(8f, 8f),
                        )
                    }
                }
            }
            SectionTitle(s.gameChooseCombi)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
                items(Micro.entries, key = { it.name }) { micro ->
                    val selected = micro == combi
                    MicroIllustration(
                        micro = micro,
                        width = GowaykiSize.comboPicker,
                        modifier = Modifier
                            .size(GowaykiSize.comboPicker)
                            .clip(boardShape)
                            .background(if (selected) colors.primarySoft else colors.surfaceElevated)
                            .border(
                                if (selected) GowaykiStroke.strong else GowaykiStroke.hairline,
                                if (selected) colors.primary else colors.outline,
                                boardShape,
                            )
                            .clickable(role = Role.Button) { combi = micro }
                            .padding(GowaykiSpace.xs),
                    )
                }
            }
            Spacer(Modifier.height(GowaykiSpace.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
                SoftButton(
                    text = if (paused) s.gameResume else s.gamePause,
                    onClick = { paused = !paused },
                    modifier = Modifier.weight(1f),
                )
                SoftButton(
                    text = s.gameRestart,
                    onClick = {
                        score = 0
                        obstacles = listOf(0.2f to 0f, 0.7f to -0.4f)
                        busX = 0.5f
                        paused = false
                        running = true
                        elapsedMs = 0
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(GowaykiSpace.sm))
            PrimaryButton(
                text = s.gameSubmitScore,
                onClick = {
                    running = false
                    val duration = elapsedMs.coerceIn(1000, 120_000)
                    val publishScore = score.coerceIn(0, 5000).coerceAtMost(duration / 1000 * 30 + 30)
                    session.submitGameScore(publishScore, duration)
                },
            )
            Spacer(Modifier.height(GowaykiSpace.sm))
            SoftButton(text = s.weeklyRanking, onClick = { session.go(Screen.Ranking) })
            session.rankingMessage?.let { InlineMessage(it, isError = false) }
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}
