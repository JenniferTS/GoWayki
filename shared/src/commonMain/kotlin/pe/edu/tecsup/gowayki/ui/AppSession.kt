package pe.edu.tecsup.gowayki.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pe.edu.tecsup.gowayki.domain.community.InMemoryReportsRepository
import pe.edu.tecsup.gowayki.domain.community.ReportDraft
import pe.edu.tecsup.gowayki.domain.community.ReportKind
import pe.edu.tecsup.gowayki.domain.community.ReportsRepository
import pe.edu.tecsup.gowayki.domain.community.StoredReport
import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
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
import pe.edu.tecsup.gowayki.domain.preferences.TutorialMigrationPolicy
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress
import pe.edu.tecsup.gowayki.domain.preferences.TutorialStatus
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.i18n.StringCatalog
import pe.edu.tecsup.gowayki.i18n.Strings

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
    data object Help : Screen()
    data object Auth : Screen()
}

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val isNewRegistration: Boolean = false,
    val isGuest: Boolean = false,
)

class AppSession(
    private val preferences: PreferencesRepository = InMemoryPreferencesRepository(),
    private val reports: ReportsRepository = InMemoryReportsRepository(),
    private val placeSearch: PlaceSearchRepository? = null,
    private val networkProvider: (() -> PlannerNetwork?)? = null,
    deviceLanguage: String? = "es",
    startAsGuest: Boolean = true,
) {
    var locale by mutableStateOf(preferences.locale(deviceLanguage))
        private set
    var reduceMotion by mutableStateOf(false)
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
    var reportKind by mutableStateOf(ReportKind.HeavyTraffic)
    var reportDescription by mutableStateOf("")
    var reportLat by mutableStateOf(-16.4090)
    var reportLng by mutableStateOf(-71.5375)
    var reportMessage by mutableStateOf<String?>(null)
    var myReports by mutableStateOf<List<StoredReport>>(emptyList())
    var packReady by mutableStateOf(false)

    val strings: Strings get() = StringCatalog.forLocale(locale)
    val baseFare: Money get() = Money.BASE

    fun bootstrap() {
        packReady = networkProvider?.invoke() != null
        val onboarding = preferences.readOnboarding()
        screen = if (onboarding == null) Screen.Onboarding else Screen.Home
        maybeStartTutorial()
    }

    fun updateLocale(value: AppLocale) {
        locale = value
        preferences.saveLocaleCode(value.code)
    }

    fun finishOnboarding(skipped: Boolean) {
        preferences.saveOnboarding(if (skipped) OnboardingStatus.Skipped else OnboardingStatus.Completed)
        screen = Screen.Home
        maybeStartTutorial()
    }

    fun go(to: Screen) {
        screen = to
    }

    fun searchPlaces(query: String) {
        val q = query.trim()
        if (q.length < 2 || placeSearch == null) {
            searchHits = emptyList()
            return
        }
        searchHits = placeSearch.search(q)
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
        planning = true
        planError = null
        try {
            val network = networkProvider?.invoke()
            journey = if (network == null) {
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
            val response = journey
            if (response != null &&
                response.status == JourneyStatus.Success &&
                response.itineraries.isNotEmpty()
            ) {
                screen = Screen.Results
            } else {
                planError = strings.emptyRoutes
                screen = Screen.Results
            }
        } catch (t: Throwable) {
            planError = t.message ?: strings.errorGeneric
        } finally {
            planning = false
        }
    }

    fun openDetail(index: Int) {
        val list = journey?.itineraries.orEmpty()
        if (index in list.indices) {
            selectedItinerary = list[index]
            screen = Screen.Detail(index)
        }
    }

    fun submitReport() {
        val current = user
        if (current == null || current.isGuest) {
            reportMessage = strings.signIn
            screen = Screen.Auth
            return
        }
        try {
            val id = reports.newId()
            val draft = ReportDraft(
                id = id,
                kind = reportKind,
                description = reportDescription.ifBlank { "${reportKind.apiValue} report" },
                latitude = reportLat,
                longitude = reportLng,
            )
            reports.saveDraft(draft, current.uid)
            reports.markPending(id)
            reports.markPublished(id, UtcDateTime.now())
            myReports = reports.mine(current.uid)
            reportMessage = "ok"
            reportDescription = ""
            screen = Screen.Home
        } catch (t: Throwable) {
            reportMessage = t.message ?: strings.errorGeneric
        }
    }

    fun withdrawReport(id: String) {
        reports.delete(id)
        myReports = reports.mine(user?.uid)
    }

    fun applyGoogleUser(uid: String, displayName: String?, isNew: Boolean) {
        user = AuthUser(
            uid = uid,
            displayName = displayName,
            isNewRegistration = isNew,
            isGuest = false,
        )
        tutorial = TutorialMigrationPolicy.resolve(isNew, preferences.readTutorial())
        preferences.saveTutorial(tutorial)
        screen = Screen.Home
        maybeStartTutorial()
    }

    fun signInWithGoogleDemo(isNew: Boolean) {
        applyGoogleUser(
            uid = if (isNew) "new-google-user" else "existing-google-user",
            displayName = "Sharon",
            isNew = isNew,
        )
    }

    fun signOutToGuest() {
        user = AuthUser(uid = "guest", displayName = null, isGuest = true)
        showTutorialOverlay = false
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
            preferences.saveTutorial(tutorial)
        }
    }

    fun tutorialBack() {
        tutorial = tutorial.copy(step = (tutorial.step - 1).coerceAtLeast(0))
        preferences.saveTutorial(tutorial)
    }

    fun finishTutorial(skipped: Boolean) {
        tutorial = tutorial.copy(
            status = if (skipped) TutorialStatus.Skipped else TutorialStatus.Completed,
            step = if (skipped) tutorial.step else 3,
        )
        preferences.saveTutorial(tutorial)
        showTutorialOverlay = false
    }

    fun replayTutorial() {
        tutorial = TutorialProgress(status = TutorialStatus.Started, step = 0)
        preferences.saveTutorial(tutorial)
        showTutorialOverlay = true
        screen = Screen.Home
    }

    fun formatFare(money: Money): String {
        val major = money.amountMinor / 100
        val minor = (money.amountMinor % 100).toString().padStart(2, '0')
        return "S/ $major.$minor"
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
                    headsign = destinationLabel.ifBlank { "Destino" },
                    boardingStop = CityStop(
                        id = "s1",
                        name = originLabel.ifBlank { "Origen" },
                        latitude = from.latitude,
                        longitude = from.longitude,
                        sourceKind = "demo",
                        nameSource = "demo",
                        needsReview = false,
                        usedInPatterns = true,
                    ),
                    alightingStop = CityStop(
                        id = "s2",
                        name = destinationLabel.ifBlank { "Destino" },
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
}
