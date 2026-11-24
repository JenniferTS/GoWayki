package pe.edu.tecsup.gowayki.domain.account

import pe.edu.tecsup.gowayki.domain.game.PendingScoreQueue
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository

enum class AccountDeletionError {
    /** El usuario cerró la verificación o la sesión ya no es de esa cuenta. */
    Cancelled,
    /** Eligió otra cuenta de Google al verificar. */
    WrongAccount,
    /** Firebase pide un inicio de sesión reciente. */
    NeedsRecentLogin,
    Network,
    /** Proveedor de acceso que la app no sabe reverificar. */
    Unsupported,
    Failed,
}

class AccountDeletionException(
    val error: AccountDeletionError,
    cause: Throwable? = null,
) : Exception(error.name, cause)

/**
 * Borrado de cuenta en el backend (Flutter: `AccountDeletionRepository`).
 * [prepare] solo verifica identidad; [erase] es idempotente y se puede reanudar tras un corte.
 */
interface AccountDeletionRepository {
    /** Reautentica a [uid] (login reciente). Si falla no se tocó ningún dato. */
    suspend fun prepare(uid: String)

    /**
     * Marca `deleting`, borra feedback, confirmaciones (devolviendo el contador del reporte),
     * reportes, puntajes y `users/{uid}/…`, deja `deletedAccounts/{uid}`, borra `users/{uid}`
     * y el usuario de Auth. Exige un [prepare] previo para la misma cuenta.
     */
    suspend fun erase(uid: String)

    fun discardPreparation()
}

/**
 * Orquesta el borrado como `account_deletion_controller.dart`:
 * verifica identidad → deja la pausa persistida → espera subidas en curso → limpia datos locales
 * de la cuenta → borra en la nube → limpia otra vez → quita la pausa.
 * Si algo falla después de verificar, la pausa queda guardada: las subidas siguen detenidas
 * (también tras reiniciar) hasta que un reintento complete el borrado.
 */
class AccountDeletionController(
    private val repository: AccountDeletionRepository,
    private val preferences: PreferencesRepository,
    private val scores: PendingScoreQueue,
) {
    var running: Boolean = false
        private set

    fun isPending(uid: String): Boolean = uid in preferences.readAccountsBeingDeleted()

    /** [settle] espera las escrituras en vuelo (ranking, preferencias, reportes) antes de borrar la nube. */
    suspend fun erase(uid: String, settle: suspend () -> Unit = {}) {
        check(!running) { "El borrado ya está en curso." }
        running = true
        try {
            repository.prepare(uid)
            preferences.saveAccountBeingDeleted(uid, true)
            settle()
            clearLocal(uid)
            repository.erase(uid)
            clearLocal(uid)
            preferences.saveAccountBeingDeleted(uid, false)
        } finally {
            repository.discardPreparation()
            running = false
        }
    }

    /** Hoy el único dato local por cuenta es su cola de puntajes; favoritos y recientes son del dispositivo. */
    private fun clearLocal(uid: String) {
        scores.clear(uid)
    }
}
