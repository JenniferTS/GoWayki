package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient

actual fun platformRemoteReports(): RemoteReportsClient? = null
actual fun platformUserStateSync(): UserStateSync = NoUserStateSync
actual fun platformLastLocation(): PlatformLocation? = null
actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) = onResult(false)
