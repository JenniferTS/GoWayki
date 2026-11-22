package pe.edu.tecsup.gowayki.domain.game

import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeeklyRankingTest {
    @Test
    fun rankingWeekMatchesFlutterFixtures() {
        assertEquals("arequipa_2020-W53", rankingWeek(UtcDateTime.of(2021, 1, 4, 4, 59)))
        assertEquals("arequipa_2021-W01", rankingWeek(UtcDateTime.of(2021, 1, 4, 5)))
        assertEquals("arequipa_2021-W52", rankingWeek(UtcDateTime.of(2022, 1, 1, 17)))
        assertEquals("arequipa_2026-W45", rankingWeek(UtcDateTime.of(2026, 11, 7, 16)))
    }

    @Test
    fun pendingScoreRejectsInvalidPayload() {
        assertFailsWith<IllegalArgumentException> {
            PendingScore.fromMap(mapOf("week" to "bad", "score" to 100, "durationMs" to 90000))
        }
        assertFailsWith<IllegalArgumentException> {
            PendingScore.fromMap(
                mapOf("week" to "arequipa_2026-W45", "score" to 5001, "durationMs" to 90000),
            )
        }
    }
}
