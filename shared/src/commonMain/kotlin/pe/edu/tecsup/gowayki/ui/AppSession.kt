package pe.edu.tecsup.gowayki.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser
import pe.edu.tecsup.gowayki.domain.community.CommunityReport
import pe.edu.tecsup.gowayki.domain.community.InMemoryReportsRepository
import pe.edu.tecsup.gowayki.domain.community.ReportDraft
import pe.edu.tecsup.gowayki.domain.community.ReportKind
import pe.edu.tecsup.gowayki.domain.community.ReportsRepository
import pe.edu.tecsup.gowayki.domain.community.StoredReport
import pe.edu.tecsup.gowayki.domain.game.PendingScore
import pe.edu.tecsup.gowayki.domain.game.WeeklyRanking
import pe.edu.tecsup.gowayki.domain.game.rankingWeek
import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
import pe.edu.tecsup.gowayki.domain.models.TransitLine
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
import pe.edu.tecsup.gowayki.domain.planner.JourneyPlanner
import pe.edu.tecsup.gowayki.domain.planner.JourneyRequest
import pe.edu.tecsup.gowayki.domain.planner.JourneyResponse
import pe.edu.tecsup.gowayki.domain.planner.JourneyStatus
import pe.edu.tecsup.gowayki.domain.planner.JourneyTime
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import pe.edu.tecsup.gowayki.domain.planner.addMinutes
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.domain.preferences.InMemoryPreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace
import pe.edu.tecsup.gowayki.domain.preferences.TutorialMigrationPolicy
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress
import pe.edu.tecsup.gowayki.domain.preferences.TutorialStatus
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix
import pe.edu.tecsup.gowayki.domain.tracking.TripAlertKind
import pe.edu.tecsup.gowayki.domain.tracking.TripPhase
import pe.edu.tecsup.gowayki.domain.tracking.TripProgress
import pe.edu.tecsup.gowayki.domain.tracking.TripTracker
import pe.edu.tecsup.gowayki.i18n.StringCatalog
import pe.edu.tecsup.gowayki.i18n.Strings
import pe.edu.tecsup.gowayki.platform.AlarmMode
import pe.edu.tecsup.gowayki.platform.PlatformLocation
import pe.edu.tecsup.gowayki.platform.platformClearTripNotifications
import pe.edu.tecsup.gowayki.platform.platformFetchCurrentLocation
import pe.edu.tecsup.gowayki.platform.platformIoDispatcher
import pe.edu.tecsup.gowayki.platform.platformIsDebugBuild
import pe.edu.tecsup.gowayki.platform.platformIsLocationEnabled
import pe.edu.tecsup.gowayki.platform.platformLastLocation
import pe.edu.tecsup.gowayki.platform.platformRanking
import pe.edu.tecsup.gowayki.platform.platformRemoteReports
import pe.edu.tecsup.gowayki.platform.platformRequestLocationPermission
import pe.edu.tecsup.gowayki.platform.platformStartTripTracking
import pe.edu.tecsup.gowayki.platform.platformStopTripTracking
import pe.edu.tecsup.gowayki.platform.platformTripAlert
import pe.edu.tecsup.gowayki.platform.platformUserStateSync
import pe.edu.tecsup.gowayki.platform.platformWatchUserLocation

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object Home : Screen()
    data object Search : Screen()
    data object Results : Screen()
    data class Detail(val index: Int) : Screen()
    data object Reports : Screen()
    data object Profile : Screen()
    data object Settings : Screen()
    data object Game : Screen()
    data object Ranking : Screen()
    data object Help : Screen()
    data object Auth : Screen()
    data object ActiveTrip : Screen()
    data object Lines : Screen()
}

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val isNewRegistration: Boolean = false,
    val isGuest: Boolean = false,
)

enum class ActiveTripStatus { Starting, Active, Completed, Stopped, Error }

data class ActiveTripState(
    val trip: Itinerary,
    val progress: TripProgress,
    val status: ActiveTripStatus,
    val stale: Boolean = true,
) {
    val running: Boolean
        get() = status == ActiveTripStatus.Starting || status == ActiveTripStatus.Active
}

