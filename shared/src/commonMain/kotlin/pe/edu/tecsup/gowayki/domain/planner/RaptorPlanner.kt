package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.TimeSource

private const val EPSILON = 0.000001

class PlannerQuery(
    val origin: GeoPoint,
    val destination: GeoPoint,
    departureAt: UtcDateTime,
    val maxBoardings: Int = 3,
) {
    val departureAt: UtcDateTime = departureAt

    init {
        if (maxBoardings !in 1..3) throw IllegalArgumentException("maxBoardings")
    }
}

class PlannerResult(
    itineraries: List<Itinerary>,
    val accessRadiusMeters: Double,
) {
    /** Candidatos de transporte no dominados, antes de la selección de combinaciones. */
    val itineraries: List<Itinerary> = itineraries.toList()
}

/**
 * Escaneo por rondas con varias etiquetas: llegada, caminata y abordajes.
 * La envolvente de abordaje y las velocidades por tramos conservan la dominancia FIFO.
 */
class RaptorPlanner(
    val network: PlannerNetwork,
    val diagnostics: MutableMap<String, Any?>? = null,
    val useDestinationBounds: Boolean = true,
) {
    private val settings: PlannerSettings get() = network.settings

    fun plan(query: PlannerQuery, accessRadiusMeters: Double? = null): PlannerResult {
        if (accessRadiusMeters != null) return search(query, accessRadiusMeters)
        var result = search(query, settings.accessMeters)
        if (result.itineraries.isEmpty() && settings.fallbackMeters > settings.accessMeters) {
            result = search(query, settings.fallbackMeters)
        }
        return result
    }

    private fun search(query: PlannerQuery, radius: Double): PlannerResult {
        val watch = if (diagnostics == null) null else MicrosWatch()
        val profiles = mutableListOf<Map<String, Any?>>()
        val access = LinkedHashMap<String, MutableList<Label>>()
        val egress = LinkedHashMap<String, Double>()
        for (stop in network.stops.values) {
            val point = GeoPoint.stop(stop)
            val exit = point.distanceTo(query.destination) * settings.walkDistanceFactor
            if (exit <= radius + 1e-7) egress[stop.id] = exit
            if (!network.stopPatterns.containsKey(stop.id)) continue
            val entry = query.origin.distanceTo(point) * settings.walkDistanceFactor
            if (entry <= radius + 1e-7) {
                val arrival = addMinutes(query.departureAt, entry / settings.walkMetersPerMinute)
                relax(
                    access,
                    Label(
                        stop.id,
                        arrival,
                        null,
                        WalkStep(
                            WalkLeg(
                                from = query.origin,
                                to = point,
                                meters = entry,
                                departureAt = query.departureAt,
                                arrivalAt = arrival,
                            ),
                        ),
                    ),
                )
            }
        }
        if (diagnostics != null) diagnostics["access_us"] = watch?.elapsedMicros()
        var previous = access
        val global = LinkedHashMap<String, MutableList<Label>>()
        for ((stopId, labels) in access) global[stopId] = labels.toMutableList()
        val finished = mutableListOf<Finished>()
        val bounds = if (useDestinationBounds) {
            DestinationBounds(network, egress, query.maxBoardings)
        } else {
            null
        }
        fun prune(time: UtcDateTime, walk: Double, boards: Int, cost: OptimisticCost?): Boolean {
            if (cost == null) return false
            if (!cost.minutes.finite() || !cost.walkingMeters.finite()) return true
            val arrival = addMinutes(time, cost.minutes) - PlannerDuration.of(milliseconds = 10)
            return finished.any { old ->
                dominates(
                    old.arrival,
                    old.walking,
                    old.label.boardings,
                    arrival,
                    walk + cost.walkingMeters,
                    boards,
                )
            }
        }
        fun pruneLabel(label: Label): Boolean = prune(
            label.arrival,
            label.walking,
            label.boardings,
            bounds?.at(label.stopId, query.maxBoardings - label.boardings),
        )
        fun pruneOnboard(state: Onboard, pattern: PlannerPattern, position: Int): Boolean = prune(
            state.time,
            state.previous.walking,
            state.previous.boardings + 1,
            bounds?.onboard(
                pattern,
                position,
                query.maxBoardings - state.previous.boardings - 1,
            ),
        )
        fun finish(label: Label) {
            val meters = egress[label.stopId] ?: return
            val candidate = Finished(
                label,
                meters,
                addMinutes(label.arrival, meters / settings.walkMetersPerMinute),
            )
            if (finished.any { old -> finishedDominates(old, candidate) || finishedEqual(old, candidate) }) return
            finished.removeAll { old -> finishedDominates(candidate, old) }
            finished.add(candidate)
        }

        var round = 1
        while (round <= query.maxBoardings && previous.isNotEmpty()) {
            watch?.reset()
            val marked = LinkedHashMap<Int, Int>()
            var maxOnboard = 0
            for (stop in previous.keys) {
                for ((patternIndex, position) in network.stopPatterns[stop].orEmpty()) {
                    val old = marked[patternIndex]
                    marked[patternIndex] = if (old == null) position else min(old, position)
                }
            }
            val boardingEstimates = LinkedHashMap<Label, BoardingEstimate?>()
            for ((stopId, labels) in previous) {
                if (!network.stopPatterns.containsKey(stopId)) continue
                for (label in labels) {
                    if (global[stopId]?.contains(label) == true) {
                        boardingEstimates[label] = settings.boarding(label.arrival)
                    }
                }
            }
            val rides = LinkedHashMap<String, MutableList<Label>>()
            for ((patternIndex, startPosition) in marked) {
                val pattern = network.patterns[patternIndex]
                var onboard = mutableListOf<Onboard>()
                for (position in startPosition until pattern.visits.size) {
                    val visit = pattern.visits[position]
                    val advanced = mutableListOf<Onboard>()
                    for (state in onboard) {
                        val arrival = settings.travel(
                            state.time,
                            visit.distanceMeters - pattern.visits[position - 1].distanceMeters,
                        ) ?: continue
                        state.time = arrival
                        if (!pruneOnboard(state, pattern, position)) insertOnboard(advanced, state)
                    }
                    onboard = advanced
                    if (diagnostics != null) maxOnboard = max(maxOnboard, onboard.size)
                    for (state in onboard) {
                        val label = Label(
                            visit.stopId,
                            state.time,
                            state.previous,
                            RideStep(
                                pattern,
                                state.boardPosition,
                                position,
                                state.boardingAt,
                                state.expectedWaitMinutes,
                                state.time,
                            ),
                        )
                        if (!pruneLabel(label) && relax(rides, label, global)) finish(label)
                    }
                    // Un abordaje solo usa la ronda anterior, nunca una llegada de esta misma ronda.
                    for (label in previous[visit.stopId].orEmpty()) {
                        val estimate = boardingEstimates[label]
                        if (estimate != null) {
                            val state = Onboard(label, position, estimate, pattern.lineId)
                            if (!pruneOnboard(state, pattern, position)) insertOnboard(onboard, state)
                        }
                    }
                }
            }
            val scanUs = watch?.elapsedMicros()
            watch?.reset()
            val next = LinkedHashMap<String, MutableList<Label>>()
            for ((stopId, labels) in rides) next[stopId] = labels.toMutableList()
            // Un transbordo a pie explícito entre vehículos, sin cadenas de caminata sin límite.
            for (labels in rides.values) {
                for (label in labels) {
                    if (global[label.stopId]?.contains(label) != true) continue
                    for (edge in network.transfers[label.stopId].orEmpty()) {
                        val arrival = addMinutes(
                            label.arrival,
                            edge.walkMeters / settings.walkMetersPerMinute,
                        )
                        val candidate = Label(
                            edge.toStopId,
                            arrival,
                            label,
                            WalkStep(
                                WalkLeg(
                                    from = GeoPoint.stop(network.stops.getValue(label.stopId)),
                                    to = GeoPoint.stop(network.stops.getValue(edge.toStopId)),
                                    meters = edge.walkMeters,
                                    departureAt = label.arrival,
                                    arrivalAt = arrival,
                                ),
                            ),
                        )
                        if (!pruneLabel(candidate) && relax(next, candidate, global)) finish(candidate)
                    }
                }
            }
            val walkUs = watch?.elapsedMicros()
            watch?.reset()
            for (labels in next.values) {
                for (label in labels) finish(label)
            }
            if (diagnostics != null) {
                profiles.add(
                    mapOf(
                        "round" to round,
                        "marked" to marked.size,
                        "previous_labels" to previous.values.sumOf { it.size },
                        "rides" to rides.values.sumOf { it.size },
                        "next_labels" to next.values.sumOf { it.size },
                        "global_labels" to global.values.sumOf { it.size },
                        "unique_scores" to global.values.sumOf { labels ->
                            labels.map { label -> scoreKey(label) }.toSet().size
                        },
                        "max_unique_scores" to global.values.fold(0) { sum, labels ->
                            max(sum, labels.map { label -> scoreKey(label) }.toSet().size)
                        },
                        "max_global_frontier" to global.values.fold(0) { sum, labels -> max(sum, labels.size) },
                        "max_onboard" to maxOnboard,
                        "scan_us" to scanUs,
                        "walking_us" to walkUs,
                        "finish_us" to watch?.elapsedMicros(),
                        "finished_candidates" to finished.size,
                    ),
                )
            }
            previous = next
            round++
        }
        finished.sortWith(compareBy { it.arrival })
        val trips = ArrayList<Itinerary>(finished.size)
        for (destination in finished) {
            val reversed = ArrayList<JourneyLeg>()
            var label: Label? = destination.label
            while (label != null) {
                reversed.add(label.step.leg(network, label.previous))
                label = label.previous
            }
            val legs = reversed.asReversed().toMutableList()
            legs.add(
                WalkLeg(
                    from = GeoPoint.stop(network.stops.getValue(destination.label.stopId)),
                    to = query.destination,
                    meters = destination.exitMeters,
                    departureAt = destination.label.arrival,
                    arrivalAt = destination.arrival,
                ),
            )
            trips.add(
                Itinerary(
                    departureAt = query.departureAt,
                    arrivalAt = destination.arrival,
                    legs = legs,
                ),
            )
        }
        if (diagnostics != null) diagnostics["rounds"] = profiles
        return PlannerResult(trips, radius)
    }
}

