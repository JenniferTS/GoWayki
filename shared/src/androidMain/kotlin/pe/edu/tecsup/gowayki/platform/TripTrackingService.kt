package pe.edu.tecsup.gowayki.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
import pe.edu.tecsup.gowayki.domain.tracking.PositionFix

/**
 * Mantiene el viaje activo con notificación persistente y GPS aunque la UI esté en segundo plano.
 */
class TripTrackingService : Service() {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfSafe()
                return START_NOT_STICKY
            }
            else -> startTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        ensureChannel()
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GoWayki · Viaje en curso")
            .setContentText("Te avisamos antes de bajar")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(NOTIF_ID, notification)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                TripLocationHub.onFix?.invoke(
                    PositionFix(
                        point = GeoPoint(loc.latitude, loc.longitude),
                        accuracyMeters = loc.accuracy.toDouble(),
                        at = UtcDateTime.now(),
                    ),
                )
            }
        }
        callback = cb
        try {
            client.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelfSafe()
        }
    }

    private fun stopSelfSafe() {
        callback?.let { runCatching { client.removeLocationUpdates(it) } }
        callback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        callback?.let { runCatching { client.removeLocationUpdates(it) } }
        callback = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Viaje GoWayki", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val CHANNEL_ID = "gowayki_trip_fg"
        const val NOTIF_ID = 4201
        const val ACTION_STOP = "pe.edu.tecsup.gowayki.STOP_TRIP"

        fun start(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TripTrackingService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}

object TripLocationHub {
    @Volatile var onFix: ((PositionFix) -> Unit)? = null
}
