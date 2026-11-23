import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.preferences.InMemoryPreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.screens.ActiveTripScreen
import pe.edu.tecsup.gowayki.ui.screens.AuthScreen
import pe.edu.tecsup.gowayki.ui.screens.DetailScreen
import pe.edu.tecsup.gowayki.ui.screens.GameScreen
import pe.edu.tecsup.gowayki.ui.screens.HelpScreen
import pe.edu.tecsup.gowayki.ui.screens.HomeScreen
import pe.edu.tecsup.gowayki.ui.screens.LinesScreen
import pe.edu.tecsup.gowayki.ui.screens.OnboardingScreen
import pe.edu.tecsup.gowayki.ui.screens.ProfileScreen
import pe.edu.tecsup.gowayki.ui.screens.RankingScreen
import pe.edu.tecsup.gowayki.ui.screens.ReportsScreen
import pe.edu.tecsup.gowayki.ui.screens.MapsJourneyScreen
import pe.edu.tecsup.gowayki.ui.screens.SearchScreen
import pe.edu.tecsup.gowayki.ui.screens.SettingsScreen
import pe.edu.tecsup.gowayki.ui.screens.SplashScreen
import pe.edu.tecsup.gowayki.ui.theme.GowaykiMotion
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import pe.edu.tecsup.gowayki.ui.theme.LocalMotionPrefs

data class GowaykiRuntime(
    val deviceLanguage: String? = "es",
    val placeSearch: PlaceSearchRepository? = null,
    val networkProvider: (() -> PlannerNetwork?)? = null,
    val cityPackProvider: (() -> CityPackRepository?)? = null,
    val preferences: PreferencesRepository = InMemoryPreferencesRepository(),
)

private var runtimeHolder: GowaykiRuntime = GowaykiRuntime()

fun installGowaykiRuntime(runtime: GowaykiRuntime) {
    runtimeHolder = runtime
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun App() {
    val session = remember {
        AppSession(
            preferences = runtimeHolder.preferences,
            placeSearch = runtimeHolder.placeSearch,
            networkProvider = runtimeHolder.networkProvider,
            cityPackProvider = runtimeHolder.cityPackProvider,
            deviceLanguage = runtimeHolder.deviceLanguage,
        )
    }
    GowaykiTheme(reduceMotion = session.reduceMotion) {
        val reduce = LocalMotionPrefs.current.reduceMotion
        AnimatedContent(
            targetState = session.screen,
            transitionSpec = {
                if (reduce) {
                    fadeIn(GowaykiMotion.none()) togetherWith fadeOut(GowaykiMotion.none())
                } else {
                    (fadeIn(GowaykiMotion.soft()) + slideInHorizontally { it / 12 }) togetherWith
                        (fadeOut(GowaykiMotion.micro()) + slideOutHorizontally { -it / 16 })
                }
            },
        ) { screen ->
            when (screen) {
                Screen.Splash -> SplashScreen(session)
                Screen.Onboarding -> OnboardingScreen(session)
                Screen.Home -> HomeScreen(session)
                Screen.Search -> SearchScreen(session)
                Screen.Results -> MapsJourneyScreen(session)
                is Screen.Detail -> DetailScreen(session)
                Screen.Reports -> ReportsScreen(session)
                Screen.Profile -> ProfileScreen(session)
                Screen.Settings -> SettingsScreen(session)
                Screen.Game -> GameScreen(session)
                Screen.Ranking -> RankingScreen(session)
                Screen.Help -> HelpScreen(session)
                Screen.Auth -> AuthScreen(session)
                Screen.ActiveTrip -> ActiveTripScreen(session)
                Screen.Lines -> LinesScreen(session)
            }
        }
    }
}

expect fun getPlatformName(): String