private fun scoreKey(label: Label): String =
    "${label.arrival.epochMicros}:${roundHalfAwayFromZero(label.walking * 1_000_000.0)}:${label.boardings}"

private fun relax(
    target: MutableMap<String, MutableList<Label>>,
    candidate: Label,
    global: MutableMap<String, MutableList<Label>>? = null,
): Boolean {
    val frontier = global?.get(candidate.stopId) ?: target[candidate.stopId] ?: emptyList()
    var equivalent = 0
    for (old in frontier) {
        if (labelDominates(old, candidate) || labelEqual(old, candidate)) return false
        if (old.arrival == candidate.arrival &&
            old.boardings == candidate.boardings &&
            abs(old.walking - candidate.walking) <= EPSILON
        ) {
            equivalent += 1
            if (equivalent >= 5) return false
        }
    }
    val labels = target.getOrPut(candidate.stopId) { mutableListOf() }
    labels.removeAll { old -> labelDominates(candidate, old) }
    labels.add(candidate)
    if (global != null) {
        val all = global.getOrPut(candidate.stopId) { mutableListOf() }
        all.removeAll { old -> labelDominates(candidate, old) }
        all.add(candidate)
    }
    return true
}

private fun dominates(
    aTime: UtcDateTime,
    aWalk: Double,
    aBoards: Int,
    bTime: UtcDateTime,
    bWalk: Double,
    bBoards: Int,
): Boolean =
    !aTime.isAfter(bTime) &&
        aWalk <= bWalk + EPSILON &&
        aBoards <= bBoards &&
        (aTime.isBefore(bTime) || aWalk < bWalk - EPSILON || aBoards < bBoards)

