package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import pe.edu.tecsup.gowayki.domain.money.Money

class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        if (!latitude.finite() ||
            !longitude.finite() ||
            abs(latitude) > 90.0 ||
            abs(longitude) > 180.0
        ) {
            throw IllegalArgumentException("Coordenada inválida.")
        }
    }

    fun distanceTo(other: GeoPoint): Double {
        val radians = PI / 180.0
        val a = sin((other.latitude - latitude) * radians / 2.0).let { it * it } +
            cos(latitude * radians) *
            cos(other.latitude * radians) *
            sin((other.longitude - longitude) * radians / 2.0).let { it * it }
        return 6371008.8 * 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    companion object {
        fun stop(stop: CityStop): GeoPoint = GeoPoint(stop.latitude, stop.longitude)
    }
}

sealed class JourneyLeg(val departureAt: UtcDateTime, val arrivalAt: UtcDateTime) {
    abstract val from: GeoPoint
    abstract val to: GeoPoint
    abstract val geometry: List<GeoPoint>

    val durationMinutes: Double
        get() = (arrivalAt.epochMicros - departureAt.epochMicros).toDouble() / 60_000_000.0
}

class WalkLeg(
    override val from: GeoPoint,
    override val to: GeoPoint,
    val meters: Double,
    departureAt: UtcDateTime,
    arrivalAt: UtcDateTime,
) : JourneyLeg(departureAt, arrivalAt) {
    /** Geometría en línea recta estimada, no un camino peatonal validado. */
    override val geometry: List<GeoPoint> = listOf(from, to)
}

class TransitLeg(
    val lineId: String,
    val lineName: String,
    val color: String,
    val patternId: String,
    val direction: String,
    val headsign: String,
    val boardingStop: CityStop,
    val alightingStop: CityStop,
    intermediateStops: List<CityStop>,
    val boardingAt: UtcDateTime,
    val expectedWaitMinutes: Double,
    val fare: Money,
    geometry: List<GeoPoint>,
    departureAt: UtcDateTime,
    arrivalAt: UtcDateTime,
) : JourneyLeg(departureAt, arrivalAt) {
    val intermediateStops: List<CityStop> = intermediateStops.toList()

    val waitingForBandMinutes: Double
        get() = max(0.0, waitMinutes - expectedWaitMinutes)

    /** Paraderos después de abordar, incluido el de bajada. */
    val stopCount: Int get() = intermediateStops.size + 1

    val waitMinutes: Double
        get() = (boardingAt.epochMicros - departureAt.epochMicros).toDouble() / 60_000_000.0

    val vehicleMinutes: Double
        get() = (arrivalAt.epochMicros - boardingAt.epochMicros).toDouble() / 60_000_000.0

    override val from: GeoPoint get() = GeoPoint.stop(boardingStop)
    override val to: GeoPoint get() = GeoPoint.stop(alightingStop)
    override val geometry: List<GeoPoint> = geometry.toList()
}

class Itinerary(
    val departureAt: UtcDateTime,
    val arrivalAt: UtcDateTime,
    legs: List<JourneyLeg>,
) {
    /** Instantes absolutos en UTC. El reloj de la ciudad es America/Lima (UTC-5). */
    val legs: List<JourneyLeg> = legs.toList()

    val durationMinutes: Double
        get() = (arrivalAt.epochMicros - departureAt.epochMicros).toDouble() / 60_000_000.0

    val walkingMinutes: Double
        get() = legs.filterIsInstance<WalkLeg>().sumOf { it.durationMinutes }

    val walkingMeters: Double
        get() = legs.filterIsInstance<WalkLeg>().sumOf { it.meters }

    val waitingMinutes: Double
        get() = legs.filterIsInstance<TransitLeg>().sumOf { it.waitMinutes }

    val vehicleMinutes: Double
        get() = legs.filterIsInstance<TransitLeg>().sumOf { it.vehicleMinutes }

    val boardings: Int
        get() = legs.count { it is TransitLeg }

    val transfers: Int
        get() = max(0, boardings - 1)

    val currency: String
        get() = legs.filterIsInstance<TransitLeg>().firstOrNull()?.fare?.currency ?: "PEN"

    val estimatedFare: Money
        get() {
            var total = Money(0L, currency)
            for (leg in legs) {
                if (leg is TransitLeg) total += leg.fare
            }
            return total
        }

    override fun toString(): String =
        "Itinerary(departure=$departureAt, arrival=$arrivalAt, boardings=$boardings, " +
            "walk=$walkingMeters, fare=$estimatedFare)"
}
