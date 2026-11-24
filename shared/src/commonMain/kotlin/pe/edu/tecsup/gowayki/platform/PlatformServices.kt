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
/** Lectura fresca (Fused getCurrentLocation); async. */
expect fun platformFetchCurrentLocation(onResult: (PlatformLocation?) -> Unit)
expect fun platformRequestLocationPermission(onResult: (Boolean) -> Unit)
expect fun platformIsLocationEnabled(): Boolean
expect fun platformTripAlert(title: String, body: String, mode: AlarmMode)
expect fun platformClearTripNotifications()
/** Starts location updates for an active trip; returns a cancel handle. */
expect fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit
/** Watch liviano para UI (mapa / origen), sin foreground service. */
expect fun platformWatchUserLocation(onFix: (PlatformLocation) -> Unit): () -> Unit
/** Preferir foreground service en Android; cae a [platformWatchTripLocation] si no hay. */
expect fun platformStartTripTracking(onFix: (PositionFix) -> Unit): () -> Unit
expect fun platformStopTripTracking()

/** Feedback háptico discreto; no-op si el dispositivo lo desactiva. */
enum class HapticKind { Light, Success }

expect fun platformHaptic(kind: HapticKind)

/** Hilo para E/S bloqueante (Firestore `Tasks.await`, SQLite, copias de assets). Nunca el hilo de UI. */
expect val platformIoDispatcher: kotlinx.coroutines.CoroutineDispatcher

/** Build de depuración (muestra atajos de demo que no deben llegar a producción). */
expect fun platformIsDebugBuild(): Boolean
