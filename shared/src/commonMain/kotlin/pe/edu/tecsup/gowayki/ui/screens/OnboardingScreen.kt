package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.components.BrandLogo
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

private data class OnboardPage(
    val title: String,
    val body: String,
    val accent: Color,
    val accentDeep: Color,
    val soft: Color,
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(session: AppSession) {
    val s = session.strings
    val reduce = LocalMotionPrefs.current.reduceMotion
    val step = session.onboardingStep.coerceIn(0, 2)
    val pages = listOf(
        OnboardPage(
            title = s.onboardingTitle1,
            body = s.onboardingBody1,
            accent = GowaykiColors.BrandOrange,
            accentDeep = GowaykiColors.BrandOrangeHot,
            soft = Color(0xFFFFE0CC),
        ),
        OnboardPage(
            title = s.onboardingTitle2,
            body = s.onboardingBody2,
            accent = GowaykiColors.BrandAmber,
            accentDeep = Color(0xFFE07A00),
            soft = Color(0xFFFFF0D6),
        ),
        OnboardPage(
            title = s.onboardingTitle3,
            body = s.onboardingBody3,
            accent = Color(0xFF1C61A5),
            accentDeep = Color(0xFF154A7C),
            soft = Color(0xFFD6E8F8),
        ),
    )
    val page = pages[step]
    val heroScale = remember { Animatable(0.86f) }
    LaunchedEffect(step) {
        if (reduce) {
            heroScale.snapTo(1f)
        } else {
            heroScale.snapTo(0.86f)
            heroScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(page.soft, GowaykiColors.Ivory, Color.White),
                ),
            ),
    ) {
        FloatingBlobs(accent = page.accent, reduce = reduce)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { session.finishOnboarding(skipped = true) }) {
                    Text(s.skip, color = GowaykiColors.Stone, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogo(size = 28.dp)
                    Spacer(Modifier.width(6.dp))
                    LocaleChip(
                        label = if (session.locale == AppLocale.Es) "ES" else "EN",
                        onClick = {
                            session.updateLocale(if (session.locale == AppLocale.Es) AppLocale.En else AppLocale.Es)
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            StepPips(step = step, total = 3, accent = page.accent)
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .scale(heroScale.value),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (reduce) {
                            fadeIn(GowaykiMotion.none()) togetherWith fadeOut(GowaykiMotion.none())
                        } else if (targetState > initialState) {
                            (slideInHorizontally { it / 3 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally { -it / 4 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally { -it / 3 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally { it / 4 } + fadeOut(tween(180)))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { current ->
                    DuoHeroCard(
                        step = current,
                        accent = pages[current].accent,
                        accentDeep = pages[current].accentDeep,
                        reduce = reduce,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally { it / 10 }) togetherWith
                        fadeOut(tween(120))
                },
            ) { current ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        pages[current].title,
                        style = MaterialTheme.typography.h3.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        fontWeight = FontWeight.Black,
                        color = GowaykiColors.BrandInk,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        pages[current].body,
                        style = MaterialTheme.typography.body1.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        color = GowaykiColors.Stone,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    SoftButton(
                        text = s.back,
                        onClick = { session.onboardingStep = step - 1 },
                        modifier = Modifier.weight(0.42f),
                    )
                }
                PrimaryButton(
                    text = if (step >= 2) s.done else s.next,
                    onClick = {
                        if (step >= 2) session.finishOnboarding(false) else session.onboardingStep = step + 1
                    },
                    modifier = Modifier.weight(if (step > 0) 0.58f else 1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LocaleChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .shadow(0.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = GowaykiColors.BrandOrange, fontSize = 13.sp)
    }
}

@Composable
private fun StepPips(step: Int, total: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val active = index == step
            val done = index < step
            val width by animateFloatAsState(
                targetValue = if (active) 34f else 10f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "pip",
            )
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .height(10.dp)
                    .width(width.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when {
                            active || done -> accent
                            else -> Color.White.copy(alpha = 0.85f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun FloatingBlobs(accent: Color, reduce: Boolean) {
    val infinite = rememberInfiniteTransition(label = "onboardBlobs")
    val driftAnim by infinite.animateFloat(
        initialValue = -8f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blobDrift",
    )
    val drift = if (reduce) 0f else driftAnim
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset(x = (-40).dp, y = (120 + drift).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (40 - drift).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f)),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = 24.dp, y = (-80).dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(GowaykiColors.BrandAmber.copy(alpha = 0.12f)),
        )
    }
}

@Composable
private fun DuoHeroCard(
    step: Int,
    accent: Color,
    accentDeep: Color,
    reduce: Boolean,
) {
    val infinite = rememberInfiniteTransition()
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val wobble by infinite.animateFloat(
        initialValue = -4f,
        targetValue = if (reduce) 0f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .shadow(22.dp, RoundedCornerShape(36.dp), spotColor = accent.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent, accentDeep, accent.copy(alpha = 0.92f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // pattern dots
        Canvas(Modifier.fillMaxSize()) {
            val stepPx = 28f
            var y = 18f
            while (y < size.height) {
                var x = 18f
                while (x < size.width) {
                    drawCircle(Color.White.copy(alpha = 0.08f), radius = 3.2f, center = Offset(x, y))
                    x += stepPx
                }
                y += stepPx
            }
        }
        Box(
            Modifier
                .padding(22.dp)
                .fillMaxSize()
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            when (step) {
                0 -> RouteScene(accent, bounce, wobble)
                1 -> FareScene(accent, bounce)
                else -> CommunityScene(accent, bounce, wobble)
            }
        }
        // floating badge
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .offset(y = (bounce * 6).dp)
                .rotate(wobble)
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (step) {
                    0 -> GowaykiIcons.Routes
                    1 -> GowaykiIcons.Bus
                    else -> GowaykiIcons.Report
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun RouteScene(accent: Color, bounce: Float, wobble: Float) {
    Canvas(Modifier.fillMaxSize().padding(16.dp)) {
        val w = size.width
        val h = size.height
        // soft hills
        drawCircle(accent.copy(alpha = 0.12f), radius = w * 0.35f, center = Offset(w * 0.2f, h * 1.05f))
        drawCircle(Color(0xFF1C61A5).copy(alpha = 0.10f), radius = w * 0.4f, center = Offset(w * 0.9f, h * 1.1f))
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.72f)
            cubicTo(w * 0.32f, h * (0.22f + bounce * 0.04f), w * 0.55f, h * 0.78f, w * 0.86f, h * 0.28f)
        }
        drawPath(path, Color.White, style = Stroke(width = 18f, cap = StrokeCap.Round))
        drawPath(path, accent, style = Stroke(width = 9f, cap = StrokeCap.Round))
        // start / end
        drawCircle(Color(0xFF1C61A5), radius = 16f, center = Offset(w * 0.12f, h * 0.72f))
        drawCircle(Color.White, radius = 6f, center = Offset(w * 0.12f, h * 0.72f))
        drawCircle(accent, radius = 16f, center = Offset(w * 0.86f, h * 0.28f))
        drawCircle(Color.White, radius = 6f, center = Offset(w * 0.86f, h * 0.28f))
        // bus blob
        val bx = w * (0.35f + bounce * 0.18f)
        val by = h * (0.55f - bounce * 0.08f)
        rotate(degrees = wobble, pivot = Offset(bx, by)) {
            drawRoundRect(
                accent,
                topLeft = Offset(bx - 28f, by - 14f),
                size = Size(56f, 28f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawCircle(Color.White, 4f, Offset(bx - 12f, by + 16f))
            drawCircle(Color.White, 4f, Offset(bx + 12f, by + 16f))
        }
    }
}

@Composable
private fun FareScene(accent: Color, bounce: Float) {
    Canvas(Modifier.fillMaxSize().padding(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            Color(0xFFFFF7F2),
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.84f, h * 0.76f),
            cornerRadius = CornerRadius(28f, 28f),
        )
        drawRoundRect(
            accent,
            topLeft = Offset(w * 0.16f, h * (0.22f + bounce * 0.02f)),
            size = Size(w * 0.42f, 28f),
            cornerRadius = CornerRadius(12f, 12f),
        )
        drawRoundRect(
            Color(0xFFE8D5CA),
            topLeft = Offset(w * 0.16f, h * 0.38f),
            size = Size(w * 0.58f, 16f),
            cornerRadius = CornerRadius(8f, 8f),
        )
        drawRoundRect(
            Color(0xFFE8D5CA),
            topLeft = Offset(w * 0.16f, h * 0.48f),
            size = Size(w * 0.48f, 16f),
            cornerRadius = CornerRadius(8f, 8f),
        )
        // coin
        val cx = w * 0.72f
        val cy = h * (0.62f - bounce * 0.05f)
        drawCircle(Color(0xFFF98E00), radius = 34f, center = Offset(cx, cy))
        drawCircle(Color(0xFFFFE0A3), radius = 24f, center = Offset(cx, cy))
        drawCircle(Color(0xFFF05709), radius = 10f, center = Offset(cx, cy))
    }
}

@Composable
private fun CommunityScene(accent: Color, bounce: Float, wobble: Float) {
    Canvas(Modifier.fillMaxSize().padding(18.dp)) {
        val w = size.width
        val h = size.height
        // speech bubbles
        fun bubble(x: Float, y: Float, color: Color, scale: Float) {
            drawRoundRect(
                color,
                topLeft = Offset(x, y),
                size = Size(88f * scale, 56f * scale),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawCircle(color, 8f * scale, Offset(x + 20f * scale, y + 56f * scale))
        }
        bubble(w * 0.12f, h * (0.28f + bounce * 0.03f), accent.copy(alpha = 0.9f), 1f)
        bubble(w * 0.52f, h * (0.18f - bounce * 0.02f), Color(0xFFF05709), 0.9f)
        bubble(w * 0.34f, h * (0.55f + bounce * 0.02f), Color(0xFF1C61A5), 0.85f)
        // alert pin
        val px = w * 0.78f
        val py = h * 0.62f
        rotate(wobble, Offset(px, py)) {
            drawCircle(Color(0xFFE84513), 22f + bounce * 3f, Offset(px, py))
            drawCircle(Color.White, 7f, Offset(px, py))
        }
    }
}
