package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.account.AccountDeletionRepository
import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun platformRemoteReports(): RemoteReportsClient? = null
actual fun platformUserStateSync(): UserStateSync = NoUserStateSync
actual fun platformRanking(): RankingClient? = null
actual fun platformLastLocation(): PlatformLocation? = null
actual fun platformFetchCurrentLocation(onResult: (PlatformLocation?) -> Unit) = onResult(null)
actual fun platformIsLocationEnabled(): Boolean = false
actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) = onResult(false)
actual fun platformHasLocationPermission(): Boolean = false
actual fun platformRequestNotificationPermission(onResult: (Boolean) -> Unit) = onResult(false)
actual fun platformOpenAppSettings(): Boolean = platformOpenUrl(UIApplicationOpenSettingsURLString)
actual fun platformOpenUrl(url: String): Boolean {
    val target = NSURL.URLWithString(url) ?: return false
    val app = UIApplication.sharedApplication
    if (!app.canOpenURL(target)) return false
    app.openURL(target, options = emptyMap<Any?, Any>(), completionHandler = null)
    return true
}
actual fun platformAccountDeletion(): AccountDeletionRepository? = null
actual fun platformWatchUserLocation(onFix: (PlatformLocation) -> Unit): () -> Unit = { }
actual fun platformTripAlert(title: String, body: String, mode: AlarmMode) = Unit
actual fun platformClearTripNotifications() = Unit
actual fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStartTripTracking(onFix: (PositionFix) -> Unit): () -> Unit = { }
actual fun platformStopTripTracking() = Unit
actual fun platformHaptic(kind: HapticKind) = Unit
actual val platformIoDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
actual fun platformIsDebugBuild(): Boolean = false
