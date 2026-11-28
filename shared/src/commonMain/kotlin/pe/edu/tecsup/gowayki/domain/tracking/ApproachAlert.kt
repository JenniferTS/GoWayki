package pe.edu.tecsup.gowayki.domain.tracking

import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ApproachAlert {
    /** Distancia haversine en metros. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    fun shouldAlert(
        user: GeoPoint,
        alightingStop: GeoPoint,
        thresholdMeters: Double,
        alreadyAlerted: Boolean,
    ): Boolean {
        if (alreadyAlerted) return false
        return distanceMeters(user, alightingStop) <= thresholdMeters
    }
}
