package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.max

/**
 * Última salida factible. El motor directo reconstruye y valida el itinerario,
 * incluido el redondeo a microsegundos.
 */
fun latestDeparture(
    network: PlannerNetwork,
    origin: GeoPoint,
    destination: GeoPoint,
    deadline: UtcDateTime,
    radius: Double,
    maxBoardings: Int,
): UtcDateTime? {
    val settings = network.settings
    val access = LinkedHashMap<String, Double>()
    val egress = LinkedHashMap<String, Double>()
    for (stop in network.stops.values) {
        val point = GeoPoint.stop(stop)
        val entry = origin.distanceTo(point) * settings.walkDistanceFactor
        val exit = point.distanceTo(destination) * settings.walkDistanceFactor
        if (entry <= radius + 1e-7) access[stop.id] = entry
        if (exit <= radius + 1e-7) egress[stop.id] = exit
    }
    val incoming = network.incomingTransfers
    var best: UtcDateTime? = null
    for ((opening, closing) in settings.serviceWindows(deadline).asReversed()) {
        val bestNow = best
        if (bestNow != null && !closing.isAfter(bestNow)) break
        var previous = LinkedHashMap<String, UtcDateTime>()
        for ((stopId, meters) in egress) {
            val alight = addMinutes(deadline, -meters / settings.walkMetersPerMinute)
            if (!alight.isBefore(opening)) {
                previous[stopId] = if (alight.isAfter(closing)) closing else alight
            }
        }
        var round = 1
        while (round <= maxBoardings && previous.isNotEmpty()) {
            val marked = LinkedHashMap<Int, Int>()
            for (id in previous.keys) {
                for ((patternIndex, index) in network.stopPatterns[id].orEmpty()) {
                    val old = marked[patternIndex]
                    marked[patternIndex] = if (old == null) index else max(old, index)
                }
            }
            val rides = LinkedHashMap<String, UtcDateTime>()
            for ((patternIndex, start) in marked) {
                val pattern = network.patterns[patternIndex]
                var onboard: UtcDateTime? = null
                for (index in start downTo 0) {
                    val visit = pattern.visits[index]
                    val riding = onboard
                    if (riding != null) {
                        val travelled = settings.travelBackwards(
                            riding,
                            pattern.visits[index + 1].distanceMeters - visit.distanceMeters,
                        )
                        onboard = travelled
                        if (travelled != null) {
                            val ready = settings.readyBefore(travelled, opening)
                            if (ready != null) latest(rides, visit.stopId, ready)
                        }
                    }
                    val alight = previous[visit.stopId]
                    val onboardNow = onboard
                    if (alight != null && (onboardNow == null || alight.isAfter(onboardNow))) {
                        onboard = alight
                    }
                }
            }
            val next = LinkedHashMap(rides)
            for ((stopId, time) in rides) {
                for ((from, meters) in incoming[stopId].orEmpty()) {
                    val ready = addMinutes(time, -meters / settings.walkMetersPerMinute)
                    if (!ready.isBefore(opening)) latest(next, from, ready)
                }
            }
            for ((stopId, time) in next) {
                val meters = access[stopId] ?: continue
                val departure = addMinutes(time, -meters / settings.walkMetersPerMinute)
                val chosen = best
                if (!departure.isBefore(opening) && (chosen == null || departure.isAfter(chosen))) {
                    best = departure
                }
            }
            previous = next
            round++
        }
    }
    return best
}

private fun latest(labels: MutableMap<String, UtcDateTime>, id: String, time: UtcDateTime) {
    val old = labels[id]
    if (old == null || time.isAfter(old)) labels[id] = time
}
