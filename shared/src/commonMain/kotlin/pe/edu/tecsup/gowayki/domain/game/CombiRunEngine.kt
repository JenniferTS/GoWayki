package pe.edu.tecsup.gowayki.domain.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class RunPhase { Idle, Playing, Paused, Ended }

/** Bache en la pista. [x] es el centro en unidades de mundo; [seed] varía su forma al dibujarlo. */
data class Pothole(val id: Long, val lane: Int, val x: Float, val seed: Int)

/**
 * Motor de Wayki Combi: pista horizontal de [Config.lanes] carriles que avanza de derecha a izquierda.
 * Sin UI ni reloj propio: la pantalla llama [step] con el tiempo de cada cuadro.
 * Un bache tocado termina la partida; solo [start] empieza otra.
 * Valores de referencia: `WaykiEngine` de Flutter (velocidad ×2 en 90 s, aparición 1,1 → 0,65 s).
 */
class CombiRunEngine(
    val config: Config = Config(),
    private val random: Random = Random.Default,
) {
    data class Config(
        val lanes: Int = 3,
        /** Largo visible de la pista en unidades de mundo (se escala al ancho del lienzo). */
        val worldWidth: Float = 1000f,
        val playerX: Float = 170f,
        val playerHalfLength: Float = 62f,
        val potholeHalfLength: Float = 30f,
        /** Margen a favor del jugador en el choque, en unidades de mundo. */
        val hitMargin: Float = 12f,
        /** Distancia máxima entre carriles (en carriles) para que cuente el choque. */
        val laneTolerance: Float = 0.4f,
        val startSpeed: Float = 380f,
        val maxSpeed: Float = 760f,
        val rampSeconds: Float = 90f,
        val startSpawnSeconds: Float = 1.1f,
        val minSpawnSeconds: Float = 0.65f,
        val firstSpawnSeconds: Float = 0.9f,
        val laneChangeSeconds: Float = 0.14f,
        /** Unidades de mundo recorridas por punto. */
        val unitsPerPoint: Float = 100f,
        /** Ventana en x donde los baches cuentan como “la misma fila” (nunca los 3 carriles a la vez). */
        val blockWindow: Float = 260f,
        /** Filas dobles (2 carriles) desde este segundo, hasta [maxPairChance] al final de la rampa. */
        val pairsAfterSeconds: Float = 15f,
        val maxPairChance: Float = 0.4f,
        /** Cuadros más largos se recortan: tras un tirón no hay túnel a través de un bache. */
        val maxStepSeconds: Float = 0.05f,
    ) {
        init {
            require(lanes >= 2) { "Se necesitan al menos 2 carriles." }
        }

        fun reducedMotion(): Config = copy(maxSpeed = startSpeed + (maxSpeed - startSpeed) * 0.6f, rampSeconds = rampSeconds * 1.6f)
    }

    var phase: RunPhase = RunPhase.Idle
        private set
    /** Carril objetivo (0 = arriba). */
    var lane: Int = config.lanes / 2
        private set
    /** Posición vertical continua en carriles; se acerca a [lane] en [Config.laneChangeSeconds]. */
    var lanePosition: Float = lane.toFloat()
        private set
    var potholes: List<Pothole> = emptyList()
        private set
    var elapsedSeconds: Float = 0f
        private set
    var distance: Float = 0f
        private set
    var score: Int = 0
        private set
    var best: Int = 0
        private set
    /** Sube con cada partida; la UI publica una vez por partida terminada. */
    var runCount: Int = 0
        private set

    private var spawnTimer = config.firstSpawnSeconds
    private var nextId = 0L

    val durationMs: Int get() = (elapsedSeconds * 1000f).toInt()

    fun speed(): Float {
        val t = (elapsedSeconds / config.rampSeconds).coerceIn(0f, 1f)
        return config.startSpeed + (config.maxSpeed - config.startSpeed) * t
    }

    fun spawnInterval(): Float {
        val t = (elapsedSeconds / config.rampSeconds).coerceIn(0f, 1f)
        return config.startSpawnSeconds + (config.minSpawnSeconds - config.startSpawnSeconds) * t
    }

    fun start() {
        phase = RunPhase.Playing
        lane = config.lanes / 2
        lanePosition = lane.toFloat()
        potholes = emptyList()
        elapsedSeconds = 0f
        distance = 0f
        score = 0
        spawnTimer = config.firstSpawnSeconds
        runCount++
    }

    fun pause() {
        if (phase == RunPhase.Playing) phase = RunPhase.Paused
    }

    fun resume() {
        if (phase == RunPhase.Paused) phase = RunPhase.Playing
    }

    fun moveUp() = changeLane(-1)

    fun moveDown() = changeLane(1)

    private fun changeLane(delta: Int) {
        if (phase != RunPhase.Playing) return
        lane = (lane + delta).coerceIn(0, config.lanes - 1)
    }

    /** Avanza [dtSeconds]. Devuelve true si la partida terminó en este paso. */
    fun step(dtSeconds: Float): Boolean {
        if (phase != RunPhase.Playing || dtSeconds <= 0f) return false
        val dt = min(dtSeconds, config.maxStepSeconds)
        elapsedSeconds += dt
        val v = speed()
        distance += v * dt
        score = (distance / config.unitsPerPoint).toInt()

        val maxShift = dt / config.laneChangeSeconds
        val gap = lane - lanePosition
        lanePosition += gap.coerceIn(-maxShift, maxShift)

        potholes = potholes
            .map { it.copy(x = it.x - v * dt) }
            .filter { it.x > -config.potholeHalfLength }

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            if (spawn()) spawnTimer += spawnInterval() else spawnTimer = SPAWN_RETRY_SECONDS
        }

        if (potholes.any(::hits)) {
            phase = RunPhase.Ended
            best = max(best, score)
            return true
        }
        return false
    }

    fun hits(pothole: Pothole): Boolean {
        val reach = config.playerHalfLength + config.potholeHalfLength - config.hitMargin
        return abs(pothole.x - config.playerX) < reach &&
            abs(pothole.lane - lanePosition) < config.laneTolerance
    }

    /** Carriles donde un bache nuevo en [x] no cerraría los 3 carriles ni pisaría otro bache del mismo carril. */
    fun allowedLanes(x: Float): List<Int> {
        val nearby = potholes.filter { abs(it.x - x) < config.blockWindow }
        val blocked = nearby.map { it.lane }.toSet()
        return (0 until config.lanes).filter { candidate ->
            val stillOpen = (blocked + candidate).size < config.lanes
            val tooClose = nearby.any { it.lane == candidate && abs(it.x - x) < config.potholeHalfLength * 4 }
            stillOpen && !tooClose
        }
    }

    /** Solo para pruebas y para reanudar escenas: coloca un bache exacto. */
    fun place(lane: Int, x: Float): Pothole {
        require(lane in 0 until config.lanes) { "Carril fuera de rango." }
        val pothole = Pothole(nextId++, lane, x, random.nextInt())
        potholes = potholes + pothole
        return pothole
    }

    /** Probabilidad de una fila doble (2 de 3 carriles); nula al inicio y crece con la velocidad. */
    fun pairChance(): Float {
        val t = ((elapsedSeconds - config.pairsAfterSeconds) / config.rampSeconds).coerceIn(0f, 1f)
        return config.maxPairChance * t
    }

    private fun spawn(): Boolean {
        val x = config.worldWidth + config.potholeHalfLength
        val options = allowedLanes(x)
        if (options.isEmpty()) return false
        place(options[random.nextInt(options.size)], x)
        if (random.nextFloat() < pairChance()) {
            val second = allowedLanes(x)
            if (second.isNotEmpty()) place(second[random.nextInt(second.size)], x)
        }
        return true
    }

    private companion object {
        const val SPAWN_RETRY_SECONDS = 0.1f
    }
}
