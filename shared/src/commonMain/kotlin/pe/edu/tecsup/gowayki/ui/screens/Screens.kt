package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import pe.edu.tecsup.gowayki.domain.community.ReportKind
import pe.edu.tecsup.gowayki.domain.legal.LegalLinks
import pe.edu.tecsup.gowayki.domain.models.TransitLine
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.domain.preferences.ThemePreference
import pe.edu.tecsup.gowayki.domain.tracking.TripPhase
import pe.edu.tecsup.gowayki.i18n.extraStrings
import pe.edu.tecsup.gowayki.platform.AlarmMode
import pe.edu.tecsup.gowayki.platform.HapticKind
import pe.edu.tecsup.gowayki.platform.platformHaptic
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.a11y.A11yLabels
import pe.edu.tecsup.gowayki.ui.components.ActionConfirmBanner
import pe.edu.tecsup.gowayki.ui.components.AtmosphereBackground
import pe.edu.tecsup.gowayki.ui.components.BottomNavBar
import pe.edu.tecsup.gowayki.ui.components.BrandHero
import pe.edu.tecsup.gowayki.ui.components.BrandMark
import pe.edu.tecsup.gowayki.ui.components.DangerButton
import pe.edu.tecsup.gowayki.ui.components.ChoiceRow
import pe.edu.tecsup.gowayki.ui.components.EnterContent
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.GowaykiScreen
import pe.edu.tecsup.gowayki.ui.components.InlineMessage
import pe.edu.tecsup.gowayki.ui.components.NavItem
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SectionTitle
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.components.StepRow
import pe.edu.tecsup.gowayki.ui.components.gowaykiFieldColors
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
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

private val ArequipaCenter = GeoPoint(-16.399, -71.537)

@Composable
fun SplashScreen(session: AppSession) {
    val s = session.strings
    LaunchedEffect(Unit) { session.bootstrap() }
    AtmosphereBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(GowaykiSpace.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHero(subtitle = s.splashTagline, logoSize = GowaykiSize.logoHero)
            Spacer(Modifier.height(GowaykiSpace.xl))
            CircularProgressIndicator(color = GowaykiTheme.colors.primary)
            Spacer(Modifier.height(GowaykiSpace.smd))
            Text(s.preparingCity, style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
fun HomeScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    LaunchedEffect(Unit) {
        session.startUserLocationWatch(setOriginIfEmpty = false)
    }
    DisposableEffect(Unit) {
        onDispose { session.stopUserLocationWatch() }
    }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            backgroundColor = colors.background,
            contentColor = colors.onSurface,
            bottomBar = {
                BottomNavBar(
                    items = listOf(
                        NavItem(s.homeExplore, GowaykiIcons.Routes, A11yLabels.NAV_HOME) { session.go(Screen.Home) },
                        NavItem(s.linesExplorer, GowaykiIcons.Lines, A11yLabels.NAV_SEARCH) { session.go(Screen.Lines) },
                        NavItem(s.homeReport, GowaykiIcons.Report, A11yLabels.NAV_REPORTS) { session.go(Screen.Reports) },
                        NavItem(s.homeProfile, GowaykiIcons.Profile, A11yLabels.NAV_PROFILE) { session.go(Screen.Profile) },
                    ),
                    activeIndex = 0,
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                HomeMap(session)
                Column(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(GowaykiSpace.md),
                    verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
                ) {
                    HomeSearchCard(session)
                    HomeShortcuts(session)
                    session.locationMessage?.takeIf { !session.locating }?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.caption,
                            color = colors.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(GowaykiRadius.sm))
                                .background(colors.surfaceElevated)
                                .padding(horizontal = GowaykiSpace.smd, vertical = GowaykiSpace.sm),
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(GowaykiSpace.md)
                        .size(GowaykiSize.touchTarget)
                        .shadow(GowaykiElevation.card, CircleShape)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .clickable(role = Role.Button) { session.recenterOnUser() }
                        .semantics { contentDescription = A11yLabels.FAB_MY_LOCATION },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GowaykiIcons.Location, contentDescription = null, tint = colors.primary, modifier = Modifier.size(GowaykiSize.icon))
                }
            }
        }
        if (session.showTutorialOverlay) TutorialOverlay(session)
    }
}

