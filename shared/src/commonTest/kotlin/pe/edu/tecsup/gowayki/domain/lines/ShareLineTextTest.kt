package pe.edu.tecsup.gowayki.domain.lines

import kotlin.test.Test
import kotlin.test.assertTrue

class ShareLineTextTest {
    @Test
    fun includesDeepLink() {
        val text = shareLineText(LineSummary("42", "Avelino", "Sur"))
        assertTrue(text.contains("/line/42"))
        assertTrue(text.contains("Avelino"))
    }
}
