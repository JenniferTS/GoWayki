package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.PI
import pe.edu.tecsup.gowayki.domain.money.Money

val metersPerDegree = 6371008.8 * PI / 180.0

fun point(meters: Double): GeoPoint = GeoPoint(0.0, meters / metersPerDegree)

fun at(hour: Int, minute: Int = 0, second: Int = 0): UtcDateTime =
    UtcDateTime.of(2026, 11, 6, hour + 5, minute, second)

fun stop(id: String, meters: Double): CityStop = CityStop(
    id = id,
    name = "Prueba $id",
    latitude = 0.0,
    longitude = meters / metersPerDegree,
    sourceKind = "gtfs_candidate",
    nameSource = "gtfs_source",
    needsReview = false,
    usedInPatterns = true,
)

fun arequipaConfig(): Map<String, Any?> = mapOf(
    "timezone" to "America/Lima",
    "frequency_minutes_by_period" to mapOf(
        "peak" to 6,
        "off_peak" to 10,
        "night" to 15,
    ),
    "speed_bands" to listOf(
        band("05:00", "07:00", 22),
        band("07:00", "09:00", 14),
        band("09:00", "12:00", 18),
        band("12:00", "14:00", 15),
        band("14:00", "17:00", 18),
        band("17:00", "20:00", 13),
        band("20:00", "23:00", 22),
        band("23:00", "05:00", null),
    ),
    "frequency_bands" to listOf(
        period("05:00", "07:00", "off_peak"),
        period("07:00", "09:00", "peak"),
        period("09:00", "17:00", "off_peak"),
        period("17:00", "20:00", "peak"),
        period("20:00", "23:00", "night"),
        period("23:00", "05:00", null),
    ),
    "walking_speed_m_per_min" to 75,
    "access_max_m" to 800,
    "access_fallback_max_m" to 1500,
)

private fun band(start: String, end: String, speedKmh: Int?): Map<String, Any?> = mapOf(
    "start" to start,
    "end" to end,
    "speed_kmh" to speedKmh,
)

private fun period(start: String, end: String, name: String?): Map<String, Any?> = mapOf(
    "start" to start,
    "end" to end,
    "period" to name,
)

fun configured(): PlannerSettings = PlannerSettings.fromConfig(arequipaConfig())

fun pattern(
    id: String,
    stops: List<String>,
    distances: List<Double>,
    all: Map<String, CityStop>,
    fare: Money = Money.BASE,
): PlannerPattern = PlannerPattern(
    id = id,
    lineId = id,
    lineName = "Línea de prueba $id",
    color = "#123456",
    direction = "ida",
    headsign = stops.last(),
    visits = stops.mapIndexed { index, stopId -> PatternVisit(stopId, distances[index]) },
    shape = PlannerShape(
        points = stops.map { stopId -> GeoPoint.stop(all.getValue(stopId)) },
        distances = distances,
    ),
    fare = fare,
)

fun toy(secondFare: Money = Money.BASE, fourth: Boolean = false): PlannerNetwork {
    val locations = linkedMapOf(
        "A" to 0.0,
        "M" to 1500.0,
        "B" to 3000.0,
        "C" to 3000.0 + 150.0 / 1.3,
        "D" to 4800.0 + 150.0 / 1.3,
        "E" to 4800.0 + 300.0 / 1.3,
        "F" to 7800.0 + 300.0 / 1.3,
    )
    if (fourth) locations["G"] = 10800.0 + 300.0 / 1.3
    val stops = locations.mapValues { (id, meters) -> stop(id, meters) }
    fun edge(id: String) = CityTransfer(toStopId = id, directMeters = 150.0 / 1.3, walkMeters = 150.0)
    val patterns = mutableListOf(
        pattern("R1", listOf("A", "M", "B"), listOf(0.0, 1500.0, 3000.0), stops),
        pattern("R2", listOf("C", "D"), listOf(0.0, 1800.0), stops, fare = secondFare),
        pattern("R3", listOf("E", "F"), listOf(0.0, 3000.0), stops),
    )
    if (fourth) patterns.add(pattern("R4", listOf("F", "G"), listOf(0.0, 3000.0), stops))
    return PlannerNetwork(
        stops = stops,
        patterns = patterns,
        transfers = mapOf(
            "B" to listOf(edge("C")),
            "C" to listOf(edge("B")),
            "D" to listOf(edge("E")),
            "E" to listOf(edge("D")),
        ),
        settings = configured(),
    )
}

fun query(
    network: PlannerNetwork,
    end: String = "F",
    entry: Double = 150.0,
    exit: Double = 150.0,
    departure: UtcDateTime? = null,
    maxBoardings: Int = 3,
): PlannerQuery = PlannerQuery(
    origin = GeoPoint(-entry / 1.3 / metersPerDegree, 0.0),
    destination = GeoPoint(exit / 1.3 / metersPerDegree, network.stops.getValue(end).longitude),
    departureAt = departure ?: at(10),
    maxBoardings = maxBoardings,
)
