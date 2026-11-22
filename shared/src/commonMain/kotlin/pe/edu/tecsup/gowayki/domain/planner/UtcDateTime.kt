package pe.edu.tecsup.gowayki.domain.planner

internal expect fun wallClockEpochMillis(): Long

internal fun Double.finite(): Boolean = !isNaN() && !isInfinite()

/**
 * Instante UTC en microsegundos, el mismo modelo que DateTime de Dart.
 * America/Lima es UTC-5 fijo: [PlannerSettings.cityClock] resta cinco horas.
 */
class UtcDateTime(val epochMicros: Long) : Comparable<UtcDateTime> {
    operator fun plus(duration: PlannerDuration): UtcDateTime =
        UtcDateTime(epochMicros + duration.inWholeMicroseconds)

    operator fun minus(duration: PlannerDuration): UtcDateTime =
        UtcDateTime(epochMicros - duration.inWholeMicroseconds)

    fun difference(other: UtcDateTime): PlannerDuration =
        PlannerDuration(epochMicros - other.epochMicros)

    fun isAfter(other: UtcDateTime): Boolean = epochMicros > other.epochMicros

    fun isBefore(other: UtcDateTime): Boolean = epochMicros < other.epochMicros

    /** Trunca hacia cero al segundo, como `microsecondsSinceEpoch ~/ 1000000 * 1000000`. */
    fun truncatedToWholeSeconds(): UtcDateTime =
        UtcDateTime((epochMicros / 1_000_000L) * 1_000_000L)

    val year: Int get() = civil().year
    val month: Int get() = civil().month
    val day: Int get() = civil().day
    val hour: Int get() = civil().hour
    val minute: Int get() = civil().minute
    val second: Int get() = civil().second

    override fun compareTo(other: UtcDateTime): Int = epochMicros.compareTo(other.epochMicros)

    override fun equals(other: Any?): Boolean = other is UtcDateTime && other.epochMicros == epochMicros

    override fun hashCode(): Int = epochMicros.hashCode()

    override fun toString(): String {
        val clock = civil()
        return buildString {
            append(clock.year.toString().padStart(4, '0'))
            append('-')
            append(clock.month.toString().padStart(2, '0'))
            append('-')
            append(clock.day.toString().padStart(2, '0'))
            append('T')
            append(clock.hour.toString().padStart(2, '0'))
            append(':')
            append(clock.minute.toString().padStart(2, '0'))
            append(':')
            append(clock.second.toString().padStart(2, '0'))
            append('.')
            append(clock.micros.toString().padStart(6, '0'))
            append('Z')
        }
    }

    private fun civil(): CivilClock = civilFromEpochMicros(epochMicros)

    companion object {
        fun now(): UtcDateTime = UtcDateTime(wallClockEpochMillis() * 1_000L)

        /**
         * Calendario gregoriano proléptico. [day] puede desbordar el mes
         * (0 es el último día del mes anterior), igual que DateTime.utc de Dart.
         */
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            micros: Int = 0,
        ): UtcDateTime {
            val epochDay = civilToEpochDay(year, month, 1) + (day - 1L)
            val secondOfDay = hour.toLong() * 3_600L + minute.toLong() * 60L + second.toLong()
            return UtcDateTime(epochDay * 86_400L * 1_000_000L + secondOfDay * 1_000_000L + micros)
        }
    }
}

class PlannerDuration(val inWholeMicroseconds: Long) : Comparable<PlannerDuration> {
    override fun compareTo(other: PlannerDuration): Int =
        inWholeMicroseconds.compareTo(other.inWholeMicroseconds)

    companion object {
        val ZERO: PlannerDuration = PlannerDuration(0L)

        fun of(
            days: Int = 0,
            hours: Int = 0,
            minutes: Int = 0,
            seconds: Int = 0,
            milliseconds: Int = 0,
            microseconds: Long = 0L,
        ): PlannerDuration {
            val micros =
                days * 86_400_000_000L +
                    hours * 3_600_000_000L +
                    minutes * 60_000_000L +
                    seconds * 1_000_000L +
                    milliseconds * 1_000L +
                    microseconds
            return PlannerDuration(micros)
        }
    }
}

internal data class CivilClock(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val micros: Int,
)

/** Howard Hinnant, days_from_civil. Día 0 = 1970-01-01. */
internal fun civilToEpochDay(year: Int, month: Int, day: Int): Long {
    var y = year.toLong()
    val m = month.toLong()
    if (m <= 2) y -= 1
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = (y - era * 400).toInt()
    val doy = ((153 * (m + if (m > 2) -3 else 9) + 2) / 5 + day - 1).toInt()
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

internal fun civilFromEpochMicros(micros: Long): CivilClock {
    val seconds = micros / 1_000_000L
    val epochDay = seconds / 86_400L
    val secondOfDay = seconds % 86_400L
    val hour = (secondOfDay / 3_600L).toInt()
    val minute = ((secondOfDay % 3_600L) / 60L).toInt()
    val second = (secondOfDay % 60L).toInt()
    val sub = (micros % 1_000_000L).toInt()
    val z = epochDay + 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = (z - era * 146097).toInt()
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe.toLong() + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + if (mp < 10) 3 else -9
    val year = (y + if (m <= 2) 1 else 0).toInt()
    return CivilClock(year, m, d, hour, minute, second, sub)
}