private fun labelDominates(a: Label, b: Label): Boolean =
    dominates(a.arrival, a.walking, a.boardings, b.arrival, b.walking, b.boardings)

private fun labelEqual(a: Label, b: Label): Boolean =
    a.arrival == b.arrival && abs(a.walking - b.walking) <= EPSILON && a.signature == b.signature

private fun finishedDominates(a: Finished, b: Finished): Boolean =
    dominates(a.arrival, a.walking, a.label.boardings, b.arrival, b.walking, b.label.boardings)

private fun finishedEqual(a: Finished, b: Finished): Boolean =
    a.arrival == b.arrival && abs(a.walking - b.walking) <= EPSILON && a.label.signature == b.label.signature

private fun insertOnboard(states: MutableList<Onboard>, candidate: Onboard) {
    fun dominates(a: Onboard, b: Onboard): Boolean = dominates(
        a.time,
        a.previous.walking,
        a.previous.boardings,
        b.time,
        b.previous.walking,
        b.previous.boardings,
    )
    var equivalent = 0
    for (old in states) {
        if (dominates(old, candidate) ||
            (old.time == candidate.time &&
                abs(old.previous.walking - candidate.previous.walking) <= EPSILON &&
                old.signature == candidate.signature)
        ) {
            return
        }
        if (old.time == candidate.time &&
            old.previous.boardings == candidate.previous.boardings &&
            abs(old.previous.walking - candidate.previous.walking) <= EPSILON
        ) {
            equivalent += 1
            if (equivalent >= 5) return
        }
    }
    states.removeAll { old -> dominates(candidate, old) }
    states.add(candidate)
}

