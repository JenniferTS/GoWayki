package pe.edu.tecsup.gowayki.domain.planner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.money.Money

class RaptorPlannerTest {
    @Test
    fun threeLineHandCalculationIsFortyNineMinutes() {
        val network = toy()
        val result = RaptorPlanner(network).plan(query(network))
        assertEquals(800.0, result.accessRadiusMeters)
        val itinerary = result.itineraries.first()
        assertEquals(49.0, itinerary.durationMinutes, 0.000001)
        assertEquals(at(10, 49), itinerary.arrivalAt)
        assertEquals(8.0, itinerary.walkingMinutes, 0.000001)
        assertEquals(600.0, itinerary.walkingMeters, 0.000001)
        assertEquals(15.0, itinerary.waitingMinutes, 0.000001)
        assertEquals(26.0, itinerary.vehicleMinutes, 0.000001)
        assertEquals(Money(390L, "PEN"), itinerary.estimatedFare)
        assertEquals("PEN", itinerary.currency)
        assertEquals(2, itinerary.transfers)
        assertEquals(3, itinerary.boardings)
        val rides = itinerary.legs.filterIsInstance<TransitLeg>()
        assertEquals(listOf("R1", "R2", "R3"), rides.map { it.lineId })
        assertEquals("M", rides.first().intermediateStops.single().id)
        assertEquals(2, rides.first().stopCount)
        assertEquals(at(10, 7), rides.first().boardingAt)
        for (i in 1 until itinerary.legs.size) {
            assertEquals(itinerary.legs[i - 1].arrivalAt, itinerary.legs[i].departureAt)
        }
        assertEquals(
            itinerary.durationMinutes,
            itinerary.walkingMinutes + itinerary.waitingMinutes + itinerary.vehicleMinutes,
            0.000001,
        )
        assertEquals(Money.BASE, network.patterns[0].fare)
        assertEquals(Money(130L, "PEN"), network.patterns[2].fare)
    }

    @Test
    fun oneLineHasNoTransferAndAddsAccessEgressAndOneFare() {
        val network = toy()
        val best = RaptorPlanner(network).plan(query(network, end = "B")).itineraries.first()
        assertEquals(19.0, best.durationMinutes, 0.000001)
        assertEquals(0, best.transfers)
        assertEquals(Money(130L, "PEN"), best.estimatedFare)
        assertEquals("Línea de prueba R1", best.legs.filterIsInstance<TransitLeg>().single().lineName)
    }

    @Test
    fun neverAddsAFourthBoardingAndTwoRoundsCannotCompleteThreeLines() {
        val network = toy(fourth = true)
        assertTrue(RaptorPlanner(network).plan(query(network, maxBoardings = 2)).itineraries.isEmpty())
        assertTrue(RaptorPlanner(network).plan(query(network, end = "G")).itineraries.isEmpty())
        assertFailsWith<IllegalArgumentException> { query(network, maxBoardings = 4) }
    }

    @Test
    fun expands800To1500OnlyWhenTheFirstSearchHasNoResult() {
        val network = toy()
        val result = RaptorPlanner(network).plan(query(network, entry = 900.0))
        assertEquals(1500.0, result.accessRadiusMeters)
        assertEquals(59.0, result.itineraries.first().durationMinutes, 0.000001)
        assertTrue(RaptorPlanner(network).plan(query(network, entry = 1501.0)).itineraries.isEmpty())
    }

    @Test
    fun lineSpecificFareAndPeakWaitingSpeedAreUsed() {
        val override = Money(150L, "PEN")
        val network = toy(secondFare = override)
        assertEquals(override, network.patterns[1].fare)
        assertEquals(Money.BASE, network.patterns[0].fare)
        assertEquals(
            Money(410L, "PEN"),
            RaptorPlanner(network).plan(query(network)).itineraries.first().estimatedFare,
        )
        val peak = RaptorPlanner(network).plan(
            query(network, end = "B", departure = at(8)),
        ).itineraries.first()
        assertEquals(3.0, peak.waitingMinutes, 0.000001)
        assertEquals(3000.0 / (14000.0 / 60.0), peak.vehicleMinutes, 0.000001)
        assertEquals(4.0 + 3.0 + 3000.0 / (14000.0 / 60.0), peak.durationMinutes, 0.000001)
    }

    @Test
    fun approvedHeadwaysBoundariesAndUtcClockConversion() {
        val settings = configured()
        val cases = listOf(
            Triple(5, 0, 10.0),
            Triple(6, 59, 10.0),
            Triple(7, 0, 6.0),
            Triple(9, 0, 10.0),
            Triple(12, 0, 10.0),
            Triple(17, 0, 6.0),
            Triple(20, 0, 15.0),
            Triple(23, 0, null),
            Triple(4, 59, null),
        )
        for ((hour, minute, headway) in cases) {
            assertEquals(headway, settings.frequencyAt(at(hour, minute)))
        }
        assertEquals(15.0, settings.speedAt(at(12)) ?: error("sin velocidad"))
        assertEquals(10, PlannerSettings.cityClock(at(10)).hour)
        assertNull(settings.boardAt(at(22, 59)))
    }

