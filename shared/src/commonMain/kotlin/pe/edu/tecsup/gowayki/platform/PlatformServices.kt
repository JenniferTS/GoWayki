package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix

data class PlatformLocation(val latitude: Double, val longitude: Double)

enum class AlarmMode { Sound, Vibration, Off }

interface UserStateSync {
    fun ensureUser(uid: String)
    fun pushTutorial(uid: String, progress: TutorialProgress)
    fun pullTutorial(uid: String): TutorialProgress?
    fun pushOnboarding(uid: String, status: OnboardingStatus)
    fun pullOnboarding(uid: String): OnboardingStatus?
}

object NoUserStateSync : UserStateSync {
    override fun ensureUser(uid: String) = Unit
    override fun pushTutorial(uid: String, progress: TutorialProgress) = Unit
    override fun pullTutorial(uid: String): TutorialProgress? = null
    override fun pushOnboarding(uid: String, status: OnboardingStatus) = Unit
    override fun pullOnboarding(uid: String): OnboardingStatus? = null
}

expect fun platformRemoteReports(): RemoteReportsClient?
expect fun platformUserStateSync(): UserStateSync
expect fun platformRanking(): RankingClient?
expect fun platformLastLocation(): PlatformLocation?
expect fun platformRequestLocationPermission(onResult: (Boolean) -> Unit)
expect fun platformTripAlert(title: String, body: String, mode: AlarmMode)
expect fun platformClearTripNotifications()
/** Starts location updates for an active trip; returns a cancel handle. */
expect fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit
