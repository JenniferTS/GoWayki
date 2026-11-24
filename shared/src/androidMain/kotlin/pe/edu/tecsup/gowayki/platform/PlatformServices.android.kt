package pe.edu.tecsup.gowayki.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pe.edu.tecsup.gowayki.data.community.FirestoreReportsClient
import pe.edu.tecsup.gowayki.data.game.FirestoreRankingClient
import pe.edu.tecsup.gowayki.data.location.AndroidLocationFix
import pe.edu.tecsup.gowayki.data.sync.FirestoreUserStateSync
import pe.edu.tecsup.gowayki.domain.account.AccountDeletionRepository
import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix

object AndroidPlatformHost {
    @Volatile var activity: ComponentActivity? = null
    @Volatile var remoteReports: RemoteReportsClient? = null
    @Volatile var userSync: UserStateSync = NoUserStateSync
    @Volatile var ranking: RankingClient? = null
    @Volatile var accountDeletion: AccountDeletionRepository? = null
    var requestLocation: ((onResult: (Boolean) -> Unit) -> Unit)? = null
    var requestNotifications: ((onResult: (Boolean) -> Unit) -> Unit)? = null
}

actual fun platformRemoteReports(): RemoteReportsClient? = AndroidPlatformHost.remoteReports

actual fun platformUserStateSync(): UserStateSync = AndroidPlatformHost.userSync

actual fun platformRanking(): RankingClient? = AndroidPlatformHost.ranking

actual fun platformLastLocation(): PlatformLocation? {
    val act = AndroidPlatformHost.activity ?: return null
    val fix = AndroidLocationFix.lastKnown(act) ?: return null
    return PlatformLocation(fix.latitude, fix.longitude)
}

actual fun platformFetchCurrentLocation(onResult: (PlatformLocation?) -> Unit) {
    val act = AndroidPlatformHost.activity
    if (act == null) {
        onResult(null)
        return
    }
    AndroidLocationFix.fetchCurrent(act) { fix ->
        onResult(fix?.let { PlatformLocation(it.latitude, it.longitude) })
    }
}

actual fun platformIsLocationEnabled(): Boolean {
    val act = AndroidPlatformHost.activity ?: return false
    return AndroidLocationFix.isLocationEnabled(act)
}

actual fun platformRequestLocationPermission(onResult: (Boolean) -> Unit) {
    val req = AndroidPlatformHost.requestLocation
    if (req == null) onResult(false) else req(onResult)
}

actual fun platformHasLocationPermission(): Boolean {
    val act = AndroidPlatformHost.activity ?: return false
    return AndroidLocationFix.hasPermission(act)
}

actual fun platformRequestNotificationPermission(onResult: (Boolean) -> Unit) {
    val req = AndroidPlatformHost.requestNotifications
    if (req == null) onResult(false) else req(onResult)
}

actual fun platformOpenAppSettings(): Boolean {
    val act = AndroidPlatformHost.activity ?: return false
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", act.packageName, null))
    return runCatching { act.startActivity(intent) }.isSuccess
}

actual fun platformOpenUrl(url: String): Boolean {
    val act = AndroidPlatformHost.activity ?: return false
    return try {
        act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

actual fun platformAccountDeletion(): AccountDeletionRepository? = AndroidPlatformHost.accountDeletion

actual fun platformWatchUserLocation(onFix: (PlatformLocation) -> Unit): () -> Unit {
    val act = AndroidPlatformHost.activity ?: return { }
    if (!AndroidLocationFix.hasPermission(act)) return { }
    val client = LocationServices.getFusedLocationProviderClient(act)
    val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 4_000L)
        .setMinUpdateIntervalMillis(2_500L)
        .setMinUpdateDistanceMeters(8f)
        .build()
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            onFix(PlatformLocation(loc.latitude, loc.longitude))
        }
    }
    try {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        // Empuja un fix inmediato si hay lastLocation
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) onFix(PlatformLocation(loc.latitude, loc.longitude))
        }
    } catch (_: SecurityException) {
        return { }
    }
    return {
        runCatching { client.removeLocationUpdates(callback) }
    }
}

