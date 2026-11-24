package pe.edu.tecsup.gowayki.testing

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Ejecuta un bloque suspendido cuyas dependencias (fakes) no suspenden de verdad.
 * Evita sumar kotlinx-coroutines-test solo para esto.
 */
fun <T> runSync(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
    return checkNotNull(outcome) { "El bloque quedó suspendido: el fake no debe suspender." }.getOrThrow()
}
