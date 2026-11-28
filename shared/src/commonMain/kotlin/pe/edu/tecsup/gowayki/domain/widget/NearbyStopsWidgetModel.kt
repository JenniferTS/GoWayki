package pe.edu.tecsup.gowayki.domain.widget

data class NearbyStopItem(
    val name: String,
    val distanceMeters: Int,
    val linesLabel: String,
)

data class NearbyStopsWidgetModel(
    val stops: List<NearbyStopItem>,
) {
    val isEmpty: Boolean get() = stops.isEmpty()
}
