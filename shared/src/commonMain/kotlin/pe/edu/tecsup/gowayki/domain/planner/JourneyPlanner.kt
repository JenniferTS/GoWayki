package pe.edu.tecsup.gowayki.domain.planner

enum class JourneyTimeMode { LeaveNow, LeaveAt, ArriveBy }

enum class JourneyStatus { Success, NoService, NoRoute }

class JourneyTime private constructor(val mode: JourneyTimeMode, val at: UtcDateTime?) {
    companion object {
        fun now(): JourneyTime = JourneyTime(JourneyTimeMode.LeaveNow, null)

        fun leaveAt(time: UtcDateTime): JourneyTime = JourneyTime(JourneyTimeMode.LeaveAt, time)

        fun arriveBy(time: UtcDateTime): JourneyTime = JourneyTime(JourneyTimeMode.ArriveBy, time)
    }
}

class JourneyRequest(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val time: JourneyTime,
    val maxBoardings: Int = 3,
    val order: JourneyOrder = JourneyOrder.Fastest,
) {
    init {
        if (maxBoardings !in 1..3) throw IllegalArgumentException("maxBoardings")
    }
}

class JourneyResponse(
    val status: JourneyStatus,
    itineraries: List<Itinerary>,
    val referenceTime: UtcDateTime,
    val accessRadiusMeters: Double,
    val walkingAlternative: Itinerary? = null,
) {
    val itineraries: List<Itinerary> = itineraries.toList()
}

class JourneyPlanner(val network: PlannerNetwork) {
    private val forward = RaptorPlanner(network)

    fun plan(request: JourneyRequest, now: UtcDateTime? = null): JourneyResponse {
        val result = transport(request, now)
        val meters = request.origin.distanceTo(request.destination) * network.settings.walkDistanceFactor
        val walk = if (meters < 2500.0) {
            val duration = meters / network.settings.walkMetersPerMinute
            val departure = if (request.time.mode == JourneyTimeMode.ArriveBy) {
                addMinutes(result.referenceTime, -duration)
            } else {
                result.referenceTime
            }
            val arrival = addMinutes(departure, duration)
            Itinerary(
                departureAt = departure,
                arrivalAt = arrival,
                legs = listOf(
                    WalkLeg(
                        from = request.origin,
                        to = request.destination,
                        meters = meters,
                        departureAt = departure,
                        arrivalAt = arrival,
                    ),
                ),
            )
        } else {
            null
        }
        return JourneyResponse(
            status = result.status,
            itineraries = selectItineraries(result.itineraries, request.order),
            referenceTime = result.referenceTime,
            accessRadiusMeters = result.accessRadiusMeters,
            walkingAlternative = walk,
        )
    }

    private fun transport(request: JourneyRequest, now: UtcDateTime?): JourneyResponse {
        val reference = request.time.at ?: now ?: UtcDateTime.now()
        if (request.time.mode == JourneyTimeMode.ArriveBy) return arriveBy(request, reference)
        if (network.settings.frequencyAt(reference) == null) {
            return JourneyResponse(
                status = JourneyStatus.NoService,
                itineraries = emptyList(),
                referenceTime = reference,
                accessRadiusMeters = network.settings.accessMeters,
            )
        }
        val result = forward.plan(query(request, reference))
        return JourneyResponse(
            status = if (result.itineraries.isEmpty()) JourneyStatus.NoRoute else JourneyStatus.Success,
            itineraries = result.itineraries,
            referenceTime = reference,
            accessRadiusMeters = result.accessRadiusMeters,
        )
    }

    private fun arriveBy(request: JourneyRequest, deadline: UtcDateTime): JourneyResponse {
        val radii = LinkedHashSet<Double>()
        radii.add(network.settings.accessMeters)
        radii.add(network.settings.fallbackMeters)
        for (radius in radii) {
            val departure = latestDeparture(
                network,
                request.origin,
                request.destination,
                deadline,
                radius,
                request.maxBoardings,
            ) ?: continue
            var safe = departure.truncatedToWholeSeconds()
            for (roundingAttempt in 0 until 2) {
                val forwardResult = forward.plan(query(request, safe), accessRadiusMeters = radius)
                val valid = forwardResult.itineraries.filter { !it.arrivalAt.isAfter(deadline) }
                if (valid.isNotEmpty()) {
                    return JourneyResponse(
                        status = JourneyStatus.Success,
                        itineraries = valid,
                        referenceTime = deadline,
                        accessRadiusMeters = radius,
                    )
                }
                safe -= PlannerDuration.of(seconds = 1)
            }
        }
        return JourneyResponse(
            status = if (network.settings.frequencyAt(deadline) == null) {
                JourneyStatus.NoService
            } else {
                JourneyStatus.NoRoute
            },
            itineraries = emptyList(),
            referenceTime = deadline,
            accessRadiusMeters = network.settings.fallbackMeters,
        )
    }

    private fun query(request: JourneyRequest, departure: UtcDateTime): PlannerQuery = PlannerQuery(
        origin = request.origin,
        destination = request.destination,
        departureAt = departure,
        maxBoardings = request.maxBoardings,
    )
}
