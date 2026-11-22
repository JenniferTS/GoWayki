package pe.edu.tecsup.gowayki.domain.planner

import pe.edu.tecsup.gowayki.domain.money.Money

data class PatternVisit(val stopId: String, val distanceMeters: Double)

class PlannerShape(points: List<GeoPoint>, distances: List<Double>) {
    val points: List<GeoPoint>
    val distances: List<Double>

    init {
        if (points.size < 2 || points.size != distances.size || distances.first() != 0.0) {
            throw IllegalArgumentException("Shape inválido.")
        }
        for (i in distances.indices) {
            if (!distances[i].finite() ||
                distances[i] < 0.0 ||
                (i > 0 && distances[i] <= distances[i - 1])
            ) {
                throw IllegalArgumentException("Distancias de shape inválidas.")
            }
        }
        this.points = points.toList()
        this.distances = distances.toList()
    }

    fun clip(start: Double, end: Double): List<GeoPoint> {
        if (start < 0.0 || end < start || end > distances.last() + 0.00001) {
            throw IllegalArgumentException("Recorte fuera del shape.")
        }
        val clipped = ArrayList<GeoPoint>(points.size)
        clipped.add(pointAt(start))
        for (i in 1 until points.lastIndex) {
            if (distances[i] > start && distances[i] < end) clipped.add(points[i])
        }
        clipped.add(pointAt(end))
        return clipped
    }

    private fun pointAt(distance: Double): GeoPoint {
        if (distance <= 0.0) return points.first()
        if (distance >= distances.last()) return points.last()
        var low = 0
        var high = distances.lastIndex
        while (low + 1 < high) {
            val middle = (low + high) / 2
            if (distances[middle] <= distance) low = middle else high = middle
        }
        val t = (distance - distances[low]) / (distances[high] - distances[low])
        return GeoPoint(
            points[low].latitude + t * (points[high].latitude - points[low].latitude),
            points[low].longitude + t * (points[high].longitude - points[low].longitude),
        )
    }
}

class PlannerPattern(
    val id: String,
    val lineId: String,
    val lineName: String,
    val color: String,
    val direction: String,
    val headsign: String,
    visits: List<PatternVisit>,
    val shape: PlannerShape,
    val fare: Money,
) {
    val visits: List<PatternVisit> = visits.toList()
}

class PlannerNetwork private constructor(
    val stops: Map<String, CityStop>,
    val patterns: List<PlannerPattern>,
    val transfers: Map<String, List<CityTransfer>>,
    val settings: PlannerSettings,
    val stopPatterns: Map<String, List<Pair<Int, Int>>>,
    val incomingTransfers: Map<String, List<Pair<String, Double>>>,
) {
    fun withSettings(settings: PlannerSettings): PlannerNetwork = PlannerNetwork(
        stops,
        patterns,
        transfers,
        settings,
        stopPatterns,
        incomingTransfers,
    )

    companion object {
        operator fun invoke(
            stops: Map<String, CityStop>,
            patterns: List<PlannerPattern>,
            transfers: Map<String, List<CityTransfer>>,
            settings: PlannerSettings,
        ): PlannerNetwork {
            val stopCopy = LinkedHashMap(stops)
            val patternCopy = patterns.toList()
            val transferCopy = LinkedHashMap<String, List<CityTransfer>>()
            for ((id, edges) in transfers) transferCopy[id] = edges.toList()
            val occurrences = LinkedHashMap<String, MutableList<Pair<Int, Int>>>()
            val ids = HashSet<String>()
            for (p in patternCopy.indices) {
                val pattern = patternCopy[p]
                if (!ids.add(pattern.id) ||
                    pattern.visits.size < 2 ||
                    pattern.fare.amountMinor < 0L ||
                    pattern.fare.currency != "PEN"
                ) {
                    throw IllegalArgumentException("Patrón/tarifa inválidos.")
                }
                for (i in pattern.visits.indices) {
                    val visit = pattern.visits[i]
                    if (!stopCopy.containsKey(visit.stopId) ||
                        !visit.distanceMeters.finite() ||
                        visit.distanceMeters < 0.0 ||
                        visit.distanceMeters > pattern.shape.distances.last() + 0.00001 ||
                        (i > 0 && visit.distanceMeters < pattern.visits[i - 1].distanceMeters)
                    ) {
                        throw IllegalArgumentException("Visita inválida.")
                    }
                    occurrences.getOrPut(visit.stopId) { mutableListOf() }.add(p to i)
                }
            }
            val incoming = LinkedHashMap<String, MutableList<Pair<String, Double>>>()
            for ((origin, edges) in transferCopy) {
                if (!stopCopy.containsKey(origin)) {
                    throw IllegalArgumentException("Origen de transbordo desconocido.")
                }
                for (edge in edges) {
                    incoming.getOrPut(edge.toStopId) { mutableListOf() }.add(origin to edge.walkMeters)
                    if (!stopCopy.containsKey(edge.toStopId) ||
                        !edge.walkMeters.finite() ||
                        !edge.directMeters.finite() ||
                        edge.walkMeters < edge.directMeters ||
                        edge.directMeters < 0.0 ||
                        edge.directMeters > 400.000002 ||
                        edge.walkMeters > 520.000002
                    ) {
                        throw IllegalArgumentException("Transbordo inválido.")
                    }
                }
            }
            return PlannerNetwork(
                stopCopy,
                patternCopy,
                transferCopy,
                settings,
                occurrences.mapValues { (_, visits) -> visits.toList() },
                incoming.mapValues { (_, edges) -> edges.toList() },
            )
        }
    }
}
