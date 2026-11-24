package pe.edu.tecsup.gowayki.domain.map

import pe.edu.tecsup.gowayki.domain.community.ReportKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportLayerFilterTest {
    @Test
    fun toggleRemovesAndKeepsAtLeastOne() {
        var filter = ReportLayerFilter(setOf(ReportKind.HeavyTraffic, ReportKind.RoadBlocked))
        filter = filter.toggle(ReportKind.HeavyTraffic)
        assertFalse(filter.allows(ReportKind.HeavyTraffic))
        assertTrue(filter.allows(ReportKind.RoadBlocked))
    }
}
