package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JourneyTimeTest {
    @Test
    fun leaveNowUsesTheSuppliedClockAndLeaveAtUsesTheExactInstant() {
        val network = toy()
        val q = query(network)
        val planner = JourneyPlanner(network)
        fun request(time: JourneyTime) = JourneyRequest(
            origin = q.origin,
            destination = q.destination,
            time = time,
        )
        val now = planner.plan(request(JourneyTime.now()), now = at(10))
        val explicit = planner.plan(request(JourneyTime.leaveAt(at(10))), now = at(20))
        assertEquals(JourneyStatus.Success, now.status)
        assertEquals(at(10, 49), explicit.itineraries.first().arrivalAt)
        assertEquals(explicit.itineraries.first().arrivalAt, now.itineraries.first().arrivalAt)
    }

    @Test
    fun arriveByMeetsItsDeadlineAndObtainsTheLatestWholeSecondDeparture() {
        val network = toy()
        val q = query(network)
        val response = JourneyPlanner(network).plan(
            JourneyRequest(
                origin = q.origin,
                destination = q.destination,
                time = JourneyTime.arriveBy(at(10, 49)),
            ),
        )
        assertEquals(JourneyStatus.Success, response.status)
        val best = response.itineraries.first()
        assertEquals(at(10), best.departureAt)
        assertEquals(at(10, 49), best.arrivalAt)
        val later = RaptorPlanner(network).plan(
            PlannerQuery(
                origin = q.origin,
                destination = q.destination,
                departureAt = best.departureAt + PlannerDuration.of(seconds = 1),
            ),
        )
        assertTrue(later.itineraries.all { it.arrivalAt.isAfter(at(10, 49)) })
    }

    @Test
    fun reverseSearchMeetsADeadlineAcrossChangingBands() {
        val network = toy()
        val q = query(network, end = "B", departure = at(6, 50))
        val planner = JourneyPlanner(network)
        for (deadline in listOf(at(7, 10), at(9, 5), at(12, 15), at(17, 15), at(20, 15))) {
            val result = planner.plan(
                JourneyRequest(
                    origin = q.origin,
                    destination = q.destination,
                    time = JourneyTime.arriveBy(deadline),
                ),
            )
            assertEquals(JourneyStatus.Success, result.status, "deadline $deadline")
            assertTrue(result.itineraries.all { !it.arrivalAt.isAfter(deadline) })
        }
    }

    @Test
    fun nightIsNoServiceAndAnUnreachableAreaIsNoRoute() {
        val network = toy()
        val q = query(network)
        val planner = JourneyPlanner(network)
        for (time in listOf(at(23), at(3), at(4, 59))) {
            val result = planner.plan(
                JourneyRequest(
                    origin = q.origin,
                    destination = q.destination,
                    time = JourneyTime.leaveAt(time),
                ),
            )
            assertEquals(JourneyStatus.NoService, result.status)
            assertTrue(result.itineraries.isEmpty())
        }
        val unreachable = query(network, entry = 2000.0)
        val result = planner.plan(
            JourneyRequest(
                origin = unreachable.origin,
                destination = unreachable.destination,
                time = JourneyTime.leaveAt(at(10)),
            ),
        )
        assertEquals(JourneyStatus.NoRoute, result.status)
    }

    @Test
    fun arriveByAfterClosureFindsAnEarlierTrip() {
        val network = toy()
        val q = query(network, end = "B")
        val result = JourneyPlanner(network).plan(
            JourneyRequest(
                origin = q.origin,
                destination = q.destination,
                time = JourneyTime.arriveBy(at(23, 15)),
            ),
        )
        assertEquals(JourneyStatus.Success, result.status)
        assertTrue(result.itineraries.all { !it.arrivalAt.isAfter(at(23, 15)) })
        assertTrue(result.itineraries.first().departureAt.isBefore(at(23)))
        assertTrue(result.itineraries.first().vehicleMinutes > 0.0)
    }

    @Test
    fun inverseTravelMatchesForwardTravelAcrossBoundaries() {
        val settings = configured()
        for (departure in listOf(at(6, 58), at(8, 59), at(11, 59), at(19, 59))) {
            val arrival = settings.travel(departure, 3000.0) ?: error("sin llegada para $departure")
            val backwards = settings.travelBackwards(arrival, 3000.0) ?: error("sin retorno para $departure")
            assertTrue(abs(backwards.difference(departure).inWholeMicroseconds) <= 2L)
        }
    }
}
