package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix

actual fun platformRemoteReports(): RemoteReportsClient? = null
actual fun platformUserStateSync(): UserStateSync = NoUserStateSync
actual fun platformRanking(): RankingClient? = null
actual fun platformLastLocation(): PlatformLocation? = null
actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) = onResult(false)
actual fun platformTripAlert(title: String, body: String, mode: AlarmMode) = Unit
actual fun platformClearTripNotifications() = Unit
actual fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStartTripTracking(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStopTripTracking() = Unit
