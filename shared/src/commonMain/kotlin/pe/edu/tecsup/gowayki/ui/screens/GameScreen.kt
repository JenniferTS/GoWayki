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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
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
            kotlinx.coroutines.delay(32)
        }
    }

    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = {
            running = false
            session.go(Screen.Home)
        }) { Text(s.back) }
        Text(s.gameTitle)
        Text("${s.gameScore}: $score")
        Text(s.gameHelp, color = GowaykiColors.Stone)
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Color(0xFF1C2A24), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        busX = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                        score += 1
                    }
                },
        ) {
            obstacles.forEach { (x, y) ->
                drawCircle(Color(0xFFE9D8C2), radius = 18f, center = Offset(x * size.width, y * size.height))
            }
            drawRoundRect(
                color = GowaykiColors.Terracotta,
                topLeft = Offset(busX * size.width - 28f, size.height * 0.82f),
                size = androidx.compose.ui.geometry.Size(56f, 28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { paused = !paused }) {
                Text(if (paused) s.gameResume else s.gamePause)
            }
            Spacer(Modifier.padding(8.dp))
            OutlinedButton(onClick = {
                score = 0
                obstacles = listOf(0.2f to 0f, 0.7f to -0.4f)
                busX = 0.5f
                paused = false
            }) { Text(s.gameRestart) }
        }
    }
}
