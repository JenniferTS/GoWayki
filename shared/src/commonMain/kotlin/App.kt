import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.preferences.InMemoryPreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.screens.ActiveTripScreen
import pe.edu.tecsup.gowayki.ui.screens.AuthScreen
import pe.edu.tecsup.gowayki.ui.screens.DetailScreen
import pe.edu.tecsup.gowayki.ui.screens.GameScreen
import pe.edu.tecsup.gowayki.ui.screens.HelpScreen
import pe.edu.tecsup.gowayki.ui.screens.HomeScreen
import pe.edu.tecsup.gowayki.ui.screens.OnboardingScreen
import pe.edu.tecsup.gowayki.ui.screens.ProfileScreen
import pe.edu.tecsup.gowayki.ui.screens.RankingScreen
import pe.edu.tecsup.gowayki.ui.screens.ReportsScreen
import pe.edu.tecsup.gowayki.ui.screens.ResultsScreen
import pe.edu.tecsup.gowayki.ui.screens.SearchScreen
import pe.edu.tecsup.gowayki.ui.screens.SettingsScreen
import pe.edu.tecsup.gowayki.ui.screens.SplashScreen
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

data class GowaykiRuntime(
    val deviceLanguage: String? = "es",
    val placeSearch: PlaceSearchRepository? = null,
    val networkProvider: (() -> PlannerNetwork?)? = null,
    val preferences: PreferencesRepository = InMemoryPreferencesRepository(),
)

private var runtimeHolder: GowaykiRuntime = GowaykiRuntime()

fun installGowaykiRuntime(runtime: GowaykiRuntime) {
    runtimeHolder = runtime
}

@Composable
fun App() {
    val session = remember {
        AppSession(
            preferences = runtimeHolder.preferences,
            placeSearch = runtimeHolder.placeSearch,
            networkProvider = runtimeHolder.networkProvider,
            deviceLanguage = runtimeHolder.deviceLanguage,
        )
    }
    GowaykiTheme(reduceMotion = session.reduceMotion) {
        when (val screen = session.screen) {
            Screen.Splash -> SplashScreen(session)
            Screen.Onboarding -> OnboardingScreen(session)
            Screen.Home -> HomeScreen(session)
            Screen.Search -> SearchScreen(session)
            Screen.Results -> ResultsScreen(session)
            is Screen.Detail -> DetailScreen(session)
            Screen.Reports -> ReportsScreen(session)
            Screen.Profile -> ProfileScreen(session)
            Screen.Settings -> SettingsScreen(session)
            Screen.Game -> GameScreen(session)
            Screen.Ranking -> RankingScreen(session)
            Screen.Help -> HelpScreen(session)
            Screen.Auth -> AuthScreen(session)
            Screen.ActiveTrip -> ActiveTripScreen(session)
        }
    }
}

expect fun getPlatformName(): String
