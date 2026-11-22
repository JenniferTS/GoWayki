package pe.edu.tecsup.gowayki.domain.tracking

import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TripTrackerTest {
    private val epoch = UtcDateTime.of(2026, 11, 7, 15)

    private fun point(meters: Double) = GeoPoint(meters / 111195.08, 0.0)

    private fun stop(name: String, meters: Double) = CityStop(
        id = name,
        name = name,
        latitude = point(meters).latitude,
        longitude = 0.0,
        sourceKind = "test",
        nameSource = "test",
        needsReview = false,
        usedInPatterns = true,
    )

    private fun bus(
        from: Double = 0.0,
        to: Double = 1000.0,
        stops: List<Double> = listOf(300.0, 900.0),
        geometry: List<GeoPoint>? = null,
    ) = TransitLeg(
        lineId = "test",
        lineName = "Test",
        color = "#ff5500",
        patternId = "test",
        direction = "0",
        headsign = "Test",
        boardingStop = stop("A", from),
        alightingStop = stop("B", to),
        intermediateStops = stops.map { stop("$it", it) },
        boardingAt = epoch,
        expectedWaitMinutes = 3.0,
        fare = Money.BASE,
        geometry = geometry ?: listOf(point(from), point(to)),
        departureAt = epoch,
        arrivalAt = UtcDateTime(epoch.epochMicros + 10L * 60_000_000L),
    )

    private fun walk(from: Double, to: Double) = WalkLeg(
        from = point(from),
        to = point(to),
        meters = kotlin.math.abs(to - from),
        departureAt = epoch,
        arrivalAt = UtcDateTime(epoch.epochMicros + 2L * 60_000_000L),
    )

    private fun itinerary(legs: List<pe.edu.tecsup.gowayki.domain.planner.JourneyLeg>) = Itinerary(
        departureAt = epoch,
        arrivalAt = UtcDateTime(epoch.epochMicros + 20L * 60_000_000L),
        legs = legs,
    )

    private fun fix(meters: Double, seconds: Int, accuracy: Double = 5.0) = PositionFix(
        point = point(meters),
        accuracyMeters = accuracy,
        at = UtcDateTime(epoch.epochMicros + seconds * 1_000_000L),
    )

    private fun feed(tracker: TripTracker, meters: Double, seconds: Int, accuracy: Double = 5.0) =
        tracker.update(
            fix(meters, seconds, accuracy),
            now = UtcDateTime(epoch.epochMicros + seconds * 1_000_000L),
        )

    @Test
    fun walkingNearRouteDoesNotArmAlightUntilBoard() {
        val tracker = TripTracker(itinerary(listOf(bus())))
        assertEquals(TripPhase.WaitingToBoard, feed(tracker, 650.0, 0).phase)
        assertTrue(feed(tracker, 950.0, 5).alerts.isEmpty())
        assertEquals(TripPhase.Riding, tracker.confirmStep().phase)
    }

    @Test
    fun threshold400mEmitsOnce() {
        val tracker = TripTracker(itinerary(listOf(bus()))).also { it.confirmStep() }
        assertTrue(feed(tracker, 200.0, 0).alerts.isEmpty())
        assertTrue(feed(tracker, 590.0, 20).alerts.isEmpty())
        val near = feed(tracker, 610.0, 25)
        assertEquals(TripAlertKind.Alight, near.alerts.single().kind)
        assertTrue(feed(tracker, 650.0, 30).alerts.isEmpty())
    }

    @Test
    fun finalWalkingCompletes() {
        val tracker = TripTracker(itinerary(listOf(walk(0.0, 200.0))))
        assertEquals(TripPhase.Walking, feed(tracker, 0.0, 0).phase)
        assertEquals(TripPhase.Completed, feed(tracker, 170.0, 20).phase)
        assertEquals(TripPhase.Completed, feed(tracker, 0.0, 30).phase)
    }

    @Test
    fun manualConfirmAdvancesWithoutGps() {
        val tracker = TripTracker(itinerary(listOf(walk(0.0, 100.0), bus(), walk(1000.0, 1100.0))))
        assertEquals(TripPhase.WaitingToBoard, tracker.confirmStep().phase)
        assertEquals(TripPhase.Riding, tracker.confirmStep().phase)
        assertEquals(TripPhase.Walking, tracker.confirmStep().phase)
        assertEquals(TripPhase.Completed, tracker.confirmStep().phase)
    }
}