    @Test
    fun travelIntegratesChangingSpeedWithinASegment() {
        val settings = configured()
        val end = settings.travel(at(6, 58), 1000.0)
        assertNotNull(end)
        val expected = 2.0 + (1000.0 - 2.0 * 22000.0 / 60.0) / (14000.0 / 60.0)
        assertEquals(
            expected,
            end.difference(at(6, 58)).inWholeMicroseconds.toDouble() / 60_000_000.0,
            0.000001,
        )
        assertNull(settings.travel(at(22, 59), 10000.0))
    }

    @Test
    fun earlierArrivalCanHoldForABetterHeadway() {
        val stops = mapOf(
            "O" to stop("O", 0.0),
            "X" to stop("X", 4000.0),
            "Y" to stop("Y", 5000.0),
        )
        val base = configured()
        val settings = PlannerSettings(
            speeds = listOf(TimeBand(300, 1380, 60.0), TimeBand(1380, 300, null)),
            frequencies = base.frequencies,
        )
        val network = PlannerNetwork(
            stops = stops,
            patterns = listOf(
                pattern("early", listOf("O", "X"), listOf(0.0, 4000.0), stops),
                pattern("later", listOf("O", "X"), listOf(0.0, 5000.0), stops),
                pattern("connection", listOf("X", "Y"), listOf(0.0, 1000.0), stops),
            ),
            transfers = emptyMap(),
            settings = settings,
        )
        val result = RaptorPlanner(network).plan(
            PlannerQuery(origin = point(0.0), destination = point(5000.0), departureAt = at(6, 50)),
        )
        val best = result.itineraries.first()
        assertEquals(at(7, 4), best.arrivalAt)
        assertEquals(listOf("early", "connection"), best.legs.filterIsInstance<TransitLeg>().map { it.lineId })
    }

    @Test
    fun boardingTimesRemainMonotoneAcrossAHeadwayDrop() {
        val settings = configured()
        val estimate = settings.boarding(at(6, 59))
        assertNotNull(estimate)
        assertEquals(at(7, 3), estimate.at)
        assertEquals(3.0, estimate.expectedWaitMinutes)
        var previous: UtcDateTime? = null
        for (second in -180..180) {
            val ready = at(7) + PlannerDuration.of(seconds = second)
            val departure = settings.boardAt(ready)
            assertNotNull(departure)
            val prior = previous
            if (prior != null) assertTrue(!departure.isBefore(prior))
            previous = departure
        }
    }

    @Test
    fun loopVisitsPreserveOrderAndClippedGeometry() {
        val stops = mapOf(
            "A" to stop("A", 0.0),
            "M" to stop("M", 1000.0),
            "B" to stop("B", 3000.0),
        )
        val network = PlannerNetwork(
            stops = stops,
            patterns = listOf(
                pattern("loop", listOf("A", "M", "A", "B"), listOf(0.0, 1000.0, 2000.0, 5000.0), stops),
            ),
            transfers = emptyMap(),
            settings = configured(),
        )
        val best = RaptorPlanner(network).plan(
            PlannerQuery(origin = point(1000.0), destination = point(3000.0), departureAt = at(10)),
        ).itineraries.first()
        val ride = best.legs.filterIsInstance<TransitLeg>().single()
        assertEquals("M", ride.boardingStop.id)
        assertEquals("A", ride.intermediateStops.single().id)
        assertEquals(
            listOf(point(1000.0).longitude, 0.0, point(3000.0).longitude),
            ride.geometry.map { it.longitude },
        )
        assertEquals(4000.0 / 300.0, ride.vehicleMinutes, 0.000001)
    }

    @Test
    fun shapeClippingInterpolatesOriginalChainages() {
        val shape = PlannerShape(
            points = listOf(point(0.0), point(1000.0), point(3000.0)),
            distances = listOf(0.0, 1000.0, 3000.0),
        )
        val clipped = shape.clip(500.0, 2000.0)
        assertEquals(
            listOf(point(500.0).longitude, point(1000.0).longitude, point(2000.0).longitude),
            clipped.map { it.longitude },
        )
        assertFailsWith<IllegalArgumentException> { shape.clip(-1.0, 500.0) }
        assertFailsWith<IllegalArgumentException> {
            PlannerShape(points = listOf(point(0.0), point(1.0)), distances = listOf(0.0, 0.0))
        }
    }

    @Test
    fun unknownPointsInvalidWalkingAndOverlappingBandsFail() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(Double.NaN, 0.0) }
        val base = configured()
        assertFailsWith<IllegalArgumentException> {
            PlannerSettings(
                speeds = base.speeds,
                frequencies = base.frequencies,
                walkMetersPerMinute = 0.0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlannerSettings(
                speeds = listOf(TimeBand(300, 1380, 22.0)),
                frequencies = base.frequencies,
            )
        }
        val config = arequipaConfig().toMutableMap()
        config.remove("frequency_bands")
        assertFailsWith<IllegalArgumentException> { PlannerSettings.fromConfig(config) }
    }
}
