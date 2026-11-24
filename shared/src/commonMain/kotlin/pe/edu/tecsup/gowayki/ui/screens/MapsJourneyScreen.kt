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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
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
import pe.edu.tecsup.gowayki.ui.components.StepRow
import pe.edu.tecsup.gowayki.ui.components.mapPaddingDp
import pe.edu.tecsup.gowayki.ui.components.parseLineColor
import pe.edu.tecsup.gowayki.ui.map.CityMap
import pe.edu.tecsup.gowayki.ui.map.MapOverlay
import pe.edu.tecsup.gowayki.ui.map.itineraryEndpoints
import pe.edu.tecsup.gowayki.ui.map.itinerarySegments
import pe.edu.tecsup.gowayki.ui.map.itineraryStops
import pe.edu.tecsup.gowayki.ui.theme.GowaykiElevation
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiStroke
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.GowaykiType
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

/**
 * Recorrido principal: mapa + comparar rutas + detalle en hoja arrastrable.
 * Estado de búsqueda/selección vive en [AppSession] (sobrevive a Atrás).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MapsJourneyScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
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
    val padBottom = sheetSnap.mapPaddingDp()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        JourneyMap(session, trip, padBottom)

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()) +
                slideInVertically(
                    animationSpec = if (reduce) GowaykiMotion.none() else GowaykiMotion.soft(),
                    initialOffsetY = { if (reduce) 0 else -28 },
                ),
        ) {
            val shape = RoundedCornerShape(GowaykiRadius.lg)
            Surface(
                modifier = Modifier
                    .padding(top = GowaykiSpace.md, start = GowaykiSpace.md, end = GowaykiSpace.md)
                    .fillMaxWidth()
                    .shadow(GowaykiElevation.panel, shape, spotColor = colors.shadow)
                    .semantics { contentDescription = "${session.originLabel} → ${session.destinationLabel}" },
                shape = shape,
                color = colors.surfaceElevated,
                contentColor = colors.onSurface,
                elevation = GowaykiElevation.none,
            ) {
                Row(Modifier.padding(GowaykiSpace.sm), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(GowaykiSize.touchTarget)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { session.go(Screen.Search) }
                            .semantics { contentDescription = s.navigateBack },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(GowaykiIcons.Back, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(GowaykiSize.icon))
                    }
                    Spacer(Modifier.width(GowaykiSpace.xs))
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { session.go(Screen.Search) }) {
                        EndpointRow(color = colors.origin, label = session.originLabel.ifBlank { s.searchOrigin })
                        Spacer(Modifier.height(GowaykiSpace.sm))
                        EndpointRow(color = colors.destination, label = session.destinationLabel.ifBlank { s.searchDestination })
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = GowaykiSpace.md, bottom = (padBottom + 12).dp)
                .size(GowaykiSize.touchTarget)
                .shadow(GowaykiElevation.card, CircleShape)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .clickable(role = Role.Button) { session.recenterOnUser() }
                .semantics { contentDescription = s.myLocation },
            contentAlignment = Alignment.Center,
        ) {
            Icon(GowaykiIcons.Location, contentDescription = null, tint = colors.primary, modifier = Modifier.size(GowaykiSize.icon))
        }

        session.locationMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 112.dp, start = GowaykiSpace.lg, end = GowaykiSpace.lg),
                shape = RoundedCornerShape(GowaykiRadius.sm),
                color = colors.surfaceElevated,
                contentColor = colors.onSurface,
                elevation = GowaykiElevation.card,
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = GowaykiSpace.md, vertical = GowaykiSpace.smd),
                    style = MaterialTheme.typography.caption,
                    color = colors.onSurface,
                )
            }
        }

        JourneySheet(
            snap = sheetSnap,
            onSnapChange = { sheetSnap = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snap ->
            if (list.isEmpty()) {
                Text(session.planError ?: s.emptyRoutes, style = MaterialTheme.typography.body2)
                Spacer(Modifier.height(GowaykiSpace.sm))
                PrimaryButton(s.retry, onClick = { session.go(Screen.Search) })
                return@JourneySheet
            }

            session.planNotice?.let { notice ->
                Text(notice, style = MaterialTheme.typography.caption, color = colors.onSurfaceMuted)
                Spacer(Modifier.height(GowaykiSpace.sm))
            }

            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(GowaykiSpace.xs),
                horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
            ) {
                itemsIndexed(list, key = { index, _ -> "$planKey#$index" }) { index, option ->
                    StaggerIn(index = index, enterKey = planKey) {
                        RouteOptionChip(
                            minutes = s.minutes(option.durationMinutes.toInt()),
                            detail = "${s.walkingMinutes(option.walkingMinutes.toInt())} · ${session.formatFare(option.estimatedFare)}",
                            lines = option.legs.filterIsInstance<TransitLeg>().joinToString(" · ") { it.lineName }
                                .ifBlank { s.walking },
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

            Spacer(Modifier.height(GowaykiSpace.smd))

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
                        Text(s.minutes(current.durationMinutes.toInt()), style = MaterialTheme.typography.h5)
                        Text(
                            "${s.walkingMinutes(current.walkingMinutes.toInt())} · ${s.transfersCount(current.transfers)} · ${s.format(s.fareAmount, session.formatFare(current.estimatedFare))}",
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }

            if (snap != JourneySheetSnap.Peek && trip != null) {
                Spacer(Modifier.height(GowaykiSpace.smd))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.stepByStep, style = MaterialTheme.typography.subtitle1, modifier = Modifier.weight(1f))
                    Text(
                        s.viewDetail,
                        style = MaterialTheme.typography.button,
                        color = colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(GowaykiRadius.pill))
                            .clickable(role = Role.Button) { session.openDetail(selected) }
                            .padding(horizontal = GowaykiSpace.sm, vertical = GowaykiSpace.xs),
                    )
                }
                Spacer(Modifier.height(GowaykiSpace.sm))
                val stepsMax: Dp = if (snap == JourneySheetSnap.Full) 280.dp else 120.dp
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(stepsMax)
                        .verticalScroll(rememberScrollState()),
                ) {
                    trip.legs.forEachIndexed { i, leg ->
                        when (leg) {
                            is WalkLeg -> StepRow(
                                iconTint = colors.walk,
                                title = s.walkMeters(leg.meters.toInt()),
                                subtitle = s.walkStraightHint,
                            )
                            is TransitLeg -> StepRow(
                                iconTint = parseLineColor(leg.color, colors.primary),
                                title = "${s.takeLine} ${leg.lineName} → ${leg.headsign}",
                                subtitle = "${s.boardAt} ${leg.boardingStop.name} · ${s.alightAt} ${leg.alightingStop.name}",
                            )
                        }
                        if (i < trip.legs.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = GowaykiSpace.xs)
                                    .width(GowaykiStroke.strong)
                                    .height(GowaykiSpace.smd)
                                    .background(colors.outline),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(GowaykiSpace.smd))
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

/** Mapa del recorrido; lee la ubicación aquí para no recomponer la hoja con cada fix. */
@Composable
private fun JourneyMap(session: AppSession, trip: Itinerary?, padBottom: Int) {
    val segments = remember(trip) { itinerarySegments(trip) }
    val stops = remember(trip) { itineraryStops(trip) }
    val ends = remember(trip) { itineraryEndpoints(trip) }
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
}

