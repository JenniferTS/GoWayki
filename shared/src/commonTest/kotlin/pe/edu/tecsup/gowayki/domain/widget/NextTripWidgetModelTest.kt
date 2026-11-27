package pe.edu.tecsup.gowayki.domain.widget

import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextTripWidgetModelTest {
    @Test
    fun prefersRecentOverFavorite() {
        val fav = SavedPlace("Mall", -16.4, -71.5)
        val recent = RecentTrip(SavedPlace("Tecsup", -16.41, -71.54), fav)
        val model = nextTripFrom(listOf(fav), listOf(recent))!!
        assertEquals("Tecsup", model.originName)
        assertEquals("Mall", model.destinationName)
    }

    @Test
    fun emptyWithoutData() {
        assertNull(nextTripFrom(emptyList(), emptyList()))
    }
}
