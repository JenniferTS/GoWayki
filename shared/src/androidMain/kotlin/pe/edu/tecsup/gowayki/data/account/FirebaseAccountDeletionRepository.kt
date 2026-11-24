package pe.edu.tecsup.gowayki.data.account

import android.os.SystemClock
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import pe.edu.tecsup.gowayki.data.auth.GoogleSignInException
import pe.edu.tecsup.gowayki.domain.account.AccountDeletionError
import pe.edu.tecsup.gowayki.domain.account.AccountDeletionException
import pe.edu.tecsup.gowayki.domain.account.AccountDeletionRepository
import pe.edu.tecsup.gowayki.platform.platformIoDispatcher

/**
 * Port de `firebase_account_deletion_repository.dart`. Todo lo de Firestore corre en
 * [platformIoDispatcher] (con `Tasks.await`); la reverificación con Google usa Credential Manager
 * desde el hilo principal sin bloquearlo. Cada paso es idempotente: un borrado cortado se reanuda
 * con otro [prepare] + [erase].
 */
class FirebaseAccountDeletionRepository(
    private val googleIdToken: suspend () -> String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AccountDeletionRepository {
    @Volatile private var prepared: String? = null
    @Volatile private var credential: AuthCredential? = null
    @Volatile private var verifiedAtMs: Long = 0L

    private fun user(uid: String): FirebaseUser {
        val current = auth.currentUser
        if (current == null || current.uid != uid) throw AccountDeletionException(AccountDeletionError.Cancelled)
        return current
    }

    override suspend fun prepare(uid: String) {
        discardPreparation()
        try {
            val current = user(uid)
            if (!current.isAnonymous) {
                val providers = current.providerData.map { it.providerId }.toSet()
                if (GoogleAuthProvider.PROVIDER_ID !in providers) {
                    throw AccountDeletionException(AccountDeletionError.Unsupported)
                }
                val google = GoogleAuthProvider.getCredential(googleIdToken(), null)
                awaitTask(current.reauthenticate(google))
                credential = google
            }
            user(uid)
            prepared = uid
            verifiedAtMs = SystemClock.elapsedRealtime()
        } catch (e: CancellationException) {
            discardPreparation()
            throw e
        } catch (e: Throwable) {
            discardPreparation()
            throw mapError(e)
        }
    }

    override fun discardPreparation() {
        prepared = null
        credential = null
        verifiedAtMs = 0L
    }

    override suspend fun erase(uid: String) {
        if (prepared != uid) throw AccountDeletionException(AccountDeletionError.Cancelled)
        try {
            withContext(platformIoDispatcher) { eraseBlocking(uid) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapError(e)
        } finally {
            discardPreparation()
        }
    }

    private fun eraseBlocking(uid: String) {
        val root = db.collection("users").document(uid)
        val marker = db.collection("deletedAccounts").document(uid)
        user(uid)
        val sealed = await(marker.get(Source.SERVER)).exists()
        if (!sealed) {
            await(
                db.runTransaction { tx ->
                    val previous = tx.get(root)
                    user(uid)
                    if (previous.getBoolean("deleting") != true) {
                        tx.set(
                            root,
                            mapOf("deleting" to true, "deletionStartedAt" to FieldValue.serverTimestamp()),
                            SetOptions.merge(),
                        )
                    }
                    Unit
                },
            )
        }
        deleteFeedback(uid)
        deleteConfirmations(uid)
        deleteReports(uid)
        deleteScores(uid)
        for (section in PRIVATE_SECTIONS) deletePlainCollection(uid, section)
        val current = user(uid)
        val reverified = credential
        if (reverified != null && SystemClock.elapsedRealtime() - verifiedAtMs >= REAUTH_AFTER_MS) {
            await(current.reauthenticate(reverified))
        }
        // Firestore y Auth no comparten transacción: si algo falla después, el borrado queda
        // pendiente en el dispositivo y cada paso de arriba se puede repetir sin daño.
        if (!sealed) {
            val seal = db.batch()
            seal.set(marker, emptyMap<String, Any>())
            seal.delete(root)
            await(seal.commit())
        }
        try {
            await(user(uid).delete())
        } catch (e: Throwable) {
            val cause = unwrap(e)
            if (cause is FirebaseAuthInvalidUserException && cause.errorCode == "ERROR_USER_NOT_FOUND") {
                if (auth.currentUser?.uid == uid) auth.signOut()
            } else {
                throw e
            }
        }
    }

    private fun deleteFeedback(uid: String) {
        val refs = db.collection("users").document(uid).collection("feedbackRefs")
        while (true) {
            user(uid)
            val rows = await(refs.limit(4).get(Source.SERVER))
            if (rows.isEmpty) return
            user(uid)
            val batch = db.batch()
            for (row in rows.documents) {
                batch.delete(db.collection("feedback").document(row.id))
                batch.delete(row.reference)
            }
            await(batch.commit())
        }
    }

    /** Quita cada voto devolviendo el contador del reporte, en la misma transacción que su índice. */
    private fun deleteConfirmations(uid: String) {
        val refs = db.collection("users").document(uid).collection("confirmationRefs")
        while (true) {
            user(uid)
            val rows = await(refs.limit(100).get(Source.SERVER))
            if (rows.isEmpty) return
            for (row in rows.documents) {
                user(uid)
                val report = db.collection("reports").document(row.id)
                val vote = report.collection("confirmations").document(uid)
                await(
                    db.runTransaction { tx ->
                        val parent = tx.get(report)
                        val voted = tx.get(vote)
                        val index = tx.get(row.reference)
                        user(uid)
                        if (!index.exists()) return@runTransaction Unit
                        if (voted.exists()) {
                            if (parent.exists()) {
                                val count = parent.getLong("confirmations") ?: 0L
                                check(count >= 1L) { "Contador de confirmaciones inconsistente." }
                                tx.update(report, "confirmations", count - 1L)
                            }
                            tx.delete(vote)
                        }
                        tx.delete(row.reference)
                        Unit
                    },
                )
            }
        }
    }

    private fun deleteReports(uid: String) {
        while (true) {
            user(uid)
            val rows = await(
                db.collection("reports").whereEqualTo("uid", uid).limit(100).get(Source.SERVER),
            )
            if (rows.isEmpty) return
            user(uid)
            val batch = db.batch()
            for (row in rows.documents) batch.delete(row.reference)
            await(batch.commit())
        }
    }

    private fun deleteScores(uid: String) {
        val refs = db.collection("users").document(uid).collection("scoreRefs")
        while (true) {
            user(uid)
            val rows = await(refs.limit(5).get(Source.SERVER))
            if (rows.isEmpty) return
            user(uid)
            val batch = db.batch()
            for (row in rows.documents) {
                batch.delete(db.collection("leaderboards").document(row.id).collection("scores").document(uid))
                batch.delete(row.reference)
            }
            await(batch.commit())
        }
    }

    private fun deletePlainCollection(uid: String, section: String) {
        val collection = db.collection("users").document(uid).collection(section)
        while (true) {
            user(uid)
            val rows = await(collection.limit(100).get(Source.SERVER))
            if (rows.isEmpty) return
            user(uid)
            val batch = db.batch()
            for (row in rows.documents) batch.delete(row.reference)
            await(batch.commit())
        }
    }

    private fun <T> await(task: Task<T>): T = Tasks.await(task, TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnCompleteListener { done ->
            if (done.isSuccessful) {
                cont.resume(done.result)
            } else {
                cont.resumeWithException(done.exception ?: AccountDeletionException(AccountDeletionError.Cancelled))
            }
        }
    }

    /** `Tasks.await` envuelve en [ExecutionException] y las transacciones pueden envolver otra vez. */
    private fun unwrap(e: Throwable): Throwable {
        var current = e
        repeat(MAX_CAUSE_DEPTH) {
            val next = current.cause
            if (current !is ExecutionException || next == null || next === current) return current
            current = next
        }
        return current
    }

    private fun mapError(e: Throwable): AccountDeletionException {
        val chain = generateSequence(e) { it.cause.takeIf { cause -> cause !== it } }.take(MAX_CAUSE_DEPTH).toList()
        chain.firstOrNull { it is AccountDeletionException }?.let { return it as AccountDeletionException }
        val error = chain.firstNotNullOfOrNull { cause ->
            when {
                cause is GoogleSignInException && cause.cancelled -> AccountDeletionError.Cancelled
                cause is FirebaseAuthInvalidUserException && cause.errorCode == "ERROR_USER_MISMATCH" ->
                    AccountDeletionError.WrongAccount
                cause is FirebaseAuthRecentLoginRequiredException -> AccountDeletionError.NeedsRecentLogin
                cause is FirebaseNetworkException || cause is TimeoutException -> AccountDeletionError.Network
                cause is FirebaseFirestoreException && cause.code in NETWORK_CODES -> AccountDeletionError.Network
                else -> null
            }
        } ?: AccountDeletionError.Failed
        return AccountDeletionException(error, unwrap(e))
    }

    private companion object {
        const val TIMEOUT_SECONDS = 20L
        const val MAX_CAUSE_DEPTH = 6
        /** Flutter reverifica si pasaron 4 minutos: Auth exige login reciente para borrar. */
        const val REAUTH_AFTER_MS = 4 * 60_000L
        val PRIVATE_SECTIONS = listOf("state", "library", "preferences")
        val NETWORK_CODES = setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        )
    }
}
