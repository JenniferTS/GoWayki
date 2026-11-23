package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.AtmosphereBackground
import pe.edu.tecsup.gowayki.ui.components.EnterContent
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import kotlin.random.Random

/**
 * Wayki Combi ligero en Compose. Recursos y temporizador solo viven mientras
 * esta pantalla está activa; al salir se cancelan.
 */
@Composable
fun GameScreen(session: AppSession) {
    val s = session.strings
    var paused by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var busX by remember { mutableStateOf(0.5f) }
    var obstacles by remember { mutableStateOf(listOf(0.2f to 0f, 0.7f to -0.4f)) }
    var running by remember { mutableStateOf(true) }
    var elapsedMs by remember { mutableStateOf(0) }

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
            elapsedMs += 32
            kotlinx.coroutines.delay(32)
        }
    }

    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TextButton(onClick = {
                    running = false
                    session.go(Screen.Home)
                }) { Text(s.back) }
                Text(s.gameTitle, style = MaterialTheme.typography.h4, color = GowaykiColors.Terracotta)
                Text(
                    "${s.gameScore}: $score",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = GowaykiColors.Charcoal,
                )
                Text(s.gameHelp, color = GowaykiColors.Stone, style = MaterialTheme.typography.body2)
                Spacer(Modifier.height(12.dp))
                GlassPanel {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(GowaykiRadius.md))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1A2E26), Color(0xFF0F1A16)),
                                ),
                            )
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    busX = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                                    score += 1
                                }
                            },
                    ) {
                        obstacles.forEach { (x, y) ->
                            drawCircle(
                                Color(0xFFE9D8C2),
                                radius = 18f,
                                center = Offset(x * size.width, y * size.height),
                            )
                        }
                        drawRoundRect(
                            color = GowaykiColors.Terracotta,
                            topLeft = Offset(busX * size.width - 28f, size.height * 0.82f),
                            size = androidx.compose.ui.geometry.Size(56f, 28f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SoftButton(
                        text = if (paused) s.gameResume else s.gamePause,
                        onClick = { paused = !paused },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SoftButton(
                        text = s.gameRestart,
                        onClick = {
                            score = 0
                            obstacles = listOf(0.2f to 0f, 0.7f to -0.4f)
                            busX = 0.5f
                            paused = false
                            elapsedMs = 0
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = s.gameSubmitScore,
                    onClick = {
                        running = false
                        val duration = elapsedMs.coerceIn(1000, 120_000)
                        val publishScore = score.coerceIn(0, 5000).coerceAtMost(duration / 1000 * 30 + 30)
                        session.submitGameScore(publishScore, duration)
                    },
                )
                SoftButton(text = s.weeklyRanking, onClick = { session.go(Screen.Ranking) })
                session.rankingMessage?.let {
                    Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
