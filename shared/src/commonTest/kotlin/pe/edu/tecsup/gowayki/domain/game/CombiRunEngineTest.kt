package pe.edu.tecsup.gowayki.domain.game

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CombiRunEngineTest {
    private fun engine(config: CombiRunEngine.Config = CombiRunEngine.Config(), seed: Int = 7) =
        CombiRunEngine(config, Random(seed))

    /** Pasos de 16 ms, como un cuadro a 60 Hz. */
    private fun CombiRunEngine.run(seconds: Float, onStep: () -> Unit = {}) {
        var t = 0f
        while (t < seconds && phase == RunPhase.Playing) {
            step(FRAME)
            onStep()
            t += FRAME
        }
    }

    @Test
    fun startsIdleAndIgnoresInputUntilStarted() {
        val e = engine()
        assertEquals(RunPhase.Idle, e.phase)
        e.moveUp()
        assertFalse(e.step(1f))
        assertEquals(1, e.lane)
        assertEquals(0, e.score)
    }

    @Test
    fun hittingAPotholeInTheSameLaneEndsTheGame() {
        val e = engine()
        e.start()
        e.place(lane = e.lane, x = e.config.playerX + 200f)
        e.run(5f)
        assertEquals(RunPhase.Ended, e.phase)
        val score = e.score
        assertFalse(e.step(1f))
        assertEquals(score, e.score)
        assertEquals(score, e.best)
    }

    @Test
    fun potholeInAnotherLanePassesHarmlessly() {
        val e = engine(CombiRunEngine.Config(firstSpawnSeconds = 100f))
        e.start()
        e.place(lane = 0, x = e.config.playerX + 200f)
        e.run(2f)
        assertEquals(RunPhase.Playing, e.phase)
    }

    @Test
    fun changingLaneInTimeDodgesThePothole() {
        val e = engine(CombiRunEngine.Config(firstSpawnSeconds = 100f))
        e.start()
        e.place(lane = 1, x = e.config.playerX + 400f)
        e.moveUp()
        e.run(3f)
        assertEquals(RunPhase.Playing, e.phase)
        assertEquals(0f, e.lanePosition)
    }

    @Test
    fun collisionIsGeometricWithAForgivingMargin() {
        val e = engine()
        e.start()
        val reach = e.config.playerHalfLength + e.config.potholeHalfLength - e.config.hitMargin
        assertTrue(e.hits(Pothole(1, e.lane, e.config.playerX + reach - 1f, 0)))
        assertFalse(e.hits(Pothole(2, e.lane, e.config.playerX + reach + 1f, 0)))
        assertFalse(e.hits(Pothole(3, e.lane + 1, e.config.playerX, 0)))
    }

    @Test
    fun laneChangesStayWithinTheRoad() {
        val e = engine()
        e.start()
        repeat(5) { e.moveUp() }
        assertEquals(0, e.lane)
        repeat(9) { e.moveDown() }
        assertEquals(e.config.lanes - 1, e.lane)
    }

    @Test
    fun laneChangeIsSmoothNotInstant() {
        val e = engine(CombiRunEngine.Config(firstSpawnSeconds = 100f))
        e.start()
        e.moveDown()
        e.step(FRAME)
        assertTrue(e.lanePosition > 1f && e.lanePosition < 2f)
        e.run(e.config.laneChangeSeconds + 0.05f)
        assertEquals(2f, e.lanePosition)
    }

    @Test
    fun neverSpawnsAWallAcrossAllLanes() {
        var doubleRows = 0
        for (seed in 1..20) {
            val e = engine(CombiRunEngine.Config(playerX = -100_000f), seed)
            e.start()
            e.run(150f) {
                for (p in e.potholes) {
                    val row = e.potholes.filter { abs(it.x - p.x) < e.config.blockWindow }.map { it.lane }.toSet()
                    assertTrue(row.size < e.config.lanes, "seed $seed: 3 carriles bloqueados cerca de x=${p.x}")
                    if (row.size == 2) doubleRows++
                }
            }
            assertEquals(RunPhase.Playing, e.phase)
        }
        assertTrue(doubleRows > 0, "Con la rampa deben aparecer filas dobles (siempre con un carril libre).")
    }

    @Test
    fun potholesComeFromTheRightAndMoveLeft() {
        val e = engine(CombiRunEngine.Config(playerX = -100_000f))
        e.start()
        e.run(e.config.firstSpawnSeconds + FRAME)
        val first = e.potholes.first()
        assertTrue(first.x > e.config.worldWidth - 50f)
        e.step(FRAME)
        assertTrue(e.potholes.first { it.id == first.id }.x < first.x)
    }

    @Test
    fun speedAndSpawnRateRampUpGently() {
        val e = engine(CombiRunEngine.Config(playerX = -100_000f))
        e.start()
        val v0 = e.speed()
        val s0 = e.spawnInterval()
        e.run(45f)
        assertTrue(e.speed() > v0 && e.speed() < e.config.maxSpeed)
        assertTrue(e.spawnInterval() < s0)
        e.run(45f)
        assertEquals(e.config.maxSpeed, e.speed(), 5f)
        assertEquals(e.config.minSpawnSeconds, e.spawnInterval(), 0.01f)
    }

    @Test
    fun keepsSpeedingUpAfterTheRampUntilTheSafeCap() {
        val e = engine(CombiRunEngine.Config(playerX = -100_000f))
        e.start()
        e.run(120f)
        val v120 = e.speed()
        assertTrue(v120 > e.config.maxSpeed, "Pasada la rampa la velocidad debe seguir subiendo.")
        assertTrue(e.spawnInterval() < e.config.minSpawnSeconds)
        e.run(60f)
        assertTrue(e.speed() > v120)
        e.run(600f)
        assertEquals(e.config.absoluteMaxSpeed, e.speed())
        assertEquals(e.config.spawnFloorSeconds, e.spawnInterval())
        val window = 2 * (e.config.playerHalfLength + e.config.potholeHalfLength - e.config.hitMargin)
        assertTrue(e.config.absoluteMaxSpeed * e.config.maxStepSeconds < window, "Un bache no puede saltar la ventana de choque.")
    }

    @Test
    fun scoreIsMonotonicWhilePlaying() {
        val e = engine(CombiRunEngine.Config(playerX = -100_000f))
        e.start()
        var last = 0
        e.run(30f) {
            assertTrue(e.score >= last)
            last = e.score
        }
        assertTrue(last > 0)
    }

    @Test
    fun restartResetsStateButKeepsTheBest() {
        val e = engine()
        e.start()
        e.place(lane = e.lane, x = e.config.playerX + 600f)
        e.run(10f)
        assertEquals(RunPhase.Ended, e.phase)
        val best = e.best
        assertTrue(best > 0)
        e.moveUp()
        e.start()
        assertEquals(RunPhase.Playing, e.phase)
        assertEquals(0, e.score)
        assertEquals(0f, e.distance)
        assertTrue(e.potholes.isEmpty())
        assertEquals(e.config.lanes / 2, e.lane)
        assertEquals(best, e.best)
        assertEquals(2, e.runCount)
    }

    @Test
    fun pausedGameDoesNotAdvance() {
        val e = engine()
        e.start()
        e.run(1f)
        e.pause()
        val distance = e.distance
        e.step(1f)
        assertEquals(distance, e.distance)
        e.resume()
        e.step(FRAME)
        assertTrue(e.distance > distance)
    }

    @Test
    fun longFramesAreClampedSoAPotholeCannotBeSkipped() {
        val e = engine(CombiRunEngine.Config(firstSpawnSeconds = 100f))
        e.start()
        e.place(lane = e.lane, x = e.config.playerX + 120f)
        e.step(5f)
        assertEquals(RunPhase.Playing, e.phase)
        e.run(2f)
        assertEquals(RunPhase.Ended, e.phase)
    }

    private companion object {
        const val FRAME = 0.016f
    }
}
