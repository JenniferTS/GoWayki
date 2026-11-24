package pe.edu.tecsup.gowayki.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class LocationFix(val latitude: Double, val longitude: Double, val accuracyMeters: Float?)

object AndroidLocationFix {
    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return runCatching {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): LocationFix? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.accuracy < best.accuracy) best = loc
        }
        val loc = best ?: return null
        return LocationFix(loc.latitude, loc.longitude, loc.accuracy)
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrent(context: Context, onResult: (LocationFix?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        if (!isLocationEnabled(context)) {
            onResult(null)
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val currentRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(CURRENT_FIX_TIMEOUT_MS)
            .setMaxUpdateAgeMillis(60_000L)
            .build()
        client.lastLocation
            .addOnSuccessListener { last ->
                if (last != null && last.accuracy <= 80f && ageMs(last) < 60_000L) {
                    onResult(LocationFix(last.latitude, last.longitude, last.accuracy))
                } else {
                    client.getCurrentLocation(currentRequest, cts.token)
                        .addOnSuccessListener { loc ->
                            when {
                                loc != null -> onResult(LocationFix(loc.latitude, loc.longitude, loc.accuracy))
                                last != null -> onResult(LocationFix(last.latitude, last.longitude, last.accuracy))
                                else -> onResult(lastKnown(context))
                            }
                        }
                        .addOnFailureListener {
                            onResult(
                                last?.let { LocationFix(it.latitude, it.longitude, it.accuracy) }
                                    ?: lastKnown(context),
                            )
                        }
                }
            }
            .addOnFailureListener {
                client.getCurrentLocation(currentRequest, cts.token)
                    .addOnSuccessListener { loc ->
                        onResult(loc?.let { LocationFix(it.latitude, it.longitude, it.accuracy) } ?: lastKnown(context))
                    }
                    .addOnFailureListener { onResult(lastKnown(context)) }
            }
    }

    /** Sin fix fresco en este plazo se cae a la última ubicación conocida (evita “Detectando…” eterno). */
    private const val CURRENT_FIX_TIMEOUT_MS = 8_000L

    private fun ageMs(loc: Location): Long {
        return SystemClock.elapsedRealtime() - loc.elapsedRealtimeNanos / 1_000_000L
    }
}
