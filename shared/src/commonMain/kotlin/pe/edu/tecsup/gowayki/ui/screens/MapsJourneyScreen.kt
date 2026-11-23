package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import pe.edu.tecsup.gowayki.platform.HapticKind
import pe.edu.tecsup.gowayki.platform.platformHaptic
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.JourneySheet
import pe.edu.tecsup.gowayki.ui.components.JourneySheetSnap
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.StaggerIn
import pe.edu.tecsup.gowayki.ui.components.mapPaddingDp
import pe.edu.tecsup.gowayki.ui.map.CityMap
import pe.edu.tecsup.gowayki.ui.map.MapOverlay
import pe.edu.tecsup.gowayki.ui.map.itineraryEndpoints
import pe.edu.tecsup.gowayki.ui.map.itinerarySegments
import pe.edu.tecsup.gowayki.ui.map.itineraryStops
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiFonts
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

/**
 * Recorrido principal: mapa + comparar rutas + detalle en hoja arrastrable.
 * Estado de búsqueda/selección vive en [AppSession] (sobrevive a Atrás).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MapsJourneyScreen(session: AppSession) {
    val s = session.strings
    val reduce = LocalMotionPrefs.current.reduceMotion
    val list = session.journey?.itineraries.orEmpty()
    val planKey = remember(session.journey) {
        list.joinToString("|") { "${it.durationMinutes}:${it.transfers}:${it.legs.size}" }
    }
    var sheetSnap by remember { mutableStateOf(JourneySheetSnap.Mid) }
    var chromeVisible by remember { mutableStateOf(reduce) }

    LaunchedEffect(Unit) {
        session.startUserLocationWatch(setOriginIfEmpty = session.origin == null)
        chromeVisible = true
    }
    DisposableEffect(Unit) {
        onDispose { session.stopUserLocationWatch() }
    }
    LaunchedEffect(list.size, planKey) {
        if (list.isEmpty()) return@LaunchedEffect
        if (session.selectedItineraryIndex !in list.indices) {
            session.selectItinerary(0)
        }
    }

    val selected = session.selectedItineraryIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    val trip = list.getOrNull(selected)
    val segments = remember(trip) { itinerarySegments(trip) }
    val stops = remember(trip) { itineraryStops(trip) }
    val ends = remember(trip) { itineraryEndpoints(trip) }
    val padBottom = sheetSnap.mapPaddingDp()

    Box(Modifier.fillMaxSize().background(GowaykiColors.Mist)) {
        CityMap(
            modifier = Modifier.fillMaxSize(),
            overlay = MapOverlay(
                segments = segments,
                stops = stops,
                markers = ends,
                user = session.lastKnownLocation,
                zoom = 13.2,
                followEpoch = session.locationFollowEpoch,
                fitPaddingBottomDp = padBottom,
            ),
        )

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
                slideInVertically(
                    animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.soft(),
                    initialOffsetY = { if (reduce) 0 else -28 },
                ),
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 18.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(28.dp))
                    .semantics { contentDescription = "${session.originLabel} → ${session.destinationLabel}" },
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                elevation = 0.dp,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            GowaykiIcons.Back,
                            contentDescription = s.back,
                            tint = GowaykiColors.BrandInk,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { session.go(Screen.Search) }
                                .padding(2.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f).clickable { session.go(Screen.Search) }) {
                            EndpointRow(color = Color(0xFF34A853), label = session.originLabel.ifBlank { s.searchOrigin })
                            Spacer(Modifier.height(8.dp))
                            EndpointRow(color = Color(0xFFEA4335), label = session.destinationLabel.ifBlank { s.searchDestination })
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = (padBottom + 12).dp)
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { session.recenterOnUser() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(GowaykiIcons.Location, contentDescription = s.myLocation, tint = GowaykiColors.BrandOrange, modifier = Modifier.size(22.dp))
        }

        session.locationMessage?.let { msg ->
            AnimatedVisibility(
                visible = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 24.dp, end = 24.dp),
                enter = fadeIn(GowaykiMotion.soft()) + slideInVertically { -12 },
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, elevation = 6.dp) {
                    Text(
                        msg,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.caption,
                        color = GowaykiColors.BrandInk,
                    )
                }
            }
        }

        JourneySheet(
            snap = sheetSnap,
            onSnapChange = { sheetSnap = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snap ->
            if (list.isEmpty()) {
                Text(session.planError ?: s.emptyRoutes, style = MaterialTheme.typography.body2)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(s.retry, onClick = { session.go(Screen.Search) })
                return@JourneySheet
            }

            val chipsScroll = rememberScrollState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                list.forEachIndexed { index, option ->
                    StaggerIn(index = index, enterKey = planKey) {
                        RouteOptionChip(
                            minutes = option.durationMinutes.toInt(),
                            walking = option.walkingMinutes.toInt(),
                            fare = session.formatFare(option.estimatedFare),
                            lines = option.legs.filterIsInstance<TransitLeg>().joinToString(" · ") { it.lineName }
                                .ifBlank { s.walking },
                            walkingLabel = s.walking,
                            selected = index == selected,
                            onClick = {
                                if (index != session.selectedItineraryIndex) {
                                    session.selectItinerary(index)
                                    if (!reduce) platformHaptic(HapticKind.Light)
                                    if (sheetSnap == JourneySheetSnap.Peek) sheetSnap = JourneySheetSnap.Mid
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(GowaykiMotion.none()) togetherWith fadeOut(GowaykiMotion.none())
                    } else {
                        (fadeIn(GowaykiMotion.micro()) + slideInVertically { it / 10 }) togetherWith
                            fadeOut(GowaykiMotion.micro())
                    }
                },
                label = "tripSummary",
            ) { idx ->
                val current = list.getOrNull(idx)
                if (current != null) {
                    Column {
                        Text(
                            "${current.durationMinutes.toInt()} ${s.minutesApprox}",
                            style = MaterialTheme.typography.h5,
                            fontFamily = GowaykiFonts.Primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${s.walking}: ${current.walkingMinutes.toInt()} min · ${s.transfers}: ${current.transfers} · ${s.fareEstimated}: ${session.formatFare(current.estimatedFare)}",
                            style = MaterialTheme.typography.body2,
                            color = GowaykiColors.Stone,
                        )
                    }
                }
            }

            if (snap != JourneySheetSnap.Peek && trip != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    s.stepByStep,
                    style = MaterialTheme.typography.subtitle1,
                    fontFamily = GowaykiFonts.Secondary,
                )
                Spacer(Modifier.height(8.dp))
                val stepsMax = if (snap == JourneySheetSnap.Full) 280.dp else 140.dp
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(stepsMax)
                        .verticalScroll(rememberScrollState()),
                ) {
                    trip.legs.forEachIndexed { i, leg ->
                        when (leg) {
                            is WalkLeg -> StepRow(
                                iconTint = Color(0xFF5C6B73),
                                title = "${s.walking} ${leg.meters.toInt()} m",
                                subtitle = s.walkStraightHint,
                            )
                            is TransitLeg -> StepRow(
                                iconTint = parseColor(leg.color),
                                title = "${s.takeLine} ${leg.lineName} → ${leg.headsign}",
                                subtitle = "${s.boardAt} ${leg.boardingStop.name} · ${s.alightAt} ${leg.alightingStop.name}",
                            )
                        }
                        if (i < trip.legs.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 15.dp)
                                    .width(2.dp)
                                    .height(10.dp)
                                    .background(GowaykiColors.Outline),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = s.startTrip,
                onClick = {
                    session.selectItinerary(selected)
                    if (!reduce) platformHaptic(HapticKind.Success)
                    session.startActiveTrip()
                },
            )
        }
    }
}

@Composable
private fun RouteOptionChip(
    minutes: Int,
    walking: Int,
    fare: String,
    lines: String,
    walkingLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selected) GowaykiColors.BrandOrange else GowaykiColors.Outline,
        animationSpec = GowaykiMotion.selection(),
        label = "chipBorder",
    )
    val bg by animateColorAsState(
        if (selected) GowaykiColors.BrandOrange.copy(alpha = 0.12f) else GowaykiColors.Mist,
        animationSpec = GowaykiMotion.selection(),
        label = "chipBg",
    )
    val scale by animateFloatAsState(
        if (selected) 1.03f else 1f,
        animationSpec = GowaykiMotion.selection(),
        label = "chipScale",
    )
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "$minutes min · $lines" },
    ) {
        Text(
            "$minutes min",
            style = MaterialTheme.typography.subtitle1,
            fontFamily = GowaykiFonts.Secondary,
            fontWeight = FontWeight.Black,
            color = if (selected) GowaykiColors.BrandOrange else GowaykiColors.BrandInk,
            fontSize = 18.sp,
        )
        Text(
            "$walking min $walkingLabel · $fare",
            style = MaterialTheme.typography.caption,
            color = GowaykiColors.Stone,
        )
        Text(
            lines,
            style = MaterialTheme.typography.body2,
            fontFamily = GowaykiFonts.Primary,
            fontWeight = FontWeight.Medium,
            color = GowaykiColors.BrandInk,
        )
    }
}

@Composable
private fun EndpointRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.subtitle1,
            fontFamily = GowaykiFonts.Primary,
            fontWeight = FontWeight.Medium,
            color = GowaykiColors.BrandInk,
            maxLines = 1,
        )
    }
}

@Composable
private fun StepRow(iconTint: Color, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(iconTint),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.subtitle1,
                fontFamily = GowaykiFonts.Secondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, style = MaterialTheme.typography.caption, color = GowaykiColors.Stone)
        }
    }
}

private fun parseColor(hex: String): Color {
    val t = hex.trim().removePrefix("#")
    return runCatching {
        Color(("FF$t").toLong(16))
    }.getOrDefault(GowaykiColors.BrandOrange)
}
