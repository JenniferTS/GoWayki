package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DestinationBoundsTest {
    @Test
    fun destinationBoundsPreserveUnprunedParetoScores() {
        val random = Random(23)
        for (sample in 0 until 20) {
            val stops = (0 until 8).associate { index -> "s$index" to stop("s$index", index * 300.0) }
            val patterns = mutableListOf<PlannerPattern>()
            for (route in 0 until 6) {
                val ids = mutableListOf("s0")
                val distances = mutableListOf(0.0)
                var index = 0
                var cumulative = 0.0
                while (index < 7) {
                    val next = min(7, index + 1 + random.nextInt(3))
                    cumulative += (next - index) * 300.0 * (1.0 + random.nextInt(4) * 0.15)
                    index = next
                    ids.add("s$index")
                    distances.add(cumulative)
                }
                patterns.add(pattern("r$route", ids, distances, stops))
            }
            val transfers = LinkedHashMap<String, MutableList<CityTransfer>>()
            for (i in 0 until 7) {
                transfers.getOrPut("s$i") { mutableListOf() }.add(
                    CityTransfer(toStopId = "s${i + 1}", directMeters = 300.0, walkMeters = 390.0),
                )
                transfers.getOrPut("s${i + 1}") { mutableListOf() }.add(
                    CityTransfer(toStopId = "s$i", directMeters = 300.0, walkMeters = 390.0),
                )
            }
            val network = PlannerNetwork(
                stops = stops,
                patterns = patterns,
                transfers = transfers.mapValues { (_, edges) -> edges.toList() },
                settings = configured(),
            )
            val q = PlannerQuery(
                origin = GeoPoint(-10.0 / 1.3 / metersPerDegree, 0.0),
                destination = GeoPoint(40.0 / 1.3 / metersPerDegree, point(2100.0).longitude),
                departureAt = if (sample % 2 == 0) at(10) else at(6, 58),
            )
            val reference = RaptorPlanner(network, useDestinationBounds = false).plan(q)
            val bounded = RaptorPlanner(network).plan(q)
            for (order in JourneyOrder.values()) {
                fun scores(choices: Iterable<Itinerary>): List<List<Any>> =
                    selectItineraries(choices, order).map { trip ->
                        listOf<Any>(trip.durationMinutes, trip.walkingMeters, trip.transfers)
                    }
                assertEquals(
                    scores(reference.itineraries),
                    scores(bounded.itineraries),
                    "seed23 sample $sample / $order",
                )
            }
        }
    }

    @Test
    fun retainsFiveDistinctCombinationsWhenMetricsMatch() {
        val stops = mapOf("A" to stop("A", 0.0), "B" to stop("B", 3000.0))
        val network = PlannerNetwork(
            stops = stops,
            patterns = (0 until 7).map { index ->
                pattern("equal$index", listOf("A", "B"), listOf(0.0, 3000.0), stops)
            },
            transfers = emptyMap(),
            settings = configured(),
        )
        val result = RaptorPlanner(network).plan(
            PlannerQuery(origin = point(0.0), destination = point(3000.0), departureAt = at(10)),
        )
        val selected = selectItineraries(result.itineraries, JourneyOrder.Fastest)
        assertEquals(5, selected.size)
        assertEquals(5, selected.map(::lineCombination).toSet().size)
        assertTrue(selected.all { it.durationMinutes == 15.0 && it.walkingMeters == 0.0 })
    }

    @Test
    fun inverseWaitingStaysStrictlyBeforeThe23BoardingClosure() {
        val ready = configured().readyBefore(at(23), at(5))
        assertEquals(at(22, 52, 30) - PlannerDuration.of(microseconds = 1), ready)
        val boarded = configured().boardAt(ready!!)
        assertTrue(boarded != null && boarded.isBefore(at(23)))
    }
}
