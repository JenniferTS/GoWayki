package pe.edu.tecsup.gowayki.domain.preferences

/** Minutos de anticipacion para el aviso de bajada. */
data class AlertLeadMinutes(val value: Int = 3) {
    init {
        require(value in 1..15) { "lead fuera de rango 1..15" }
    }

    fun thresholdMeters(averageWalkSpeedMps: Double = 1.3): Double =
        value * 60 * averageWalkSpeedMps
}
