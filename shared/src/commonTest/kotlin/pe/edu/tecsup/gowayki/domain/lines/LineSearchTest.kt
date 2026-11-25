package pe.edu.tecsup.gowayki.domain.lines

import kotlin.test.Test
import kotlin.test.assertEquals

class LineSearchTest {
    @Test
    fun filtersByAgencyAndCode() {
        val lines = listOf(
            LineSummary("1", "Avelino", "Consorcio Sur", "C-1"),
            LineSummary("2", "Dolores", "Consorcio Norte", "B2"),
        )
        assertEquals(listOf(lines[1]), filterLines("b2", lines))
        assertEquals(listOf(lines[0]), filterLines("sur", lines))
    }
}
