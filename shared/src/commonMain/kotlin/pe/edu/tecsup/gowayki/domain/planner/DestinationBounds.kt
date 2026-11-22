package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.min

/** Minutos y metros a pie. Cada componente es optimista por separado, así que podar es seguro. */
data class OptimisticCost(val minutes: Double, val walkingMeters: Double)

private val UNREACHABLE = OptimisticCost(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

/**
 * Rondas inversas relajadas: velocidad máxima del bus, espera cero y como máximo
 * N vehículos más.
 */
class DestinationBounds(
    val network: PlannerNetwork,
    egress: Map<String, Double>,
    rounds: Int,
) {
    val rate: Double
    private val levels: List<Map<String, OptimisticCost>>
    private val suffixes = mutableMapOf<Pair<String, Int>, List<OptimisticCost>>()

    init {
        val speeds = network.settings.speeds.mapNotNull { it.value }
        rate = if (speeds.isEmpty()) {
            network.settings.walkMetersPerMinute
        } else {
            speeds.maxOrNull()!! * 1000.0 / 60.0
        }
        val built = mutableListOf<Map<String, OptimisticCost>>()
        var previous: Map<String, OptimisticCost> = egress.mapValues { (_, meters) ->
            OptimisticCost(meters / network.settings.walkMetersPerMinute, meters)
        }
        previous = walking(previous, previous)
        built.add(previous)
        for (round in 1..rounds) {
            val rides = LinkedHashMap<String, OptimisticCost>()
            for (pattern in network.patterns) {
                var minutes = Double.POSITIVE_INFINITY
                var walk = Double.POSITIVE_INFINITY
                for (i in pattern.visits.lastIndex downTo 0) {
                    val visit = pattern.visits[i]
                    if (i < pattern.visits.lastIndex) {
                        minutes += (pattern.visits[i + 1].distanceMeters - visit.distanceMeters) / rate
                        merge(rides, visit.stopId, minutes, walk)
                    }
                    val tail = previous[visit.stopId] ?: UNREACHABLE
                    minutes = min(minutes, tail.minutes)
                    walk = min(walk, tail.walkingMeters)
                }
            }
            previous = walking(rides, previous)
            built.add(previous)
        }
        levels = built
    }

    fun at(stop: String, remaining: Int): OptimisticCost = levels[remaining][stop] ?: UNREACHABLE

    fun onboard(pattern: PlannerPattern, position: Int, remaining: Int): OptimisticCost {
        val values = suffixes.getOrPut(pattern.id to remaining) {
            val result = MutableList(pattern.visits.size) { UNREACHABLE }
            var minutes = Double.POSITIVE_INFINITY
            var walk = Double.POSITIVE_INFINITY
            for (i in pattern.visits.lastIndex downTo 0) {
                if (i < pattern.visits.lastIndex) {
                    minutes +=
                        (pattern.visits[i + 1].distanceMeters - pattern.visits[i].distanceMeters) / rate
                }
                val tail = at(pattern.visits[i].stopId, remaining)
                minutes = min(minutes, tail.minutes)
                walk = min(walk, tail.walkingMeters)
                result[i] = OptimisticCost(minutes, walk)
            }
            result
        }
        return values[position]
    }

    private fun walking(
        source: Map<String, OptimisticCost>,
        carry: Map<String, OptimisticCost>,
    ): Map<String, OptimisticCost> {
        val target = LinkedHashMap(carry)
        for ((stop, cost) in source) merge(target, stop, cost.minutes, cost.walkingMeters)
        for ((fromStop, edges) in network.transfers) {
            for (edge in edges) {
                val tail = source[edge.toStopId] ?: continue
                merge(
                    target,
                    fromStop,
                    tail.minutes + edge.walkMeters / network.settings.walkMetersPerMinute,
                    tail.walkingMeters + edge.walkMeters,
                )
            }
        }
        return target
    }
}

private fun merge(
    target: MutableMap<String, OptimisticCost>,
    stop: String,
    minutes: Double,
    walk: Double,
) {
    val previous = target[stop] ?: UNREACHABLE
    if (minutes < previous.minutes || walk < previous.walkingMeters) {
        target[stop] = OptimisticCost(min(minutes, previous.minutes), min(walk, previous.walkingMeters))
    }
}