/** Mapa a pantalla completa; aislado para que cada fix de ubicación no recomponga el resto. */
@Composable
private fun HomeMap(session: AppSession) {
    val reports = session.nearbyReports
    val markers = remember(reports) { reports.map { GeoPoint(it.latitude, it.longitude) } }
    val user = session.lastKnownLocation
    CityMap(
        modifier = Modifier.fillMaxSize(),
        overlay = MapOverlay(
            markers = markers,
            stops = session.mapStopPoints,
            route = session.selectedLineGeometry,
            user = user,
            camera = user ?: ArequipaCenter,
            zoom = if (user != null) 14.2 else 12.2,
            followEpoch = session.locationFollowEpoch,
        ),
    )
}

/** Tarjeta flotante “¿A dónde vas?” sobre el mapa (estilo Google Maps). */
@Composable
private fun HomeSearchCard(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    GlassPanel(onClick = { session.go(Screen.Search) }, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.homeSearchCta, style = MaterialTheme.typography.h5)
                Text(s.searchHint, style = MaterialTheme.typography.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(GowaykiSpace.smd))
            Box(
                Modifier
                    .size(GowaykiSize.touchTarget)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(colors.primaryStrong, colors.primary, colors.accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(GowaykiIcons.Search, contentDescription = s.findRoutes, tint = colors.onPrimary, modifier = Modifier.size(GowaykiSize.icon))
            }
        }
    }
}

/** Atajos pequeños bajo la tarjeta: viaje en curso y viajes recientes. */
@Composable
private fun HomeShortcuts(session: AppSession) {
    val s = session.strings
    val running = session.activeTrip?.running == true
    val recents = session.recents
    if (!running && recents.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
        if (running) {
            item(key = "active-trip") {
                MapChip(s.activeTrip, GowaykiIcons.Bus, highlighted = true) { session.go(Screen.ActiveTrip) }
            }
        }
        items(recents.take(5), key = { "${it.from.name}→${it.to.name}" }) { trip ->
            MapChip("${trip.from.name} → ${trip.to.name}", GowaykiIcons.Routes) {
                session.applyRecent(trip)
                session.go(Screen.Search)
            }
        }
    }
}

@Composable
private fun MapChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = GowaykiTheme.colors
    val shape = RoundedCornerShape(GowaykiRadius.pill)
    Row(
        Modifier
            .shadow(GowaykiElevation.low, shape, spotColor = colors.shadow)
            .clip(shape)
            .background(if (highlighted) colors.primary else colors.surfaceElevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = GowaykiSpace.smd, vertical = GowaykiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (highlighted) colors.onPrimary else colors.primary
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(GowaykiSize.iconSm))
        Spacer(Modifier.width(GowaykiSpace.xs))
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = if (highlighted) colors.onPrimary else colors.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TutorialOverlay(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val messages = listOf(s.tutorial1, s.tutorial2, s.tutorial3, s.tutorial4)
    val step = session.tutorial.step.coerceIn(0, 3)
    // El velo va en la ventana principal (de borde a borde): la ventana del Popup no cubre las barras del sistema.
    Box(Modifier.fillMaxSize().background(colors.scrim))
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = true, onClick = {}),
    ) {
        GlassPanel(modifier = Modifier.align(Alignment.Center).padding(GowaykiSpace.md).fillMaxWidth()) {
            Text(s.format(s.tutorialStep, step + 1, messages.size), style = MaterialTheme.typography.overline)
            Spacer(Modifier.height(GowaykiSpace.sm))
            Text(messages[step], style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(GowaykiSpace.md))
            PrimaryButton(
                text = if (step >= 3) s.done else s.next,
                onClick = { session.tutorialNext() },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { session.finishTutorial(skipped = true) }) {
                    Text(s.skip, style = MaterialTheme.typography.button, color = colors.onSurfaceMuted)
                }
                if (step > 0) {
                    TextButton(onClick = { session.tutorialBack() }) {
                        Text(s.back, style = MaterialTheme.typography.button, color = colors.primary)
                    }
                }
            }
        }
    }
    }
}

