package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import pe.edu.tecsup.gowayki.ui.map.CityMap
import pe.edu.tecsup.gowayki.ui.map.MapOverlay
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

@Composable
fun SplashScreen(session: AppSession) {
    LaunchedEffect(Unit) {
        session.bootstrap()
    }
    Box(Modifier.fillMaxSize().background(GowaykiColors.Ivory), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GoWayki", style = MaterialTheme.typography.h4, color = GowaykiColors.Terracotta, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = GowaykiColors.Terracotta)
            Text(session.strings.loading, color = GowaykiColors.Stone, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
fun OnboardingScreen(session: AppSession) {
    val s = session.strings
    val reduce = LocalMotionPrefs.current.reduceMotion
    val titles = listOf(s.onboardingTitle1, s.onboardingTitle2, s.onboardingTitle3)
    val bodies = listOf(s.onboardingBody1, s.onboardingBody2, s.onboardingBody3)
    val step = session.onboardingStep.coerceIn(0, 2)
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { session.finishOnboarding(skipped = true) }) { Text(s.skip) }
            Row {
                TextButton(onClick = { session.updateLocale(AppLocale.Es) }) { Text(s.spanish) }
                TextButton(onClick = { session.updateLocale(AppLocale.En) }) { Text(s.english) }
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = (step + 1) / 3f,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(8.dp)),
            color = GowaykiColors.Terracotta,
            backgroundColor = GowaykiColors.Sand,
        )
        Spacer(Modifier.weight(0.2f))
        OnboardingIllustration(step = step, reduceMotion = reduce)
        Spacer(Modifier.height(24.dp))
        Text(titles[step], style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = GowaykiColors.Charcoal)
        Spacer(Modifier.height(8.dp))
        Text(bodies[step], color = GowaykiColors.Stone, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 0) {
                OutlinedButton(onClick = { session.onboardingStep = step - 1 }) { Text(s.back) }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Button(
                onClick = {
                    if (step >= 2) session.finishOnboarding(false) else session.onboardingStep = step + 1
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
            ) {
                Text(if (step >= 2) s.done else s.next)
            }
        }
    }
}

@Composable
private fun OnboardingIllustration(step: Int, reduceMotion: Boolean) {
    val pulse by animateFloatAsState(
        targetValue = if (reduceMotion) 0f else if (step % 2 == 0) 0.4f else -0.4f,
        animationSpec = tween(if (reduceMotion) 0 else GowaykiMotion.TransitionMs),
    )
    Canvas(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(GowaykiColors.Sand)) {
        val w = size.width
        val h = size.height
        when (step) {
            0 -> {
                drawCircle(GowaykiColors.Terracotta, radius = 14f, center = Offset(w * 0.2f, h * 0.7f))
                drawCircle(GowaykiColors.TransitOutbound, radius = 14f, center = Offset(w * 0.8f, h * 0.3f))
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.7f)
                    quadraticBezierTo(w * 0.5f, h * (0.2f + pulse * 0.05f), w * 0.8f, h * 0.3f)
                }
                drawPath(path, GowaykiColors.Charcoal, style = Stroke(width = 6f))
            }
            1 -> {
                drawRoundRect(Color.White, topLeft = Offset(w * 0.15f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f))
                drawRect(GowaykiColors.Terracotta.copy(alpha = 0.85f), topLeft = Offset(w * 0.2f, h * (0.35f + pulse * 0.02f)), size = androidx.compose.ui.geometry.Size(w * 0.35f, 18f))
                drawRect(GowaykiColors.Stone.copy(alpha = 0.5f), topLeft = Offset(w * 0.2f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.5f, 12f))
            }
            else -> {
                drawCircle(GowaykiColors.Warning, radius = 18f + pulse * 4f, center = Offset(w * 0.5f, h * 0.45f))
                drawCircle(Color.White, radius = 6f, center = Offset(w * 0.5f, h * 0.45f))
            }
        }
    }
}