@Composable
private fun RouteOptionChip(
    minutes: String,
    detail: String,
    lines: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = GowaykiTheme.colors
    val borderColor by animateColorAsState(
        if (selected) colors.primary else colors.outline,
        animationSpec = GowaykiMotion.selection(),
        label = "chipBorder",
    )
    val bg by animateColorAsState(
        if (selected) colors.primarySoft else colors.surfaceSunken,
        animationSpec = GowaykiMotion.selection(),
        label = "chipBg",
    )
    val scale by animateFloatAsState(
        if (selected) 1.03f else 1f,
        animationSpec = GowaykiMotion.selection(),
        label = "chipScale",
    )
    val shape = RoundedCornerShape(GowaykiRadius.md)
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .border(width = if (selected) GowaykiStroke.strong else GowaykiStroke.hairline, color = borderColor, shape = shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = GowaykiSpace.md, vertical = GowaykiSpace.smd)
            .semantics { contentDescription = "$minutes · $lines" },
    ) {
        Text(minutes, style = GowaykiType.metric, color = if (selected) colors.primary else colors.onSurface)
        Text(detail, style = MaterialTheme.typography.caption)
        Text(lines, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EndpointRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(GowaykiSize.dot).clip(CircleShape).background(color))
        Spacer(Modifier.width(GowaykiSpace.smd))
        Text(
            label,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
