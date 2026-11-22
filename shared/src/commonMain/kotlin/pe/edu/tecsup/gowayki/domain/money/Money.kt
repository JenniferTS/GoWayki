package pe.edu.tecsup.gowayki.domain.money

import kotlin.math.roundToLong

/**
 * Dinero en unidades menores: 130 y "PEN" son S/ 1.30.
 * No hay almacenamiento en Double. La tarifa base es [BASE]; una tarifa de patrón la reemplaza.
 */
data class Money(val amountMinor: Long, val currency: String) {
    init {
        require(amountMinor >= 0L) { "Monto inválido." }
        require(currency.isNotEmpty()) { "Moneda inválida." }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Monedas distintas." }
        return Money(amountMinor + other.amountMinor, currency)
    }

    companion object {
        /** Tarifa base de la ciudad: S/ 1.30. */
        val BASE: Money = Money(130L, "PEN")

        val ZERO_PEN: Money = Money(0L, "PEN")

        /** [override] reemplaza [base] cuando el patrón trae tarifa propia. */
        fun resolve(override: Money? = null, base: Money = BASE): Money {
            if (override == null) return base
            require(override.currency == base.currency) { "La tarifa del patrón usa otra moneda." }
            return override
        }

        /**
         * El `default_fare` del paquete publica 1.0 PEN. Ese monto no se cobra:
         * sin fila en `line_fares` la tarifa es [BASE] (S/ 1.30).
         * Si la línea o el patrón traen monto, pasa a unidades menores con
         * `(amount * 100).roundToLong()`.
         */
        fun resolvePackFare(
            defaultFareAmount: Double,
            lineFareAmount: Double?,
            currency: String,
        ): Money {
            require(defaultFareAmount.finite()) { "Tarifa predeterminada inválida." }
            if (lineFareAmount == null) {
                require(currency == BASE.currency) { "La tarifa del patrón usa otra moneda." }
                return BASE
            }
            require(lineFareAmount.finite() && lineFareAmount >= 0.0) { "Monto inválido." }
            return resolve(Money((lineFareAmount * 100.0).roundToLong(), currency))
        }
    }
}

private fun Double.finite(): Boolean = !isNaN() && !isInfinite()