private class MicrosWatch {
    private var mark = TimeSource.Monotonic.markNow()

    fun reset() {
        mark = TimeSource.Monotonic.markNow()
    }

    fun elapsedMicros(): Long = mark.elapsedNow().inWholeMicroseconds
}

private class Finished(val label: Label, val exitMeters: Double, val arrival: UtcDateTime) {
    val walking: Double get() = label.walking + exitMeters
}

private class Label(
    val stopId: String,
    val arrival: UtcDateTime,
    val previous: Label?,
    val step: Step,
) {
    val walking: Double = (previous?.walking ?: 0.0) + if (step is WalkStep) step.walk.meters else 0.0
    val boardings: Int = (previous?.boardings ?: 0) + if (step is RideStep) 1 else 0
    val signature: String = (previous?.signature ?: "") + if (step is RideStep) {
        "${step.pattern.lineId.length}:${step.pattern.lineId}"
    } else {
        ""
    }
}

private class Onboard(
    val previous: Label,
    val boardPosition: Int,
    estimate: BoardingEstimate,
    lineId: String,
) {
    val boardingAt: UtcDateTime = estimate.at
    val expectedWaitMinutes: Double = estimate.expectedWaitMinutes
    var time: UtcDateTime = estimate.at
    val signature: String = "${previous.signature}${lineId.length}:$lineId"
}

private sealed class Step {
    abstract fun leg(network: PlannerNetwork, previous: Label?): JourneyLeg
}

private class WalkStep(val walk: WalkLeg) : Step() {
    override fun leg(network: PlannerNetwork, previous: Label?): JourneyLeg = walk
}

private class RideStep(
    val pattern: PlannerPattern,
    val board: Int,
    val alight: Int,
    val boardingAt: UtcDateTime,
    val expectedWaitMinutes: Double,
    val arrivalAt: UtcDateTime,
) : Step() {
    override fun leg(network: PlannerNetwork, previous: Label?): JourneyLeg {
        val prior = previous ?: error("El tramo en bus no tiene etiqueta previa.")
        return TransitLeg(
            lineId = pattern.lineId,
            lineName = pattern.lineName,
            color = pattern.color,
            patternId = pattern.id,
            direction = pattern.direction,
            headsign = pattern.headsign,
            boardingStop = network.stops.getValue(pattern.visits[board].stopId),
            alightingStop = network.stops.getValue(pattern.visits[alight].stopId),
            intermediateStops = (board + 1 until alight).map { index ->
                network.stops.getValue(pattern.visits[index].stopId)
            },
            boardingAt = boardingAt,
            expectedWaitMinutes = expectedWaitMinutes,
            fare = pattern.fare,
            geometry = pattern.shape.clip(
                pattern.visits[board].distanceMeters,
                pattern.visits[alight].distanceMeters,
            ),
            departureAt = prior.arrival,
            arrivalAt = arrivalAt,
        )
    }
}
