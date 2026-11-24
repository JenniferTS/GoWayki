package pe.edu.tecsup.gowayki.domain.game

import kotlin.coroutines.cancellation.CancellationException
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository

/** Resultado de vaciar la cola: [aborted] si cambió la cuenta o empezó su borrado a mitad. */
data class ScoreFlushResult(
    val published: Int,
    val failed: Boolean = false,
    val aborted: Boolean = false,
)

/**
 * Cola offline de puntajes semanales, una por cuenta (como `weekly_scores.dart` de Flutter):
 * - los invitados no encolan;
 * - por semana solo se guarda el mejor puntaje;
 * - una cuenta nunca publica la cola de otra, aunque cambie la sesión con una subida en curso;
 * - la cola de una cuenta en borrado queda en pausa.
 */
class PendingScoreQueue(private val preferences: PreferencesRepository) {

    fun pending(uid: String): List<PendingScore> = decode(preferences.readPendingScores(uid))

    fun isPaused(uid: String): Boolean = uid in preferences.readAccountsBeingDeleted()

    /**
     * Guarda [score] en la cola de [uid]. Falso si no se encola: invitado (`uid` nulo),
     * cuenta en borrado o puntaje inválido. Un puntaje menor al ya encolado esa semana se ignora.
     */
    fun record(uid: String?, score: PendingScore): Boolean {
        if (uid == null || isPaused(uid) || !isValid(score)) return false
        val current = pending(uid)
        val previous = current.firstOrNull { it.week == score.week }
        if (previous != null && previous.score >= score.score) return true
        save(uid, current.filterNot { it.week == score.week } + score)
        return true
    }

    /** Saca la semana solo si lo publicado ([best]) cubre lo encolado: un puntaje más alto llegado en vuelo se queda. */
    fun acknowledge(uid: String, week: String, best: Int) {
        val current = pending(uid)
        val entry = current.firstOrNull { it.week == week } ?: return
        if (entry.score <= best) save(uid, current - entry)
    }

    fun clear(uid: String) {
        preferences.savePendingScores(uid, emptyList())
    }

    /**
     * Publica la cola de [uid] con [publish] (devuelve el mejor puntaje vigente en el servidor).
     * [isCurrent] se revisa antes y después de cada subida: si la sesión ya no es de [uid],
     * se detiene sin tocar la cola. Repite si llegaron puntajes nuevos durante la subida.
     */
    suspend fun flush(
        uid: String,
        isCurrent: () -> Boolean,
        publish: suspend (PendingScore) -> Int,
    ): ScoreFlushResult {
        var published = 0
        repeat(MAX_PASSES) {
            val batch = pending(uid)
            if (batch.isEmpty()) return ScoreFlushResult(published)
            for (score in batch) {
                if (!isCurrent() || isPaused(uid)) return ScoreFlushResult(published, aborted = true)
                val best = try {
                    publish(score)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    return ScoreFlushResult(published, failed = true)
                }
                if (!isCurrent()) return ScoreFlushResult(published, aborted = true)
                acknowledge(uid, score.week, best)
                published++
            }
        }
        return ScoreFlushResult(published, failed = pending(uid).isNotEmpty())
    }

    private fun save(uid: String, scores: List<PendingScore>) {
        preferences.savePendingScores(uid, scores.map(::encode))
    }

    companion object {
        private const val MAX_PASSES = 3

        fun encode(score: PendingScore): String = "${score.week}|${score.score}|${score.durationMs}"

        /** Descarta líneas inválidas y deja el mejor puntaje por semana. */
        fun decode(lines: List<String>): List<PendingScore> {
            val best = LinkedHashMap<String, PendingScore>()
            for (line in lines) {
                val parts = line.split('|')
                if (parts.size != 3) continue
                val score = PendingScore(
                    week = parts[0],
                    score = parts[1].toIntOrNull() ?: continue,
                    durationMs = parts[2].toIntOrNull() ?: continue,
                )
                if (!isValid(score)) continue
                val previous = best[score.week]
                if (previous == null || score.score > previous.score) best[score.week] = score
            }
            return best.values.toList()
        }

        private fun isValid(score: PendingScore): Boolean =
            runCatching { PendingScore.validate(score.week, score.score, score.durationMs) }.isSuccess
    }
}
