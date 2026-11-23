package pe.edu.tecsup.gowayki.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.Itinerary
import pe.edu.tecsup.gowayki.domain.planner.TransitLeg
import pe.edu.tecsup.gowayki.domain.planner.WalkLeg

enum class RouteKind { Walk, Transit }

data class RouteSegment(
    val points: List<GeoPoint>,
    val kind: RouteKind,
    /** Hex #RRGGBB para tramos de micro. */
    val colorHex: String = "#F05709",
    val label: String? = null,
)

data class MapOverlay(
    val route: List<GeoPoint> = emptyList(),
    val segments: List<RouteSegment> = emptyList(),
    /** Reportes / pins secundarios. */
    val markers: List<GeoPoint> = emptyList(),
    /** Paraderos (muestra limitada). */
    val stops: List<GeoPoint> = emptyList(),
    val user: GeoPoint? = null,
    val camera: GeoPoint? = null,
    val zoom: Double = 13.0,
    /** Si >0 y hay [user], la cámara se centra en el usuario (FAB mi ubicación). */
    val followEpoch: Long = 0L,
) {
    val effectiveRoute: List<GeoPoint>
        get() = when {
            segments.isNotEmpty() -> segments.flatMap { it.points }
            else -> route
        }
}

/** Construye capas estilo Google Maps Transit: caminata punteada + micro con color de línea. */
fun itinerarySegments(trip: Itinerary?): List<RouteSegment> {
    if (trip == null) return emptyList()
    return trip.legs.mapNotNull { leg ->
        val raw = when {
            leg.geometry.size >= 2 -> leg.geometry
            else -> listOf(leg.from, leg.to)
        }
        when (leg) {
            is WalkLeg -> {
                val base = if (raw.size <= 2) densify(raw, steps = 8) else raw
                RouteSegment(
                    points = downsample(base, maxPoints = 64),
                    kind = RouteKind.Walk,
                    colorHex = "#5C6B73",
                    label = "Caminar",
                )
            }
            is TransitLeg -> RouteSegment(
                points = downsample(raw, maxPoints = 160),
                kind = RouteKind.Transit,
                colorHex = normalizeHex(leg.color),
                label = leg.lineName,
            )
        }
    }
}

fun itineraryStops(trip: Itinerary?): List<GeoPoint> {
    if (trip == null) return emptyList()
    return trip.legs.filterIsInstance<TransitLeg>().flatMap { leg ->
        (listOf(leg.boardingStop) + leg.intermediateStops.take(16) + listOf(leg.alightingStop))
            .map { GeoPoint(it.latitude, it.longitude) }
    }.distinctBy { "${"%.5f".format(it.latitude)},${"%.5f".format(it.longitude)}" }
        .take(80)
}

fun itineraryEndpoints(trip: Itinerary?): List<GeoPoint> {
    if (trip == null) return emptyList()
    val from = trip.legs.firstOrNull()?.from ?: return emptyList()
    val to = trip.legs.lastOrNull()?.to ?: return emptyList()
    return listOf(from, to)
}

private fun normalizeHex(raw: String): String {
    val t = raw.trim().removePrefix("#")
    return if (t.length == 6) "#$t" else "#F05709"
}

/** Interpolación lineal para que la caminata se vea como trazo continuo (sin red peatonal offline). */
private fun densify(points: List<GeoPoint>, steps: Int): List<GeoPoint> {
    if (points.size < 2 || steps <= 1) return points
    val out = ArrayList<GeoPoint>((points.size - 1) * steps + 1)
    for (i in 0 until points.lastIndex) {
        val a = points[i]
        val b = points[i + 1]
        for (s in 0 until steps) {
            val t = s.toDouble() / steps
            out.add(
                GeoPoint(
                    a.latitude + (b.latitude - a.latitude) * t,
                    a.longitude + (b.longitude - a.longitude) * t,
                ),
            )
        }
    }
    out.add(points.last())
    return out
}

/** Evita polilíneas enormes que tiran el mapa / OOM al planificar. */
private fun downsample(points: List<GeoPoint>, maxPoints: Int): List<GeoPoint> {
    if (points.size <= maxPoints || maxPoints < 3) return points
    val out = ArrayList<GeoPoint>(maxPoints)
    out.add(points.first())
    val step = (points.size - 1).toDouble() / (maxPoints - 1)
    for (i in 1 until maxPoints - 1) {
        val idx = (i * step).toInt().coerceIn(1, points.lastIndex - 1)
        out.add(points[idx])
    }
    out.add(points.last())
    return out
}

@Composable
expect fun CityMap(
    modifier: Modifier = Modifier,
    overlay: MapOverlay = MapOverlay(),
)
