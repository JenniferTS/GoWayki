package pe.edu.tecsup.gowayki.domain.planner

import kotlin.math.floor

private const val MINUTE_MICROS = 60_000_000L
private const val DAY_MICROS = 86_400_000_000L
private const val LIMA_OFFSET_MICROS = 18_000_000_000L

private val CLOCK_REGEX = Regex("""^(?:[01][0-9]|2[0-3]):[0-5][0-9]$""")

data class TimeBand<T>(val start: Int, val end: Int, val value: T) {
    fun contains(minute: Int): Boolean =
        if (start < end) minute in start until end else minute >= start || minute < end
}

/** Redondeo half-away-from-zero, el de `num.round()` en Dart. */
internal fun roundHalfAwayFromZero(value: Double): Long {
    if (!value.finite()) throw IllegalArgumentException("Duración inválida.")
    val negative = value < 0.0
    val absolute = if (negative) -value else value
    val whole = floor(absolute)
    val rounded = if (absolute - whole >= 0.5) whole + 1.0 else whole
    val magnitude = rounded.toLong()
    return if (negative) -magnitude else magnitude
}

fun addMinutes(time: UtcDateTime, minutes: Double): UtcDateTime =
    time + PlannerDuration.of(microseconds = roundHalfAwayFromZero(minutes * 60_000_000.0))

data class BoardingEstimate(val at: UtcDateTime, val expectedWaitMinutes: Double)

