package pe.edu.tecsup.gowayki.i18n

import kotlin.test.Test
import kotlin.test.assertTrue

class ExtraStringsTest {
    @Test
    fun spanishHasWidgetEmpty() {
        val s = extraStrings(es = true)
        assertTrue(s.widgetEmptyTrip.contains("favorito", ignoreCase = true))
    }
}
