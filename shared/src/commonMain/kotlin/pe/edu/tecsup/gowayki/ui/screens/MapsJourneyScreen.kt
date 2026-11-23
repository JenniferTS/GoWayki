package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.map.CityMap
import pe.edu.tecsup.gowayki.ui.map.MapOverlay
import pe.edu.tecsup.gowayki.ui.map.itineraryEndpoints
import pe.edu.tecsup.gowayki.ui.map.itinerarySegments
import pe.edu.tecsup.gowayki.ui.map.itineraryStops
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors

/**
 * Experiencia tipo Google Maps Transit: mapa a sangre, chips origen/destino,
 * alternativas, hoja de pasos caminata+micro e iniciar viaje.
 */
@Composable
fun MapsJourneyScreen(session: AppSession) {
    val s = session.strings
    val list = session.journey?.itineraries.orEmpty()
    var selected by remember { mutableStateOf(0) }
    var sheetExpanded by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        session.startUserLocationWatch(setOriginIfEmpty = session.origin == null)
    }
    DisposableEffect(Unit) {
        onDispose { session.stopUserLocationWatch() }
    }
    LaunchedEffect(list.size) {
        selected = 0
        if (list.isNotEmpty()) session.selectItinerary(0)
    }
    val trip = list.getOrNull(selected)
    val segments = remember(trip) { itinerarySegments(trip) }
    val stops = remember(trip) { itineraryStops(trip) }
    val ends = remember(trip) { itineraryEndpoints(trip) }

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
            ),
        )

        // Top search-like card (Google style)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 14.dp, end = 14.dp)
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(28.dp)),
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

        // Recenter FAB — pide GPS fresco y centra cámara en el usuario
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (sheetExpanded) 340.dp else 168.dp)
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
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 24.dp, end = 24.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                elevation = 6.dp,
            ) {
                Text(msg, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 13.sp, color = GowaykiColors.BrandInk)
            }
        }

        // Bottom sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize()
                .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            elevation = 0.dp,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(GowaykiColors.Outline)
                        .clickable { sheetExpanded = !sheetExpanded },
                )
                Spacer(Modifier.height(10.dp))

                if (list.isEmpty()) {
                    Text(session.planError ?: s.emptyRoutes, color = GowaykiColors.Stone)
                    PrimaryButton(s.retry, onClick = { session.go(Screen.Search) })
                    return@Column
                }

                // Alternative routes chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    list.forEachIndexed { index, option ->
                        val on = index == selected
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (on) GowaykiColors.BrandOrange.copy(alpha = 0.12f) else GowaykiColors.Mist)
                                .border(
                                    width = if (on) 2.dp else 1.dp,
                                    color = if (on) GowaykiColors.BrandOrange else GowaykiColors.Outline,
                                    shape = RoundedCornerShape(18.dp),
                                )
                                .clickable {
                                    selected = index
                                    session.selectItinerary(index)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "${option.durationMinutes.toInt()} min",
                                fontWeight = FontWeight.Black,
                                color = if (on) GowaykiColors.BrandOrange else GowaykiColors.BrandInk,
                                fontSize = 18.sp,
                            )
                            Text(
                                "${option.walkingMinutes.toInt()} min ${s.walking} · ${session.formatFare(option.estimatedFare)}",
                                color = GowaykiColors.Stone,
                                fontSize = 12.sp,
                            )
                            Text(
                                option.legs.filterIsInstance<TransitLeg>().joinToString(" · ") { it.lineName }
                                    .ifBlank { s.walking },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = GowaykiColors.BrandInk,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (trip != null) {
                    Text(
                        "${trip.durationMinutes.toInt()} ${s.minutesApprox}",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "${s.walking}: ${trip.walkingMinutes.toInt()} min · ${s.transfers}: ${trip.transfers} · ${s.fareEstimated}: ${session.formatFare(trip.estimatedFare)}",
                        color = GowaykiColors.Stone,
                        fontSize = 13.sp,
                    )
                }

                if (sheetExpanded && trip != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(s.stepByStep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(168.dp)
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
                        session.startActiveTrip()
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun EndpointRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GowaykiColors.BrandInk, maxLines = 1)
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = GowaykiColors.Stone, fontSize = 12.sp)
        }
    }
}

private fun parseColor(hex: String): Color {
    val t = hex.trim().removePrefix("#")
    return runCatching {
        Color(("FF$t").toLong(16))
    }.getOrDefault(GowaykiColors.BrandOrange)
}