/** Todo su estado público vive en `mutableStateOf`, así que Compose puede saltar recomposiciones. */
@Stable
class AppSession(
    private val preferences: PreferencesRepository = InMemoryPreferencesRepository(),
    private val reports: ReportsRepository = InMemoryReportsRepository(),
    private val placeSearch: PlaceSearchRepository? = null,
    private val networkProvider: (() -> PlannerNetwork?)? = null,
    private val cityPackProvider: (() -> CityPackRepository?)? = null,
    private val deviceLanguage: String? = "es",
    startAsGuest: Boolean = true,
    private val initialSignedIn: SignedInUser? = null,
) {
    var locale by mutableStateOf(preferences.locale(deviceLanguage))
        private set
    /** Idioma elegido en la app; null = seguir al dispositivo. */
    var localeChoice by mutableStateOf(AppLocale.parse(preferences.readLocaleCode()))
        private set
    var reduceMotion by mutableStateOf(false)
    var alarmMode by mutableStateOf(
        when (preferences.readAlarmMode()) {
            "vibration" -> AlarmMode.Vibration
            "off" -> AlarmMode.Off
            else -> AlarmMode.Sound
        },
    )
    var screen by mutableStateOf<Screen>(Screen.Splash)
        private set
    var onboardingStep by mutableStateOf(0)
    var tutorial by mutableStateOf(
        TutorialMigrationPolicy.forExistingAccount(preferences.readTutorial()),
    )
    var showTutorialOverlay by mutableStateOf(false)
    var user by mutableStateOf(
        if (startAsGuest) AuthUser(uid = "guest", displayName = null, isGuest = true) else null,
    )
    var originQuery by mutableStateOf("")
    var destinationQuery by mutableStateOf("")
    var origin by mutableStateOf<GeoPoint?>(null)
    var destination by mutableStateOf<GeoPoint?>(null)
    var originLabel by mutableStateOf("")
    var destinationLabel by mutableStateOf("")
    var searchHits by mutableStateOf<List<PlaceResult>>(emptyList())
    var searchingDestination by mutableStateOf(true)
    var planning by mutableStateOf(false)
    var planError by mutableStateOf<String?>(null)
    var journey by mutableStateOf<JourneyResponse?>(null)
    var selectedItinerary by mutableStateOf<Itinerary?>(null)
    var selectedItineraryIndex by mutableStateOf(0)
    var lastKnownLocation by mutableStateOf<GeoPoint?>(null)
    var reportKind by mutableStateOf(ReportKind.HeavyTraffic)
    var reportDescription by mutableStateOf("")
    var reportLat by mutableStateOf(-16.4090)
    var reportLng by mutableStateOf(-71.5375)
    var reportMessage by mutableStateOf<String?>(null)
    var myReports by mutableStateOf<List<StoredReport>>(emptyList())
    var nearbyReports by mutableStateOf<List<CommunityReport>>(emptyList())
    var packReady by mutableStateOf(false)
    var activeTrip by mutableStateOf<ActiveTripState?>(null)
        private set
    var tripMessage by mutableStateOf<String?>(null)
    var ranking by mutableStateOf<WeeklyRanking?>(null)
    var rankingMessage by mutableStateOf<String?>(null)
    var rankingLoading by mutableStateOf(false)
    var lastGameScore by mutableStateOf(0)
    var lastGameDurationMs by mutableStateOf(0)
    var pendingScoreCount by mutableStateOf(0)
    var favorites by mutableStateOf(preferences.readFavorites())
    var recents by mutableStateOf(preferences.readRecents())
    var transitLines by mutableStateOf<List<TransitLine>>(emptyList())
    var selectedLine by mutableStateOf<TransitLine?>(null)
    var selectedLineGeometry by mutableStateOf<List<GeoPoint>>(emptyList())
    var mapStopPoints by mutableStateOf<List<GeoPoint>>(emptyList())
    var locationMessage by mutableStateOf<String?>(null)
    var locating by mutableStateOf(false)
    /** Sube al tocar “mi ubicación” para recentrar el mapa. */
    var locationFollowEpoch by mutableStateOf(0L)
    var authBusy by mutableStateOf(false)
    var reportSending by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)

    private var tracker: TripTracker? = null
    private var stopLocationWatch: (() -> Unit)? = null
    private var stopUserLocationWatchHandle: (() -> Unit)? = null
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var packJob: Job? = null
    private var reportsJob: Job? = null
    private var rankingJob: Job? = null
    private var flushJob: Job? = null
    private var searchJob: Job? = null

    /** Red/SQLite bloqueantes fuera del hilo de UI; el resultado vuelve al hilo principal. */
    private suspend fun <T> io(block: () -> T): T = withContext(platformIoDispatcher) { block() }

    val strings: Strings get() = StringCatalog.forLocale(locale)
    val isDebugBuild: Boolean by lazy { platformIsDebugBuild() }

    init {
        StringCatalog.current = strings
    }
    val baseFare: Money get() = Money.BASE

    /**
     * Arranque: decide la pantalla con datos locales y navega de inmediato.
     * Paquete de ciudad, reportes, ranking y sincronización llegan después en segundo plano.
     */
    fun bootstrap() {
        packReady = networkProvider?.invoke() != null
        pendingScoreCount = preferences.readPendingScores().size
        val restored = initialSignedIn?.takeIf { !it.isAnonymous }
        if (restored != null) {
            applyGoogleUser(restored.uid, restored.displayName, isNew = false)
        } else {
            val onboarding = preferences.readOnboarding()
            screen = if (onboarding == null) Screen.Onboarding else Screen.Home
            maybeStartTutorial()
            refreshNearbyReports()
        }
        refreshPackMapData()
        warmLocation(setAsOrigin = false)
    }

    fun setAlarm(mode: AlarmMode) {
        alarmMode = mode
        preferences.saveAlarmMode(
            when (mode) {
                AlarmMode.Sound -> "sound"
                AlarmMode.Vibration -> "vibration"
                AlarmMode.Off -> "off"
            },
        )
    }

    /**
     * Lee líneas y paraderos del paquete una sola vez por sesión, en segundo plano.
     * Si el paquete aún se está copiando/abriendo, espera a que esté listo.
     */
    fun refreshPackMapData() {
        val provider = cityPackProvider ?: return
        if (transitLines.isNotEmpty() || packJob?.isActive == true) return
        packJob = sessionScope.launch {
            var pack = provider()
            var waitedMs = 0
            while (pack == null && waitedMs < 60_000) {
                delay(150)
                waitedMs += 150
                pack = provider()
            }
            val ready = pack ?: return@launch
            val loaded = io {
                runCatching {
                    ready.lines().sortedBy { it.shortName } to ready.mapStops().take(80).map {
                        GeoPoint(it.stop.latitude, it.stop.longitude)
                    }
                }.getOrNull()
            } ?: return@launch
            transitLines = loaded.first
            mapStopPoints = loaded.second
            packReady = true
        }
    }

    /** [value] null vuelve al idioma del dispositivo. La elección se guarda. */
    fun updateLocale(value: AppLocale?) {
        localeChoice = value
        locale = value ?: AppLocale.resolve(null, deviceLanguage)
        preferences.saveLocaleCode(value?.code)
        StringCatalog.current = strings
    }

    fun finishOnboarding(skipped: Boolean) {
        val status = if (skipped) OnboardingStatus.Skipped else OnboardingStatus.Completed
        preferences.saveOnboarding(status)
        val uid = user?.takeIf { !it.isGuest }?.uid
        if (uid != null) {
            sessionScope.launch { io { runCatching { platformUserStateSync().pushOnboarding(uid, status) } } }
        }
        screen = Screen.Home
        maybeStartTutorial()
    }

    fun go(to: Screen) {
        screen = to
        if (to == Screen.Home) {
            refreshNearbyReports()
            refreshPackMapData()
        }
        if (to == Screen.Search) {
            warmLocation(setAsOrigin = origin == null, onlyIfOriginEmpty = true)
        }
        if (to == Screen.Ranking) loadRanking()
        if (to == Screen.Lines) refreshPackMapData()
    }

    /** Pide permiso + fix fresco (Fused). Opcionalmente rellena origen. */
    fun warmLocation(setAsOrigin: Boolean = false, onlyIfOriginEmpty: Boolean = false) {
        locationMessage = null
        platformRequestLocationPermission { granted ->
            if (!granted) {
                locationMessage = strings.permissionLocationRationale
                return@platformRequestLocationPermission
            }
            if (!platformIsLocationEnabled()) {
                locationMessage = strings.locationDisabled
                // Igual intenta lastKnown por si el sistema aún entrega algo
            }
            locating = true
            locationMessage = strings.locating
            platformFetchCurrentLocation { fix ->
                locating = false
                if (fix == null) {
                    val cached = platformLastLocation()
                    if (cached != null) {
                        applyLocationFix(cached, setAsOrigin && (!onlyIfOriginEmpty || origin == null))
                    } else {
                        locationMessage = strings.locationUnavailable
                    }
                } else {
                    applyLocationFix(fix, setAsOrigin && (!onlyIfOriginEmpty || origin == null))
                }
            }
        }
    }

    fun useCurrentLocationAsOrigin() {
        warmLocation(setAsOrigin = true)
        locationFollowEpoch += 1
    }

    /** Recentrar mapa en el usuario (FAB). */
    fun recenterOnUser() {
        warmLocation(setAsOrigin = origin == null)
        locationFollowEpoch += 1
    }

    fun startUserLocationWatch(setOriginIfEmpty: Boolean = false) {
        stopUserLocationWatch()
        val fillOrigin = setOriginIfEmpty && origin == null
        platformRequestLocationPermission { granted ->
            if (!granted) {
                locationMessage = strings.permissionLocationRationale
                return@platformRequestLocationPermission
            }
            if (!platformIsLocationEnabled()) {
                locationMessage = strings.locationDisabled
            }
            locating = true
            locationMessage = strings.locating
            platformFetchCurrentLocation { fix ->
                locating = false
                val resolved = fix ?: platformLastLocation()
                if (resolved == null) {
                    locationMessage = strings.locationUnavailable
                } else {
                    applyLocationFix(resolved, fillOrigin)
                }
            }
            stopUserLocationWatchHandle = platformWatchUserLocation { loc ->
                lastKnownLocation = GeoPoint(loc.latitude, loc.longitude)
            }
        }
    }

    fun stopUserLocationWatch() {
        stopUserLocationWatchHandle?.invoke()
        stopUserLocationWatchHandle = null
    }

    private fun applyLocationFix(fix: PlatformLocation, setAsOrigin: Boolean) {
        val point = GeoPoint(fix.latitude, fix.longitude)
        lastKnownLocation = point
        if (setAsOrigin) {
            origin = point
            originLabel = strings.myLocation
            originQuery = strings.myLocation
        }
        locationMessage = null
    }

    fun applyRecent(trip: RecentTrip) {
        origin = GeoPoint(trip.from.latitude, trip.from.longitude)
        originLabel = trip.from.name
        originQuery = trip.from.name
        destination = GeoPoint(trip.to.latitude, trip.to.longitude)
        destinationLabel = trip.to.name
        destinationQuery = trip.to.name
    }

    fun toggleFavoriteDestination() {
        val dest = destination ?: return
        val name = destinationLabel.ifBlank { strings.searchDestination }
        val place = SavedPlace(name, dest.latitude, dest.longitude)
        val current = favorites.toMutableList()
        val idx = current.indexOfFirst {
            kotlin.math.abs(it.latitude - place.latitude) < 1e-5 &&
                kotlin.math.abs(it.longitude - place.longitude) < 1e-5
        }
        if (idx >= 0) current.removeAt(idx) else current.add(0, place)
        favorites = current.take(20)
        preferences.saveFavorites(favorites)
    }

    fun applyFavorite(place: SavedPlace) {
        destination = GeoPoint(place.latitude, place.longitude)
        destinationLabel = place.name
        destinationQuery = place.name
    }

    fun selectLine(line: TransitLine) {
        selectedLine = line
        val dir = line.ida ?: line.directions.firstOrNull()
        selectedLineGeometry = dir?.geometry?.mapNotNull { pair ->
            if (pair.size < 2) null else GeoPoint(pair[0], pair[1])
        }.orEmpty()
    }

    fun confirmNearbyReport(reportId: String) {
        val current = user
        if (current == null || current.isGuest) {
            reportMessage = strings.reportNeedsAccount
            screen = Screen.Auth
            return
        }
        sessionScope.launch {
            val result = io { runCatching { platformRemoteReports()?.confirm(reportId, current.uid) ?: false } }
            result.onSuccess { ok ->
                reportMessage = if (ok) REPORT_CONFIRMED else strings.alreadyConfirmed
                refreshNearbyReports(force = true)
            }.onFailure {
                reportMessage = strings.errorGeneric
            }
        }
    }

    /** Búsqueda con pausa corta entre teclas y fuera del hilo de UI; un error no tumba la app. */
    fun searchPlaces(query: String) {
        val q = query.trim()
        searchJob?.cancel()
        val search = placeSearch
        if (q.length < 2 || search == null) {
            searchHits = emptyList()
            return
        }
        searchJob = sessionScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val hits = io { runCatching { search.search(q) }.getOrDefault(emptyList()) }
            searchHits = hits
        }
    }

    fun selectPlace(hit: PlaceResult) {
        val point = GeoPoint(hit.latitude, hit.longitude)
        if (searchingDestination) {
            destination = point
            destinationLabel = hit.name
            destinationQuery = hit.name
        } else {
            origin = point
            originLabel = hit.name
            originQuery = hit.name
        }
        searchJob?.cancel()
        searchHits = emptyList()
    }

    fun useDemoEndpoints() {
        origin = GeoPoint(-16.3988, -71.5369)
        originLabel = "Plaza de Armas"
        originQuery = originLabel
        destination = GeoPoint(-16.4304, -71.5188)
        destinationLabel = "TECSUP Arequipa"
        destinationQuery = destinationLabel
    }

    fun planTrip() {
        val from = origin
        val to = destination
        if (from == null || to == null) {
            planError = strings.errorGeneric
            return
        }
        if (planning) return
        planning = true
        planError = null
        sessionScope.launch {
            val planned = withContext(Dispatchers.Default) {
                runCatching {
                    val network = networkProvider?.invoke()
                    if (network == null) {
                        demoJourney(from, to)
                    } else {
                        JourneyPlanner(network).plan(
                            JourneyRequest(
                                origin = from,
                                destination = to,
                                time = JourneyTime.now(),
                            ),
                        )
                    }
                }
            }
            planning = false
            planned.onSuccess { response ->
                journey = response
                if (response.status == JourneyStatus.Success && response.itineraries.isNotEmpty()) {
                    selectedItineraryIndex = 0
                    selectedItinerary = response.itineraries.first()
                    rememberRecentTrip()
                    planError = null
                } else {
                    selectedItinerary = null
                    planError = strings.emptyRoutes
                }
                screen = Screen.Results
            }.onFailure {
                planError = strings.errorGeneric
            }
        }
    }

    fun openDetail(index: Int) {
        selectItinerary(index)
        screen = Screen.Detail(index)
    }

    fun selectItinerary(index: Int) {
        val list = journey?.itineraries.orEmpty()
        if (index in list.indices) {
            selectedItineraryIndex = index
            selectedItinerary = list[index]
        }
    }

    fun useCurrentLocationForReport(onNeedPermission: (() -> Unit)? = null) {
        platformRequestLocationPermission { granted ->
            if (!granted) {
                reportMessage = strings.permissionLocationRationale
                onNeedPermission?.invoke()
                return@platformRequestLocationPermission
            }
            if (!platformIsLocationEnabled()) {
                reportMessage = strings.locationDisabled
            }
            platformFetchCurrentLocation { fix ->
                val resolved = fix ?: platformLastLocation()
                if (resolved == null) {
                    reportMessage = strings.locationUnavailable
                } else {
                    reportLat = resolved.latitude
                    reportLng = resolved.longitude
                    reportMessage = null
                }
            }
        }
    }

    fun submitReport() {
        val current = user
        if (current == null || current.isGuest) {
            reportMessage = strings.reportNeedsAccount
            screen = Screen.Auth
            return
        }
        if (reportSending) return
        val remote = platformRemoteReports()
        val draft = try {
            val id = remote?.newId() ?: reports.newId()
            ReportDraft(
                id = id,
                kind = reportKind,
                description = reportDescription.ifBlank { "${reportKind.apiValue} report" },
                latitude = reportLat,
                longitude = reportLng,
            ).also {
                reports.saveDraft(it, current.uid)
                reports.markPending(id)
            }
        } catch (t: Throwable) {
            reportMessage = strings.reportValidation
            return
        }
        reportSending = true
        reportMessage = null
        sessionScope.launch {
            val sent = io { runCatching { remote?.createOrReuse(draft, current.uid) } }
            reportSending = false
            sent.onSuccess {
                reports.markPublished(draft.id, UtcDateTime.now())
                myReports = reports.mine(current.uid)
                reportMessage = REPORT_OK
                reportDescription = ""
                refreshNearbyReports(force = true)
            }.onFailure {
                reportMessage = strings.reportSendError
            }
        }
    }

    fun withdrawReport(id: String) {
        val uid = user?.uid
        val remoteUid = uid?.takeIf { user?.isGuest == false }
        sessionScope.launch {
            if (remoteUid != null) io { runCatching { platformRemoteReports()?.deleteMine(id, remoteUid) } }
            reports.delete(id)
            myReports = reports.mine(uid)
            refreshNearbyReports(force = true)
        }
    }

    fun refreshNearbyReports(force: Boolean = false) {
        val client = platformRemoteReports() ?: return
        if (reportsJob?.isActive == true) {
            if (!force) return
            reportsJob?.cancel()
        }
        val loc = platformLastLocation()
        val centerLat = loc?.latitude ?: reportLat
        val centerLng = loc?.longitude ?: reportLng
        reportsJob = sessionScope.launch {
            val since = UtcDateTime.now().let {
                UtcDateTime(it.epochMicros - 72L * 3_600_000_000L)
            }
            val feed = io {
                runCatching {
                    client.activeAround(
                        since = since,
                        centerLat = centerLat,
                        centerLng = centerLng,
                        radiusKm = 8.0,
                    )
                }.getOrNull()
            }
            if (feed != null) nearbyReports = feed.items
        }
    }

    fun applyGoogleUser(uid: String, displayName: String?, isNew: Boolean) {
        authBusy = false
        authError = null
        user = AuthUser(
            uid = uid,
            displayName = displayName,
            isNewRegistration = isNew,
            isGuest = false,
        )
        tutorial = if (isNew) {
            TutorialMigrationPolicy.forNewGoogleRegistration()
        } else {
            TutorialMigrationPolicy.resolve(false, preferences.readTutorial())
        }
        preferences.saveTutorial(tutorial)
        val hadLocalOnboarding = preferences.readOnboarding() != null
        if (!hadLocalOnboarding) preferences.saveOnboarding(OnboardingStatus.Completed)
        screen = Screen.Home
        maybeStartTutorial()
        refreshNearbyReports()
        syncSignedInUser(uid, isNew, hadLocalOnboarding)
    }

    /**
     * Sincronización con Firestore tras iniciar sesión, sin bloquear la navegación:
     * alta del documento y lectura de tutorial/onboarding en paralelo; luego
     * se publica el tutorial, se vacía la cola de puntajes y se carga el ranking.
     */
    private fun syncSignedInUser(uid: String, isNew: Boolean, hadLocalOnboarding: Boolean) {
        val sync = platformUserStateSync()
        sessionScope.launch {
            val (remoteTutorial, remoteOnboarding) = withContext(platformIoDispatcher) {
                val ensured = async { runCatching { sync.ensureUser(uid) } }
                val pulledTutorial = async { runCatching { sync.pullTutorial(uid) }.getOrNull() }
                val pulledOnboarding = async { runCatching { sync.pullOnboarding(uid) }.getOrNull() }
                ensured.await()
                pulledTutorial.await() to pulledOnboarding.await()
            }
            if (user?.uid != uid) return@launch
            if (!isNew && remoteTutorial != null && remoteTutorial != tutorial) {
                tutorial = TutorialMigrationPolicy.resolve(false, remoteTutorial)
                preferences.saveTutorial(tutorial)
                if (tutorial.status == TutorialStatus.Pending || tutorial.status == TutorialStatus.Started) {
                    maybeStartTutorial()
                } else {
                    showTutorialOverlay = false
                }
            }
            if (remoteOnboarding != null && !hadLocalOnboarding) {
                preferences.saveOnboarding(remoteOnboarding)
            }
            val progress = tutorial
            io { runCatching { sync.pushTutorial(uid, progress) } }
            flushPendingScores()
            loadRanking()
        }
    }

    fun beginGoogleSignIn() {
        authBusy = true
        authError = null
        pe.edu.tecsup.gowayki.platform.launchPlatformGoogleSignIn(
            onSuccess = { signed ->
                applyGoogleUser(signed.uid, signed.displayName, signed.isNewUser)
            },
            onError = { msg ->
                authBusy = false
                authError = when {
                    msg.isBlank() -> null
                    isDebugBuild -> "${strings.authGoogleError} ($msg)"
                    else -> strings.authGoogleError
                }
            },
        )
    }

    fun signInWithGoogleDemo(isNew: Boolean) {
        if (!isDebugBuild) return
        applyGoogleUser(
            uid = if (isNew) "new-google-user" else "existing-google-user",
            displayName = "Sharon",
            isNew = isNew,
        )
    }

    fun signOutToGuest() {
        pe.edu.tecsup.gowayki.platform.signOutPlatformGoogle()
        user = AuthUser(uid = "guest", displayName = null, isGuest = true)
        showTutorialOverlay = false
        authError = null
        authBusy = false
    }

    fun maybeStartTutorial() {
        if (tutorial.status == TutorialStatus.Pending || tutorial.status == TutorialStatus.Started) {
            showTutorialOverlay = true
            if (tutorial.status == TutorialStatus.Pending) {
                tutorial = tutorial.copy(status = TutorialStatus.Started, step = 0)
                preferences.saveTutorial(tutorial)
            }
        }
    }

    fun tutorialNext() {
        if (tutorial.step >= 3) {
            finishTutorial(skipped = false)
        } else {
            tutorial = tutorial.copy(step = tutorial.step + 1, status = TutorialStatus.Started)
            persistTutorial()
        }
    }

    fun tutorialBack() {
        tutorial = tutorial.copy(step = (tutorial.step - 1).coerceAtLeast(0))
        persistTutorial()
    }

    fun finishTutorial(skipped: Boolean) {
        tutorial = tutorial.copy(
            status = if (skipped) TutorialStatus.Skipped else TutorialStatus.Completed,
            step = if (skipped) tutorial.step else 3,
        )
        persistTutorial()
        showTutorialOverlay = false
    }

    fun replayTutorial() {
        tutorial = TutorialProgress(status = TutorialStatus.Started, step = 0)
        persistTutorial()
        showTutorialOverlay = true
        screen = Screen.Home
    }

    private fun persistTutorial() {
        preferences.saveTutorial(tutorial)
        val uid = user?.takeIf { !it.isGuest }?.uid ?: return
        val progress = tutorial
        sessionScope.launch { io { runCatching { platformUserStateSync().pushTutorial(uid, progress) } } }
    }

    fun formatFare(money: Money): String {
        val major = money.amountMinor / 100
        val minor = (money.amountMinor % 100).toString().padStart(2, '0')
        return "S/ $major.$minor"
    }

    fun startActiveTrip() {
        val trip = selectedItinerary ?: return
        if (activeTrip?.running == true) {
            tripMessage = strings.activeTrip
            screen = Screen.ActiveTrip
            return
        }
        tripMessage = null
        tracker = TripTracker(trip)
        activeTrip = ActiveTripState(
            trip = trip,
            progress = tracker!!.initial,
            status = ActiveTripStatus.Starting,
        )
        platformRequestLocationPermission { granted ->
            if (!granted) {
                activeTrip = activeTrip?.copy(status = ActiveTripStatus.Active, stale = true)
                screen = Screen.ActiveTrip
                return@platformRequestLocationPermission
            }
            stopLocationWatch?.invoke()
            stopLocationWatch = platformStartTripTracking { fix -> acceptFix(fix) }
            activeTrip = activeTrip?.copy(status = ActiveTripStatus.Active, stale = false)
            screen = Screen.ActiveTrip
        }
    }

    fun confirmTripStep() {
        val t = tracker ?: return
        val progress = t.confirmStep()
        acceptProgress(progress)
    }

    fun stopActiveTrip(status: ActiveTripStatus = ActiveTripStatus.Stopped) {
        stopLocationWatch?.invoke()
        stopLocationWatch = null
        platformStopTripTracking()
        platformClearTripNotifications()
        activeTrip = activeTrip?.copy(status = status)
        tracker = null
    }

    private fun acceptFix(fix: PositionFix) {
        val t = tracker ?: return
        val progress = t.update(fix, UtcDateTime.now())
        acceptProgress(progress)
    }

    private fun acceptProgress(progress: TripProgress) {
        val current = activeTrip ?: return
        activeTrip = current.copy(
            progress = progress,
            stale = progress.fix == null,
            status = if (progress.phase == TripPhase.Completed) {
                ActiveTripStatus.Completed
            } else {
                current.status
            },
        )
        for (alert in progress.alerts) {
            val title = if (alert.kind == TripAlertKind.Alight) {
                strings.alightAlertTitle
            } else {
                strings.transferAlertTitle
            }
            val body = if (alert.kind == TripAlertKind.Alight) {
                strings.alightAlertBody.replace("%s", alert.stopName)
            } else {
                strings.transferAlertBody.replace("%s", alert.stopName)
            }
            platformTripAlert(title, body, alarmMode)
        }
        if (progress.phase == TripPhase.Completed) {
            stopActiveTrip(ActiveTripStatus.Completed)
        }
    }

    fun loadRanking() {
        if (rankingJob?.isActive == true) return
        val client = platformRanking()
        val uid = user?.takeIf { !it.isGuest }?.uid
        rankingLoading = true
        rankingMessage = null
        rankingJob = sessionScope.launch {
            val week = rankingWeek()
            val loaded = io { runCatching { client?.load(week, uid) ?: WeeklyRanking(top = emptyList()) } }
            loaded.onSuccess { ranking = it }
                .onFailure { rankingMessage = strings.rankingUnavailable }
            rankingLoading = false
        }
    }

    fun submitGameScore(score: Int, durationMs: Int) {
        lastGameScore = score
        lastGameDurationMs = durationMs
        val pending = PendingScore(
            week = rankingWeek(),
            score = score.coerceIn(0, 5000),
            durationMs = durationMs.coerceIn(1000, 120_000),
        )
        enqueuePendingScore(pending)
        val current = user
        if (current == null || current.isGuest) {
            rankingMessage = strings.scoreSavedOffline
            return
        }
        rankingMessage = null
        sessionScope.launch {
            val published = publishPending(pending, current)
            if (published) {
                rankingMessage = null
                loadRanking()
                screen = Screen.Ranking
            } else {
                rankingMessage = strings.scoreSavedOffline
            }
        }
    }

    fun flushPendingScores() {
        val current = user?.takeIf { !it.isGuest } ?: return
        if (flushJob?.isActive == true) return
        val lines = preferences.readPendingScores().toMutableList()
        if (lines.isEmpty()) return
        flushJob = sessionScope.launch {
            val remaining = mutableListOf<String>()
            for (line in lines) {
                val parts = line.split('|')
                if (parts.size != 3) continue
                val pending = runCatching {
                    PendingScore(parts[0], parts[1].toInt(), parts[2].toInt())
                }.getOrNull() ?: continue
                if (!publishPending(pending, current)) remaining.add(line)
            }
            preferences.savePendingScores(remaining)
            pendingScoreCount = remaining.size
            if (remaining.isEmpty()) loadRanking()
        }
    }

    private fun enqueuePendingScore(pending: PendingScore) {
        val line = "${pending.week}|${pending.score}|${pending.durationMs}"
        val lines = preferences.readPendingScores().toMutableList()
        lines.removeAll { it.startsWith("${pending.week}|") && it.split('|').getOrNull(1)?.toIntOrNull()?.let { s -> s <= pending.score } == true }
        val existing = lines.firstOrNull { it.startsWith("${pending.week}|") }
        if (existing != null) {
            val oldScore = existing.split('|').getOrNull(1)?.toIntOrNull() ?: -1
            if (oldScore >= pending.score) {
                pendingScoreCount = lines.size
                return
            }
            lines.remove(existing)
        }
        lines.add(line)
        preferences.savePendingScores(lines)
        pendingScoreCount = lines.size
    }

    private suspend fun publishPending(pending: PendingScore, current: AuthUser): Boolean {
        val client = platformRanking() ?: return false
        val name = current.displayName?.ifBlank { null } ?: "Wayki"
        val ok = io { runCatching { client.publish(current.uid, name, pending) }.isSuccess }
        if (ok) {
            val left = preferences.readPendingScores().filterNot { it.startsWith("${pending.week}|") }
            preferences.savePendingScores(left)
            pendingScoreCount = left.size
        }
        return ok
    }

    private fun rememberRecentTrip() {
        val from = origin ?: return
        val to = destination ?: return
        val trip = RecentTrip(
            from = SavedPlace(originLabel.ifBlank { strings.searchOrigin }, from.latitude, from.longitude),
            to = SavedPlace(destinationLabel.ifBlank { strings.searchDestination }, to.latitude, to.longitude),
        )
        val next = (listOf(trip) + recents.filterNot {
            it.from.name == trip.from.name && it.to.name == trip.to.name
        }).take(10)
        recents = next
        preferences.saveRecents(next)
    }

    fun routeGeometry(trip: Itinerary?): List<GeoPoint> {
        if (trip == null) return emptyList()
        return trip.legs.flatMap { leg ->
            when {
                leg.geometry.size >= 2 -> leg.geometry
                leg is TransitLeg && leg.intermediateStops.isNotEmpty() -> {
                    listOf(leg.from) +
                        leg.intermediateStops.map { GeoPoint(it.latitude, it.longitude) } +
                        listOf(leg.to)
                }
                else -> listOf(leg.from, leg.to)
            }
        }
    }

    fun routeEndpoints(trip: Itinerary?): List<GeoPoint> {
        if (trip == null) return emptyList()
        val from = trip.legs.firstOrNull()?.from ?: return emptyList()
        val to = trip.legs.lastOrNull()?.to ?: return emptyList()
        return listOf(from, to)
    }

    private fun demoJourney(from: GeoPoint, to: GeoPoint): JourneyResponse {
        val now = UtcDateTime.now()
        val arrive = addMinutes(now, 30.0)
        val boarding = addMinutes(now, 5.0)
        val alight = addMinutes(now, 27.0)
        val itinerary = Itinerary(
            departureAt = now,
            arrivalAt = arrive,
            legs = listOf(
                WalkLeg(
                    from = from,
                    to = from,
                    meters = 180.0,
                    departureAt = now,
                    arrivalAt = addMinutes(now, 2.5),
                ),
                TransitLeg(
                    lineId = "demo-1",
                    lineName = "A-01",
                    color = "#B9532F",
                    patternId = "demo-p",
                    direction = "outbound",
                    headsign = destinationLabel.ifBlank { strings.searchDestination },
                    boardingStop = CityStop(
                        id = "s1",
                        name = originLabel.ifBlank { strings.searchOrigin },
                        latitude = from.latitude,
                        longitude = from.longitude,
                        sourceKind = "demo",
                        nameSource = "demo",
                        needsReview = false,
                        usedInPatterns = true,
                    ),
                    alightingStop = CityStop(
                        id = "s2",
                        name = destinationLabel.ifBlank { strings.searchDestination },
                        latitude = to.latitude,
                        longitude = to.longitude,
                        sourceKind = "demo",
                        nameSource = "demo",
                        needsReview = false,
                        usedInPatterns = true,
                    ),
                    intermediateStops = emptyList(),
                    boardingAt = boarding,
                    expectedWaitMinutes = 3.0,
                    fare = Money.BASE,
                    geometry = listOf(from, to),
                    departureAt = addMinutes(now, 2.5),
                    arrivalAt = alight,
                ),
                WalkLeg(
                    from = to,
                    to = to,
                    meters = 120.0,
                    departureAt = alight,
                    arrivalAt = arrive,
                ),
            ),
        )
        return JourneyResponse(
            status = JourneyStatus.Success,
            itineraries = listOf(itinerary),
            referenceTime = now,
            accessRadiusMeters = 800.0,
        )
    }

    companion object {
        /** Marcadores de [reportMessage] que la UI traduce a confirmaciones (no son texto visible). */
        const val REPORT_OK = "report-ok"
        const val REPORT_CONFIRMED = "report-confirmed"
        private const val SEARCH_DEBOUNCE_MS = 180L
    }
}