@Composable
fun HomeScreen(session: AppSession) {
    val s = session.strings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.appName) },
                backgroundColor = GowaykiColors.Ivory,
                contentColor = GowaykiColors.Charcoal,
                actions = {
                    if (session.activeTrip?.running == true) {
                        TextButton(onClick = { session.go(Screen.ActiveTrip) }) { Text(s.activeTrip) }
                    }
                    TextButton(onClick = { session.go(Screen.Game) }) { Text(s.gameTitle) }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = { session.go(Screen.Search) }) { Text(s.homeSearchCta) }
                TextButton(onClick = { session.go(Screen.Search) }) { Text(s.homeRoutes) }
                TextButton(onClick = { session.go(Screen.Reports) }) { Text(s.homeReport) }
                TextButton(onClick = { session.go(Screen.Profile) }) { Text(s.homeProfile) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(GowaykiColors.Ivory)) {
            Column(Modifier.padding(GowaykiSpace.lg)) {
                Text(s.homeSearchCta, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${s.fareEstimated}: ${session.formatFare(session.baseFare)}",
                    color = GowaykiColors.Stone,
                )
                Spacer(Modifier.height(12.dp))
                CityMap(
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
                    overlay = MapOverlay(
                        markers = session.nearbyReports.map {
                            pe.edu.tecsup.gowayki.domain.planner.GeoPoint(it.latitude, it.longitude)
                        },
                        camera = pe.edu.tecsup.gowayki.domain.planner.GeoPoint(-16.399, -71.537),
                        zoom = 12.0,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { session.go(Screen.Search) },
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color.White,
                    elevation = 2.dp,
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(s.searchDestination, fontWeight = FontWeight.SemiBold)
                        Text(s.searchHint, color = GowaykiColors.Stone)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(s.nearbyReports, fontWeight = FontWeight.SemiBold)
                Text(s.communityDisclaimer, color = GowaykiColors.Stone, fontSize = 13.sp)
                session.nearbyReports.take(5).forEach { report ->
                    Text(
                        "${report.kind.apiValue} · ${report.author.publicId.take(8)}",
                        color = GowaykiColors.Charcoal,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (session.showTutorialOverlay) {
                TutorialOverlay(session)
            }
        }
    }
}

@Composable
fun TutorialOverlay(session: AppSession) {
    val s = session.strings
    val messages = listOf(s.tutorial1, s.tutorial2, s.tutorial3, s.tutorial4)
    val step = session.tutorial.step.coerceIn(0, 3)
    Box(Modifier.fillMaxSize().background(Color(0x99000000))) {
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("${step + 1} ${s.tutorialStepOf} 4", color = GowaykiColors.Stone, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(messages[step], fontWeight = FontWeight.Medium, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { session.finishTutorial(skipped = true) }) { Text(s.skip) }
                    Row {
                        if (step > 0) TextButton(onClick = { session.tutorialBack() }) { Text(s.back) }
                        Button(
                            onClick = { session.tutorialNext() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
                        ) { Text(if (step >= 3) s.done else s.next) }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(session: AppSession) {
    val s = session.strings
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Home) }) { Text(s.back) }
        Text(s.findRoutes, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = session.originQuery,
            onValueChange = {
                session.originQuery = it
                session.searchingDestination = false
                session.searchPlaces(it)
            },
            label = { Text(s.searchOrigin) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchOrigin },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = session.destinationQuery,
            onValueChange = {
                session.destinationQuery = it
                session.searchingDestination = true
                session.searchPlaces(it)
            },
            label = { Text(s.searchDestination) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchDestination },
        )
        AnimatedVisibility(visible = session.searchHits.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            LazyColumn(Modifier.fillMaxWidth().height(180.dp)) {
                itemsIndexed(session.searchHits) { _, hit ->
                    Text(
                        "${hit.name} · ${hit.address}",
                        modifier = Modifier.fillMaxWidth().clickable { session.selectPlace(hit) }.padding(12.dp),
                    )
                }
            }
        }
        if (session.searchHits.isEmpty() &&
            (session.originQuery.length >= 2 || session.destinationQuery.length >= 2)
        ) {
            Text(s.noResults, color = GowaykiColors.Stone, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { session.useDemoEndpoints() }, modifier = Modifier.fillMaxWidth()) {
            Text("Plaza de Armas → TECSUP")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { session.planTrip() },
            enabled = !session.planning && session.origin != null && session.destination != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
        ) {
            if (session.planning) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(s.findRoutes)
        }
        session.planError?.let { Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
fun ResultsScreen(session: AppSession) {
    val s = session.strings
    val list = session.journey?.itineraries.orEmpty()
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Search) }) { Text(s.back) }
        Text(s.homeRoutes, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        if (list.isEmpty()) {
            Text(session.planError ?: s.emptyRoutes, color = GowaykiColors.Stone)
            Button(onClick = { session.go(Screen.Search) }) { Text(s.retry) }
        } else {
            LazyColumn {
                itemsIndexed(list) { index, trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { session.openDetail(index) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = 1.dp,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${trip.durationMinutes.toInt()} ${s.minutesApprox} · ${trip.transfers} ${s.transfers}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${s.walking}: ${trip.walkingMinutes.toInt()} min · ${s.fareEstimated}: ${session.formatFare(trip.estimatedFare)}",
                                color = GowaykiColors.Stone,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(session: AppSession) {
    val s = session.strings
    val trip = session.selectedItinerary
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Results) }) { Text(s.back) }
        if (trip == null) {
            Text(s.errorGeneric)
            return
        }
        Text(s.fareEstimated + ": " + session.formatFare(trip.estimatedFare), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        CityMap(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
            overlay = MapOverlay(
                route = session.routeGeometry(trip),
                camera = trip.legs.firstOrNull()?.from,
                zoom = 13.0,
            ),
        )
        Spacer(Modifier.height(12.dp))
        trip.legs.forEach { leg ->
            when (leg) {
                is WalkLeg -> Text("${s.walking}: ${leg.meters.toInt()} m")
                is TransitLeg -> Text("${leg.lineName} → ${leg.headsign} · ${session.formatFare(leg.fare)}")
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { session.startActiveTrip() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
        ) { Text(s.startTrip) }
    }
}

@Composable
private fun RoutePreview(segments: List<Pair<pe.edu.tecsup.gowayki.domain.planner.GeoPoint, pe.edu.tecsup.gowayki.domain.planner.GeoPoint>>) {
    Canvas(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(GowaykiColors.Sand)) {
        if (segments.isEmpty()) return@Canvas
        val pts = segments.flatMap { listOf(it.first, it.second) }
        val minLat = pts.minOf { it.latitude }
        val maxLat = pts.maxOf { it.latitude }
        val minLng = pts.minOf { it.longitude }
        val maxLng = pts.maxOf { it.longitude }
        fun map(p: pe.edu.tecsup.gowayki.domain.planner.GeoPoint): Offset {
            val x = ((p.longitude - minLng) / (maxLng - minLng + 1e-9) * (size.width - 40f) + 20f).toFloat()
            val y = ((maxLat - p.latitude) / (maxLat - minLat + 1e-9) * (size.height - 40f) + 20f).toFloat()
            return Offset(x, y)
        }
        segments.forEachIndexed { i, pair ->
            drawLine(
                color = if (i % 2 == 0) GowaykiColors.Stone else GowaykiColors.Terracotta,
                start = map(pair.first),
                end = map(pair.second),
                strokeWidth = 6f,
            )
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
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Home) }) { Text(s.back) }
        Text(s.reportTitle, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Text(s.communityDisclaimer, color = GowaykiColors.Stone, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        kinds.forEach { (kind, label) ->
            val selected = session.reportKind == kind
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { session.reportKind = kind },
                color = if (selected) GowaykiColors.Sand else Color.White,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(label, modifier = Modifier.padding(14.dp))
            }
        }
        OutlinedTextField(
            value = session.reportDescription,
            onValueChange = { session.reportDescription = it },
            label = { Text(s.reportDescription) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${s.reportPickMap}: ${session.reportLat}, ${session.reportLng}",
            color = GowaykiColors.Stone,
            fontSize = 12.sp,
        )
        OutlinedButton(
            onClick = { session.useCurrentLocationForReport() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(s.reportUseLocation) }
        Text(s.communityDisclaimer, color = GowaykiColors.Stone, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
        Button(
            onClick = { session.submitReport() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
        ) { Text(s.reportSubmit) }
        session.reportMessage?.let { if (it != "ok") Text(it, color = MaterialTheme.colors.error) }
        session.myReports.forEach { report ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${report.kind.apiValue} · ${report.state}")
                TextButton(onClick = { session.withdrawReport(report.id) }) { Text(s.reportWithdraw) }
            }
        }
    }
}

@Composable
fun ProfileScreen(session: AppSession) {
    val s = session.strings
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Home) }) { Text(s.back) }
        Text(s.profile, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(session.user?.displayName ?: s.guest)
        Spacer(Modifier.height(8.dp))
        if (session.user?.isGuest != false) {
            Button(onClick = { session.go(Screen.Auth) }) { Text(s.signIn) }
        } else {
            OutlinedButton(onClick = { session.signOutToGuest() }) { Text(s.signOut) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { session.go(Screen.Settings) }, modifier = Modifier.fillMaxWidth()) { Text(s.settings) }
        OutlinedButton(onClick = { session.go(Screen.Help) }, modifier = Modifier.fillMaxWidth()) { Text(s.help) }
        OutlinedButton(onClick = { session.go(Screen.Game) }, modifier = Modifier.fillMaxWidth()) { Text(s.gameTitle) }
        OutlinedButton(onClick = { session.go(Screen.Ranking) }, modifier = Modifier.fillMaxWidth()) { Text(s.weeklyRanking) }
        if (session.activeTrip?.running == true) {
            OutlinedButton(onClick = { session.go(Screen.ActiveTrip) }, modifier = Modifier.fillMaxWidth()) {
                Text(s.activeTrip)
            }
        }
    }
}

@Composable
fun SettingsScreen(session: AppSession) {
    val s = session.strings
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Profile) }) { Text(s.back) }
        Text(s.settings, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(s.language, fontWeight = FontWeight.SemiBold)
        Row {
            Button(onClick = { session.updateLocale(AppLocale.Es) }) { Text(s.spanish) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { session.updateLocale(AppLocale.En) }) { Text(s.english) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material.Checkbox(
                checked = session.reduceMotion,
                onCheckedChange = { session.reduceMotion = it },
            )
            Text(s.reducedMotion)
        }
        Spacer(Modifier.height(12.dp))
        Text(s.alarmMode, fontWeight = FontWeight.SemiBold)
        Row {
            listOf(
                AlarmMode.Sound to s.alarmSound,
                AlarmMode.Vibration to s.alarmVibration,
                AlarmMode.Off to s.alarmOff,
            ).forEach { (mode, label) ->
                val selected = session.alarmMode == mode
                OutlinedButton(
                    onClick = { session.alarmMode = mode },
                    modifier = Modifier.padding(end = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selected) GowaykiColors.Terracotta else GowaykiColors.Charcoal,
                    ),
                ) { Text(label) }
            }
        }
        Text("${s.fareEstimated}: ${session.formatFare(session.baseFare)}", color = GowaykiColors.Stone)
    }
}

@Composable
fun ActiveTripScreen(session: AppSession) {
    val s = session.strings
    val state = session.activeTrip
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Home) }) { Text(s.back) }
        Text(s.activeTrip, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        if (state == null) {
            Text(s.noActiveTrip, color = GowaykiColors.Stone)
            return
        }
        val phaseLabel = when (state.progress.phase) {
            TripPhase.Walking -> s.tripPhaseWalking
            TripPhase.WaitingToBoard -> s.tripPhaseWaiting
            TripPhase.Riding -> s.tripPhaseRiding
            TripPhase.Completed -> s.tripPhaseDone
        }
        Spacer(Modifier.height(8.dp))
        CityMap(
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            overlay = MapOverlay(
                route = session.routeGeometry(state.trip),
                user = state.progress.fix?.point,
                camera = state.progress.fix?.point ?: state.trip.legs.getOrNull(state.progress.legIndex)?.from,
                zoom = 14.0,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Text(phaseLabel, fontWeight = FontWeight.SemiBold)
        state.progress.remainingMeters?.let {
            Text("${s.remainingMeters}: ${it.toInt()} m", color = GowaykiColors.Stone)
        }
        state.progress.stopsRemaining?.let {
            Text("${s.stopsRemaining}: $it", color = GowaykiColors.Stone)
        }
        if (state.stale) {
            Text(s.permissionLocationRationale, color = GowaykiColors.Warning, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { session.confirmTripStep() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
        ) {
            Text(
                if (state.progress.phase == TripPhase.WaitingToBoard) s.confirmBoard else s.confirmStep,
            )
        }
        OutlinedButton(
            onClick = {
                session.stopActiveTrip()
                session.go(Screen.Home)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(s.stopTrip) }
    }
}

@Composable
fun RankingScreen(session: AppSession) {
    val s = session.strings
    LaunchedEffect(Unit) { session.loadRanking() }
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Game) }) { Text(s.back) }
        Text(s.weeklyRanking, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (session.rankingLoading) {
            CircularProgressIndicator(color = GowaykiColors.Terracotta)
        }
        session.rankingMessage?.let { Text(it, color = MaterialTheme.colors.error) }
        session.ranking?.own?.let { own ->
            Text(
                "${s.progress}: #${session.ranking?.position ?: "—"} · ${own.score} pts",
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            itemsIndexed(session.ranking?.top.orEmpty()) { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${index + 1}. ${row.name}")
                    Text("${row.score}")
                }
            }
        }
        if (session.ranking?.top.isNullOrEmpty() && !session.rankingLoading) {
            Text(s.noResults, color = GowaykiColors.Stone)
        }
    }
}

@Composable
fun HelpScreen(session: AppSession) {
    val s = session.strings
    Column(Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.md)) {
        TextButton(onClick = { session.go(Screen.Profile) }) { Text(s.back) }
        Text(s.help, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(s.permissionLocationRationale, color = GowaykiColors.Stone)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { session.replayTutorial() }) { Text(s.seeTutorial) }
    }
}

@Composable
fun AuthScreen(session: AppSession) {
    val s = session.strings
    var authError by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().background(GowaykiColors.Ivory).padding(GowaykiSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(s.signIn, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                authError = null
                pe.edu.tecsup.gowayki.platform.launchPlatformGoogleSignIn(
                    onSuccess = { signed ->
                        session.applyGoogleUser(
                            uid = signed.uid,
                            displayName = signed.displayName,
                            isNew = signed.isNewUser,
                        )
                    },
                    onError = { authError = it },
                )
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = GowaykiColors.Terracotta, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(s.signInGoogle) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { session.signInWithGoogleDemo(isNew = true) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("${s.signInGoogle} (demo)") }
        authError?.let { Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp)) }
        TextButton(onClick = { session.go(Screen.Home) }) { Text(s.skip) }
    }
}