actual fun platformTripAlert(title: String, body: String, mode: AlarmMode) {
    if (mode == AlarmMode.Off) return
    val act = AndroidPlatformHost.activity ?: return
    val nm = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "gowayki_trip"
    if (Build.VERSION.SDK_INT >= 26) {
        nm.createNotificationChannel(
            NotificationChannel(channelId, pe.edu.tecsup.gowayki.i18n.StringCatalog.current.tripChannelName, NotificationManager.IMPORTANCE_HIGH),
        )
    }
    val notification = NotificationCompat.Builder(act, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_map)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    if (mode == AlarmMode.Vibration || mode == AlarmMode.Sound) {
        vibrate(act)
    }
}

actual fun platformClearTripNotifications() {
    val act = AndroidPlatformHost.activity ?: return
    val nm = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancelAll()
}

actual fun platformWatchTripLocation(onFix: (PositionFix) -> Unit): () -> Unit {
    val act = AndroidPlatformHost.activity ?: return { }
    if (!AndroidLocationFix.hasPermission(act)) return { }
    val client = LocationServices.getFusedLocationProviderClient(act)
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
        .setMinUpdateIntervalMillis(2_000L)
        .setMinUpdateDistanceMeters(5f)
        .build()
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            onFix(
                PositionFix(
                    point = GeoPoint(loc.latitude, loc.longitude),
                    accuracyMeters = loc.accuracy.toDouble(),
                    at = UtcDateTime.now(),
                ),
            )
        }
    }
    try {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: SecurityException) {
        return { }
    }
    return {
        runCatching { client.removeLocationUpdates(callback) }
    }
}

actual fun platformStartTripTracking(onFix: (PositionFix) -> Unit): () -> Unit {
    val act = AndroidPlatformHost.activity ?: return platformWatchTripLocation(onFix)
    TripLocationHub.onFix = onFix
    runCatching { TripTrackingService.start(act.applicationContext) }
        .onFailure {
            TripLocationHub.onFix = null
            return platformWatchTripLocation(onFix)
        }
    return {
        platformStopTripTracking()
    }
}

actual fun platformStopTripTracking() {
    TripLocationHub.onFix = null
    val act = AndroidPlatformHost.activity ?: return
    runCatching { TripTrackingService.stop(act.applicationContext) }
}

private fun vibrate(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(400)
            }
        }
    } catch (_: Throwable) {
    }
}

fun installAndroidPlatform(
    activity: ComponentActivity,
    remote: RemoteReportsClient = FirestoreReportsClient(),
    sync: FirestoreUserStateSync = FirestoreUserStateSync(),
    ranking: RankingClient = FirestoreRankingClient(),
) {
    AndroidPlatformHost.activity = activity
    AndroidPlatformHost.remoteReports = remote
    AndroidPlatformHost.ranking = ranking
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
    // POST_NOTIFICATIONS ya no se pide al abrir: solo al iniciar un viaje activo (alertas de bajada).
    val notificationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingNotificationCallback?.invoke(granted)
        pendingNotificationCallback = null
    }
    AndroidPlatformHost.requestNotifications = { cb ->
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            cb(true)
        } else {
            pendingNotificationCallback = cb
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private var pendingLocationCallback: ((Boolean) -> Unit)? = null
private var pendingNotificationCallback: ((Boolean) -> Unit)? = null

actual fun platformHaptic(kind: HapticKind) {
    val act = AndroidPlatformHost.activity ?: return
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val mgr = act.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            act.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) {
            val effect = when (kind) {
                HapticKind.Light -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticKind.Success -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(if (kind == HapticKind.Success) 28 else 12, 48))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (kind == HapticKind.Success) 28 else 12)
        }
    }
}

actual val platformIoDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO

actual fun platformIsDebugBuild(): Boolean {
    val act = AndroidPlatformHost.activity ?: return false
    return (act.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
