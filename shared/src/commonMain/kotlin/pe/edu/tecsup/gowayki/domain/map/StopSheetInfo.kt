package pe.edu.tecsup.gowayki.domain.map

/** Datos para la ficha inferior de un paradero. */
data class StopSheetInfo(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lineNames: List<String>,
    val favorite: Boolean = false,
)
