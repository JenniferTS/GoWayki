package pe.edu.tecsup.gowayki.domain.tracking

import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApproachAlertTest {
    @Test
    fun alertsOnceWithinThreshold() {
        val user = GeoPoint(-16.4090, -71.5375)
        val stop = GeoPoint(-16.4091, -71.5376)
        assertTrue(ApproachAlert.shouldAlert(user, stop, 200.0, alreadyAlerted = false))
        assertFalse(ApproachAlert.shouldAlert(user, stop, 200.0, alreadyAlerted = true))
    }
}
