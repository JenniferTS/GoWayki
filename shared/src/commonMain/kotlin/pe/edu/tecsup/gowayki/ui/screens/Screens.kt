package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.tecsup.gowayki.domain.community.ReportKind
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.domain.tracking.TripPhase
import pe.edu.tecsup.gowayki.platform.AlarmMode
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.AtmosphereBackground
import pe.edu.tecsup.gowayki.ui.components.BottomNavBar
import pe.edu.tecsup.gowayki.ui.components.BrandMark
import pe.edu.tecsup.gowayki.ui.components.EnterContent
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.NavItem
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.ScreenHeader
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.map.CityMap
import pe.edu.tecsup.gowayki.ui.map.MapOverlay
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

@Composable
fun SplashScreen(session: AppSession) {
    val reduce = LocalMotionPrefs.current.reduceMotion
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(if (reduce) 0 else GowaykiMotion.EmphasizedMs),
    )
    LaunchedEffect(Unit) { session.bootstrap() }
    AtmosphereBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
                BrandMark(subtitle = "Arequipa · rutas con criterio", large = true)
                Spacer(Modifier.height(28.dp))
                CircularProgressIndicator(color = GowaykiColors.Terracotta, strokeWidth = 3.dp)
                Text(session.strings.loading, color = GowaykiColors.Stone, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
}

