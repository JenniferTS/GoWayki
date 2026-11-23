package pe.edu.tecsup.gowayki.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint

data class MapOverlay(
    val route: List<GeoPoint> = emptyList(),
    /** Reportes / pins secundarios. */
    val markers: List<GeoPoint> = emptyList(),
    /** Paraderos (muestra limitada). */
    val stops: List<GeoPoint> = emptyList(),
    val user: GeoPoint? = null,
    val camera: GeoPoint? = null,
    val zoom: Double = 13.0,
)

@Composable
expect fun CityMap(
    modifier: Modifier = Modifier,
    overlay: MapOverlay = MapOverlay(),
)
