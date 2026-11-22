package pe.edu.tecsup.gowayki.domain.money

import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test
    fun baseFareIsOneSolThirtyAndAPatternFareReplacesIt() {
        assertEquals(130L, Money.BASE.amountMinor)
        assertEquals("PEN", Money.BASE.currency)
        assertEquals(Money.BASE, Money.resolve(null))
        val override = Money(200L, "PEN")
        assertEquals(override, Money.resolve(override))
        assertEquals(Money(330L, "PEN"), Money.BASE + override)
        assertFailsWith<IllegalArgumentException> { Money.resolve(Money(200L, "USD")) }
        assertFailsWith<IllegalArgumentException> { Money(-1L, "PEN") }
    }

    @Test
    fun packDefaultOfOneSolIsIgnoredUnlessTheLineHasItsOwnFare() {
        assertEquals(Money.BASE, Money.resolvePackFare(defaultFareAmount = 1.0, lineFareAmount = null, currency = "PEN"))
        assertEquals(Money(100L, "PEN"), Money.resolvePackFare(1.0, 1.0, "PEN"))
        assertEquals(Money(250L, "PEN"), Money.resolvePackFare(1.0, 2.5, "PEN"))
        assertEquals(130L, (1.30 * 100.0).roundToLong())
        assertEquals(Money(130L, "PEN"), Money.resolvePackFare(1.0, 1.30, "PEN"))
        assertFailsWith<IllegalArgumentException> { Money.resolvePackFare(1.0, 2.0, "USD") }
        assertFailsWith<IllegalArgumentException> { Money.resolvePackFare(Double.NaN, null, "PEN") }
    }
}
