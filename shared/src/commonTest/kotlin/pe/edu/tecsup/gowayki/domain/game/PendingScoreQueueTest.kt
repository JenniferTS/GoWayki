package pe.edu.tecsup.gowayki.domain.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.preferences.InMemoryPreferencesRepository
import pe.edu.tecsup.gowayki.testing.runSync

/** Port de `flutter/test/weekly_account_test.dart` (cola de puntajes por cuenta). */
class PendingScoreQueueTest {
    private val week = "arequipa_2026-W45"
    private val prefs = InMemoryPreferencesRepository()
    private val queue = PendingScoreQueue(prefs)
    private var session: String? = "alice"
    private val calls = mutableListOf<Pair<String, PendingScore>>()

    private fun score(value: Int, w: String = week) = PendingScore(w, value, 90_000)

    private fun publisher(uid: String, onPublish: (PendingScore) -> Unit = {}): suspend (PendingScore) -> Int = { s ->
        calls += uid to s
        onPublish(s)
        s.score
    }

    @Test
    fun guestsNeverQueueScores() {
        assertFalse(queue.record(null, score(100)))
        assertTrue(queue.pending("alice").isEmpty())
        assertTrue(prefs.readPendingScores("guest").isEmpty())
    }

    @Test
    fun duplicateWeeksKeepOnlyTheBestScore() {
        assertTrue(queue.record("alice", score(100)))
        assertTrue(queue.record("alice", score(50)))
        assertEquals(listOf(100), queue.pending("alice").map { it.score })
        assertEquals(
            listOf(100),
            PendingScoreQueue.decode(listOf("$week|100|90000", "$week|50|90000")).map { it.score },
        )
    }

    @Test
    fun invalidPayloadsAreRejected() {
        assertFalse(queue.record("alice", PendingScore("arequipa_2026-W99", 100, 90_000)))
        assertFalse(queue.record("alice", PendingScore(week, 5001, 90_000)))
        assertFalse(queue.record("alice", PendingScore(week, 5000, 1_000)))
        assertTrue(PendingScoreQueue.decode(listOf("garbage", "$week|x|1", "$week|5001|90000")).isEmpty())
    }

    @Test
    fun offlineScoresStayDurableAndRetryPublishesOnlyTheBest() {
        queue.record("alice", score(100))
        queue.record("alice", score(50))
        val failed = runSync { queue.flush("alice", { session == "alice" }) { error("offline") } }
        assertTrue(failed.failed)
        assertEquals(listOf(100), queue.pending("alice").map { it.score })

        val ok = runSync { queue.flush("alice", { session == "alice" }, publisher("alice")) }
        assertFalse(ok.failed)
        assertEquals(1, ok.published)
        assertEquals(100, calls.single().second.score)
        assertTrue(queue.pending("alice").isEmpty())
    }

    @Test
    fun scoreArrivingDuringUploadIsNotRemovedByTheOldAcknowledgement() {
        queue.record("alice", score(100))
        var recorded = false
        val result = runSync {
            queue.flush("alice", { session == "alice" }, publisher("alice") {
                if (!recorded) {
                    recorded = true
                    queue.record("alice", score(200))
                }
            })
        }
        assertFalse(result.failed)
        assertEquals(listOf(100, 200), calls.map { it.second.score })
        assertTrue(queue.pending("alice").isEmpty())
    }

    @Test
    fun switchingAccountCannotPublishOldQueuedPointsAsTheNewUser() {
        queue.record("alice", score(100))
        val result = runSync {
            queue.flush("alice", { session == "alice" }, publisher("alice") { session = "bob" })
        }
        assertTrue(result.aborted)
        assertTrue(calls.all { it.first == "alice" })
        assertEquals(listOf(100), queue.pending("alice").map { it.score })
        assertTrue(queue.pending("bob").isEmpty())

        val bob = runSync { queue.flush("bob", { session == "bob" }, publisher("bob")) }
        assertEquals(0, bob.published)
        assertTrue(calls.none { it.first == "bob" })
    }

    @Test
    fun flushStopsBeforePublishingWhenTheSessionAlreadyChanged() {
        queue.record("alice", score(100))
        session = "bob"
        val result = runSync { queue.flush("alice", { session == "alice" }, publisher("alice")) }
        assertTrue(result.aborted)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun queuesArePerAccount() {
        queue.record("alice", score(100))
        queue.record("bob", score(300))
        runSync { queue.flush("bob", { true }, publisher("bob")) }
        assertEquals(listOf("bob" to 300), calls.map { it.first to it.second.score })
        assertEquals(listOf(100), queue.pending("alice").map { it.score })
    }

    @Test
    fun accountsBeingDeletedNeitherQueueNorPublish() {
        queue.record("alice", score(100))
        prefs.saveAccountBeingDeleted("alice", true)
        assertFalse(queue.record("alice", score(200)))
        val result = runSync { queue.flush("alice", { true }, publisher("alice")) }
        assertTrue(result.aborted)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun legacySharedQueueIsDiscardedInsteadOfBeingAttributedToAnAccount() {
        prefs.seedLegacyPendingScores(listOf("$week|900|90000"))
        assertTrue(prefs.hasLegacyPendingScores())
        prefs.discardLegacyPendingScores()
        assertFalse(prefs.hasLegacyPendingScores())
        assertTrue(queue.pending("alice").isEmpty())
    }
}
