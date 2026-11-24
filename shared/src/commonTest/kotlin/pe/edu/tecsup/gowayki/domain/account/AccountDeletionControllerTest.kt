package pe.edu.tecsup.gowayki.domain.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.game.PendingScore
import pe.edu.tecsup.gowayki.domain.game.PendingScoreQueue
import pe.edu.tecsup.gowayki.domain.preferences.InMemoryPreferencesRepository
import pe.edu.tecsup.gowayki.testing.runSync

/** Port de los casos de borrado de `flutter/test/weekly_account_test.dart`. */
class AccountDeletionControllerTest {
    private val prefs = InMemoryPreferencesRepository()
    private val scores = PendingScoreQueue(prefs)
    private val repo = FakeDeletionRepository()
    private val controller = AccountDeletionController(repo, prefs, scores)
    private val week = "arequipa_2026-W45"

    private class FakeDeletionRepository : AccountDeletionRepository {
        val calls = mutableListOf<String>()
        var verifyError: AccountDeletionError? = null
        var failCleanup = false
        var prepared: String? = null

        override suspend fun prepare(uid: String) {
            calls += "verify:$uid"
            verifyError?.let { throw AccountDeletionException(it) }
            prepared = uid
        }

        override suspend fun erase(uid: String) {
            check(prepared == uid)
            calls += "erase:$uid"
            if (failCleanup) throw AccountDeletionException(AccountDeletionError.Network)
        }

        override fun discardPreparation() {
            calls += "discard"
            prepared = null
        }
    }

    @Test
    fun failedIdentityVerificationLeavesLocalDataUntouchedAndStartsNoErasure() {
        scores.record("alice", PendingScore(week, 100, 90_000))
        repo.verifyError = AccountDeletionError.WrongAccount
        val error = assertFailsWith<AccountDeletionException> { runSync { controller.erase("alice") } }
        assertEquals(AccountDeletionError.WrongAccount, error.error)
        assertEquals(listOf("verify:alice", "discard"), repo.calls)
        assertFalse(controller.isPending("alice"))
        assertEquals(1, scores.pending("alice").size)
        assertFalse(controller.running)
    }

    @Test
    fun erasureSettlesInFlightUploadsAfterPausingAndBeforeWipingTheCloud() {
        val order = mutableListOf<String>()
        runSync {
            controller.erase("alice") {
                order += "settle"
                order += if (controller.isPending("alice")) "paused" else "not-paused"
                order += repo.calls.joinToString(",")
            }
        }
        assertEquals(listOf("settle", "paused", "verify:alice"), order)
        assertEquals(listOf("verify:alice", "erase:alice", "discard"), repo.calls)
    }

    @Test
    fun interruptedErasurePausesUploadsAndBlocksNewScoresUntilRetryCompletes() {
        scores.record("alice", PendingScore(week, 100, 90_000))
        repo.failCleanup = true
        assertFailsWith<AccountDeletionException> { runSync { controller.erase("alice") } }
        assertTrue(controller.isPending("alice"))
        assertTrue(scores.pending("alice").isEmpty())
        assertFalse(scores.record("alice", PendingScore(week, 300, 90_000)))

        val again = AccountDeletionController(repo, prefs, scores)
        assertTrue(again.isPending("alice"))
        repo.failCleanup = false
        runSync { again.erase("alice") }
        assertFalse(again.isPending("alice"))
        assertTrue(prefs.readAccountsBeingDeleted().isEmpty())
    }

    @Test
    fun erasureClearsOnlyItsOwnQueuedScores() {
        scores.record("alice", PendingScore(week, 100, 90_000))
        scores.record("bob", PendingScore(week, 200, 90_000))
        runSync { controller.erase("alice") }
        assertTrue(scores.pending("alice").isEmpty())
        assertEquals(listOf(200), scores.pending("bob").map { it.score })
        assertEquals(listOf("verify:alice", "erase:alice", "discard"), repo.calls)
    }

    @Test
    fun cancelledVerificationIsNotPending() {
        repo.verifyError = AccountDeletionError.Cancelled
        assertFailsWith<AccountDeletionException> { runSync { controller.erase("alice") } }
        assertFalse(controller.isPending("alice"))
    }
}
