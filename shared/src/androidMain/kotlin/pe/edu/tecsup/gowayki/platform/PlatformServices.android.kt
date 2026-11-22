package pe.edu.tecsup.gowayki.platform

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import pe.edu.tecsup.gowayki.data.community.FirestoreReportsClient
import pe.edu.tecsup.gowayki.data.location.AndroidLocationFix
import pe.edu.tecsup.gowayki.data.sync.FirestoreUserStateSync
import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress

object AndroidPlatformHost {
    @Volatile var activity: ComponentActivity? = null
    @Volatile var remoteReports: RemoteReportsClient? = null
    @Volatile var userSync: UserStateSync = NoUserStateSync
    var requestLocation: ((onResult: (Boolean) -> Unit) -> Unit)? = null
}

actual fun platformRemoteReports(): RemoteReportsClient? = AndroidPlatformHost.remoteReports

actual fun platformUserStateSync(): UserStateSync = AndroidPlatformHost.userSync

actual fun platformLastLocation(): PlatformLocation? {
    val act = AndroidPlatformHost.activity ?: return null
    val fix = AndroidLocationFix.lastKnown(act) ?: return null
    return PlatformLocation(fix.latitude, fix.longitude)
}

actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) {
    val req = AndroidPlatformHost.requestLocation
    if (req == null) onResult(false) else req(onResult)
}

fun installAndroidPlatform(
    activity: ComponentActivity,
    remote: RemoteReportsClient = FirestoreReportsClient(),
    sync: FirestoreUserStateSync = FirestoreUserStateSync(),
) {
    AndroidPlatformHost.activity = activity
    AndroidPlatformHost.remoteReports = remote
    AndroidPlatformHost.userSync = object : UserStateSync {
        override fun ensureUser(uid: String) = sync.ensureUserDoc(uid)
        override fun pushTutorial(uid: String, progress: TutorialProgress) = sync.pushTutorial(uid, progress)
        override fun pullTutorial(uid: String) = sync.pullTutorial(uid)
        override fun pushOnboarding(uid: String, status: OnboardingStatus) = sync.pushOnboarding(uid, status)
        override fun pullOnboarding(uid: String) = sync.pullOnboarding(uid)
    }
    val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        pendingLocationCallback?.invoke(granted)
        pendingLocationCallback = null
    }
    AndroidPlatformHost.requestLocation = { cb ->
        if (AndroidLocationFix.hasPermission(activity)) {
            cb(true)
        } else {
            pendingLocationCallback = cb
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}

private var pendingLocationCallback: ((Boolean) -> Unit)? = null
