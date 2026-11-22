package pe.edu.tecsup.gowayki.domain.tracking

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
import pe.edu.tecsup.gowayki.domain.planner.JourneyLeg
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
data class PositionFix(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val at: UtcDateTime,
)

enum class TripPhase { Walking, WaitingToBoard, Riding, Completed }

enum class TripAlertKind { Alight, Transfer }

data class TripAlert(val kind: TripAlertKind, val stopName: String)

data class TripProgress(
    val legIndex: Int,
    val phase: TripPhase,
    val remainingMeters: Double? = null,
    val stopsRemaining: Int? = null,
    val fix: PositionFix? = null,
    val offRoute: Boolean = false,
    val alerts: List<TripAlert> = emptyList(),
)

/**
 * Ayuda local de navegación. El abordaje es explícito: caminar cerca de la ruta
 * no arma la alarma de bajada hasta confirmar.
 */
class TripTracker(private val trip: Itinerary) {
    init {
        require(trip.legs.isNotEmpty()) { "Empty trip" }
    }

    private var index = 0
    private var phase: TripPhase =
        if (trip.legs.first() is TransitLeg) TripPhase.WaitingToBoard else TripPhase.Walking
    private var along = 0.0
    private var lastAt: UtcDateTime? = null
    private var warned = false
    private var fix: PositionFix? = null

    val initial: TripProgress get() = TripProgress(legIndex = index, phase = phase)

    private val leg: JourneyLeg get() = trip.legs[index]

    fun confirmStep(): TripProgress {
        if (phase == TripPhase.WaitingToBoard) {
            phase = TripPhase.Riding
            along = 0.0
            lastAt = null
            warned = false
            return snapshot()
        }
        if (phase == TripPhase.Completed) return snapshot()
        val alerts = mutableListOf<TripAlert>()
        advance(alerts)
        return snapshot(alerts = alerts)
    }

    fun update(fix: PositionFix, now: UtcDateTime): TripProgress {
        val ageMicros = now.epochMicros - fix.at.epochMicros
        if (phase == TripPhase.Completed ||
            !fix.accuracyMeters.isFinite() ||
            fix.accuracyMeters < 0 ||
            fix.accuracyMeters > 60 ||
            ageMicros > 30_000_000L ||
            ageMicros < -5_000_000L ||
            (lastAt != null && fix.at.epochMicros <= lastAt!!.epochMicros)
        ) {
            return snapshot(offRoute = true)
        }
        this.fix = fix
        if (phase == TripPhase.WaitingToBoard) {
            lastAt = fix.at
            return snapshot(remaining = fix.point.distanceTo(leg.from))
        }
        val alerts = mutableListOf<TripAlert>()
        if (phase == TripPhase.Walking) {
            lastAt = fix.at
            val distance = fix.point.distanceTo(leg.to)
            if (distance <= 50) {
                advance(alerts)
                return snapshot(alerts = alerts)
            }
            return snapshot(remaining = distance)
        }
        val transit = leg as TransitLeg
        val path = Path(if (transit.geometry.size >= 2) transit.geometry else listOf(transit.from, transit.to))
        val elapsed = if (lastAt == null) {
            0.0
        } else {
            (fix.at.epochMicros - lastAt!!.epochMicros).toDouble() / 1_000_000.0
        }
        val advanceMeters = if (lastAt == null) 250.0 else max(150.0, elapsed * 35.0)
        val projection = path.project(
            fix.point,
            minAlong = max(0.0, along - 50),
            maxAlong = along + advanceMeters,
        )
        if (projection.offset > 100) return snapshot(offRoute = true)
        lastAt = fix.at
        along = max(along, projection.along)
        val remaining = max(0.0, path.length - along)
        var stopAlong = 0.0
        var stopsRemaining = 1
        for (stop in transit.intermediateStops) {
            val p = path.project(GeoPoint.stop(stop), minAlong = stopAlong)
            stopAlong = p.along
            if (stopAlong > along + 40) stopsRemaining++
        }
        if (!warned &&
            along >= min(80.0, path.length * 0.1) &&
            (remaining <= 400 || stopsRemaining == 1)
        ) {
            warned = true
            alerts.add(TripAlert(TripAlertKind.Alight, transit.alightingStop.name))
        }
        if (remaining <= 60 && fix.point.distanceTo(transit.to) <= 60) {
            advance(alerts)
            return snapshot(alerts = alerts)
        }
        return snapshot(remaining = remaining, stops = stopsRemaining, alerts = alerts)
    }

    private fun advance(alerts: MutableList<TripAlert>) {
        if (leg is TransitLeg && trip.legs.drop(index + 1).any { it is TransitLeg }) {
            alerts.add(TripAlert(TripAlertKind.Transfer, (leg as TransitLeg).alightingStop.name))
        }
        if (index == trip.legs.lastIndex) {
            phase = TripPhase.Completed
        } else {
            index++
            phase = if (leg is TransitLeg) TripPhase.WaitingToBoard else TripPhase.Walking
        }
        along = 0.0
        lastAt = null
        warned = false
    }

    private fun snapshot(
        remaining: Double? = null,
        stops: Int? = null,
        offRoute: Boolean = false,
        alerts: List<TripAlert> = emptyList(),
    ): TripProgress = TripProgress(
        legIndex = index,
        phase = phase,
        remainingMeters = remaining,
        stopsRemaining = stops,
        fix = fix,
        offRoute = offRoute,
        alerts = alerts.toList(),
    )

    private data class Projection(val along: Double, val offset: Double)

    private class Path(private val points: List<GeoPoint>) {
        private val cumulative = mutableListOf(0.0)

        init {
            for (i in 1 until points.size) {
                cumulative.add(cumulative.last() + points[i - 1].distanceTo(points[i]))
            }
        }

        val length: Double get() = cumulative.last()

        fun project(point: GeoPoint, minAlong: Double = 0.0, maxAlong: Double = Double.POSITIVE_INFINITY): Projection {
            var best = Projection(0.0, Double.POSITIVE_INFINITY)
            val meters = 111195.08
            val cosine = cos(point.latitude * PI / 180.0)
            for (i in 1 until points.size) {
                val start = cumulative[i - 1]
                val end = cumulative[i]
                if (end < minAlong || start > maxAlong || end == start) continue
                val a = points[i - 1]
                val b = points[i]
                val ax = (a.longitude - point.longitude) * meters * cosine
                val ay = (a.latitude - point.latitude) * meters
                val dx = (b.longitude - a.longitude) * meters * cosine
                val dy = (b.latitude - a.latitude) * meters
                val denom = dx * dx + dy * dy
                val raw = if (denom == 0.0) 0.0 else -(ax * dx + ay * dy) / denom
                val lo = ((minAlong - start) / (end - start)).coerceIn(0.0, 1.0)
                val hi = ((maxAlong - start) / (end - start)).coerceIn(0.0, 1.0)
                val fraction = raw.coerceIn(lo, hi)
                val offset = sqrt((ax + fraction * dx).let { it * it } + (ay + fraction * dy).let { it * it })
                if (offset < best.offset) {
                    best = Projection(start + fraction * (end - start), offset)
                }
            }
            return best
        }
    }
}

private fun Double.isFinite(): Boolean = !isNaN() && !isInfinite()
