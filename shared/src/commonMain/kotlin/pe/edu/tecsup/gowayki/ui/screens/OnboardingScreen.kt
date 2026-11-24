package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.components.BrandLogo
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.Micro
import pe.edu.tecsup.gowayki.ui.components.MicroIllustration
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.theme.GowaykiElevation
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.GowaykiType
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

private data class OnboardPage(
    val title: String,
    val body: String,
    val accent: Color,
    val accentDeep: Color,
    val soft: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

/** Como en Flutter: cada página muestra una combi grande y cuatro pequeñas (micros[page * 5 + i]). */
private fun microsForPage(page: Int): List<Micro> = Micro.entries.drop(page * 5).take(5)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    val step = session.onboardingStep.coerceIn(0, 2)
    val pages = listOf(
        OnboardPage(s.onboardingTitle1, s.onboardingBody1, colors.primary, colors.primaryStrong, colors.onboardingSoft[0], GowaykiIcons.Routes),
        OnboardPage(s.onboardingTitle2, s.onboardingBody2, colors.accent, colors.primary, colors.onboardingSoft[1], GowaykiIcons.Bus),
        OnboardPage(s.onboardingTitle3, s.onboardingBody3, colors.transitOutbound, colors.transitOutboundDeep, colors.onboardingSoft[2], GowaykiIcons.Report),
    )
    val page = pages[step]
    val heroScale = remember { Animatable(0.86f) }
    LaunchedEffect(step) {
        if (reduce) {
            heroScale.snapTo(1f)
        } else {
            heroScale.snapTo(0.86f)
            heroScale.animateTo(1f, GowaykiMotion.hero())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(page.soft, colors.backgroundAlt, colors.background))),
    ) {
        FloatingBlobs(accent = page.accent, reduce = reduce)
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = GowaykiSpace.mdl, vertical = GowaykiSpace.smd),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogo(size = GowaykiSize.logo)
                Spacer(Modifier.weight(1f))
                LocaleChip(
                    label = session.locale.code.uppercase(),
                    description = s.language,
                    onClick = {
                        session.updateLocale(if (session.locale == AppLocale.Es) AppLocale.En else AppLocale.Es)
                    },
                )
                Spacer(Modifier.width(GowaykiSpace.xs))
                TextButton(onClick = { session.finishOnboarding(skipped = true) }) {
                    Text(s.skip, style = MaterialTheme.typography.button, color = colors.onSurfaceMuted)
                }
            }

            Spacer(Modifier.height(GowaykiSpace.sm))
            StepPips(step = step, total = 3, accent = page.accent)
            Spacer(Modifier.height(GowaykiSpace.smd))

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val heroHeight = (maxHeight * 0.62f).coerceIn(160.dp, GowaykiSize.onboardingHero)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (reduce) {
                                fadeIn(GowaykiMotion.none()) togetherWith fadeOut(GowaykiMotion.none())
                            } else if (targetState > initialState) {
                                (slideInHorizontally { it / 3 } + fadeIn(GowaykiMotion.soft())) togetherWith
                                    (slideOutHorizontally { -it / 4 } + fadeOut(GowaykiMotion.micro()))
                            } else {
                                (slideInHorizontally { -it / 3 } + fadeIn(GowaykiMotion.soft())) togetherWith
                                    (slideOutHorizontally { it / 4 } + fadeOut(GowaykiMotion.micro()))
                            }
                        },
                        label = "onboardingHero",
                    ) { current ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DuoHeroCard(
                                page = pages[current],
                                micros = microsForPage(current),
                                height = heroHeight,
                                reduce = reduce,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = heroScale.value
                                    scaleY = heroScale.value
                                },
                            )
                            Spacer(Modifier.height(GowaykiSpace.smd))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.xs)) {
                                microsForPage(current).drop(1).forEach { micro ->
                                    MicroIllustration(
                                        micro = micro,
                                        width = GowaykiSize.microThumb * 1.4f,
                                        modifier = Modifier.weight(1f).height(GowaykiSize.microThumb),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(GowaykiSpace.md))
                    Text(
                        page.title,
                        style = GowaykiType.heroTitle,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    Text(
                        page.body,
                        style = GowaykiType.lead,
                        color = colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = GowaykiSpace.sm),
                    )
                    Spacer(Modifier.height(GowaykiSpace.md))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.smd),
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
                    text = if (step >= 2) s.start else s.next,
                    onClick = {
                        if (step >= 2) session.finishOnboarding(false) else session.onboardingStep = step + 1
                    },
                    modifier = Modifier.weight(if (step > 0) 0.58f else 1f),
                )
            }
            Spacer(Modifier.height(GowaykiSpace.smd))
        }
    }
}

