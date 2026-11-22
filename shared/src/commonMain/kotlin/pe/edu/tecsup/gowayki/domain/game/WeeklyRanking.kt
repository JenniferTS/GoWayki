package pe.edu.tecsup.gowayki.domain.game

import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

/**
 * Semana ISO en Arequipa (UTC-05:00, sin DST).
 * Formato: `arequipa_YYYY-Www`.
 */
fun rankingWeek(now: UtcDateTime = UtcDateTime.now()): String {
    val limaMicros = now.epochMicros - 5L * 3_600_000_000L
    val lima = UtcDateTime(limaMicros)
    val day = UtcDateTime.of(lima.year, lima.month, lima.day)
    // weekday: Monday=1 .. Sunday=7 (ISO)
    val weekday = isoWeekday(day)
    val thursday = day.plusDays(4 - weekday)
    val jan1 = UtcDateTime.of(thursday.year, 1, 1)
    val week = ((thursday.epochMicros - jan1.epochMicros) / 86_400_000_000L).toInt() / 7 + 1
    return "arequipa_${thursday.year}-W${week.toString().padStart(2, '0')}"
}

private fun UtcDateTime.plusDays(days: Int): UtcDateTime =
    UtcDateTime(epochMicros + days * 86_400_000_000L)

/** Monday=1 … Sunday=7 for a UTC civil day. */
private fun isoWeekday(day: UtcDateTime): Int {
    // 1970-01-01 was Thursday → epoch day 0 → weekday 4
    val epochDay = day.epochMicros / 86_400_000_000L
    val sun0 = ((epochDay + 4) % 7).toInt().let { if (it < 0) it + 7 else it } // 0=Sun
    return if (sun0 == 0) 7 else sun0
}

data class RankingScore(
    val uid: String,
    val name: String,
    val score: Int,
    val durationMs: Int,
)

data class WeeklyRanking(
    val top: List<RankingScore>,
    val own: RankingScore? = null,
    val position: Int? = null,
)

data class PendingScore(
    val week: String,
    val score: Int,
    val durationMs: Int,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "week" to week,
        "score" to score,
        "durationMs" to durationMs,
    )

    companion object {
        private val WEEK = Regex("^arequipa_[0-9]{4}-W(0[1-9]|[1-4][0-9]|5[0-3])$")

        fun fromMap(data: Map<String, Any?>): PendingScore {
            val week = data["week"] as String
            val score = (data["score"] as Number).toInt()
            val duration = (data["durationMs"] as Number).toInt()
            validate(week, score, duration)
            return PendingScore(week, score, duration)
        }

        fun validate(week: String, score: Int, durationMs: Int) {
            if (!WEEK.matches(week) ||
                score < 0 ||
                score > 5000 ||
                durationMs < 1000 ||
                durationMs > 120_000 ||
                score > durationMs / 1000 * 30 + 30
            ) {
                throw IllegalArgumentException("Invalid weekly score")
            }
        }
    }
}

interface RankingClient {
    fun load(week: String, uid: String?): WeeklyRanking
    /** Publica si mejora el mejor; devuelve el mejor score vigente. */
    fun publish(uid: String, name: String, score: PendingScore): Int
}
