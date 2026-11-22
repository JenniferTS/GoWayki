package pe.edu.tecsup.gowayki.domain.planner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.money.Money

private fun synthetic(id: String, minutes: Double, meters: Double, boardings: Int = 1): Itinerary {
    val a = stop("a", 0.0)
    val b = stop("b", 1000.0)
    val start = at(10)
    var time = addMinutes(start, meters / 75.0)
    val legs = mutableListOf<JourneyLeg>(
        WalkLeg(
            from = point(0.0),
            to = point(0.0),
            meters = meters,
            departureAt = start,
            arrivalAt = time,
        ),
    )
    val vehicle = (minutes - meters / 75.0 - boardings * 3.0) / boardings
    for (i in 0 until boardings) {
        val boarding = addMinutes(time, 3.0)
        val end = addMinutes(boarding, vehicle)
        legs.add(
            TransitLeg(
                lineId = "$id-$i",
                lineName = id,
                color = "#123456",
                patternId = "$id-$i",
                direction = "ida",
                headsign = "b",
                boardingStop = a,
                alightingStop = b,
                intermediateStops = emptyList(),
                boardingAt = boarding,
                expectedWaitMinutes = 3.0,
                fare = Money(100L, "PEN"),
                geometry = listOf(point(0.0), point(1000.0)),
                departureAt = time,
                arrivalAt = end,
            ),
        )
        time = end
    }
    return Itinerary(departureAt = start, arrivalAt = time, legs = legs)
}

class ItinerarySelectionTest {
    @Test
    fun keepsTradeoffsRemovesDominatedRoutesAndSortsByPreference() {
        val stops = mapOf(
            "A" to stop("A", 0.0),
            "P" to stop("P", 500.0),
            "B" to stop("B", 1000.0),
        )
        val slow = pattern("slow", listOf("A", "B"), listOf(0.0, 5000.0), stops)
        val fast = pattern("fast", listOf("P", "B"), listOf(0.0, 500.0), stops)
        val dominated = pattern("dominated", listOf("P", "B"), listOf(0.0, 600.0), stops)
        val duplicate = PlannerPattern(
            id = "duplicate",
            lineId = fast.lineId,
            lineName = fast.lineName,
            color = fast.color,
            direction = fast.direction,
            headsign = fast.headsign,
            visits = fast.visits,
            shape = fast.shape,
            fare = fast.fare,
        )
        val network = PlannerNetwork(
            stops = stops,
            patterns = listOf(slow, fast, dominated, duplicate),
            transfers = emptyMap(),
            settings = configured(),
        )
        fun request(order: JourneyOrder) = JourneyRequest(
            origin = point(0.0),
            destination = point(1000.0),
            time = JourneyTime.leaveAt(at(10)),
            order = order,
        )
        val planner = JourneyPlanner(network)
        val fastest = planner.plan(request(JourneyOrder.Fastest))
        assertEquals(2, fastest.itineraries.size)
        assertEquals("fast", fastest.itineraries.first().legs.filterIsInstance<TransitLeg>().single().lineId)
        assertEquals(
            "slow",
            planner.plan(request(JourneyOrder.LeastWalking))
                .itineraries
                .first()
                .legs
                .filterIsInstance<TransitLeg>()
                .single()
                .lineId,
        )
        assertFalse(
            fastest.itineraries.any { trip ->
                trip.legs.filterIsInstance<TransitLeg>().any { it.lineId == "dominated" }
            },
        )
        val walk = fastest.walkingAlternative
        assertNotNull(walk)
        assertEquals(1300.0, walk.walkingMeters, 0.000001)
        assertEquals(Money.ZERO_PEN, walk.estimatedFare)
        assertEquals("PEN", walk.currency)
    }

    @Test
    fun preservesASlowerLowWalkingLabelForALaterConnection() {
        val stops = mapOf(
            "A" to stop("A", 0.0),
            "P" to stop("P", 500.0),
            "X" to stop("X", 3000.0),
            "Y" to stop("Y", 5000.0),
        )
        val network = PlannerNetwork(
            stops = stops,
            patterns = listOf(
                pattern("lowWalk", listOf("A", "X"), listOf(0.0, 9000.0), stops),
                pattern("fast", listOf("P", "X"), listOf(0.0, 2500.0), stops),
                pattern("last", listOf("X", "Y"), listOf(0.0, 2000.0), stops),
            ),
            transfers = emptyMap(),
            settings = configured(),
        )
        val result = RaptorPlanner(network).plan(
            PlannerQuery(origin = point(0.0), destination = point(5000.0), departureAt = at(10)),
        )
        assertEquals(2, result.itineraries.size)
        assertTrue(result.itineraries.any { it.walkingMeters < 0.000001 })
        assertTrue(result.itineraries.any { it.walkingMeters > 600.0 })
        assertTrue(result.itineraries.all { it.transfers == 1 })
    }

    @Test
    fun returnsUpToFiveUniqueLineCombinations() {
        val choices = (0 until 7).map { index ->
            synthetic("route$index", 60.0 - index * 2.0, 100.0 + index * 100.0)
        } + listOf(synthetic("dominated", 80.0, 1000.0), synthetic("route0", 60.0, 100.0))
        val selected = selectItineraries(choices, JourneyOrder.Fastest)
        assertEquals(5, selected.size)
        assertEquals(5, selected.map(::lineCombination).toSet().size)
        for (chosen in selected) {
            assertFalse(choices.any { other -> itineraryDominates(other, chosen) })
        }
        assertEquals(48.0, selected.first().durationMinutes, 0.000001)
        assertEquals(100.0, selectItineraries(choices, JourneyOrder.LeastWalking).first().walkingMeters)
        assertFailsWith<IllegalArgumentException> {
            selectItineraries(choices, JourneyOrder.Fastest, limit = 6)
        }
    }

    @Test
    fun fewestTransfersUsesItsOwnPrimaryOrderingCriterion() {
        val direct = synthetic("direct", 60.0, 500.0)
        val change = synthetic("change", 40.0, 100.0, boardings = 2)
        val selected = selectItineraries(listOf(change, direct), JourneyOrder.FewestTransfers)
        assertEquals(0, selected.first().transfers)
        assertEquals(1, selectItineraries(listOf(change, direct), JourneyOrder.Fastest).first().transfers)
    }

    @Test
    fun walkingRemainsAvailableAtNightAndArriveByWalkingMeetsTheDeadline() {
        val network = toy()
        val planner = JourneyPlanner(network)
        val night = planner.plan(
            JourneyRequest(
                origin = point(0.0),
                destination = point(1000.0),
                time = JourneyTime.leaveAt(at(23)),
            ),
        )
        assertEquals(JourneyStatus.NoService, night.status)
        assertTrue(night.itineraries.isEmpty())
        assertEquals(0, night.walkingAlternative!!.transfers)
        val arrival = planner.plan(
            JourneyRequest(
                origin = point(0.0),
                destination = point(1000.0),
                time = JourneyTime.arriveBy(at(10)),
            ),
        )
        assertEquals(at(10), arrival.walkingAlternative!!.arrivalAt)
        val long = planner.plan(
            JourneyRequest(
                origin = point(0.0),
                destination = point(3000.0),
                time = JourneyTime.leaveAt(at(10)),
            ),
        )
        assertNull(long.walkingAlternative)
    }
}