@Composable
private fun LocaleChip(label: String, description: String, onClick: () -> Unit) {
    val colors = GowaykiTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GowaykiRadius.pill))
            .background(colors.surfaceElevated)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$description: $label" }
            .padding(horizontal = GowaykiSpace.smd, vertical = GowaykiSpace.sm),
    ) {
        Text(label, style = MaterialTheme.typography.button, color = colors.primary)
    }
}

@Composable
private fun StepPips(step: Int, total: Int, accent: Color) {
    val colors = GowaykiTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val active = index == step
            val done = index < step
            val width by animateDpAsState(
                targetValue = if (active) GowaykiSize.pipActive else GowaykiSize.pip,
                animationSpec = GowaykiMotion.snappy(),
                label = "pip",
            )
            Box(
                Modifier
                    .padding(horizontal = GowaykiSpace.xs)
                    .height(GowaykiSize.pip)
                    .width(width)
                    .clip(RoundedCornerShape(GowaykiRadius.pill))
                    .background(if (active || done) accent else colors.surfaceElevated.copy(alpha = 0.85f)),
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
            animation = GowaykiMotion.ambient(GowaykiMotion.AmbientSlowMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blobDrift",
    )
    val accentAlt = GowaykiTheme.colors.accent
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset(x = (-40).dp, y = 120.dp)
                .graphicsLayer { translationY = if (reduce) 0f else driftAnim * density }
                .size(160.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = 40.dp)
                .graphicsLayer { translationY = if (reduce) 0f else -driftAnim * density }
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
                .background(accentAlt.copy(alpha = 0.12f)),
        )
    }
}

@Composable
private fun DuoHeroCard(
    page: OnboardPage,
    micros: List<Micro>,
    height: androidx.compose.ui.unit.Dp,
    reduce: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GowaykiTheme.colors
    val infinite = rememberInfiniteTransition(label = "hero")
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = GowaykiMotion.ambient(GowaykiMotion.BounceMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroBounce",
    )
    val wobble by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = GowaykiMotion.ambient(GowaykiMotion.WobbleMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroWobble",
    )
    val outer = RoundedCornerShape(GowaykiRadius.xl)
    val inner = RoundedCornerShape(GowaykiRadius.lg)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .shadow(GowaykiElevation.hero, outer, spotColor = page.accent.copy(alpha = 0.35f))
            .clip(outer)
            .background(Brush.linearGradient(listOf(page.accent, page.accentDeep, page.accent.copy(alpha = 0.92f)))),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stepPx = 28f
            var y = 18f
            while (y < size.height) {
                var x = 18f
                while (x < size.width) {
                    drawCircle(colors.onHero.copy(alpha = 0.08f), radius = 3.2f, center = Offset(x, y))
                    x += stepPx
                }
                y += stepPx
            }
        }
        Box(
            Modifier
                .padding(GowaykiSpace.mdl)
                .fillMaxSize()
                .shadow(GowaykiElevation.card, inner)
                .clip(inner)
                .background(colors.illustrationCard),
            contentAlignment = Alignment.Center,
        ) {
            MicroIllustration(
                micro = micros.first(),
                width = height * 1.2f,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GowaykiSpace.sm)
                    .graphicsLayer { translationY = if (reduce) 0f else -bounce * 6f * density },
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(GowaykiSpace.md)
                .graphicsLayer {
                    translationY = if (reduce) 0f else bounce * 6f * density
                    rotationZ = if (reduce) 0f else wobble
                }
                .size(GowaykiSize.touchTarget)
                .shadow(GowaykiElevation.card, CircleShape)
                .clip(CircleShape)
                .background(colors.illustrationCard),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.accent,
                modifier = Modifier.size(GowaykiSize.icon),
            )
        }
    }
}
