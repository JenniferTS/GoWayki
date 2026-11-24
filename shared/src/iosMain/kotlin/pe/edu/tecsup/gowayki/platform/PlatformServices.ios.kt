package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix

actual fun platformRemoteReports(): RemoteReportsClient? = null
actual fun platformUserStateSync(): UserStateSync = NoUserStateSync
actual fun platformRanking(): RankingClient? = null
actual fun platformLastLocation(): PlatformLocation? = null
actual fun platformFetchCurrentLocation(onResult: (PlatformLocation?) -> Unit) = onResult(null)
actual fun platformIsLocationEnabled(): Boolean = false
actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) = onResult(false)
actual fun platformWatchUserLocation(onFix: (PlatformLocation) -> Unit): () -> Unit = { }
actual fun platformTripAlert(title: String, body: String, mode: AlarmMode) = Unit
actual fun platformClearTripNotifications() = Unit
actual fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStartTripTracking(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStopTripTracking() = Unit
actual fun platformHaptic(kind: HapticKind) = Unit
actual val platformIoDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
actual fun platformIsDebugBuild(): Boolean = false
