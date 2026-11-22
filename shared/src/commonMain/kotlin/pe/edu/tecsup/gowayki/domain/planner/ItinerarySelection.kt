package pe.edu.tecsup.gowayki.domain.planner

enum class JourneyOrder { Fastest, FewestTransfers, LeastWalking }

fun lineCombination(trip: Itinerary): String = trip.legs.filterIsInstance<TransitLeg>().joinToString(separator = "") {
    "${it.lineId.length}:${it.lineId}"
}

fun itineraryDominates(a: Itinerary, b: Itinerary): Boolean =
    a.durationMinutes <= b.durationMinutes + 0.000001 &&
        a.transfers <= b.transfers &&
        a.walkingMeters <= b.walkingMeters + 0.000001 &&
        (a.durationMinutes < b.durationMinutes - 0.000001 ||
            a.transfers < b.transfers ||
            a.walkingMeters < b.walkingMeters - 0.000001)

fun selectItineraries(
    input: Iterable<Itinerary>,
    order: JourneyOrder,
    limit: Int = 5,
): List<Itinerary> {
    if (limit !in 1..5) throw IllegalArgumentException("limit")
    val pareto = mutableListOf<Itinerary>()
    for (trip in input) {
        if (pareto.any { old -> itineraryDominates(old, trip) }) continue
        pareto.removeAll { old -> itineraryDominates(trip, old) }
        pareto.add(trip)
    }
    fun compare(a: Itinerary, b: Itinerary): Int {
        val duration = a.durationMinutes.compareTo(b.durationMinutes)
        val transfers = a.transfers.compareTo(b.transfers)
        val walking = a.walkingMeters.compareTo(b.walkingMeters)
        val criteria = when (order) {
            JourneyOrder.Fastest -> listOf(duration, transfers, walking)
            JourneyOrder.FewestTransfers -> listOf(transfers, duration, walking)
            JourneyOrder.LeastWalking -> listOf(walking, transfers, duration)
        }
        for (comparison in criteria) {
            if (comparison != 0) return comparison
        }
        return lineCombination(a).compareTo(lineCombination(b))
    }
    pareto.sortWith(::compare)
    val seen = HashSet<String>()
    return pareto.asSequence().filter { seen.add(lineCombination(it)) }.take(limit).toList()
}