@Composable
fun SearchScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val reduce = LocalMotionPrefs.current.reduceMotion
    GowaykiScreen(
        title = s.findRoutes,
        onBack = {
            if (session.journey?.itineraries?.isNotEmpty() == true) session.go(Screen.Results)
            else session.go(Screen.Home)
        },
        // La flecha vuelve a Resultados, y Resultados vuelve aquí: el Atrás del sistema sube a Inicio
        // para no quedar en un ciclo Buscar ↔ Resultados.
        systemBack = { session.go(Screen.Home) },
        modifier = Modifier.imePadding(),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GlassPanel(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = session.originQuery,
                    onValueChange = {
                        session.originQuery = it
                        session.searchingDestination = false
                        session.searchPlaces(it)
                    },
                    label = { Text(s.searchOrigin) },
                    singleLine = true,
                    colors = gowaykiFieldColors(),
                    textStyle = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchOrigin },
                    shape = RoundedCornerShape(GowaykiRadius.md),
                )
                Spacer(Modifier.height(GowaykiSpace.smd))
                OutlinedTextField(
                    value = session.destinationQuery,
                    onValueChange = {
                        session.destinationQuery = it
                        session.searchingDestination = true
                        session.searchPlaces(it)
                    },
                    label = { Text(s.searchDestination) },
                    placeholder = { Text(s.searchExamples, style = MaterialTheme.typography.body2) },
                    singleLine = true,
                    colors = gowaykiFieldColors(),
                    textStyle = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.searchDestination },
                    shape = RoundedCornerShape(GowaykiRadius.md),
                )
            }
            AnimatedVisibility(
                visible = session.searchHits.isNotEmpty(),
                enter = fadeIn(if (reduce) GowaykiMotion.none() else GowaykiMotion.soft()),
                exit = fadeOut(GowaykiMotion.micro()),
            ) {
                GlassPanel(Modifier.fillMaxWidth().padding(top = GowaykiSpace.smd).heightIn(max = GowaykiSize.searchHits)) {
                    LazyColumn {
                        items(session.searchHits, key = { "${it.name}|${it.latitude}|${it.longitude}" }) { hit ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        session.selectPlace(hit)
                                        if (!reduce) platformHaptic(HapticKind.Light)
                                    }
                                    .padding(vertical = GowaykiSpace.sm),
                            ) {
                                Text(hit.name, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (hit.address.isNotBlank()) {
                                    Text(hit.address, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(GowaykiSpace.md))
            Column(verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
                SoftButton(
                    if (session.locating) s.locating else s.myLocation,
                    onClick = { session.useCurrentLocationAsOrigin() },
                )
                session.locationMessage?.takeIf { !session.locating }?.let {
                    InlineMessage(it, isError = true)
                }
                SoftButton(s.demoRoute, onClick = { session.useDemoEndpoints() })
            }
            if (session.favorites.isNotEmpty()) {
                SectionTitle(s.favorites)
                session.favorites.take(5).forEach { fav ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GowaykiRadius.sm))
                            .clickable(role = Role.Button) { session.applyFavorite(fav) }
                            .padding(vertical = GowaykiSpace.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(GowaykiIcons.Location, contentDescription = null, tint = colors.primary, modifier = Modifier.size(GowaykiSize.iconSm))
                        Spacer(Modifier.width(GowaykiSpace.sm))
                        Text(fav.name, style = MaterialTheme.typography.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
            if (session.journey?.itineraries?.isNotEmpty() == true) {
                SoftButton(s.homeRoutes, onClick = { session.go(Screen.Results) })
                Spacer(Modifier.height(GowaykiSpace.sm))
            }
            PrimaryButton(
                text = if (session.planning) s.loading else s.findRoutes,
                onClick = { session.planTrip() },
                enabled = !session.planning && session.origin != null && session.destination != null,
            )
            session.planError?.let { InlineMessage(it, isError = true, modifier = Modifier.padding(top = GowaykiSpace.sm)) }
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

@Composable
fun DetailScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val trip = session.selectedItinerary
    GowaykiScreen(title = s.routeDetail, onBack = { session.go(Screen.Results) }) {
        if (trip == null) {
            InlineMessage(s.errorGeneric, isError = true)
            return@GowaykiScreen
        }
        val segments = remember(trip) { itinerarySegments(trip) }
        val markers = remember(trip) { itineraryEndpoints(trip) }
        val stops = remember(trip) { itineraryStops(trip) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(s.fareEstimated, style = MaterialTheme.typography.caption)
            Text(session.formatFare(trip.estimatedFare), style = MaterialTheme.typography.h4, color = colors.primary)
            Text(
                "${s.minutes(trip.durationMinutes.toInt())} · ${s.walkingMinutes(trip.walkingMinutes.toInt())} · ${s.transfersCount(trip.transfers)}",
                style = MaterialTheme.typography.body2,
            )
            Spacer(Modifier.height(GowaykiSpace.smd))
            CityMap(
                modifier = Modifier.fillMaxWidth().height(GowaykiSize.mapCardTall).clip(RoundedCornerShape(GowaykiRadius.lg)),
                overlay = MapOverlay(segments = segments, markers = markers, stops = stops, zoom = 13.0),
            )
            SectionTitle(s.stepByStep)
            GlassPanel(Modifier.fillMaxWidth()) {
                trip.legs.forEach { leg ->
                    when (leg) {
                        is WalkLeg -> StepRow(
                            iconTint = colors.walk,
                            title = s.walkMeters(leg.meters.toInt()),
                            subtitle = s.walkStraightHint,
                        )
                        is TransitLeg -> StepRow(
                            iconTint = parseLineColor(leg.color, colors.primary),
                            title = "${s.takeLine} ${leg.lineName} → ${leg.headsign}",
                            subtitle = "${s.boardAt} ${leg.boardingStop.name} · ${s.alightAt} ${leg.alightingStop.name} · ${session.formatFare(leg.fare)}",
                        )
                    }
                }
            }
            Spacer(Modifier.height(GowaykiSpace.md))
            val isFavorite = session.favorites.any { fav ->
                val dest = session.destination
                dest != null && kotlin.math.abs(fav.latitude - dest.latitude) < 1e-5 && kotlin.math.abs(fav.longitude - dest.longitude) < 1e-5
            }
            SoftButton(
                if (isFavorite) s.removeFavorite else s.addFavorite,
                onClick = { session.toggleFavoriteDestination() },
                selected = isFavorite,
            )
            Spacer(Modifier.height(GowaykiSpace.sm))
            PrimaryButton(s.startTrip, onClick = { session.startActiveTrip() })
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

@Composable
fun ReportsScreen(session: AppSession) {
    val s = session.strings
    val kinds = listOf(
        ReportKind.HeavyTraffic,
        ReportKind.Detour,
        ReportKind.RoadBlocked,
        ReportKind.Other,
    )
    val message = session.reportMessage
    val showOk = message == AppSession.REPORT_OK
    val isGuest = session.user?.isGuest != false
    LaunchedEffect(showOk) {
        if (showOk) {
            platformHaptic(HapticKind.Success)
            delay(GowaykiMotion.ConfirmVisibleMs)
            if (session.reportMessage == AppSession.REPORT_OK) {
                session.reportMessage = null
                session.go(Screen.Home)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        GowaykiScreen(title = s.reportTitle, onBack = { session.go(Screen.Home) }, modifier = Modifier.imePadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(s.communityDisclaimer, style = MaterialTheme.typography.caption)
                SectionTitle(s.reportType)
                Column(verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
                    kinds.forEach { kind ->
                        SoftButton(
                            text = s.reportKind(kind),
                            onClick = { session.reportKind = kind },
                            selected = session.reportKind == kind,
                        )
                    }
                }
                Spacer(Modifier.height(GowaykiSpace.md))
                OutlinedTextField(
                    value = session.reportDescription,
                    onValueChange = { if (it.length <= 500) session.reportDescription = it },
                    label = { Text(s.reportDescription) },
                    colors = gowaykiFieldColors(),
                    textStyle = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GowaykiRadius.md),
                )
                Spacer(Modifier.height(GowaykiSpace.smd))
                SoftButton(s.reportUseLocation, onClick = { session.useCurrentLocationForReport() })
                Text(
                    s.format(s.reportLocationSet, "${formatCoord(session.reportLat)}, ${formatCoord(session.reportLng)}"),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = GowaykiSpace.xs),
                )
                Spacer(Modifier.height(GowaykiSpace.md))
                if (isGuest) {
                    InlineMessage(s.reportNeedsAccount, isError = false)
                    Spacer(Modifier.height(GowaykiSpace.sm))
                    PrimaryButton(s.signIn, onClick = { session.go(Screen.Auth) })
                } else {
                    PrimaryButton(
                        text = if (session.reportSending) s.loading else s.reportSubmit,
                        onClick = { session.submitReport() },
                        enabled = !session.reportSending,
                    )
                }
                if (message != null && message != AppSession.REPORT_OK && message != AppSession.REPORT_CONFIRMED) {
                    InlineMessage(message, isError = true)
                }
                Spacer(Modifier.height(GowaykiSpace.xxl))
            }
        }
        ActionConfirmBanner(
            visible = showOk,
            message = s.reportSent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(GowaykiSpace.md)
                .fillMaxWidth(),
        )
    }
}

private fun formatCoord(value: Double): String {
    val scaled = kotlin.math.round(value * 10_000) / 10_000
    return scaled.toString()
}

@Composable
fun ProfileScreen(session: AppSession) {
    val s = session.strings
    val user = session.user
    val isGuest = user?.isGuest != false
    GowaykiScreen(title = s.profile, onBack = { session.go(Screen.Home) }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
        ) {
            GlassPanel(Modifier.fillMaxWidth()) {
                BrandMark(
                    subtitle = if (isGuest) s.guest else s.format(s.signedInAs, user.displayName ?: s.guest),
                )
            }
            Spacer(Modifier.height(GowaykiSpace.xs))
            session.accountNotice?.let { InlineMessage(it, isError = false) }
            if (isGuest) {
                PrimaryButton(s.signIn, onClick = { session.go(Screen.Auth) })
            } else {
                if (session.accountDeletionPending) {
                    InlineMessage(s.accountDeletionPending, isError = true)
                    PrimaryButton(s.accountDeletionResume, onClick = { session.openDeleteAccount() })
                }
                SoftButton(s.signOut, onClick = { session.signOutToGuest() })
            }
            Spacer(Modifier.height(GowaykiSpace.smd))
            SoftButton(s.settings, onClick = { session.go(Screen.Settings) })
            SoftButton(s.help, onClick = { session.go(Screen.Help) })
            SoftButton(s.linesExplorer, onClick = { session.go(Screen.Lines) })
            SoftButton(s.gameTitle, onClick = { session.go(Screen.Game) })
            SoftButton(s.weeklyRanking, onClick = { session.go(Screen.Ranking) })
            if (!isGuest && !session.accountDeletionPending) {
                Spacer(Modifier.height(GowaykiSpace.lg))
                DangerButton(s.deleteAccount, onClick = { session.openDeleteAccount() })
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
    if (session.showDeleteAccountDialog && !isGuest) DeleteAccountDialog(session)
}

/** Confirmación de borrado (Flutter: `delete_account_dialog.dart`): qué se borra, irreversible, progreso y error. */
@Composable
private fun DeleteAccountDialog(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val busy = session.deletingAccount
    Box(Modifier.fillMaxSize().background(colors.scrim))
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = { session.dismissDeleteAccount() },
        properties = PopupProperties(focusable = true, dismissOnBackPress = !busy, dismissOnClickOutside = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(enabled = true, onClick = {}),
        ) {
            GlassPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(GowaykiSpace.md)
                    .fillMaxWidth(),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(s.deleteAccount, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    Text(s.accountDeletionWarning, style = MaterialTheme.typography.body2)
                    Spacer(Modifier.height(GowaykiSpace.sm))
                    Text(s.accountDeletionMarkerNotice, style = MaterialTheme.typography.caption, color = colors.onSurfaceMuted)
                    Spacer(Modifier.height(GowaykiSpace.sm))
                    Text(s.accountDeletionGoogle, style = MaterialTheme.typography.caption, color = colors.onSurfaceMuted)
                    session.accountDeletionError?.let {
                        Spacer(Modifier.height(GowaykiSpace.sm))
                        InlineMessage(it, isError = true)
                    }
                    if (busy) {
                        Spacer(Modifier.height(GowaykiSpace.smd))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(GowaykiSize.icon))
                            Spacer(Modifier.width(GowaykiSpace.sm))
                            Text(s.accountDeleting, style = MaterialTheme.typography.body2)
                        }
                    }
                    Spacer(Modifier.height(GowaykiSpace.md))
                    DangerButton(
                        if (session.accountDeletionPending) s.accountDeletionResume else s.deleteAccount,
                        onClick = { session.deleteAccount() },
                        enabled = !busy,
                    )
                    Spacer(Modifier.height(GowaykiSpace.sm))
                    SoftButton(s.cancel, onClick = { session.dismissDeleteAccount() }, enabled = !busy)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    GowaykiScreen(title = s.settings, onBack = { session.go(Screen.Profile) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle(s.language)
            ChoiceRow(
                options = listOf<Pair<AppLocale?, String>>(
                    null to s.systemDefault,
                    AppLocale.Es to s.spanish,
                    AppLocale.En to s.english,
                ),
                selected = session.localeChoice,
                onSelect = { session.updateLocale(it) },
            )
            SectionTitle(s.alarmMode)
            ChoiceRow(
                options = listOf(
                    AlarmMode.Sound to s.alarmSound,
                    AlarmMode.Vibration to s.alarmVibration,
                    AlarmMode.Off to s.alarmOff,
                ),
                selected = session.alarmMode,
                onSelect = { session.setAlarm(it) },
            )
            val extra = extraStrings(session.locale == AppLocale.Es)
            SectionTitle(extra.themeTitle)
            ChoiceRow(
                options = listOf(
                    ThemePreference.System to extra.themeSystem,
                    ThemePreference.Light to extra.themeLight,
                    ThemePreference.Dark to extra.themeDark,
                ),
                selected = session.themePreference,
                onSelect = { session.updateTheme(it) },
            )
            Spacer(Modifier.height(GowaykiSpace.mdl))
            GlassPanel(Modifier.fillMaxWidth(), onClick = { session.reduceMotion = !session.reduceMotion }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.reducedMotion, style = MaterialTheme.typography.subtitle1, modifier = Modifier.weight(1f))
                    Switch(
                        checked = session.reduceMotion,
                        onCheckedChange = { session.reduceMotion = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.surfaceElevated,
                            uncheckedTrackColor = colors.onSurfaceMuted,
                        ),
                    )
                }
            }
            AboutSection(session)
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

/** Atribución del mapa (ODbL) y enlaces legales; privacidad/términos solo si están configurados. */
@Composable
private fun AboutSection(session: AppSession) {
    val s = session.strings
    val legal = session.legalLinks
    SectionTitle(s.about)
    GlassPanel(Modifier.fillMaxWidth()) {
        Text(s.aboutMapData, style = MaterialTheme.typography.body2)
    }
    Spacer(Modifier.height(GowaykiSpace.sm))
    Column(verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
        SoftButton(s.osmAttribution, onClick = { session.openLink(LegalLinks.OSM_COPYRIGHT) })
        SoftButton(s.openMapTilesAttribution, onClick = { session.openLink(LegalLinks.OPENMAPTILES) })
        legal.privacy?.let { url -> SoftButton(s.privacy, onClick = { session.openLink(url) }) }
        legal.terms?.let { url -> SoftButton(s.terms, onClick = { session.openLink(url) }) }
    }
    if (legal.privacy == null || legal.terms == null) {
        InlineMessage(s.legalUnavailable, isError = false)
    }
    session.linkError?.let { InlineMessage(it, isError = true) }
}

@Composable
fun ActiveTripScreen(session: AppSession) {
    val s = session.strings
    val state = session.activeTrip
    GowaykiScreen(title = s.activeTrip, onBack = { session.go(Screen.Home) }) {
        if (state == null) {
            Text(s.noActiveTrip, style = MaterialTheme.typography.body1)
            return@GowaykiScreen
        }
        val phaseLabel = when (state.progress.phase) {
            TripPhase.Walking -> s.tripPhaseWalking
            TripPhase.WaitingToBoard -> s.tripPhaseWaiting
            TripPhase.Riding -> s.tripPhaseRiding
            TripPhase.Completed -> s.tripPhaseDone
        }
        val segments = remember(state.trip) { itinerarySegments(state.trip) }
        val stops = remember(state.trip) { itineraryStops(state.trip) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CityMap(
                modifier = Modifier.fillMaxWidth().height(GowaykiSize.mapCardTall).clip(RoundedCornerShape(GowaykiRadius.lg)),
                overlay = MapOverlay(
                    segments = segments,
                    stops = stops,
                    user = state.progress.fix?.point,
                    camera = state.progress.fix?.point ?: state.trip.legs.getOrNull(state.progress.legIndex)?.from,
                    zoom = 14.0,
                ),
            )
            Spacer(Modifier.height(GowaykiSpace.smd))
            GlassPanel(Modifier.fillMaxWidth()) {
                Text(phaseLabel, style = MaterialTheme.typography.h6)
                state.progress.remainingMeters?.let {
                    Text(s.format(s.remainingDistance, it.toInt()), style = MaterialTheme.typography.body2)
                }
                state.progress.stopsRemaining?.let {
                    Text(s.format(s.stopsRemainingCount, it), style = MaterialTheme.typography.body2)
                }
                session.tripMessage?.takeIf { state.running }?.let { InlineMessage(it, isError = false) }
            }
            Spacer(Modifier.height(GowaykiSpace.md))
            Column(verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm)) {
                if (state.running) {
                    PrimaryButton(
                        if (state.progress.phase == TripPhase.WaitingToBoard) s.confirmBoard else s.confirmStep,
                        onClick = { session.confirmTripStep() },
                    )
                }
                SoftButton(
                    text = s.stopTrip,
                    onClick = {
                        session.stopActiveTrip()
                        session.go(Screen.Home)
                    },
                )
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

@Composable
fun RankingScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    LaunchedEffect(Unit) { session.loadRanking() }
    val ranking = session.ranking
    val rows = ranking?.top.orEmpty()
    GowaykiScreen(title = s.weeklyRanking, onBack = { session.go(Screen.Game) }) {
        if (session.user?.isGuest != false) {
            InlineMessage(s.rankingSignIn, isError = false)
        }
        ranking?.own?.let { own ->
            val position = ranking.position
            if (position != null) {
                Text(s.rankingOwn(position, own.score), style = MaterialTheme.typography.subtitle1, color = colors.primary)
            }
        }
        if (session.rankingLoading) {
            CircularProgressIndicator(color = colors.primary, modifier = Modifier.padding(GowaykiSpace.sm))
        }
        session.rankingMessage?.let { InlineMessage(it, isError = true) }
        if (!session.rankingLoading && rows.isEmpty() && session.rankingMessage == null) {
            Text(s.rankingEmpty, style = MaterialTheme.typography.body2)
        }
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
        ) {
            itemsIndexed(rows, key = { _, row -> row.uid }) { index, row ->
                GlassPanel(Modifier.fillMaxWidth(), selected = row.uid == ranking?.own?.uid) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", style = MaterialTheme.typography.subtitle1, color = colors.primary)
                        Spacer(Modifier.width(GowaykiSpace.smd))
                        Text(row.name, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${row.score}", style = MaterialTheme.typography.subtitle1, color = colors.primary)
                    }
                }
            }
        }
        if (session.pendingScoreCount > 0) {
            InlineMessage(s.format(s.pendingScoresCount, session.pendingScoreCount), isError = false)
            PrimaryButton(s.retryScores, onClick = { session.flushPendingScores() })
            Spacer(Modifier.height(GowaykiSpace.sm))
        }
    }
}

@Composable
fun LinesScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    LaunchedEffect(Unit) { session.refreshPackMapData() }
    val lines = session.transitLines
    val geometry = session.selectedLineGeometry
    GowaykiScreen(title = s.linesExplorer, onBack = { session.go(Screen.Home) }) {
        CityMap(
            modifier = Modifier.fillMaxWidth().height(GowaykiSize.mapCompact).clip(RoundedCornerShape(GowaykiRadius.lg)),
            overlay = MapOverlay(
                route = geometry,
                stops = if (geometry.isEmpty()) session.mapStopPoints.take(40) else emptyList(),
                camera = geometry.getOrNull(geometry.size / 2) ?: ArequipaCenter,
                zoom = 12.5,
            ),
        )
        Spacer(Modifier.height(GowaykiSpace.sm))
        if (lines.isEmpty()) {
            if (session.packReady) {
                Text(s.linesEmpty, style = MaterialTheme.typography.body2)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(GowaykiSize.icon))
                    Spacer(Modifier.width(GowaykiSpace.sm))
                    Text(s.preparingCity, style = MaterialTheme.typography.body2)
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
        ) {
            items(lines, key = { it.id }) { line ->
                LineRow(line, selected = session.selectedLine?.id == line.id) { session.selectLine(line) }
            }
            item { Spacer(Modifier.height(GowaykiSpace.md)) }
        }
    }
}

@Composable
private fun LineRow(line: TransitLine, selected: Boolean, onClick: () -> Unit) {
    val colors = GowaykiTheme.colors
    val direction = line.ida ?: line.directions.firstOrNull()
    GlassPanel(modifier = Modifier.fillMaxWidth(), onClick = onClick, selected = selected) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(GowaykiSize.iconLg)
                    .clip(CircleShape)
                    .background(parseLineColor(line.color, colors.primary)),
            )
            Spacer(Modifier.width(GowaykiSpace.smd))
            Column(Modifier.weight(1f)) {
                Text(
                    line.shortName,
                    style = MaterialTheme.typography.subtitle1,
                    color = if (selected) colors.primary else colors.onSurface,
                )
                val subtitle = direction?.longName?.ifBlank { direction.headsign } ?: direction?.headsign.orEmpty()
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.body2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun HelpScreen(session: AppSession) {
    val s = session.strings
    GowaykiScreen(title = s.help, onBack = { session.go(Screen.Profile) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Text(s.helpBody, style = MaterialTheme.typography.body1)
            }
            SectionTitle(s.locationTitle)
            Text(s.permissionLocationRationale, style = MaterialTheme.typography.body2)
            SectionTitle(s.communityTitle)
            Text(s.communityDisclaimer, style = MaterialTheme.typography.body2)
            Text(s.reportLifetime, style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(GowaykiSpace.lg))
            PrimaryButton(s.seeTutorial, onClick = { session.replayTutorial() })
            Spacer(Modifier.height(GowaykiSpace.lg))
        }
    }
}

@Composable
fun AuthScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    AtmosphereBackground {
        EnterContent(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(GowaykiSpace.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(GowaykiSpace.xl))
                BrandHero(subtitle = s.signIn, logoSize = GowaykiSize.logoHero)
                Spacer(Modifier.height(GowaykiSpace.md))
                Text(s.authHelp, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
                Spacer(Modifier.height(GowaykiSpace.xl))
                PrimaryButton(
                    text = if (session.authBusy) s.loading else s.signInGoogle,
                    onClick = { session.beginGoogleSignIn() },
                    enabled = !session.authBusy,
                )
                if (session.isDebugBuild) {
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    SoftButton(
                        text = s.signInGoogleDemo,
                        onClick = { session.signInWithGoogleDemo(isNew = true) },
                    )
                }
                session.authError?.let {
                    Spacer(Modifier.height(GowaykiSpace.smd))
                    Text(
                        it,
                        color = colors.error,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = GowaykiSpace.smd),
                    )
                }
                Spacer(Modifier.height(GowaykiSpace.sm))
                TextButton(onClick = { session.go(Screen.Home) }) {
                    Text(s.guestEntry, style = MaterialTheme.typography.button, color = colors.primary)
                }
                Spacer(Modifier.height(GowaykiSpace.xl))
            }
        }
    }
}