@Composable
fun HomeScreen(session: AppSession) {
    val s = session.strings
    Scaffold(
        backgroundColor = Color.Transparent,
        bottomBar = {
            BottomNavBar(
                items = listOf(
                    NavItem(s.homeRoutes, GowaykiIcons.Routes) { session.go(Screen.Home) },
                    NavItem(s.homeSearchCta, GowaykiIcons.Search) { session.go(Screen.Search) },
                    NavItem(s.homeReport, GowaykiIcons.Report) { session.go(Screen.Reports) },
                    NavItem(s.homeProfile, GowaykiIcons.Profile) { session.go(Screen.Profile) },
                ),
                activeIndex = 0,
            )
        },
    ) { padding ->
        AtmosphereBackground {
            EnterContent(Modifier.padding(padding)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(280.dp)) {
                        CityMap(
                            modifier = Modifier.fillMaxSize(),
                            overlay = MapOverlay(
                                markers = session.nearbyReports.map {
                                    pe.edu.tecsup.gowayki.domain.planner.GeoPoint(it.latitude, it.longitude)
                                },
                                stops = session.mapStopPoints,
                                route = session.selectedLineGeometry,
                                camera = pe.edu.tecsup.gowayki.domain.planner.GeoPoint(-16.399, -71.537),
                                zoom = 12.2,
                            ),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x66000000), Color.Transparent, Color(0xEEF6F1EA)),
                                    ),
                                ),
                        )
                        Column(Modifier.align(Alignment.TopStart).padding(GowaykiSpace.lg)) {
                            BrandMark(subtitle = "Arequipa en movimiento", large = true, light = true)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${s.fareEstimated}: ${session.formatFare(session.baseFare)}",
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Medium,
                            )
                            Row {
                                if (session.activeTrip?.running == true) {
                                    TextButton(onClick = { session.go(Screen.ActiveTrip) }) {
                                        Text(s.activeTrip, color = Color.White)
                                    }
                                }
                                TextButton(onClick = { session.go(Screen.Game) }) {
                                    Text(s.gameTitle, color = Color.White)
                                }
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = GowaykiSpace.lg).padding(bottom = GowaykiSpace.xl)) {
                        GlassPanel(onClick = { session.go(Screen.Search) }, modifier = Modifier.fillMaxWidth()) {
                            Text(s.homeSearchCta, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.searchHint, color = GowaykiColors.Stone)
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton(s.findRoutes, onClick = { session.go(Screen.Search) })
                        }
                        if (session.recents.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            Text(s.recentTrips, style = MaterialTheme.typography.h6)
                            Spacer(Modifier.height(8.dp))
                            session.recents.take(3).forEach { trip ->
                                GlassPanel(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    onClick = {
                                        session.applyRecent(trip)
                                        session.go(Screen.Search)
                                    },
                                ) {
                                    Text("${trip.from.name} → ${trip.to.name}", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(s.nearbyReports, style = MaterialTheme.typography.h6)
                        Text(s.communityDisclaimer, style = MaterialTheme.typography.caption)
                        Spacer(Modifier.height(8.dp))
                        session.nearbyReports.take(4).forEach { report ->
                            GlassPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(report.kind.apiValue, fontWeight = FontWeight.SemiBold)
                                        Text("👍 ${report.confirmations}", color = GowaykiColors.Stone, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { session.confirmNearbyReport(report.id) }) {
                                        Text(s.confirmReport, color = GowaykiColors.Terracotta)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (session.showTutorialOverlay) TutorialOverlay(session)
        }
    }
}

@Composable
fun TutorialOverlay(session: AppSession) {
    val s = session.strings
    val messages = listOf(s.tutorial1, s.tutorial2, s.tutorial3, s.tutorial4)
    val step = session.tutorial.step.coerceIn(0, 3)
    Box(Modifier.fillMaxSize().background(Color(0x99000000))) {
        GlassPanel(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()) {
            Text("${step + 1} ${s.tutorialStepOf} 4", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text(messages[step], style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { session.finishTutorial(skipped = true) }) { Text(s.skip) }
                Row {
                    if (step > 0) TextButton(onClick = { session.tutorialBack() }) { Text(s.back) }
                    PrimaryButton(
                        text = if (step >= 3) s.done else s.next,
                        onClick = { session.tutorialNext() },
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(session: AppSession) {
    val s = session.strings
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ScreenHeader(s.findRoutes, onBack = { session.go(Screen.Home) })
                Spacer(Modifier.height(8.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = session.originQuery,
                        onValueChange = {
                            session.originQuery = it
                            session.searchingDestination = false
                            session.searchPlaces(it)
                        },
                        label = { Text(s.searchOrigin) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchOrigin },
                        shape = RoundedCornerShape(GowaykiRadius.md),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = session.destinationQuery,
                        onValueChange = {
                            session.destinationQuery = it
                            session.searchingDestination = true
                            session.searchPlaces(it)
                        },
                        label = { Text(s.searchDestination) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchDestination },
                        shape = RoundedCornerShape(GowaykiRadius.md),
                    )
                }
                AnimatedVisibility(visible = session.searchHits.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    GlassPanel(Modifier.fillMaxWidth().padding(top = 10.dp).height(160.dp)) {
                        LazyColumn {
                            itemsIndexed(session.searchHits) { _, hit ->
                                Text(
                                    "${hit.name} · ${hit.address}",
                                    modifier = Modifier.fillMaxWidth().clickable { session.selectPlace(hit) }.padding(10.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SoftButton(s.myLocation, onClick = { session.useCurrentLocationAsOrigin() })
                session.locationMessage?.let { Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp) }
                SoftButton("Plaza de Armas → TECSUP", onClick = { session.useDemoEndpoints() })
                if (session.favorites.isNotEmpty()) {
                    Text(s.favorites, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                    session.favorites.take(5).forEach { fav ->
                        Text(fav.name, modifier = Modifier.fillMaxWidth().clickable { session.applyFavorite(fav) }.padding(vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = s.findRoutes,
                    onClick = { session.planTrip() },
                    enabled = !session.planning && session.origin != null && session.destination != null,
                )
                if (session.planning) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = GowaykiColors.Terracotta)
                }
                session.planError?.let { Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
fun ResultsScreen(session: AppSession) {
    val s = session.strings
    val list = session.journey?.itineraries.orEmpty()
    var previewIndex by remember { mutableStateOf(0) }
    val preview = list.getOrNull(previewIndex.coerceAtMost(list.lastIndex.coerceAtLeast(0)))
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.homeRoutes, onBack = { session.go(Screen.Search) })
                if (list.isEmpty()) {
                    Text(session.planError ?: s.emptyRoutes, color = GowaykiColors.Stone)
                    PrimaryButton(s.retry, onClick = { session.go(Screen.Search) })
                } else {
                    CityMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(GowaykiRadius.lg)),
                        overlay = MapOverlay(
                            route = session.routeGeometry(preview),
                            markers = session.routeEndpoints(preview),
                            zoom = 12.8,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        itemsIndexed(list) { index, trip ->
                            val selected = index == previewIndex
                            GlassPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                2.dp,
                                                GowaykiColors.Terracotta.copy(alpha = 0.55f),
                                                RoundedCornerShape(GowaykiRadius.lg),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                                onClick = { previewIndex = index },
                            ) {
                                Text(
                                    "${trip.durationMinutes.toInt()} ${s.minutesApprox} · ${trip.transfers} ${s.transfers}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.h6,
                                    color = if (selected) GowaykiColors.Terracotta else GowaykiColors.Charcoal,
                                )
                                Text(
                                    "${s.walking}: ${trip.walkingMinutes.toInt()} min · ${s.fareEstimated}: ${session.formatFare(trip.estimatedFare)}",
                                    color = GowaykiColors.Stone,
                                )
                                trip.legs.filterIsInstance<TransitLeg>().forEach { leg ->
                                    Text(
                                        "${leg.lineName} → ${leg.headsign}",
                                        color = GowaykiColors.Charcoal,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = s.viewDetail,
                        onClick = { session.openDetail(previewIndex) },
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreen(session: AppSession) {
    val s = session.strings
    val trip = session.selectedItinerary
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ScreenHeader(s.fareEstimated, onBack = { session.go(Screen.Results) })
                if (trip == null) {
                    Text(s.errorGeneric)
                    return@Column
                }
                Text(session.formatFare(trip.estimatedFare), style = MaterialTheme.typography.h4, color = GowaykiColors.Terracotta)
                Spacer(Modifier.height(10.dp))
                CityMap(
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(GowaykiRadius.lg)),
                    overlay = MapOverlay(
                        route = session.routeGeometry(trip),
                        markers = session.routeEndpoints(trip),
                        stops = trip.legs.filterIsInstance<TransitLeg>().flatMap { leg ->
                            (listOf(leg.boardingStop) + leg.intermediateStops + listOf(leg.alightingStop))
                                .map { pe.edu.tecsup.gowayki.domain.planner.GeoPoint(it.latitude, it.longitude) }
                        },
                        zoom = 13.0,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                trip.legs.forEach { leg ->
                    GlassPanel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        when (leg) {
                            is WalkLeg -> Text("${s.walking}: ${leg.meters.toInt()} m", fontWeight = FontWeight.Medium)
                            is TransitLeg -> Text("${leg.lineName} → ${leg.headsign} · ${session.formatFare(leg.fare)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SoftButton(s.addFavorite, onClick = { session.toggleFavoriteDestination() })
                Spacer(Modifier.height(8.dp))
                PrimaryButton(s.startTrip, onClick = { session.startActiveTrip() })
            }
        }
    }
}

@Composable
fun ReportsScreen(session: AppSession) {
    val s = session.strings
    val kinds = listOf(
        ReportKind.HeavyTraffic to s.reportHeavyTraffic,
        ReportKind.Detour to s.reportDetour,
        ReportKind.RoadBlocked to s.reportRoadBlocked,
        ReportKind.Other to s.reportOther,
    )
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ScreenHeader(s.reportTitle, onBack = { session.go(Screen.Home) })
                Text(s.communityDisclaimer, style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(12.dp))
                kinds.forEach { (kind, label) ->
                    val selected = session.reportKind == kind
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { session.reportKind = kind },
                        color = if (selected) GowaykiColors.TerracottaSoft.copy(alpha = 0.55f) else GowaykiColors.SurfaceElevated,
                        shape = RoundedCornerShape(GowaykiRadius.md),
                    ) { Text(label, modifier = Modifier.padding(14.dp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
                }
                OutlinedTextField(
                    value = session.reportDescription,
                    onValueChange = { session.reportDescription = it },
                    label = { Text(s.reportDescription) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GowaykiRadius.md),
                )
                SoftButton(s.reportUseLocation, onClick = { session.useCurrentLocationForReport() })
                PrimaryButton(s.reportSubmit, onClick = { session.submitReport() })
                session.reportMessage?.let { if (it != "ok") Text(it, color = MaterialTheme.colors.error) }
            }
        }
    }
}

@Composable
fun ProfileScreen(session: AppSession) {
    val s = session.strings
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ScreenHeader(s.profile, onBack = { session.go(Screen.Home) })
                GlassPanel(Modifier.fillMaxWidth()) {
                    BrandMark(subtitle = session.user?.displayName ?: s.guest)
                }
                Spacer(Modifier.height(12.dp))
                if (session.user?.isGuest != false) PrimaryButton(s.signIn, onClick = { session.go(Screen.Auth) })
                else SoftButton(s.signOut, onClick = { session.signOutToGuest() })
                SoftButton(s.settings, onClick = { session.go(Screen.Settings) })
                SoftButton(s.help, onClick = { session.go(Screen.Help) })
                SoftButton(s.linesExplorer, onClick = { session.go(Screen.Lines) })
                SoftButton(s.gameTitle, onClick = { session.go(Screen.Game) })
                SoftButton(s.weeklyRanking, onClick = { session.go(Screen.Ranking) })
            }
        }
    }
}

@Composable
fun SettingsScreen(session: AppSession) {
    val s = session.strings
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.settings, onBack = { session.go(Screen.Profile) })
                Text(s.language, fontWeight = FontWeight.SemiBold)
                Row {
                    SoftButton(s.spanish, onClick = { session.updateLocale(AppLocale.Es) }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SoftButton(s.english, onClick = { session.updateLocale(AppLocale.En) }, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material.Checkbox(checked = session.reduceMotion, onCheckedChange = { session.reduceMotion = it })
                    Text(s.reducedMotion)
                }
                Text(s.alarmMode, fontWeight = FontWeight.SemiBold)
                Row {
                    listOf(AlarmMode.Sound to s.alarmSound, AlarmMode.Vibration to s.alarmVibration, AlarmMode.Off to s.alarmOff).forEach { (mode, label) ->
                        SoftButton(label, onClick = { session.setAlarm(mode) }, Modifier.weight(1f).padding(end = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTripScreen(session: AppSession) {
    val s = session.strings
    val state = session.activeTrip
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.activeTrip, onBack = { session.go(Screen.Home) })
                if (state == null) {
                    Text(s.noActiveTrip, color = GowaykiColors.Stone)
                    return@Column
                }
                val phaseLabel = when (state.progress.phase) {
                    TripPhase.Walking -> s.tripPhaseWalking
                    TripPhase.WaitingToBoard -> s.tripPhaseWaiting
                    TripPhase.Riding -> s.tripPhaseRiding
                    TripPhase.Completed -> s.tripPhaseDone
                }
                CityMap(
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(GowaykiRadius.lg)),
                    overlay = MapOverlay(
                        route = session.routeGeometry(state.trip),
                        user = state.progress.fix?.point,
                        camera = state.progress.fix?.point ?: state.trip.legs.getOrNull(state.progress.legIndex)?.from,
                        zoom = 14.0,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(phaseLabel, style = MaterialTheme.typography.h6)
                    state.progress.remainingMeters?.let { Text("${s.remainingMeters}: ${it.toInt()} m", color = GowaykiColors.Stone) }
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    if (state.progress.phase == TripPhase.WaitingToBoard) s.confirmBoard else s.confirmStep,
                    onClick = { session.confirmTripStep() },
                )
                SoftButton(
                    text = s.stopTrip,
                    onClick = {
                        session.stopActiveTrip()
                        session.go(Screen.Home)
                    },
                )
            }
        }
    }
}

@Composable
fun RankingScreen(session: AppSession) {
    val s = session.strings
    LaunchedEffect(Unit) { session.loadRanking() }
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.weeklyRanking, onBack = { session.go(Screen.Game) })
                if (session.rankingLoading) CircularProgressIndicator(color = GowaykiColors.Terracotta)
                session.rankingMessage?.let { Text(it, color = MaterialTheme.colors.error) }
                LazyColumn {
                    itemsIndexed(session.ranking?.top.orEmpty()) { index, row ->
                        GlassPanel(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. ${row.name}", fontWeight = FontWeight.Medium)
                                Text("${row.score}", color = GowaykiColors.Terracotta, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (session.pendingScoreCount > 0) {
                    PrimaryButton(s.retryScores, onClick = { session.flushPendingScores() })
                }
            }
        }
    }
}

@Composable
fun LinesScreen(session: AppSession) {
    val s = session.strings
    LaunchedEffect(Unit) { session.refreshPackMapData() }
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.linesExplorer, onBack = { session.go(Screen.Home) })
                CityMap(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(GowaykiRadius.lg)),
                    overlay = MapOverlay(
                        route = session.selectedLineGeometry,
                        stops = session.mapStopPoints.take(40),
                        camera = session.selectedLineGeometry.getOrNull(session.selectedLineGeometry.size / 2)
                            ?: pe.edu.tecsup.gowayki.domain.planner.GeoPoint(-16.399, -71.537),
                        zoom = 12.5,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(session.transitLines) { _, line ->
                        val selected = session.selectedLine?.id == line.id
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { session.selectLine(line) },
                        ) {
                            Text(
                                line.shortName,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) GowaykiColors.Terracotta else GowaykiColors.Charcoal,
                            )
                            Text(line.ida?.headsign ?: line.directions.firstOrNull()?.headsign.orEmpty(), color = GowaykiColors.Stone, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpScreen(session: AppSession) {
    val s = session.strings
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.md).fillMaxSize()) {
            Column {
                ScreenHeader(s.help, onBack = { session.go(Screen.Profile) })
                Text(s.permissionLocationRationale, color = GowaykiColors.Stone)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(s.seeTutorial, onClick = { session.replayTutorial() })
            }
        }
    }
}

@Composable
fun AuthScreen(session: AppSession) {
    val s = session.strings
    var authError by remember { mutableStateOf<String?>(null) }
    AtmosphereBackground {
        EnterContent(Modifier.padding(GowaykiSpace.lg).fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrandMark(subtitle = s.signIn, large = true)
                Spacer(Modifier.height(28.dp))
                PrimaryButton(
                    text = s.signInGoogle,
                    onClick = {
                        authError = null
                        pe.edu.tecsup.gowayki.platform.launchPlatformGoogleSignIn(
                            onSuccess = { signed ->
                                session.applyGoogleUser(signed.uid, signed.displayName, signed.isNewUser)
                            },
                            onError = { authError = it },
                        )
                    },
                )
                SoftButton(
                    text = "${s.signInGoogle} (demo)",
                    onClick = { session.signInWithGoogleDemo(isNew = true) },
                )
                authError?.let { Text(it, color = MaterialTheme.colors.error) }
                TextButton(onClick = { session.go(Screen.Home) }) { Text(s.skip) }
            }
        }
    }
}
