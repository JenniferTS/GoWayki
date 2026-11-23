package pe.edu.tecsup.gowayki.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors

@Composable
actual fun CityMap(modifier: Modifier, overlay: MapOverlay) {
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(GowaykiColors.Sand)) {
        val segments = overlay.segments
        val pts = overlay.effectiveRoute + overlay.markers + overlay.stops +
            listOfNotNull(overlay.user, overlay.camera)
        if (pts.isEmpty()) return@Canvas
        val minLat = pts.minOf { it.latitude }
        val maxLat = pts.maxOf { it.latitude }
        val minLng = pts.minOf { it.longitude }
        val maxLng = pts.maxOf { it.longitude }
        fun map(p: pe.edu.tecsup.gowayki.domain.planner.GeoPoint): Offset {
            val x = ((p.longitude - minLng) / (maxLng - minLng + 1e-9) * (size.width - 40f) + 20f).toFloat()
            val y = ((maxLat - p.latitude) / (maxLat - minLat + 1e-9) * (size.height - 40f) + 20f).toFloat()
            return Offset(x, y)
        }
        if (segments.isNotEmpty()) {
            segments.forEach { seg ->
                val color = when (seg.kind) {
                    RouteKind.Walk -> Color(0xFF5C6B73)
                    RouteKind.Transit -> GowaykiColors.BrandOrange
                }
                val effect = if (seg.kind == RouteKind.Walk) PathEffect.dashPathEffect(floatArrayOf(12f, 10f)) else null
                for (i in 0 until seg.points.lastIndex) {
                    drawLine(
                        color = color,
                        start = map(seg.points[i]),
                        end = map(seg.points[i + 1]),
                        strokeWidth = if (seg.kind == RouteKind.Walk) 5f else 7f,
                        pathEffect = effect,
                    )
                }
            }
        } else {
            for (i in 0 until overlay.route.lastIndex) {
                drawLine(
                    color = GowaykiColors.Terracotta,
                    start = map(overlay.route[i]),
                    end = map(overlay.route[i + 1]),
                    strokeWidth = 6f,
                )
            }
        }
        overlay.user?.let { drawCircle(Color(0xFF1E88E5), radius = 10f, center = map(it)) }
    }
}
