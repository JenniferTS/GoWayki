package pe.edu.tecsup.gowayki.domain.map

import pe.edu.tecsup.gowayki.domain.planner.GeoPoint

data class CameraEaseRequest(
    val points: List<GeoPoint>,
    val paddingDp: Int = 72,
    val durationMs: Int = 420,
) {
    init {
        require(points.isNotEmpty()) { "se necesita al menos un punto" }
        require(durationMs >= 0) { "duración inválida" }
    }
}
