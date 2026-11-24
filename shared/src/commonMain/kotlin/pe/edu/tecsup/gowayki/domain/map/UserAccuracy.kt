package pe.edu.tecsup.gowayki.domain.map

/** Circulo de precision del GPS del usuario. */
data class UserAccuracy(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
) {
    init {
        require(accuracyMeters >= 0.0) { "accuracy inválida" }
    }
}