class PlannerSettings(
    speeds: List<TimeBand<Double?>>,
    frequencies: List<TimeBand<Double?>>,
    val walkMetersPerMinute: Double = 75.0,
    val accessMeters: Double = 800.0,
    val fallbackMeters: Double = 1500.0,
    val walkDistanceFactor: Double = 1.3,
) {
    val speeds: List<TimeBand<Double?>> = speeds.toList()
    val frequencies: List<TimeBand<Double?>> = frequencies.toList()
    private val speedLookup: List<TimeBand<Double?>>
    private val frequencyLookup: List<TimeBand<Double?>>

    init {
        for (value in listOf(walkMetersPerMinute, accessMeters, fallbackMeters, walkDistanceFactor)) {
            if (!value.finite() || value <= 0.0) {
                throw IllegalArgumentException("Parámetro peatonal inválido.")
            }
        }
        if (fallbackMeters < accessMeters || walkDistanceFactor < 1.0) {
            throw IllegalArgumentException("Radios/factor inválidos.")
        }
        validate(this.speeds)
        validate(this.frequencies)
        speedLookup = List(1440) { minute -> this.speeds.single { it.contains(minute) } }
        frequencyLookup = List(1440) { minute -> this.frequencies.single { it.contains(minute) } }
        for (minute in 0 until 1440) {
            if ((speedLookup[minute].value == null) != (frequencyLookup[minute].value == null)) {
                throw IllegalArgumentException("Franjas de servicio incompatibles.")
            }
        }
    }

    fun speedAt(time: UtcDateTime): Double? = speedLookup[minuteIndex(time)].value

    fun frequencyAt(time: UtcDateTime): Double? = frequencyLookup[minuteIndex(time)].value

    /**
     * Separa etiquetas de llegada entre discontinuidades de espera.
     * El día es la medianoche UTC del calendario de Lima.
     */
    fun arrivalBucket(time: UtcDateTime): Int {
        val local = cityClock(time)
        val day = civilToEpochDay(local.year, local.month, local.day)
        return (day * frequencies.size + frequencies.indexOfFirst { it.contains(minuteIndex(time)) }).toInt()
    }

    fun boardAt(ready: UtcDateTime): UtcDateTime? = boarding(ready)?.at

    /**
     * La espera esperada sigue siendo headway/2. El pasajero también puede
     * esperar un cambio de franja cercano si así aborda antes. Esa envolvente
     * FIFO evita que llegar más tarde al mismo paradero sea inherentemente mejor.
     */
    fun boarding(ready: UtcDateTime): BoardingEstimate? {
        val current = frequencyLookup[minuteIndex(ready)]
        val frequency = current.value ?: return null
        var untilChange = current.end - clockMinutes(ready)
        if (untilChange <= 0.0) untilChange += 1440.0
        if (frequency / 2.0 <= untilChange) {
            val at = addMinutes(ready, frequency / 2.0)
            return if (speedAt(at) == null) null else BoardingEstimate(at, frequency / 2.0)
        }
        val local = cityClock(ready)
        val midnight = UtcDateTime.of(local.year, local.month, local.day, 5)
        val changes = mutableListOf<UtcDateTime>()
        for (day in 0..1) {
            for (band in frequencies) {
                val change = midnight + PlannerDuration.of(days = day, minutes = band.start)
                if (change.isAfter(ready) && change.difference(ready) <= PlannerDuration.of(days = 1)) {
                    changes.add(change)
                }
            }
        }
        changes.sort()
        var closure: UtcDateTime? = null
        for (change in changes) {
            if (frequencyAt(change) == null) {
                closure = change
                break
            }
        }
        var best: BoardingEstimate? = null
        for (candidate in listOf(ready) + changes) {
            val closed = closure
            if (closed != null && !candidate.isBefore(closed)) break
            val bestNow = best
            if (bestNow != null && !candidate.isBefore(bestNow.at)) break
            val headway = frequencyAt(candidate) ?: error("Frecuencia ausente.")
            val departure = addMinutes(candidate, headway / 2.0)
            val closureAt = closure
            if ((closureAt == null || departure.isBefore(closureAt)) && speedAt(departure) != null) {
                val chosen = best
                if (chosen == null || departure.isBefore(chosen.at)) {
                    best = BoardingEstimate(departure, headway / 2.0)
                }
            }
        }
        return best
    }

    /** Intervalos de servicio de un día local, en instantes UTC de America/Lima. */
    fun serviceWindows(deadline: UtcDateTime): List<Pair<UtcDateTime, UtcDateTime>> {
        val local = cityClock(deadline)
        val result = mutableListOf<Pair<UtcDateTime, UtcDateTime>>()
        for (day in -1..0) {
            val midnight = UtcDateTime.of(local.year, local.month, local.day + day, 5)
            val active = mutableListOf<Pair<UtcDateTime, UtcDateTime>>()
            for (band in frequencies) {
                if (band.value == null) continue
                val start = midnight + PlannerDuration.of(minutes = band.start)
                val endMinutes = band.end + if (band.end <= band.start) 1440 else 0
                val end = midnight + PlannerDuration.of(minutes = endMinutes)
                if (start.isBefore(deadline)) active.add(start to end)
            }
            active.sortBy { it.first }
            for (window in active) {
                if (result.isNotEmpty() && !window.first.isAfter(result.last().second)) {
                    val previous = result.removeAt(result.lastIndex)
                    val end = if (window.second.isAfter(previous.second)) window.second else previous.second
                    result.add(previous.first to end)
                } else {
                    result.add(window)
                }
            }
        }
        return result
    }

    fun readyBefore(boardingDeadline: UtcDateTime, windowStart: UtcDateTime): UtcDateTime? {
        val current = frequencyLookup[minuteIndex(boardingDeadline)]
        val frequency = current.value
        if (frequency != null) {
            var elapsed = clockMinutes(boardingDeadline) - current.start
            if (elapsed < 0.0) elapsed += 1440.0
            if (frequency / 2.0 <= elapsed) {
                val candidate = addMinutes(boardingDeadline, -frequency / 2.0)
                if (!candidate.isBefore(windowStart)) return candidate
            }
        }
        var best: UtcDateTime? = null
        val local = cityClock(boardingDeadline)
        for (day in -1..0) {
            val midnight = UtcDateTime.of(local.year, local.month, local.day + day, 5)
            for (band in frequencies) {
                val headway = band.value ?: continue
                val start = midnight + PlannerDuration.of(minutes = band.start)
                val end = midnight + PlannerDuration.of(
                    minutes = band.end + if (band.end <= band.start) 1440 else 0,
                )
                var candidate = addMinutes(boardingDeadline, -headway / 2.0)
                if (!candidate.isBefore(end)) {
                    candidate = end - PlannerDuration.of(microseconds = 1)
                }
                if (candidate.isBefore(start) || candidate.isBefore(windowStart)) continue
                if (speedAt(addMinutes(candidate, headway / 2.0)) == null) {
                    candidate = candidate - PlannerDuration.of(microseconds = 1)
                    if (candidate.isBefore(start) || candidate.isBefore(windowStart)) continue
                }
                val board = addMinutes(candidate, headway / 2.0)
                val chosen = best
                if (speedAt(board) != null &&
                    !board.isAfter(boardingDeadline) &&
                    (chosen == null || candidate.isAfter(chosen))
                ) {
                    best = candidate
                }
            }
        }
        return best
    }

    fun travelBackwards(arrival: UtcDateTime, meters: Double): UtcDateTime? {
        if (!meters.finite() || meters < 0.0) throw IllegalArgumentException("Distancia inválida.")
        var time = arrival
        var remaining = meters
        while (remaining > 1e-8) {
            val band = speedLookup[minuteIndexFromMicros(time.epochMicros - 1)]
            val speed = band.value ?: return null
            var available = clockMinutes(time) - band.start
            if (available <= 0.0) available += 1440.0
            val rate = speed * 1000.0 / 60.0
            if (remaining <= available * rate + 1e-8) {
                return addMinutes(time, -remaining / rate)
            }
            remaining -= available * rate
            time = addMinutes(time, -available)
        }
        return time
    }

    /** Integra la distancia cruzando franjas de velocidad. No viaja con el servicio cerrado. */
    fun travel(departure: UtcDateTime, meters: Double): UtcDateTime? {
        if (!meters.finite() || meters < 0.0) throw IllegalArgumentException("Distancia inválida.")
        var time = departure
        var remaining = meters
        while (remaining > 1e-8) {
            val band = speedLookup[minuteIndex(time)]
            val speed = band.value ?: return null
            val clock = clockMinutes(time)
            var available = band.end - clock
            if (available <= 0.0) available += 1440.0
            val metersPerMinute = speed * 1000.0 / 60.0
            if (remaining <= available * metersPerMinute + 1e-8) {
                return addMinutes(time, remaining / metersPerMinute)
            }
            remaining -= available * metersPerMinute
            time = addMinutes(time, available)
        }
        return time
    }

    private fun minuteIndex(time: UtcDateTime): Int = minuteIndexFromMicros(time.epochMicros)

    private fun minuteIndexFromMicros(micros: Long): Int {
        val shifted = micros - LIMA_OFFSET_MICROS
        return ((shifted % DAY_MICROS) / MINUTE_MICROS).toInt()
    }

    private fun clockMinutes(time: UtcDateTime): Double {
        val shifted = time.epochMicros - LIMA_OFFSET_MICROS
        return (shifted % DAY_MICROS).toDouble() / MINUTE_MICROS.toDouble()
    }

    companion object {
        fun cityClock(time: UtcDateTime): UtcDateTime = time - PlannerDuration.of(hours = 5)

        fun fromConfig(config: Map<String, Any?>): PlannerSettings {
            if (config["timezone"] != "America/Lima") {
                throw IllegalArgumentException("Ciudad horaria incompatible.")
            }
            val periods = config["frequency_minutes_by_period"] as? Map<*, *>
                ?: throw IllegalArgumentException("Ciudad horaria incompatible.")
            val speedRows = config["speed_bands"] as? List<*>
                ?: throw IllegalArgumentException("Franja inválida.")
            val frequencyRows = config["frequency_bands"] as? List<*>
                ?: throw IllegalArgumentException("Faltan franjas de frecuencia: regenerar el paquete v2.")
            return PlannerSettings(
                speeds = speedRows.map { row -> speedBand(row) },
                frequencies = frequencyRows.map { row -> frequencyBand(row, periods) },
                walkMetersPerMinute = number(config["walking_speed_m_per_min"]),
                accessMeters = number(config["access_max_m"]),
                fallbackMeters = number(config["access_fallback_max_m"]),
            )
        }

        private fun speedBand(row: Any?): TimeBand<Double?> {
            val map = row as? Map<*, *> ?: throw IllegalArgumentException("Franja inválida.")
            val speed = map["speed_kmh"]
            return TimeBand(clock(map["start"]), clock(map["end"]), (speed as? Number)?.toDouble())
        }

        private fun frequencyBand(row: Any?, periods: Map<*, *>): TimeBand<Double?> {
            val map = row as? Map<*, *> ?: throw IllegalArgumentException("Franja inválida.")
            val period = map["period"]
            val value = if (period == null) {
                null
            } else {
                val raw = periods[period] ?: throw IllegalArgumentException("Franja inválida.")
                (raw as Number).toDouble()
            }
            return TimeBand(clock(map["start"]), clock(map["end"]), value)
        }

        private fun clock(value: Any?): Int {
            val text = value as? String ?: throw IllegalArgumentException("Hora inválida.")
            if (!CLOCK_REGEX.matches(text)) throw IllegalArgumentException("Hora inválida.")
            val parts = text.split(':')
            return parts[0].toInt() * 60 + parts[1].toInt()
        }

        private fun number(value: Any?): Double = (value as Number).toDouble()

        private fun validate(bands: List<TimeBand<Double?>>) {
            for (band in bands) {
                val speed = band.value
                if (band.start < 0 ||
                    band.start >= 1440 ||
                    band.end < 0 ||
                    band.end >= 1440 ||
                    band.start == band.end ||
                    (speed != null && (!speed.finite() || speed <= 0.0))
                ) {
                    throw IllegalArgumentException("Franja inválida.")
                }
            }
            for (minute in 0 until 1440) {
                if (bands.count { it.contains(minute) } != 1) {
                    throw IllegalArgumentException("Franjas con huecos o solapes.")
                }
            }
        }
    }
}
